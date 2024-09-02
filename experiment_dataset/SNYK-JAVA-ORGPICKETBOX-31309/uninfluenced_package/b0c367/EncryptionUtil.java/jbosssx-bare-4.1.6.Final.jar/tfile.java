// 
// Decompiled by Procyon v0.5.36
// 

package org.picketbox.util;

import java.security.KeyPair;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.PublicKey;
import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class EncryptionUtil
{
    private String encryptionAlgorithm;
    private int keySize;
    
    public EncryptionUtil(final String encryptionAlgorithm, final int keySize) {
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.keySize = keySize;
    }
    
    public SecretKey generateKey() throws NoSuchAlgorithmException {
        final KeyGenerator kgen = KeyGenerator.getInstance(this.encryptionAlgorithm);
        kgen.init(this.keySize);
        final SecretKey key = kgen.generateKey();
        return key;
    }
    
    public byte[] encrypt(final byte[] data, final PublicKey publicKey, final SecretKey key) throws Exception {
        final KeyGenerator kgen = KeyGenerator.getInstance(this.encryptionAlgorithm);
        kgen.init(this.keySize);
        final byte[] publicKeyEncoded = publicKey.getEncoded();
        final SecretKeySpec skeySpec = new SecretKeySpec(key.getEncoded(), this.encryptionAlgorithm);
        final Cipher cipher = Cipher.getInstance(this.encryptionAlgorithm);
        cipher.init(1, skeySpec);
        final byte[] encrypted = cipher.doFinal(data);
        return encrypted;
    }
    
    public byte[] decrypt(final byte[] encryptedData, final KeyPair keypair, final SecretKeySpec keySpec) throws Exception {
        final KeyGenerator kgen = KeyGenerator.getInstance(this.encryptionAlgorithm);
        kgen.init(this.keySize);
        final byte[] publicKeyEncoded = keypair.getPrivate().getEncoded();
        final Cipher cipher = Cipher.getInstance(this.encryptionAlgorithm);
        cipher.init(2, keySpec);
        final byte[] original = cipher.doFinal(encryptedData);
        return original;
    }
    
    public byte[] decrypt(final byte[] encryptedData, final KeyPair keypair, final SecretKey key) throws Exception {
        final KeyGenerator kgen = KeyGenerator.getInstance(this.encryptionAlgorithm);
        kgen.init(this.keySize);
        final byte[] publicKeyEncoded = keypair.getPrivate().getEncoded();
        final SecretKeySpec skeySpec = new SecretKeySpec(key.getEncoded(), this.encryptionAlgorithm);
        final Cipher cipher = Cipher.getInstance(this.encryptionAlgorithm);
        cipher.init(2, skeySpec);
        final byte[] original = cipher.doFinal(encryptedData);
        return original;
    }
    
    public byte[] encrypt(final byte[] data, final SecretKey key) throws Exception {
        final SecretKeySpec skeySpec = new SecretKeySpec(key.getEncoded(), this.encryptionAlgorithm);
        final Cipher cipher = Cipher.getInstance(this.encryptionAlgorithm);
        cipher.init(1, skeySpec);
        final byte[] encrypted = cipher.doFinal(data);
        return encrypted;
    }
    
    public byte[] decrypt(final byte[] encryptedData, final SecretKeySpec keySpec) throws Exception {
        final Cipher cipher = Cipher.getInstance(this.encryptionAlgorithm);
        cipher.init(2, keySpec);
        final byte[] original = cipher.doFinal(encryptedData);
        return original;
    }
}
