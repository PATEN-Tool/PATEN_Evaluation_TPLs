// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.nifi.security.util.crypto;

import java.util.Collections;
import java.util.HashMap;
import java.nio.charset.StandardCharsets;
import org.apache.nifi.security.util.KeyDerivationFunction;
import org.apache.nifi.stream.io.ByteCountingOutputStream;
import org.apache.nifi.stream.io.ByteCountingInputStream;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.security.GeneralSecurityException;
import javax.crypto.SecretKey;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import javax.crypto.NoSuchPaddingException;
import java.security.spec.InvalidKeySpecException;
import java.security.NoSuchProviderException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.Key;
import javax.crypto.spec.PBEParameterSpec;
import java.security.spec.KeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import org.apache.nifi.security.util.EncryptionMethod;
import org.apache.commons.codec.binary.Base64;
import java.io.IOException;
import org.apache.nifi.stream.io.StreamUtils;
import java.io.ByteArrayOutputStream;
import org.apache.nifi.processor.exception.ProcessException;
import java.io.OutputStream;
import java.io.InputStream;
import javax.crypto.Cipher;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import java.util.Map;
import java.util.regex.Pattern;

public class CipherUtility
{
    public static final int BUFFER_SIZE = 65536;
    private static final Pattern KEY_LENGTH_PATTERN;
    private static final Map<String, Integer> MAX_PASSWORD_LENGTH_BY_ALGORITHM;
    private static final int DEFAULT_MAX_ALLOWED_KEY_LENGTH = 128;
    
    public static String parseCipherFromAlgorithm(final String algorithm) {
        if (StringUtils.isEmpty((CharSequence)algorithm)) {
            return algorithm;
        }
        final String formattedAlgorithm = algorithm.toUpperCase();
        final String AES = "AES";
        final String TDES = "TRIPLEDES";
        final String TDES_ALTERNATE = "DESEDE";
        final String DES = "DES";
        final String RC4 = "RC4";
        final String RC5 = "RC2";
        final String TWOFISH = "TWOFISH";
        final List<String> SYMMETRIC_CIPHERS = Arrays.asList("AES", "TRIPLEDES", "DESEDE", "DES", "RC4", "RC2", "TWOFISH");
        final String ACTUAL_TDES_CIPHER = "DESede";
        for (final String cipher : SYMMETRIC_CIPHERS) {
            if (formattedAlgorithm.contains(cipher)) {
                if (cipher.equals("TRIPLEDES") || cipher.equals("DESEDE")) {
                    return "DESede";
                }
                return cipher;
            }
        }
        return algorithm;
    }
    
    public static int parseKeyLengthFromAlgorithm(final String algorithm) {
        final int keyLength = parseActualKeyLengthFromAlgorithm(algorithm);
        if (keyLength != -1) {
            return keyLength;
        }
        final String cipher = parseCipherFromAlgorithm(algorithm);
        return getDefaultKeyLengthForCipher(cipher);
    }
    
    private static int parseActualKeyLengthFromAlgorithm(final String algorithm) {
        final Matcher matcher = CipherUtility.KEY_LENGTH_PATTERN.matcher(algorithm);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return -1;
    }
    
    public static boolean isValidKeyLength(final int keyLength, final String cipher) {
        return !StringUtils.isEmpty((CharSequence)cipher) && getValidKeyLengthsForAlgorithm(cipher).contains(keyLength);
    }
    
    public static boolean isValidKeyLengthForAlgorithm(final int keyLength, final String algorithm) {
        return !StringUtils.isEmpty((CharSequence)algorithm) && getValidKeyLengthsForAlgorithm(algorithm).contains(keyLength);
    }
    
    public static List<Integer> getValidKeyLengthsForAlgorithm(final String algorithm) {
        final List<Integer> validKeyLengths = new ArrayList<Integer>();
        if (StringUtils.isEmpty((CharSequence)algorithm)) {
            return validKeyLengths;
        }
        final int keyLength = parseActualKeyLengthFromAlgorithm(algorithm);
        if (keyLength != -1) {
            validKeyLengths.add(keyLength);
            return validKeyLengths;
        }
        final String cipher = parseCipherFromAlgorithm(algorithm);
        final String upperCase = cipher.toUpperCase();
        switch (upperCase) {
            case "DESEDE": {
                return Arrays.asList(56, 64, 112, 128, 168, 192);
            }
            case "DES": {
                return Arrays.asList(56, 64);
            }
            case "RC2":
            case "RC4":
            case "RC5": {
                for (int i = 40; i <= 2048; ++i) {
                    validKeyLengths.add(i);
                }
                return validKeyLengths;
            }
            case "AES":
            case "TWOFISH": {
                return Arrays.asList(128, 192, 256);
            }
            default: {
                return validKeyLengths;
            }
        }
    }
    
    private static int getDefaultKeyLengthForCipher(String cipher) {
        if (StringUtils.isEmpty((CharSequence)cipher)) {
            return -1;
        }
        final String upperCase;
        cipher = (upperCase = cipher.toUpperCase());
        switch (upperCase) {
            case "DESEDE": {
                return 112;
            }
            case "DES": {
                return 64;
            }
            default: {
                return 128;
            }
        }
    }
    
