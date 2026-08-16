package org.geoservermcp.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import org.junit.jupiter.api.Test;

class McpExtensionConfigTest {

    @Test
    void defaultsAreSecureAndEnabled() {
        McpExtensionConfig config = new McpExtensionConfig();
        assertTrue(config.isEnabled());
        assertEquals("/mcp", config.getPath());
        assertTrue(config.isRequireAuthentication());
        assertFalse(config.isCorsEnabled());
        assertEquals(1000, config.getMaxListResults());
        assertEquals("2025-03-26", config.negotiateProtocolVersion("nope"));
        assertEquals("2025-11-25", config.negotiateProtocolVersion("2025-11-25"));
    }

    @Test
    void propertiesOverrideDefaults() {
        Properties properties = new Properties();
        properties.setProperty("mcp.enabled", "false");
        properties.setProperty("mcp.path", "assistant");
        properties.setProperty("mcp.requireAuthentication", "false");
        properties.setProperty("mcp.cors.enabled", "true");
        properties.setProperty("mcp.maxListResults", "25");

        McpExtensionConfig config = new McpExtensionConfig(properties);

        assertFalse(config.isEnabled());
        assertEquals("/assistant", config.getPath());
        assertFalse(config.isRequireAuthentication());
        assertTrue(config.isCorsEnabled());
        assertEquals(25, config.getMaxListResults());
    }

    @Test
    void normalizePathAddsLeadingSlash() {
        assertEquals("/mcp", McpExtensionConfig.normalizePath("mcp"));
        assertEquals("/mcp", McpExtensionConfig.normalizePath("/mcp/"));
    }
}
