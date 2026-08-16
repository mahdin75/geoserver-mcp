package org.geoservermcp.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reads the current GeoServer / Spring Security authentication. Catalog queries still go through
 * {@code SecureCatalogImpl}; this helper only enforces the optional "MCP requires a real login"
 * gate.
 */
public final class McpSecurity {

    private McpSecurity() {}

    public static Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static boolean isAuthenticated() {
        Authentication authentication = currentAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        Object principal = authentication.getPrincipal();
        return principal != null && !"anonymousUser".equals(String.valueOf(principal));
    }
}
