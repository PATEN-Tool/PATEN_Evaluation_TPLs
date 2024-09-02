// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.tapestry5.internal.services;

import java.io.OutputStream;
import org.apache.tapestry5.internal.TapestryInternalUtils;
import org.apache.tapestry5.internal.util.MacOutputStream;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import org.apache.tapestry5.internal.util.Base64InputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import org.apache.tapestry5.services.ClientDataSink;
import java.io.UnsupportedEncodingException;
import javax.crypto.spec.SecretKeySpec;
import org.apache.tapestry5.alerts.AlertManager;
import org.slf4j.Logger;
import org.apache.tapestry5.ioc.annotations.Symbol;
import java.security.Key;
import org.apache.tapestry5.services.URLEncoder;
import org.apache.tapestry5.services.ClientDataEncoder;

public class ClientDataEncoderImpl implements ClientDataEncoder
{
    private final URLEncoder urlEncoder;
    private final Key hmacKey;
    
    public ClientDataEncoderImpl(final URLEncoder urlEncoder, @Symbol("tapestry.hmac-passphrase") String passphrase, final Logger logger, @Symbol("tapestry.app-package") final String applicationPackageName, final AlertManager alertManager) throws UnsupportedEncodingException {
        this.urlEncoder = urlEncoder;
        if (passphrase.equals("")) {
            final String message = String.format("The symbol '%s' has not been configured. This is used to configure hash-based message authentication of Tapestry data stored in forms, or in the URL. You application is less secure, and more vulnerable to denial-of-service attacks, when this symbol is not configured.", "tapestry.hmac-passphrase");
            alertManager.error(message);
            logger.error(message);
            passphrase = applicationPackageName;
        }
        this.hmacKey = new SecretKeySpec(passphrase.getBytes("UTF8"), "HmacSHA1");
    }
    
    @Override
    public ClientDataSink createSink() {
        try {
            return new ClientDataSinkImpl(this.urlEncoder, this.hmacKey);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @Override
    public ObjectInputStream decodeClientData(final String clientData) {
        final int colonx = clientData.indexOf(58);
        if (colonx < 0) {
            throw new IllegalArgumentException("Client data must be prefixed with its HMAC code.");
        }
        final String storedHmacResult = clientData.substring(0, colonx);
        final String clientStream = clientData.substring(colonx + 1);
        try {
            final Base64InputStream b64in = new Base64InputStream(clientStream);
            this.validateHMAC(storedHmacResult, b64in);
            b64in.reset();
            final BufferedInputStream buffered = new BufferedInputStream(new GZIPInputStream(b64in));
            return new ObjectInputStream(buffered);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private void validateHMAC(final String storedHmacResult, final Base64InputStream b64in) throws IOException {
        final MacOutputStream macOs = MacOutputStream.streamFor(this.hmacKey);
        TapestryInternalUtils.copy(b64in, macOs);
        macOs.close();
        final String actual = macOs.getResult();
        if (!storedHmacResult.equals(actual)) {
            throw new IOException("Client data associated with the current request appears to have been tampered with (the HMAC signature does not match).");
        }
    }
    
    @Override
    public ObjectInputStream decodeEncodedClientData(final String clientData) throws IOException {
        return this.decodeClientData(this.urlEncoder.decode(clientData));
    }
}
