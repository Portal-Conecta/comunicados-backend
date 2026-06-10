package com.portal.conecta.comunicados.shared.security.user;

import com.portal.conecta.comunicados.shared.context.RequestContext;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

    private final RequestContext requestContext;
    private final List<? extends GrantedAuthority> authorities;

    public CustomUserDetails(RequestContext requestContext) {
        this.requestContext = requestContext;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + requestContext.userType().name()));
    }

    public RequestContext toRequestContext() {
        return requestContext;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return requestContext.userId().toString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
