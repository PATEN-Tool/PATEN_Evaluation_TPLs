// 
// Decompiled by Procyon v0.5.36
// 

package org.neo4j.server.security.auth;

import java.util.concurrent.ThreadLocalRandom;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import org.neo4j.string.UTF8;
import org.neo4j.string.HexString;
import java.util.Arrays;

public class Credential
{
    public static final String DIGEST_ALGO = "SHA-256";
    public static final Credential INACCESSIBLE;
    private final byte[] salt;
    private final byte[] passwordHash;
    
    public static Credential forPassword(final String password) {
        final byte[] salt = randomSalt();
        return new Credential(salt, hash(salt, password));
    }
    
    public Credential(final byte[] salt, final byte[] passwordHash) {
        this.salt = salt;
        this.passwordHash = passwordHash;
    }
    
    public byte[] salt() {
        return this.salt;
    }
    
    public byte[] passwordHash() {
        return this.passwordHash;
    }
    
    public boolean matchesPassword(final String password) {
        return Arrays.equals(this.passwordHash, hash(this.salt, password));
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final Credential that = (Credential)o;
        return Arrays.equals(this.salt, that.salt) && Arrays.equals(this.passwordHash, that.passwordHash);
    }
    
    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(this.salt) + Arrays.hashCode(this.passwordHash);
    }
    
    @Override
    public String toString() {
        return "Credential{salt=0x" + HexString.encodeHexString(this.salt) + ", passwordHash=0x" + HexString.encodeHexString(this.passwordHash) + '}';
    }
    
    private static byte[] hash(final byte[] salt, final String password) {
        try {
            final byte[] passwordBytes = UTF8.encode(password);
            final MessageDigest m = MessageDigest.getInstance("SHA-256");
            m.update(salt, 0, salt.length);
            m.update(passwordBytes, 0, passwordBytes.length);
            return m.digest();
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm is not available on this platform: " + e.getMessage(), e);
        }
    }
    
    private static byte[] randomSalt() {
        final byte[] salt = new byte[16];
        ThreadLocalRandom.current().nextBytes(salt);
        return salt;
    }
    
    static {
        INACCESSIBLE = new Credential(new byte[0], new byte[0]);
    }
}
