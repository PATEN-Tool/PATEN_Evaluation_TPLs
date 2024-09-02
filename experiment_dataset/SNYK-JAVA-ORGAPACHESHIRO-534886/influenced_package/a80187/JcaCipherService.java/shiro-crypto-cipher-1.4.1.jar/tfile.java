// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.shiro.crypto;

import org.slf4j.LoggerFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.CipherInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.security.spec.AlgorithmParameterSpec;
import java.security.Key;
import javax.crypto.Cipher;
import org.apache.shiro.util.ByteSource;
import java.security.NoSuchAlgorithmException;
import org.apache.shiro.util.StringUtils;
import java.security.SecureRandom;
import org.slf4j.Logger;

public abstract class JcaCipherService implements CipherService
{
    private static final Logger log;
    private static final int DEFAULT_KEY_SIZE = 128;
    private static final int DEFAULT_STREAMING_BUFFER_SIZE = 512;
    private static final int BITS_PER_BYTE = 8;
    private static final String RANDOM_NUM_GENERATOR_ALGORITHM_NAME = "SHA1PRNG";
    private String algorithmName;
    private int keySize;
    private int streamingBufferSize;
    private boolean generateInitializationVectors;
    private int initializationVectorSize;
    private SecureRandom secureRandom;
    
    protected JcaCipherService(final String algorithmName) {
        if (!StringUtils.hasText(algorithmName)) {
            throw new IllegalArgumentException("algorithmName argument cannot be null or empty.");
        }
        this.algorithmName = algorithmName;
        this.keySize = 128;
        this.initializationVectorSize = 128;
        this.streamingBufferSize = 512;
        this.generateInitializationVectors = true;
    }
    
    public String getAlgorithmName() {
        return this.algorithmName;
    }
    
    public int getKeySize() {
        return this.keySize;
    }
    
    public void setKeySize(final int keySize) {
        this.keySize = keySize;
    }
    
    public boolean isGenerateInitializationVectors() {
        return this.generateInitializationVectors;
    }
    
    public void setGenerateInitializationVectors(final boolean generateInitializationVectors) {
        this.generateInitializationVectors = generateInitializationVectors;
    }
    
    public int getInitializationVectorSize() {
        return this.initializationVectorSize;
    }
    
    public void setInitializationVectorSize(final int initializationVectorSize) throws IllegalArgumentException {
        if (initializationVectorSize % 8 != 0) {
            final String msg = "Initialization vector sizes are specified in bits, but must be a multiple of 8 so they can be easily represented as a byte array.";
            throw new IllegalArgumentException(msg);
        }
        this.initializationVectorSize = initializationVectorSize;
    }
    
    protected boolean isGenerateInitializationVectors(final boolean streaming) {
        return this.isGenerateInitializationVectors();
    }
    
    public int getStreamingBufferSize() {
        return this.streamingBufferSize;
    }
    
    public void setStreamingBufferSize(final int streamingBufferSize) {
        this.streamingBufferSize = streamingBufferSize;
    }
    
    public SecureRandom getSecureRandom() {
        return this.secureRandom;
    }
    
    public void setSecureRandom(final SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }
    
    protected static SecureRandom getDefaultSecureRandom() {
        try {
            return SecureRandom.getInstance("SHA1PRNG");
        }
        catch (NoSuchAlgorithmException e) {
            JcaCipherService.log.debug("The SecureRandom SHA1PRNG algorithm is not available on the current platform.  Using the platform's default SecureRandom algorithm.", (Throwable)e);
            return new SecureRandom();
        }
    }
    
    protected SecureRandom ensureSecureRandom() {
        SecureRandom random = this.getSecureRandom();
        if (random == null) {
            random = getDefaultSecureRandom();
        }
        return random;
    }
    
    protected String getTransformationString(final boolean streaming) {
        return this.getAlgorithmName();
    }
    
    protected byte[] generateInitializationVector(final boolean streaming) {
        final int size = this.getInitializationVectorSize();
        if (size <= 0) {
            final String msg = "initializationVectorSize property must be greater than zero.  This number is typically set in the " + CipherService.class.getSimpleName() + " subclass constructor.  Also check your configuration to ensure that if you are setting a value, it is positive.";
            throw new IllegalStateException(msg);
        }
        if (size % 8 != 0) {
            final String msg = "initializationVectorSize property must be a multiple of 8 to represent as a byte array.";
            throw new IllegalStateException(msg);
        }
        final int sizeInBytes = size / 8;
        final byte[] ivBytes = new byte[sizeInBytes];
        final SecureRandom random = this.ensureSecureRandom();
        random.nextBytes(ivBytes);
        return ivBytes;
    }
    
