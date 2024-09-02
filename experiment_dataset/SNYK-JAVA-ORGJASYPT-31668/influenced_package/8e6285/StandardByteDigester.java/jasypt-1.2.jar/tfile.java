// 
// Decompiled by Procyon v0.5.36
// 

package org.jasypt.digest;

import java.util.Arrays;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.apache.commons.lang.ArrayUtils;
import java.security.NoSuchAlgorithmException;
import org.jasypt.exceptions.EncryptionInitializationException;
import org.jasypt.salt.RandomSaltGenerator;
import org.jasypt.exceptions.AlreadyInitializedException;
import org.apache.commons.lang.Validate;
import java.security.MessageDigest;
import org.jasypt.digest.config.DigesterConfig;
import org.jasypt.salt.SaltGenerator;

public final class StandardByteDigester implements ByteDigester
{
    public static final String DEFAULT_ALGORITHM = "MD5";
    public static final int DEFAULT_SALT_SIZE_BYTES = 8;
    public static final int DEFAULT_ITERATIONS = 1000;
    private String algorithm;
    private int saltSizeBytes;
    private int iterations;
    private SaltGenerator saltGenerator;
    private DigesterConfig config;
    private boolean algorithmSet;
    private boolean saltSizeBytesSet;
    private boolean iterationsSet;
    private boolean saltGeneratorSet;
    private boolean initialized;
    private boolean useSalt;
    private MessageDigest md;
    
    public StandardByteDigester() {
        this.algorithm = "MD5";
        this.saltSizeBytes = 8;
        this.iterations = 1000;
        this.saltGenerator = null;
        this.config = null;
        this.algorithmSet = false;
        this.saltSizeBytesSet = false;
        this.iterationsSet = false;
        this.saltGeneratorSet = false;
        this.initialized = false;
        this.useSalt = true;
        this.md = null;
    }
    
    public synchronized void setConfig(final DigesterConfig config) {
        Validate.notNull((Object)config, "Config cannot be set null");
        if (this.isInitialized()) {
            throw new AlreadyInitializedException();
        }
        this.config = config;
    }
    
    public synchronized void setAlgorithm(final String algorithm) {
        Validate.notEmpty(algorithm, "Algorithm cannot be empty");
        if (this.isInitialized()) {
            throw new AlreadyInitializedException();
        }
        this.algorithm = algorithm;
        this.algorithmSet = true;
    }
    
    public synchronized void setSaltSizeBytes(final int saltSizeBytes) {
        Validate.isTrue(saltSizeBytes >= 0, "Salt size in bytes must be non-negative");
        if (this.isInitialized()) {
            throw new AlreadyInitializedException();
        }
        this.saltSizeBytes = saltSizeBytes;
        this.useSalt = (saltSizeBytes > 0);
        this.saltSizeBytesSet = true;
    }
    
    public synchronized void setIterations(final int iterations) {
        Validate.isTrue(iterations > 0, "Number of iterations must be greater than zero");
        if (this.isInitialized()) {
            throw new AlreadyInitializedException();
        }
        this.iterations = iterations;
        this.iterationsSet = true;
    }
    
    public synchronized void setSaltGenerator(final SaltGenerator saltGenerator) {
        Validate.notNull((Object)saltGenerator, "Salt generator cannot be set null");
        if (this.isInitialized()) {
            throw new AlreadyInitializedException();
        }
        this.saltGenerator = saltGenerator;
        this.saltGeneratorSet = true;
    }
    
    public synchronized boolean isInitialized() {
        return this.initialized;
    }
    
    public synchronized void initialize() {
        if (!this.initialized) {
            if (this.config != null) {
                final String configAlgorithm = this.config.getAlgorithm();
                if (configAlgorithm != null) {
                    Validate.notEmpty(configAlgorithm, "Algorithm cannot be empty");
                }
                final Integer configSaltSizeBytes = this.config.getSaltSizeBytes();
                if (configSaltSizeBytes != null) {
                    Validate.isTrue(configSaltSizeBytes >= 0, "Salt size in bytes must be non-negative");
                }
                final Integer configIterations = this.config.getIterations();
                if (configIterations != null) {
                    Validate.isTrue(configIterations > 0, "Number of iterations must be greater than zero");
                }
                final SaltGenerator configSaltGenerator = this.config.getSaltGenerator();
                this.algorithm = ((this.algorithmSet || configAlgorithm == null) ? this.algorithm : configAlgorithm);
                this.saltSizeBytes = ((this.saltSizeBytesSet || configSaltSizeBytes == null) ? this.saltSizeBytes : configSaltSizeBytes);
                this.iterations = ((this.iterationsSet || configIterations == null) ? this.iterations : configIterations);
                this.saltGenerator = ((this.saltGeneratorSet || configSaltGenerator == null) ? this.saltGenerator : configSaltGenerator);
            }
            if (this.saltGenerator == null) {
                this.saltGenerator = new RandomSaltGenerator();
            }
            try {
                this.md = MessageDigest.getInstance(this.algorithm);
            }
            catch (NoSuchAlgorithmException e) {
                throw new EncryptionInitializationException(e);
            }
            this.initialized = true;
        }
    }
    
    public byte[] digest(final byte[] message) {
        if (message == null) {
            return null;
        }
        if (!this.isInitialized()) {
            this.initialize();
        }
        byte[] salt = null;
        if (this.useSalt) {
            salt = this.saltGenerator.generateSalt(this.saltSizeBytes);
        }
        return this.digest(message, salt);
    }
    
    private byte[] digest(final byte[] message, final byte[] salt) {
        try {
            byte[] encryptedMessage = new byte[0];
            if (salt != null) {
                encryptedMessage = ArrayUtils.addAll(encryptedMessage, salt);
            }
            byte[] digest = null;
            synchronized (this.md) {
                this.md.reset();
                if (salt != null) {
                    this.md.update(salt);
                }
                this.md.update(message);
                digest = this.md.digest();
                for (int i = 0; i < this.iterations - 1; ++i) {
                    this.md.reset();
                    digest = this.md.digest(digest);
                }
            }
            if (this.saltGenerator.includePlainSaltInEncryptionResults()) {
                encryptedMessage = ArrayUtils.addAll(encryptedMessage, digest);
            }
            else {
                encryptedMessage = digest;
            }
            return encryptedMessage;
        }
        catch (Exception e) {
            throw new EncryptionOperationNotPossibleException();
        }
    }
    
    public boolean matches(final byte[] message, final byte[] digest) {
        if (message == null) {
            return digest == null;
        }
        if (digest == null) {
            return false;
        }
        if (!this.isInitialized()) {
            this.initialize();
        }
        try {
            byte[] salt = null;
            if (this.useSalt) {
                if (this.saltGenerator.includePlainSaltInEncryptionResults()) {
                    salt = ArrayUtils.subarray(digest, 0, this.saltSizeBytes);
                }
                else {
                    salt = this.saltGenerator.generateSalt(this.saltSizeBytes);
                }
            }
            final byte[] encryptedMessage = this.digest(message, salt);
            return Arrays.equals(encryptedMessage, digest);
        }
        catch (Exception e) {
            throw new EncryptionOperationNotPossibleException();
        }
    }
}
