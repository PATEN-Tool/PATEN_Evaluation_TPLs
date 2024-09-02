// 
// Decompiled by Procyon v0.5.36
// 

package org.springframework.security.web.authentication.logout;

import javax.servlet.ServletException;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.AbstractAuthenticationTargetUrlRequestHandler;

public class SimpleUrlLogoutSuccessHandler extends AbstractAuthenticationTargetUrlRequestHandler implements LogoutSuccessHandler
{
    public SimpleUrlLogoutSuccessHandler() {
        super.setTargetUrlParameter(null);
    }
    
    public void onLogoutSuccess(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) throws IOException, ServletException {
        super.handle(request, response, authentication);
    }
}
