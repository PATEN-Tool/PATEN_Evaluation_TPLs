// 
// Decompiled by Procyon v0.5.36
// 

package org.apache.shiro.crypto;

import javax.crypto.spec.GCMParameterSpec;
import java.security.spec.AlgorithmParameterSpec;

public class AesCipherService extends DefaultBlockCipherService
{
    private static final String ALGORITHM_NAME = "AES";
    
    public AesCipherService() {
        super("AES");
        this.setMode(OperationMode.GCM);
        this.setStreamingMode(OperationMode.GCM);
    }
    
    @Override
    protected AlgorithmParameterSpec createParameterSpec(final byte[] iv, final boolean streaming) {
        if ((streaming && OperationMode.GCM.name().equals(this.getStreamingModeName())) || (!streaming && OperationMode.GCM.name().equals(this.getModeName()))) {
            return new GCMParameterSpec(this.getKeySize(), iv);
        }
        return super.createParameterSpec(iv, streaming);
    }
}