    @Override
    public ByteSource encrypt(final byte[] plaintext, final byte[] key) {
        byte[] ivBytes = null;
        final boolean generate = this.isGenerateInitializationVectors(false);
        if (generate) {
            ivBytes = this.generateInitializationVector(false);
            if (ivBytes == null || ivBytes.length == 0) {
                throw new IllegalStateException("Initialization vector generation is enabled - generated vectorcannot be null or empty.");
            }
        }
        return this.encrypt(plaintext, key, ivBytes, generate);
    }
    
    private ByteSource encrypt(final byte[] plaintext, final byte[] key, final byte[] iv, final boolean prependIv) throws CryptoException {
        final int MODE = 1;
        byte[] output;
        if (prependIv && iv != null && iv.length > 0) {
            final byte[] encrypted = this.crypt(plaintext, key, iv, 1);
            output = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(encrypted, 0, output, iv.length, encrypted.length);
        }
        else {
            output = this.crypt(plaintext, key, iv, 1);
        }
        if (JcaCipherService.log.isTraceEnabled()) {
            JcaCipherService.log.trace("Incoming plaintext of size " + ((plaintext != null) ? plaintext.length : 0) + ".  Ciphertext byte array is size " + ((output != null) ? output.length : 0));
        }
        return ByteSource.Util.bytes(output);
    }
    
    @Override
    public ByteSource decrypt(final byte[] ciphertext, final byte[] key) throws CryptoException {
        byte[] encrypted = ciphertext;
        byte[] iv = null;
        if (this.isGenerateInitializationVectors(false)) {
            try {
                final int ivSize = this.getInitializationVectorSize();
                final int ivByteSize = ivSize / 8;
                iv = new byte[ivByteSize];
                System.arraycopy(ciphertext, 0, iv, 0, ivByteSize);
                final int encryptedSize = ciphertext.length - ivByteSize;
                encrypted = new byte[encryptedSize];
                System.arraycopy(ciphertext, ivByteSize, encrypted, 0, encryptedSize);
            }
            catch (Exception e) {
                final String msg = "Unable to correctly extract the Initialization Vector or ciphertext.";
                throw new CryptoException(msg, e);
            }
        }
        return this.decrypt(encrypted, key, iv);
    }
    
    private ByteSource decrypt(final byte[] ciphertext, final byte[] key, final byte[] iv) throws CryptoException {
        if (JcaCipherService.log.isTraceEnabled()) {
            JcaCipherService.log.trace("Attempting to decrypt incoming byte array of length " + ((ciphertext != null) ? ciphertext.length : 0));
        }
        final byte[] decrypted = this.crypt(ciphertext, key, iv, 2);
        return (decrypted == null) ? null : ByteSource.Util.bytes(decrypted);
    }
    
    private Cipher newCipherInstance(final boolean streaming) throws CryptoException {
        final String transformationString = this.getTransformationString(streaming);
        try {
            return Cipher.getInstance(transformationString);
        }
        catch (Exception e) {
            final String msg = "Unable to acquire a Java JCA Cipher instance using " + Cipher.class.getName() + ".getInstance( \"" + transformationString + "\" ). " + this.getAlgorithmName() + " under this configuration is required for the " + this.getClass().getName() + " instance to function.";
            throw new CryptoException(msg, e);
        }
    }
    
    private byte[] crypt(final byte[] bytes, final byte[] key, final byte[] iv, final int mode) throws IllegalArgumentException, CryptoException {
        if (key == null || key.length == 0) {
            throw new IllegalArgumentException("key argument cannot be null or empty.");
        }
        final Cipher cipher = this.initNewCipher(mode, key, iv, false);
        return this.crypt(cipher, bytes);
    }
    
