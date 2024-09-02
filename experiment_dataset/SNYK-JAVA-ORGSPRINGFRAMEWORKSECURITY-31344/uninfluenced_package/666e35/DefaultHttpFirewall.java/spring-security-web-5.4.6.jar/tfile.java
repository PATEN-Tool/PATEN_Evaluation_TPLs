// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.security.web.firewall;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

public class DefaultHttpFirewall implements HttpFirewall
{
    private boolean allowUrlEncodedSlash;
    
    @Override
    public FirewalledRequest getFirewalledRequest(final HttpServletRequest request) throws RequestRejectedException {
        final FirewalledRequest firewalledRequest = new RequestWrapper(request);
        if (!this.isNormalized(firewalledRequest.getServletPath()) || !this.isNormalized(firewalledRequest.getPathInfo())) {
            throw new RequestRejectedException("Un-normalized paths are not supported: " + firewalledRequest.getServletPath() + ((firewalledRequest.getPathInfo() != null) ? firewalledRequest.getPathInfo() : ""));
        }
        final String requestURI = firewalledRequest.getRequestURI();
        if (this.containsInvalidUrlEncodedSlash(requestURI)) {
            throw new RequestRejectedException("The requestURI cannot contain encoded slash. Got " + requestURI);
        }
        return firewalledRequest;
    }
    
    @Override
    public HttpServletResponse getFirewalledResponse(final HttpServletResponse response) {
        return (HttpServletResponse)new FirewalledResponse(response);
    }
    
    public void setAllowUrlEncodedSlash(final boolean allowUrlEncodedSlash) {
        this.allowUrlEncodedSlash = allowUrlEncodedSlash;
    }
    
    private boolean containsInvalidUrlEncodedSlash(final String uri) {
        return !this.allowUrlEncodedSlash && uri != null && (uri.contains("%2f") || uri.contains("%2F"));
    }
    
    private boolean isNormalized(final String path) {
        if (path == null) {
            return true;
        }
        int slashIndex;
        for (int i = path.length(); i > 0; i = slashIndex) {
            slashIndex = path.lastIndexOf(47, i - 1);
            final int gap = i - slashIndex;
            if (gap == 2 && path.charAt(slashIndex + 1) == '.') {
                return false;
            }
            if (gap == 3 && path.charAt(slashIndex + 1) == '.' && path.charAt(slashIndex + 2) == '.') {
                return false;
            }
        }
        return true;
    }
}
