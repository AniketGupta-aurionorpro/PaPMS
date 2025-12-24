package com.aurionpro.papms.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Custom UserDetails implementation that includes additional user information
 * such as user ID and organization ID for role-based access control.
 */
@Getter
public class CustomUserDetails extends User {

    private final Long id;
    private final Integer organizationId;

    public CustomUserDetails(Long id, String username, String password,
            Integer organizationId, boolean enabled,
            Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, true, true, true, authorities);
        this.id = id;
        this.organizationId = organizationId;
    }
}
