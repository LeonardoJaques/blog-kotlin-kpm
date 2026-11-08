package br.com.jaquesprojetos.blog.backend

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWKMatcher
import com.nimbusds.jose.jwk.JWKSelector
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.SignedJWT
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AutheliaOidc(
    private val database: Database,
    private val environment: Map<String, String> = System.getenv(),
) {
    private val issuer get() = environment["OIDC_ISSUER"]?.trimEnd('/')
    private val clientId get() = environment["OIDC_CLIENT_ID"]
    private val clientSecret get() = environment["OIDC_CLIENT_SECRET"]
    private val redirectUri get() = environment["OIDC_REDIRECT_URI"]
    val adminGroup get() = environment["OIDC_ADMIN_GROUP"] ?: "blog-admin"
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build()
    private val random = SecureRandom()

    fun configured(): Boolean =
        !clientId.isNullOrBlank() && !clientSecret.isNullOrBlank() &&
            issuer?.let(::isTrustedEndpoint) == true && redirectUri?.let(::isTrustedEndpoint) == true

    fun authorizationUrl(): String {
        check(configured()) { "OIDC is not configured" }
        val metadata = discovery()
        val state = randomValue()
        val nonce = randomValue()
        val verifier = randomValue() + randomValue()
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(sha256(verifier.toByteArray()))
        database.query { connection ->
            connection.prepareStatement(
                "INSERT INTO oidc_transactions(state_hash, nonce_hash, pkce_verifier, expires_at) VALUES (?, ?, ?, now() + interval '10 minutes')"
            ).use {
                it.setBytes(1, sha256(state.toByteArray()))
                it.setBytes(2, sha256(nonce.toByteArray()))
                it.setString(3, verifier)
                it.executeUpdate()
            }
            connection.prepareStatement("DELETE FROM oidc_transactions WHERE expires_at <= now()").use { it.executeUpdate() }
        }
        return metadata.authorizationEndpoint + "?" + listOf(
            "response_type" to "code",
            "scope" to "openid profile email groups",
            "client_id" to clientId!!,
            "redirect_uri" to redirectUri!!,
            "state" to state,
            "nonce" to nonce,
            "code_challenge" to challenge,
            "code_challenge_method" to "S256",
        ).joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
    }

    fun complete(state: String, code: String): AdminSession? {
        check(configured()) { "OIDC is not configured" }
        val transaction = database.query { connection ->
            connection.autoCommit = false
            try {
                val row = connection.prepareStatement(
                    "DELETE FROM oidc_transactions WHERE state_hash = ? AND expires_at > now() RETURNING nonce_hash, pkce_verifier"
                ).use {
                    it.setBytes(1, sha256(state.toByteArray()))
                    it.executeQuery().use { rows -> if (rows.next()) OidcTransaction(rows.getBytes(1), rows.getString(2)) else null }
                }
                connection.commit()
                row
            } catch (failure: Exception) {
                connection.rollback()
                throw failure
            } finally {
                connection.autoCommit = true
            }
        } ?: return null

        val metadata = discovery()
        val basic = Base64.getEncoder().encodeToString("${clientId!!}:${clientSecret!!}".toByteArray())
        val form = listOf(
            "grant_type" to "authorization_code",
            "code" to code,
            "redirect_uri" to redirectUri!!,
            "client_id" to clientId!!,
            "code_verifier" to transaction.verifier,
        ).joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
        val tokenJson = request(
            HttpRequest.newBuilder(URI(metadata.tokenEndpoint))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Basic $basic")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build()
        )
        val idToken = Json.parseToJsonElement(tokenJson).jsonObject["id_token"]?.jsonPrimitive?.content ?: return null
        val claims = verifyIdToken(idToken, metadata.jwksUri, transaction.nonceHash) ?: return null
        val subject = claims.subject ?: return null
        val groups = claimGroups(claims.getClaim("groups"))
        if (adminGroup !in groups) return null

        val expiresAt = minOf(claims.expirationTime?.toInstant() ?: Instant.now(), Instant.now().plus(Duration.ofHours(8)))
        if (!expiresAt.isAfter(Instant.now())) return null
        val sessionToken = randomValue()
        database.query { connection ->
            connection.prepareStatement("INSERT INTO admin_sessions(id_hash, subject, groups, expires_at) VALUES (?, ?, ?, ?)").use {
                it.setBytes(1, sha256(sessionToken.toByteArray()))
                it.setString(2, subject)
                it.setArray(3, connection.createArrayOf("text", groups.toTypedArray()))
                it.setObject(4, expiresAt)
                it.executeUpdate()
            }
        }
        return AdminSession(sessionToken, subject)
    }

    fun session(token: String?): AdminSession? {
        if (token.isNullOrBlank()) return null
        return database.query { connection ->
            connection.prepareStatement("SELECT subject, groups FROM admin_sessions WHERE id_hash = ? AND revoked_at IS NULL AND expires_at > now()").use {
                it.setBytes(1, sha256(token.toByteArray()))
                it.executeQuery().use { rows ->
                    if (!rows.next()) null
                    else {
                        val groups = (rows.getArray("groups").array as Array<*>).map { it.toString() }
                        if (adminGroup in groups) AdminSession(token, rows.getString("subject")) else null
                    }
                }
            }
        }
    }

    fun revoke(token: String?) {
        if (token.isNullOrBlank()) return
        database.query { connection ->
            connection.prepareStatement("UPDATE admin_sessions SET revoked_at = now() WHERE id_hash = ? AND revoked_at IS NULL").use {
                it.setBytes(1, sha256(token.toByteArray()))
                it.executeUpdate()
            }
        }
    }

    private fun discovery(): OidcMetadata {
        val response = request(HttpRequest.newBuilder(URI("${issuer}/.well-known/openid-configuration"))
            .timeout(Duration.ofSeconds(5)).GET().build())
        val json = Json.parseToJsonElement(response).jsonObject
        val discoveredIssuer = json.getValue("issuer").jsonPrimitive.content
        require(discoveredIssuer.trimEnd('/') == issuer) { "OIDC issuer mismatch" }
        val authorizationEndpoint = json.getValue("authorization_endpoint").jsonPrimitive.content
        val tokenEndpoint = json.getValue("token_endpoint").jsonPrimitive.content
        val jwksUri = json.getValue("jwks_uri").jsonPrimitive.content
        require(isTrustedEndpoint(authorizationEndpoint) && isTrustedEndpoint(tokenEndpoint) && isTrustedEndpoint(jwksUri))
        return OidcMetadata(
            authorizationEndpoint,
            tokenEndpoint,
            jwksUri,
        )
    }

    private fun verifyIdToken(rawToken: String, jwksUri: String, expectedNonceHash: ByteArray): com.nimbusds.jwt.JWTClaimsSet? = runCatching {
        val token = SignedJWT.parse(rawToken)
        require(token.header.algorithm == JWSAlgorithm.RS256 || token.header.algorithm == JWSAlgorithm.ES256)
        val jwkSetResponse = request(HttpRequest.newBuilder(URI(jwksUri)).timeout(Duration.ofSeconds(5)).GET().build())
        val jwkSet = JWKSet.parse(jwkSetResponse)
        val keyId = token.header.keyID ?: error("OIDC signing key id is missing")
        val key = JWKSelector(JWKMatcher.Builder().keyID(keyId).build()).select(jwkSet).first()
        val signatureValid = when {
            key is RSAKey && token.header.algorithm == JWSAlgorithm.RS256 -> token.verify(RSASSAVerifier(key.toRSAPublicKey()))
            key is ECKey && token.header.algorithm == JWSAlgorithm.ES256 -> token.verify(ECDSAVerifier(key.toECPublicKey()))
            else -> false
        }
        require(signatureValid)
        val claims = token.jwtClaimsSet
        require(claims.issuer?.trimEnd('/') == issuer)
        require(clientId!! in claims.audience)
        require(claims.audience.size == 1 || claims.getStringClaim("azp") == clientId)
        require(claims.expirationTime?.toInstant()?.isAfter(Instant.now()) == true)
        require(claims.notBeforeTime == null || !claims.notBeforeTime.toInstant().isAfter(Instant.now().plusSeconds(30)))
        require(claims.issueTime == null || !claims.issueTime.toInstant().isAfter(Instant.now().plusSeconds(30)))
        val nonce = claims.getStringClaim("nonce") ?: error("OIDC nonce is missing")
        require(MessageDigest.isEqual(expectedNonceHash, sha256(nonce.toByteArray())))
        claims
    }.getOrNull()

    private fun claimGroups(claim: Any?): Set<String> = when (claim) {
        is Collection<*> -> claim.filterIsInstance<String>().toSet()
        is String -> setOf(claim)
        else -> emptySet()
    }

    private fun request(request: HttpRequest): String {
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        require(response.statusCode() in 200..299) { "OIDC provider returned ${response.statusCode()}" }
        return response.body()
    }

    private fun randomValue(): String = ByteArray(32).also(random::nextBytes).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8)
    private fun sha256(value: ByteArray) = MessageDigest.getInstance("SHA-256").digest(value)
    private fun isTrustedEndpoint(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.isAbsolute && uri.host != null && (uri.scheme == "https" ||
            (uri.scheme == "http" && uri.host in setOf("localhost", "127.0.0.1", "::1")))
    }.getOrDefault(false)

    data class AdminSession(val token: String, val subject: String)
    private data class OidcTransaction(val nonceHash: ByteArray, val verifier: String)
    private data class OidcMetadata(val authorizationEndpoint: String, val tokenEndpoint: String, val jwksUri: String)
}