    private byte[] crypt(final Cipher cipher, final byte[] bytes) throws CryptoException {
        try {
            return cipher.doFinal(bytes);
        }
        catch (Exception e) {
            final String msg = "Unable to execute 'doFinal' with cipher instance [" + cipher + "].";
            throw new CryptoException(msg, e);
        }
    }
    
    private void init(final Cipher cipher, final int mode, final Key key, final AlgorithmParameterSpec spec, final SecureRandom random) throws CryptoException {
        try {
            if (random != null) {
                if (spec != null) {
                    cipher.init(mode, key, spec, random);
                }
                else {
                    cipher.init(mode, key, random);
                }
            }
            else if (spec != null) {
                cipher.init(mode, key, spec);
            }
            else {
                cipher.init(mode, key);
            }
        }
        catch (Exception e) {
            final String msg = "Unable to init cipher instance.";
            throw new CryptoException(msg, e);
        }
    }
    
    @Override
    public void encrypt(final InputStream in, final OutputStream out, final byte[] key) throws CryptoException {
        byte[] iv = null;
        final boolean generate = this.isGenerateInitializationVectors(true);
        if (generate) {
            iv = this.generateInitializationVector(true);
            if (iv == null || iv.length == 0) {
                throw new IllegalStateException("Initialization vector generation is enabled - generated vectorcannot be null or empty.");
            }
        }
        this.encrypt(in, out, key, iv, generate);
    }
    
    private void encrypt(final InputStream in, final OutputStream out, final byte[] key, final byte[] iv, final boolean prependIv) throws CryptoException {
        if (prependIv && iv != null && iv.length > 0) {
            try {
                out.write(iv);
            }
            catch (IOException e) {
                throw new CryptoException(e);
            }
        }
        this.crypt(in, out, key, iv, 1);
    }
    
    @Override
    public void decrypt(final InputStream in, final OutputStream out, final byte[] key) throws CryptoException {
        this.decrypt(in, out, key, this.isGenerateInitializationVectors(true));
    }
    
    private void decrypt(final InputStream in, final OutputStream out, final byte[] key, final boolean ivPrepended) throws CryptoException {
        byte[] iv = null;
        if (ivPrepended) {
            final int ivSize = this.getInitializationVectorSize();
            final int ivByteSize = ivSize / 8;
            iv = new byte[ivByteSize];
            int read;
            try {
                read = in.read(iv);
            }
            catch (IOException e) {
                final String msg = "Unable to correctly read the Initialization Vector from the input stream.";
                throw new CryptoException(msg, e);
            }
            if (read != ivByteSize) {
                throw new CryptoException("Unable to read initialization vector bytes from the InputStream.  This is required when initialization vectors are autogenerated during an encryption operation.");
            }
        }
        this.decrypt(in, out, key, iv);
    }
    
    private void decrypt(final InputStream in, final OutputStream out, final byte[] decryptionKey, final byte[] iv) throws CryptoException {
        this.crypt(in, out, decryptionKey, iv, 2);
    }
    
    private void crypt(final InputStream in, final OutputStream out, final byte[] keyBytes, final byte[] iv, final int cryptMode) throws CryptoException {
        if (in == null) {
            throw new NullPointerException("InputStream argument cannot be null.");
        }
        if (out == null) {
            throw new NullPointerException("OutputStream argument cannot be null.");
        }
        final Cipher cipher = this.initNewCipher(cryptMode, keyBytes, iv, true);
        final CipherInputStream cis = new CipherInputStream(in, cipher);
        final int bufSize = this.getStreamingBufferSize();
        final byte[] buffer = new byte[bufSize];
        try {
            int bytesRead;
            while ((bytesRead = cis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        catch (IOException e) {
            throw new CryptoException(e);
        }
    }
    
    private Cipher initNewCipher(final int jcaCipherMode, final byte[] key, final byte[] iv, final boolean streaming) throws CryptoException {
        final Cipher cipher = this.newCipherInstance(streaming);
        final Key jdkKey = new SecretKeySpec(key, this.getAlgorithmName());
        IvParameterSpec ivSpec = null;
        if (iv != null && iv.length > 0) {
            ivSpec = new IvParameterSpec(iv);
        }
        this.init(cipher, jcaCipherMode, jdkKey, ivSpec, this.getSecureRandom());
        return cipher;
    }
    
    static {
        log = LoggerFactory.getLogger((Class)JcaCipherService.class);
    }
}