    public static void processStreams(final Cipher cipher, final InputStream in, final OutputStream out) {
        try {
            final byte[] buffer = new byte[65536];
            int len;
            while ((len = in.read(buffer)) > 0) {
                final byte[] transformedBytes = cipher.update(buffer, 0, len);
                if (transformedBytes != null) {
                    out.write(transformedBytes);
                }
            }
            out.write(cipher.doFinal());
        }
        catch (Exception e) {
            throw new ProcessException((Throwable)e);
        }
    }
    
    public static byte[] readBytesFromInputStream(final InputStream in, final String label, final int limit, final byte[] delimiter) throws IOException, ProcessException {
        if (in == null) {
            throw new IllegalArgumentException("Cannot read " + label + " from null InputStream");
        }
        in.mark(limit);
        final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        final byte[] stoppedBy = StreamUtils.copyExclusive(in, (OutputStream)bytesOut, limit + delimiter.length, new byte[][] { delimiter });
        if (stoppedBy != null) {
            final byte[] bytes = bytesOut.toByteArray();
            return bytes;
        }
        in.reset();
        return null;
    }
    
    public static void writeBytesToOutputStream(final OutputStream out, final byte[] value, final String label, final byte[] delimiter) throws IOException {
        if (out == null) {
            throw new IllegalArgumentException("Cannot write " + label + " to null OutputStream");
        }
        out.write(value);
        out.write(delimiter);
    }
    
    public static String encodeBase64NoPadding(final byte[] bytes) {
        String base64UrlNoPadding = Base64.encodeBase64URLSafeString(bytes);
        base64UrlNoPadding = base64UrlNoPadding.replaceAll("-", "+");
        base64UrlNoPadding = base64UrlNoPadding.replaceAll("_", "/");
        return base64UrlNoPadding;
    }
    
    public static boolean passwordLengthIsValidForAlgorithmOnLimitedStrengthCrypto(final int passwordLength, final EncryptionMethod encryptionMethod) {
        if (encryptionMethod == null) {
            throw new IllegalArgumentException("Cannot evaluate an empty encryption method algorithm");
        }
        return passwordLength <= getMaximumPasswordLengthForAlgorithmOnLimitedStrengthCrypto(encryptionMethod);
    }
    
    public static int getMaximumPasswordLengthForAlgorithmOnLimitedStrengthCrypto(final EncryptionMethod encryptionMethod) {
        if (encryptionMethod == null) {
            throw new IllegalArgumentException("Cannot evaluate an empty encryption method algorithm");
        }
        return CipherUtility.MAX_PASSWORD_LENGTH_BY_ALGORITHM.getOrDefault(encryptionMethod.getAlgorithm(), -1);
    }
    
    public static boolean isUnlimitedStrengthCryptoSupported() {
        try {
            return Cipher.getMaxAllowedKeyLength("AES") > 128;
        }
        catch (NoSuchAlgorithmException e) {
            return false;
        }
    }
    
    public static boolean isPBECipher(final String algorithm) {
        final EncryptionMethod em = EncryptionMethod.forAlgorithm(algorithm);
        return em != null && em.isPBECipher();
    }
    
    public static boolean isKeyedCipher(final String algorithm) {
        final EncryptionMethod em = EncryptionMethod.forAlgorithm(algorithm);
        return em != null && em.isKeyedCipher();
    }
    
