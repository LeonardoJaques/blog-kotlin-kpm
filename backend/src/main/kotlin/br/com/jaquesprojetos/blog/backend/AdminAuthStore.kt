package br.com.jaquesprojetos.blog.backend

import java.time.Instant

data class OidcTransactionRecord(val nonceHash: ByteArray, val verifier: String)
data class StoredAdminSession(val subject: String, val groups: Set<String>)

interface AdminAuthStore {
    fun insertOidcTransaction(stateHash: ByteArray, nonceHash: ByteArray, verifier: String)
    fun consumeOidcTransaction(stateHash: ByteArray): OidcTransactionRecord?
    fun insertAdminSession(tokenHash: ByteArray, subject: String, groups: Set<String>, expiresAt: Instant)
    fun findAdminSession(tokenHash: ByteArray): StoredAdminSession?
    fun revokeAdminSession(tokenHash: ByteArray)
}
