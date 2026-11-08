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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AutheliaOidc(
    private val store: AdminAuthStore,
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
        store.insertOidcTransaction(sha256(state.toByteArray()), sha256(nonce.toByteArray()), verifier)
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
        val transaction = try {
            store.consumeOidcTransaction(sha256(state.toByteArray()))
        } catch (_: Exception) {
            throw OidcCallbackFailure("state_store")
        } ?: throw OidcCallbackFailure("state")

        val metadata = try {
            discovery()
        } catch (_: Exception) {
            throw OidcCallbackFailure("discovery")
        }
        val basic = basicAuthCredentials(clientId!!, clientSecret!!)
        val form = authorizationCodeTokenForm(code, redirectUri!!, transaction.verifier)
        val tokenJson = try {
            request(
                HttpRequest.newBuilder(URI(metadata.tokenEndpoint))
                    .timeout(Duration.ofSeconds(5))
                    .header("Authorization", "Basic $basic")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build()
            )
        } catch (_: Exception) {
            throw OidcCallbackFailure("token_exchange")
        }
        val tokenResponse = try {
            Json.parseToJsonElement(tokenJson).jsonObject
        } catch (_: Exception) {
            throw OidcCallbackFailure("token_response_parse")
        }
        val idToken = tokenResponse.stringValue("id_token") ?: throw OidcCallbackFailure("id_token_missing")
        val accessToken = tokenResponse.stringValue("access_token") ?: throw OidcCallbackFailure("access_token_missing")
        val claims = verifyIdToken(idToken, metadata.jwksUri, transaction.nonceHash)
            ?: throw OidcCallbackFailure("id_token_verification")
        val subject = claims.subject ?: throw OidcCallbackFailure("subject_missing")
        val userInfoBody = try {
            request(userInfoRequest(metadata.userInfoEndpoint, accessToken))
        } catch (_: Exception) {
            throw OidcCallbackFailure("userinfo")
        }
        val userInfo = parseUserInfoClaims(userInfoBody, subject)
        val groups = userInfo.groups
        requireAdminGroup(groups, adminGroup)

        val expiresAt = minOf(claims.expirationTime?.toInstant() ?: Instant.now(), Instant.now().plus(Duration.ofHours(8)))
        if (!expiresAt.isAfter(Instant.now())) throw OidcCallbackFailure("token_expired")
        val sessionToken = randomValue()
        try {
            store.insertAdminSession(sha256(sessionToken.toByteArray()), subject, groups, expiresAt)
        } catch (_: Exception) {
            throw OidcCallbackFailure("session_store")
        }
        return AdminSession(sessionToken, subject)
    }

    fun session(token: String?): AdminSession? {
        if (token.isNullOrBlank()) return null
        val stored = store.findAdminSession(sha256(token.toByteArray())) ?: return null
        return if (adminGroup in stored.groups) AdminSession(token, stored.subject) else null
    }

    fun revoke(token: String?) {
        if (token.isNullOrBlank()) return
        store.revokeAdminSession(sha256(token.toByteArray()))
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
        val userInfoEndpoint = json.getValue("userinfo_endpoint").jsonPrimitive.content
        require(isTrustedEndpoint(authorizationEndpoint) && isTrustedEndpoint(tokenEndpoint) &&
            isTrustedEndpoint(jwksUri) && isTrustedEndpoint(userInfoEndpoint))
        return OidcMetadata(
            authorizationEndpoint,
            tokenEndpoint,
            jwksUri,
            userInfoEndpoint,
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
    private data class OidcMetadata(
        val authorizationEndpoint: String,
        val tokenEndpoint: String,
        val jwksUri: String,
        val userInfoEndpoint: String,
    )
}

private fun kotlinx.serialization.json.JsonObject.stringValue(name: String): String? =
    (this[name] as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content

internal data class OidcUserInfo(val subject: String, val groups: Set<String>)

internal fun requireAdminGroup(groups: Set<String>, adminGroup: String) {
    if (adminGroup !in groups) throw OidcCallbackFailure("admin_group")
}

internal fun userInfoRequest(endpoint: String, accessToken: String): HttpRequest =
    HttpRequest.newBuilder(URI(endpoint))
        .timeout(Duration.ofSeconds(5))
        .header("Authorization", "Bearer $accessToken")
        .header("Accept", "application/json")
        .GET()
        .build()

internal fun parseUserInfoClaims(body: String, expectedSubject: String): OidcUserInfo {
    val json = try {
        Json.parseToJsonElement(body).jsonObject
    } catch (_: Exception) {
        throw OidcCallbackFailure("userinfo_parse")
    }
    val subject = json["sub"]?.jsonPrimitive?.contentOrNull
        ?: throw OidcCallbackFailure("userinfo_subject_missing")
    if (subject != expectedSubject) throw OidcCallbackFailure("userinfo_subject_mismatch")
    val groups = when (val claim = json["groups"]) {
        is JsonArray -> claim.mapNotNull { (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content }.toSet()
        is JsonPrimitive -> if (claim.isString) setOf(claim.content) else emptySet()
        else -> emptySet()
    }
    return OidcUserInfo(subject, groups)
}

internal fun basicAuthCredentials(clientId: String, clientSecret: String): String =
    Base64.getEncoder().encodeToString(
        "${URLEncoder.encode(clientId, StandardCharsets.UTF_8)}:${URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)}"
            .toByteArray(StandardCharsets.UTF_8)
    )

internal fun authorizationCodeTokenForm(code: String, redirectUri: String, verifier: String): String =
    listOf(
        "grant_type" to "authorization_code",
        "code" to code,
        "redirect_uri" to redirectUri,
        "code_verifier" to verifier,
    ).joinToString("&") { (key, value) ->
        "${URLEncoder.encode(key, StandardCharsets.UTF_8)}=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
    }

internal class OidcCallbackFailure(val stage: String) : RuntimeException(stage)