    public static Cipher initPBECipher(final String algorithm, final String provider, final String password, final byte[] salt, final int iterationCount, final boolean encryptMode) throws IllegalArgumentException {
        try {
            final PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());
            final SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm, provider);
            final SecretKey tempKey = factory.generateSecret(pbeKeySpec);
            final PBEParameterSpec parameterSpec = new PBEParameterSpec(salt, iterationCount);
            final Cipher cipher = Cipher.getInstance(algorithm, provider);
            cipher.init(encryptMode ? 1 : 2, tempKey, parameterSpec);
            return cipher;
        }
        catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException ex2) {
            final GeneralSecurityException ex;
            final GeneralSecurityException e = ex;
            throw new IllegalArgumentException("One or more parameters to initialize the PBE cipher were invalid", e);
        }
    }
    
    public static int getIterationCountForAlgorithm(final String algorithm) {
        int iterationCount = 0;
        if (algorithm.matches("DES|RC|SHAA|SHA256")) {
            iterationCount = 1000;
        }
        return iterationCount;
    }
    
    public static int getSaltLengthForAlgorithm(final String algorithm) {
        int saltLength = 16;
        if (algorithm.contains("DES") || algorithm.contains("RC")) {
            saltLength = 8;
        }
        return saltLength;
    }
    
    public static String getLoggableRepresentationOfSensitiveValue(final String sensitivePropertyValue) {
        final SecureHasher secureHasher = new Argon2SecureHasher();
        return getLoggableRepresentationOfSensitiveValue(sensitivePropertyValue, secureHasher);
    }
    
    public static String getLoggableRepresentationOfSensitiveValue(final String sensitivePropertyValue, final SecureHasher secureHasher) {
        return "[MASKED] (" + secureHasher.hashBase64(sensitivePropertyValue) + ")";
    }
    
    public static String getTimestampString() {
        final Locale currentLocale = Locale.getDefault();
        final String pattern = "yyyy-MM-dd HH:mm:ss.SSS Z";
        final SimpleDateFormat formatter = new SimpleDateFormat(pattern, currentLocale);
        final Date now = new Date();
        return formatter.format(now);
    }
    
    public static ByteCountingInputStream wrapStreamForCounting(final InputStream inputStream) {
        ByteCountingInputStream bcis;
        if (!(inputStream instanceof ByteCountingInputStream)) {
            bcis = new ByteCountingInputStream(inputStream);
        }
        else {
            bcis = (ByteCountingInputStream)inputStream;
        }
        return bcis;
    }
    
    public static ByteCountingOutputStream wrapStreamForCounting(final OutputStream outputStream) {
        ByteCountingOutputStream bcos;
        if (!(outputStream instanceof ByteCountingOutputStream)) {
            bcos = new ByteCountingOutputStream(outputStream);
        }
        else {
            bcos = (ByteCountingOutputStream)outputStream;
        }
        return bcos;
    }
    
    public static int calculateCipherTextLength(final int ptLength, final int saltLength) {
        final int ctBlocks = Math.ceil(ptLength / 16.0).intValue();
        final int ctLength = ((ptLength % 16 == 0) ? (ctBlocks + 1) : ctBlocks) * 16;
        return ctLength + saltLength + 16 + ((saltLength > 0) ? 8 : 0) + 6;
    }
    
    public static int findSequence(final byte[] haystack, final byte[] needle) {
        for (int i = 0; i < haystack.length - needle.length; ++i) {
            boolean match = true;
            for (int j = 0; j < needle.length; ++j) {
                if (haystack[i + j] != needle[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }
    
    public static byte[] extractRawSalt(final byte[] fullSalt, final KeyDerivationFunction kdf) {
        final String saltString = new String(fullSalt, StandardCharsets.UTF_8);
        switch (kdf) {
            case ARGON2: {
                return Argon2CipherProvider.isArgon2FormattedSalt(saltString) ? Argon2CipherProvider.extractRawSaltFromArgon2Salt(saltString) : fullSalt;
            }
            case BCRYPT: {
                return BcryptCipherProvider.isBcryptFormattedSalt(saltString) ? BcryptCipherProvider.extractRawSalt(saltString) : fullSalt;
            }
            case SCRYPT: {
                return ScryptCipherProvider.isScryptFormattedSalt(saltString) ? ScryptCipherProvider.extractRawSaltFromScryptSalt(saltString) : fullSalt;
            }
            default: {
                return fullSalt;
            }
        }
    }
    
    static {
        KEY_LENGTH_PATTERN = Pattern.compile("([\\d]+)BIT");
        final Map<String, Integer> aMap = new HashMap<String, Integer>();
        aMap.put("PBEWITHMD5AND128BITAES-CBC-OPENSSL", 16);
        aMap.put("PBEWITHMD5AND192BITAES-CBC-OPENSSL", 16);
        aMap.put("PBEWITHMD5AND256BITAES-CBC-OPENSSL", 16);
        aMap.put("PBEWITHMD5ANDDES", 16);
        aMap.put("PBEWITHMD5ANDRC2", 16);
        aMap.put("PBEWITHSHA1ANDRC2", 16);
        aMap.put("PBEWITHSHA1ANDDES", 16);
        aMap.put("PBEWITHSHAAND128BITAES-CBC-BC", 7);
        aMap.put("PBEWITHSHAAND192BITAES-CBC-BC", 7);
        aMap.put("PBEWITHSHAAND256BITAES-CBC-BC", 7);
        aMap.put("PBEWITHSHAAND40BITRC2-CBC", 7);
        aMap.put("PBEWITHSHAAND128BITRC2-CBC", 7);
        aMap.put("PBEWITHSHAAND40BITRC4", 7);
        aMap.put("PBEWITHSHAAND128BITRC4", 7);
        aMap.put("PBEWITHSHA256AND128BITAES-CBC-BC", 7);
        aMap.put("PBEWITHSHA256AND192BITAES-CBC-BC", 7);
        aMap.put("PBEWITHSHA256AND256BITAES-CBC-BC", 7);
        aMap.put("PBEWITHSHAAND2-KEYTRIPLEDES-CBC", 7);
        aMap.put("PBEWITHSHAAND3-KEYTRIPLEDES-CBC", 7);
        aMap.put("PBEWITHSHAANDTWOFISH-CBC", 7);
        MAX_PASSWORD_LENGTH_BY_ALGORITHM = Collections.unmodifiableMap((Map<? extends String, ? extends Integer>)aMap);
    }
}
