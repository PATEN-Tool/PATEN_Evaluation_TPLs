// 
// Decompiled by Procyon v0.5.36
// 

package org.jasypt.digest;

import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import java.security.NoSuchProviderException;
import java.security.NoSuchAlgorithmException;
import org.jasypt.exceptions.EncryptionInitializationException;
import org.jasypt.salt.RandomSaltGenerator;
import org.jasypt.exceptions.AlreadyInitializedException;
import org.jasypt.commons.CommonUtils;
import java.security.MessageDigest;
import org.jasypt.digest.config.DigesterConfig;
import java.security.Provider;
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
    private String providerName;
    private Provider provider;
    private boolean invertPositionOfSaltInMessageBeforeDigesting;
    private boolean invertPositionOfPlainSaltInEncryptionResults;
    private boolean useLenientSaltSizeCheck;
    private DigesterConfig config;
    private boolean algorithmSet;
    private boolean saltSizeBytesSet;
    private boolean iterationsSet;
    private boolean saltGeneratorSet;
    private boolean providerNameSet;
    private boolean providerSet;
    private boolean invertPositionOfSaltInMessageBeforeDigestingSet;
    private boolean invertPositionOfPlainSaltInEncryptionResultsSet;
    private boolean useLenientSaltSizeCheckSet;
    private boolean initialized;
    private boolean useSalt;
    private MessageDigest md;
    private int digestLengthBytes;
    
    public StandardByteDigester() {
        this.algorithm = "MD5";
        this.saltSizeBytes = 8;
        this.iterations = 1000;
        this.saltGenerator = null;
        this.providerName = null;
        this.provider = null;
        this.invertPositionOfSaltInMessageBeforeDigesting = false;
        this.invertPositionOfPlainSaltInEncryptionResults = false;
        this.useLenientSaltSizeCheck = false;
        this.config = null;
        this.algorithmSet = false;
        this.saltSizeBytesSet = false;
        this.iterationsSet = false;
        this.saltGeneratorSet = false;
        this.providerNameSet = false;
        this.providerSet = false;
        this.invertPositionOfSaltInMessageBeforeDigestingSet = false;
        this.invertPositionOfPlainSaltInEncryptionResultsSet = false;
        this.useLenientSaltSizeCheckSet = false;
        this.initialized = false;
        this.useSalt = true;
        this.md = null;
        this.digestLengthBytes = 0;
    }
    
    public synchronized void setConfig(final DigesterConfig config) {
        CommonUtils.validateNotNull(config, "Config cannot be set null");
        if (this.isInitialized()) {
            throw new AlreadyInitializedException();
        }
        this.config = config;
    }
    
    public synchronized void setAlgorithm(final String algorithm) {
        CommonUtils.validateNotEmpty(algorithm, "Algorithm cannot be empty");
        if (this.isInitialized()) {
            throw new AlreadyInitializedException();
        }
        this.algorithm = algorithm;
        this.algorithmSet = true;
    }
    
    public synchronized void setSaltSizeBytes(final int saltSizeBytes) {
        CommonUtils.validateIsTrue(saltSizeBytes >= 0, "Salt size in bytes must be non-negative");
        if (this.isInitialized()) {
            throw new AlreadyInitializedException();
        }
        this.saltSizeBytes = saltSizeBytes;
        this.useSalt = (saltSizeBytes > 0);
        this.saltSizeBytesSet = true;
    }
    
    public synchronized void setIterations(final int iterations) {
        CommonUtils.validateIsTrue(iterations > 0, "Number of iterations must be greater than zero");
        if (this.isInitialized()) {
            throw new AlreadyInitializedException();
        }
        this.iterations = iterations;
        this.iterationsSet = true;
    }
    
    public synchronized void setSaltGenerator(final SaltGenerator saltGenerator) {
        CommonUtils.validateNotNull(saltGenerator, "Salt generator cannot be set null");
        if (this.isInitialized()) {
            throw new AlreadyInitializedException();
        }
        this.saltGenerator = saltGenerator;
        this.saltGeneratorSet = true;
    }
    
    public synchronized void setProviderName(final String providerName) {
        CommonUtils.validateNotNull(providerName, "Provider name cannot be set null");
        if (this.isInitialized()) {
            throw new AlreadyInitializedException();
        }
        this.providerName = providerName;
        this.providerNameSet = true;
    }
    
    public synchronized void setProvider(final Provider provider) {
        CommonUtils.validateNotNull(provider, "Provider cannot be set null");
        if (this.isInitialized()) {
            throw new AlreadyInitializedException();
        }
        this.provider = provider;
        this.providerSet = true;
    }
    
    public synchronized void setInvertPositionOfSaltInMessageBeforeDigesting(final boolean invertPositionOfSaltInMessageBeforeDigesting) {
        if (this.isInitialized()) {
            throw new AlreadyInitializedException();
        }
        this.invertPositionOfSaltInMessageBeforeDigesting = invertPositionOfSaltInMessageBeforeDigesting;
        this.invertPositionOfSaltInMessageBeforeDigestingSet = true;
    }
    
    public synchronized void setInvertPositionOfPlainSaltInEncryptionResults(final boolean invertPositionOfPlainSaltInEncryptionResults) {
        if (this.isInitialized()) {
            throw new AlreadyInitializedException();
        }
        this.invertPositionOfPlainSaltInEncryptionResults = invertPositionOfPlainSaltInEncryptionResults;
        this.invertPositionOfPlainSaltInEncryptionResultsSet = true;
    }
    
    public synchronized void setUseLenientSaltSizeCheck(final boolean useLenientSaltSizeCheck) {
        if (this.isInitialized()) {
            throw new AlreadyInitializedException();
        }
        this.useLenientSaltSizeCheck = useLenientSaltSizeCheck;
        this.useLenientSaltSizeCheckSet = true;
    }
    
    StandardByteDigester cloneDigester() {
        if (!this.isInitialized()) {
            this.initialize();
        }
        final StandardByteDigester cloned = new StandardByteDigester();
        if (CommonUtils.isNotEmpty(this.algorithm)) {
            cloned.setAlgorithm(this.algorithm);
        }
        cloned.setInvertPositionOfPlainSaltInEncryptionResults(this.invertPositionOfPlainSaltInEncryptionResults);
        cloned.setInvertPositionOfSaltInMessageBeforeDigesting(this.invertPositionOfSaltInMessageBeforeDigesting);
        cloned.setIterations(this.iterations);
        if (this.provider != null) {
            cloned.setProvider(this.provider);
        }
        if (this.providerName != null) {
            cloned.setProviderName(this.providerName);
        }
        if (this.saltGenerator != null) {
            cloned.setSaltGenerator(this.saltGenerator);
        }
        cloned.setSaltSizeBytes(this.saltSizeBytes);
        cloned.setUseLenientSaltSizeCheck(this.useLenientSaltSizeCheck);
        return cloned;
    }
    
    public boolean isInitialized() {
        return this.initialized;
    }
    
    public synchronized void initialize() {
        if (!this.initialized) {
            if (this.config != null) {
                final String configAlgorithm = this.config.getAlgorithm();
                if (configAlgorithm != null) {
                    CommonUtils.validateNotEmpty(configAlgorithm, "Algorithm cannot be empty");
                }
                final Integer configSaltSizeBytes = this.config.getSaltSizeBytes();
                if (configSaltSizeBytes != null) {
                    CommonUtils.validateIsTrue(configSaltSizeBytes >= 0, "Salt size in bytes must be non-negative");
                }
                final Integer configIterations = this.config.getIterations();
                if (configIterations != null) {
                    CommonUtils.validateIsTrue(configIterations > 0, "Number of iterations must be greater than zero");
                }
                final SaltGenerator configSaltGenerator = this.config.getSaltGenerator();
                final String configProviderName = this.config.getProviderName();
                if (configProviderName != null) {
                    CommonUtils.validateNotEmpty(configProviderName, "Provider name cannot be empty");
                }
                final Provider configProvider = this.config.getProvider();
                final Boolean configInvertPositionOfSaltInMessageBeforeDigesting = this.config.getInvertPositionOfSaltInMessageBeforeDigesting();
                final Boolean configInvertPositionOfPlainSaltInEncryptionResults = this.config.getInvertPositionOfPlainSaltInEncryptionResults();
                final Boolean configUseLenientSaltSizeCheck = this.config.getUseLenientSaltSizeCheck();
                this.algorithm = ((this.algorithmSet || configAlgorithm == null) ? this.algorithm : configAlgorithm);
                this.saltSizeBytes = ((this.saltSizeBytesSet || configSaltSizeBytes == null) ? this.saltSizeBytes : configSaltSizeBytes);
                this.iterations = ((this.iterationsSet || configIterations == null) ? this.iterations : configIterations);
                this.saltGenerator = ((this.saltGeneratorSet || configSaltGenerator == null) ? this.saltGenerator : configSaltGenerator);
                this.providerName = ((this.providerNameSet || configProviderName == null) ? this.providerName : configProviderName);
                this.provider = ((this.providerSet || configProvider == null) ? this.provider : configProvider);
                this.invertPositionOfSaltInMessageBeforeDigesting = ((this.invertPositionOfSaltInMessageBeforeDigestingSet || configInvertPositionOfSaltInMessageBeforeDigesting == null) ? this.invertPositionOfSaltInMessageBeforeDigesting : configInvertPositionOfSaltInMessageBeforeDigesting);
                this.invertPositionOfPlainSaltInEncryptionResults = ((this.invertPositionOfPlainSaltInEncryptionResultsSet || configInvertPositionOfPlainSaltInEncryptionResults == null) ? this.invertPositionOfPlainSaltInEncryptionResults : configInvertPositionOfPlainSaltInEncryptionResults);
                this.useLenientSaltSizeCheck = ((this.useLenientSaltSizeCheckSet || configUseLenientSaltSizeCheck == null) ? this.useLenientSaltSizeCheck : configUseLenientSaltSizeCheck);
            }
            if (this.saltGenerator == null) {
                this.saltGenerator = new RandomSaltGenerator();
            }
            if (this.useLenientSaltSizeCheck && !this.saltGenerator.includePlainSaltInEncryptionResults()) {
                throw new EncryptionInitializationException("The configured Salt Generator (" + this.saltGenerator.getClass().getName() + ") does not include plain salt in encryption results, which is not compatiblewith setting the salt size checking behaviour to \"lenient\".");
            }
            try {
                if (this.provider != null) {
                    this.md = MessageDigest.getInstance(this.algorithm, this.provider);
                }
                else if (this.providerName != null) {
                    this.md = MessageDigest.getInstance(this.algorithm, this.providerName);
                }
                else {
                    this.md = MessageDigest.getInstance(this.algorithm);
                }
            }
            catch (NoSuchAlgorithmException e) {
                throw new EncryptionInitializationException(e);
            }
            catch (NoSuchProviderException e2) {
                throw new EncryptionInitializationException(e2);
            }
            this.digestLengthBytes = this.md.getDigestLength();
            if (this.digestLengthBytes <= 0) {
                throw new EncryptionInitializationException("The configured algorithm (" + this.algorithm + ") or its provider do  not allow knowing the digest length beforehand (getDigestLength() operation), which is not compatiblewith setting the salt size checking behaviour to \"lenient\".");
            }
            this.initialized = true;
        }
    }
    
    @Override
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
            byte[] digest = null;
            synchronized (this.md) {
                this.md.reset();
                if (salt != null) {
                    if (!this.invertPositionOfSaltInMessageBeforeDigesting) {
                        this.md.update(salt);
                        this.md.update(message);
                    }
                    else {
                        this.md.update(message);
                        this.md.update(salt);
                    }
                }
                else {
                    this.md.update(message);
                }
                digest = this.md.digest();
                for (int i = 0; i < this.iterations - 1; ++i) {
                    this.md.reset();
                    digest = this.md.digest(digest);
                }
            }
            if (!this.saltGenerator.includePlainSaltInEncryptionResults() || salt == null) {
                return digest;
            }
            if (!this.invertPositionOfPlainSaltInEncryptionResults) {
                return CommonUtils.appendArrays(salt, digest);
            }
            return CommonUtils.appendArrays(digest, salt);
        }
        catch (Exception e) {
            throw new EncryptionOperationNotPossibleException();
        }
    }
    
    @Override
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
                    int digestSaltSize = this.saltSizeBytes;
                    if (this.digestLengthBytes > 0) {
                        if (this.useLenientSaltSizeCheck) {
                            if (digest.length < this.digestLengthBytes) {
                                throw new EncryptionOperationNotPossibleException();
                            }
                            digestSaltSize = digest.length - this.digestLengthBytes;
                        }
                        else if (digest.length != this.digestLengthBytes + this.saltSizeBytes) {
                            throw new EncryptionOperationNotPossibleException();
                        }
                    }
                    else if (digest.length < this.saltSizeBytes) {
                        throw new EncryptionOperationNotPossibleException();
                    }
                    if (!this.invertPositionOfPlainSaltInEncryptionResults) {
                        salt = new byte[digestSaltSize];
                        System.arraycopy(digest, 0, salt, 0, digestSaltSize);
                    }
                    else {
                        salt = new byte[digestSaltSize];
                        System.arraycopy(digest, digest.length - digestSaltSize, salt, 0, digestSaltSize);
                    }
                }
                else {
                    salt = this.saltGenerator.generateSalt(this.saltSizeBytes);
                }
            }
            final byte[] encryptedMessage = this.digest(message, salt);
            return digestsAreEqual(encryptedMessage, digest);
        }
        catch (Exception e) {
            throw new EncryptionOperationNotPossibleException();
        }
    }
    
    private static boolean digestsAreEqual(final byte[] a, final byte[] b) {
        if (a == null || b == null) {
            return false;
        }
        final int aLen = a.length;
        if (b.length != aLen) {
            return false;
        }
        int match = 0;
        for (int i = 0; i < aLen; ++i) {
            match |= (a[i] ^ b[i]);
        }
        return match == 0;
    }
}
