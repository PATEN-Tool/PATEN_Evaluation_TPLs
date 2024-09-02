// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.security.web.firewall;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

public class DefaultHttpFirewall implements HttpFirewall
{
    @Override
    public FirewalledRequest getFirewalledRequest(final HttpServletRequest request) throws RequestRejectedException {
        final FirewalledRequest fwr = new RequestWrapper(request);
        if (!this.isNormalized(fwr.getServletPath()) || !this.isNormalized(fwr.getPathInfo())) {
            throw new RequestRejectedException("Un-normalized paths are not supported: " + fwr.getServletPath() + ((fwr.getPathInfo() != null) ? fwr.getPathInfo() : ""));
        }
        return fwr;
    }
    
    @Override
    public HttpServletResponse getFirewalledResponse(final HttpServletResponse response) {
        return (HttpServletResponse)new FirewalledResponse(response);
    }
    
    private boolean isNormalized(final String path) {
        if (path == null) {
            return true;
        }
        int i;
        for (int j = path.length(); j > 0; j = i) {
            i = path.lastIndexOf(47, j - 1);
            final int gap = j - i;
            if (gap == 2 && path.charAt(i + 1) == '.') {
                return false;
            }
            if (gap == 3 && path.charAt(i + 1) == '.' && path.charAt(i + 2) == '.') {
                return false;
            }
        }
        return true;
    }
}
