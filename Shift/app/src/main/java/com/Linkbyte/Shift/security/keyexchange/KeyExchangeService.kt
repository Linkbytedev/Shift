package com.Linkbyte.Shift.security.keyexchange

import android.util.Base64
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements Diffie-Hellman key exchange for establishing shared secrets.
 * 
 * This allows two users to derive the same encryption key without
 * transmitting it over Firebase.
 * 
 * Process:
 * 1. Each user generates a private/public key pair
 * 2. Public keys are exchanged via Firebase
 * 3. Each user computes the shared secret using their private key + other's public key
 * 4. Shared secret is hashed to produce AES-256 key
 */
@Singleton
class KeyExchangeService @Inject constructor() {
    
    companion object {
        // Using a 2048-bit safe prime (RFC 3526 Group 14)
        private val PRIME = BigInteger(
            "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
            "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
            "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
            "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
            "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
            "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
            "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
            "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B" +
            "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9" +
            "DE2BCBF6955817183995497CEA956AE515D2261898FA0510" +
            "15728E5A8AACAA68FFFFFFFFFFFFFFFF", 16
        )
        
        private val GENERATOR = BigInteger.valueOf(2)
        private const val KEY_SIZE = 256
    }
    
    /**
     * Generate a new key pair for a user
     */
    fun generateKeyPair(): DHKeyPair {
        val random = SecureRandom()
        val privateKey = BigInteger(2048, random).mod(PRIME)
        val publicKey = GENERATOR.modPow(privateKey, PRIME)
        
        return DHKeyPair(
            privateKey = privateKey.toString(16),
            publicKey = publicKey.toString(16)
        )
    }
    
    /**
     * Compute shared secret from own private key and other party's public key
     */
    fun computeSharedSecret(ownPrivateKey: String, otherPublicKey: String): SecretKey {
        val privateKeyBigInt = BigInteger(ownPrivateKey, 16)
        val otherPublicKeyBigInt = BigInteger(otherPublicKey, 16)
        
        // Compute shared secret: s = (other_public_key ^ own_private_key) mod p
        val sharedSecret = otherPublicKeyBigInt.modPow(privateKeyBigInt, PRIME)
        
        // Hash the shared secret to derive AES-256 key
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(sharedSecret.toByteArray())
        
        return SecretKeySpec(keyBytes, "AES")
    }
    
    /**
     * Generate conversation key for a new conversation.
     * 
     * This is called when initiating a conversation to generate keys
     * and exchange public keys via Firebase.
     */
    fun initiateKeyExchange(): KeyExchangeData {
        val keyPair = generateKeyPair()
        return KeyExchangeData(
            privateKey = keyPair.privateKey,
            publicKey = keyPair.publicKey
        )
    }
}

data class DHKeyPair(
    val privateKey: String,
    val publicKey: String
)

data class KeyExchangeData(
    val privateKey: String,
    val publicKey: String
)
