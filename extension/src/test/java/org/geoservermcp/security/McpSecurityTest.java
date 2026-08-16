package org.geoservermcp.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class McpSecurityTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingAuthenticationIsNotAuthenticated() {
        SecurityContextHolder.clearContext();
        assertFalse(McpSecurity.isAuthenticated());
    }

    @Test
    void anonymousTokenIsNotAuthenticated() {
        SecurityContextHolder.getContext()
                .setAuthentication(new AnonymousAuthenticationToken(
                        "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertFalse(McpSecurity.isAuthenticated());
    }

    @Test
    void usernamePasswordIsAuthenticated() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "admin", "n/a", AuthorityUtils.createAuthorityList("ROLE_AUTHENTICATED")));
        assertTrue(McpSecurity.isAuthenticated());
    }
}
