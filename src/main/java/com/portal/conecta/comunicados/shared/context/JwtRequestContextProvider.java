package com.portal.conecta.comunicados.shared.context;

import com.portal.conecta.comunicados.shared.security.token.JwtExtractToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class JwtRequestContextProvider implements RequestContextProvider {

    private final HttpServletRequest request;
    private final JwtExtractToken jwtExtractToken;

    public JwtRequestContextProvider(HttpServletRequest request, JwtExtractToken jwtExtractToken) {
        this.request = request;
        this.jwtExtractToken = jwtExtractToken;
    }

    @Override
    public RequestContext getRequestContext() {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de autenticação não informado!");
        }

        String token = authorizationHeader.substring(7);

        if (!jwtExtractToken.isValidToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de autenticação inválido!");
        }

        return jwtExtractToken.extractUserDetails(token);
    }

}