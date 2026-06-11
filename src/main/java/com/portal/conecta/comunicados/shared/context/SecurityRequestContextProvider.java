package com.portal.conecta.comunicados.shared.context;


import com.portal.conecta.comunicados.shared.exception.UnauthorizedUserException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityRequestContextProvider implements RequestContextProvider {

    @Override
    public RequestContext getRequestContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedUserException("Authentication is required.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof RequestContext requestContext) {
            return requestContext;
        }

        throw new UnauthorizedUserException("Authenticated request context is not available.");
    }
}
