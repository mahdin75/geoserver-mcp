package org.geoservermcp.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;

/**
 * Minimal configuration for the MCP extension.
 *
 * <p>Lookup order, later sources override earlier ones:
 *
 * <ol>
 *   <li>built-in defaults
 *   <li>{@code mcp.properties} in the GeoServer data directory, if present
 *   <li>environment variables / system properties ({@code GEOSERVER_MCP_*})
 * </ol>
 *
 * <p>The GeoServer URL is intentionally not configurable: the extension runs inside GeoServer.
 */
public class McpExtensionConfig {

    private static final Logger LOGGER = Logger.getLogger(McpExtensionConfig.class.getName());

    public static final String DATA_DIR_FILE = "mcp.properties";
    public static final String DEFAULT_PATH = "/mcp";
    public static final String DEFAULT_PROTOCOL_VERSION = "2025-03-26";
    public static final int DEFAULT_MAX_LIST_RESULTS = 1000;

    static final Set<String> SUPPORTED_PROTOCOL_VERSIONS =
            new LinkedHashSet<>(
                    Arrays.asList(
                            "2025-03-26",
                            "2025-06-18",
                            "2025-11-25",
                            "2026-07-28"));

    private boolean enabled = true;
    private String path = DEFAULT_PATH;
    private boolean requireAuthentication = true;
    private boolean corsEnabled = false;
    private String corsAllowOrigin = "*";
    private int maxListResults = DEFAULT_MAX_LIST_RESULTS;
    private String serverName = "geoserver-mcp-extension";
    private String serverVersion = readImplementationVersion();

    public McpExtensionConfig() {}

    public McpExtensionConfig(Properties properties) {
        apply(properties);
    }

    public McpExtensionConfig(ResourceStore resourceStore) {
        loadFromDataDirectory(resourceStore);
        applyEnvironmentOverrides();
        LOGGER.info("MCP extension config: " + summary());
    }

    public void loadFromDataDirectory(ResourceStore resourceStore) {
        if (resourceStore == null) {
            return;
        }
        try {
            Resource resource = resourceStore.get(DATA_DIR_FILE);
            if (resource == null || resource.getType() != Resource.Type.RESOURCE) {
                LOGGER.fine("No " + DATA_DIR_FILE + " in the GeoServer data directory; using defaults.");
                return;
            }
            Properties properties = new Properties();
            try (InputStream in = resource.in()) {
                properties.load(in);
            }
            apply(properties);
            LOGGER.info("Loaded MCP configuration from data directory " + DATA_DIR_FILE);
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.WARNING, "Failed to read " + DATA_DIR_FILE + "; using defaults.", e);
        }
    }

    public void applyEnvironmentOverrides() {
        try {
            applyProperty("enabled", firstProperty("GEOSERVER_MCP_ENABLED", "mcp.enabled"));
            applyProperty("path", firstProperty("GEOSERVER_MCP_PATH", "mcp.path"));
            applyProperty(
                    "requireAuthentication",
                    firstProperty("GEOSERVER_MCP_REQUIRE_AUTHENTICATION", "mcp.requireAuthentication"));
            applyProperty("cors.enabled", firstProperty("GEOSERVER_MCP_CORS_ENABLED", "mcp.cors.enabled"));
            applyProperty(
                    "cors.allowOrigin",
                    firstProperty("GEOSERVER_MCP_CORS_ALLOW_ORIGIN", "mcp.cors.allowOrigin"));
            applyProperty(
                    "maxListResults",
                    firstProperty("GEOSERVER_MCP_MAX_LIST_RESULTS", "mcp.maxListResults"));
        } catch (RuntimeException e) {
            LOGGER.log(Level.FINE, "Could not read GeoServer/environment MCP overrides", e);
        }
    }

    void apply(Properties properties) {
        if (properties == null) {
            return;
        }
        applyProperty("enabled", properties.getProperty("mcp.enabled"));
        applyProperty("path", properties.getProperty("mcp.path"));
        applyProperty("requireAuthentication", properties.getProperty("mcp.requireAuthentication"));
        applyProperty("cors.enabled", properties.getProperty("mcp.cors.enabled"));
        applyProperty("cors.allowOrigin", properties.getProperty("mcp.cors.allowOrigin"));
        applyProperty("maxListResults", properties.getProperty("mcp.maxListResults"));
    }

    private void applyProperty(String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String trimmed = value.trim();
        switch (key) {
            case "enabled" -> enabled = parseBoolean(trimmed, enabled);
            case "path" -> path = normalizePath(trimmed);
            case "requireAuthentication" -> requireAuthentication = parseBoolean(trimmed, requireAuthentication);
            case "cors.enabled" -> corsEnabled = parseBoolean(trimmed, corsEnabled);
            case "cors.allowOrigin" -> corsAllowOrigin = trimmed;
            case "maxListResults" -> maxListResults = parsePositiveInt(trimmed, maxListResults);
            default -> LOGGER.fine("Ignoring unknown MCP config key: " + key);
        }
    }

    private static String firstProperty(String... names) {
        for (String name : names) {
            String value = GeoServerExtensions.getProperty(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
            value = System.getenv(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    public static String normalizePath(String path) {
        String value = path.trim();
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        if (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value.toLowerCase(Locale.ROOT));
        }
        return fallback;
    }

    private static int parsePositiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String readImplementationVersion() {
        Package pkg = McpExtensionConfig.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            return pkg.getImplementationVersion();
        }
        return "0.1.0";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = normalizePath(path);
    }

    public boolean isRequireAuthentication() {
        return requireAuthentication;
    }

    public void setRequireAuthentication(boolean requireAuthentication) {
        this.requireAuthentication = requireAuthentication;
    }

    public boolean isCorsEnabled() {
        return corsEnabled;
    }

    public void setCorsEnabled(boolean corsEnabled) {
        this.corsEnabled = corsEnabled;
    }

    public String getCorsAllowOrigin() {
        return corsAllowOrigin;
    }

    public void setCorsAllowOrigin(String corsAllowOrigin) {
        this.corsAllowOrigin = corsAllowOrigin;
    }

    public int getMaxListResults() {
        return maxListResults;
    }

    public void setMaxListResults(int maxListResults) {
        this.maxListResults = maxListResults > 0 ? maxListResults : DEFAULT_MAX_LIST_RESULTS;
    }

    public String getServerName() {
        return serverName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public Set<String> getSupportedProtocolVersions() {
        return SUPPORTED_PROTOCOL_VERSIONS;
    }

    public String negotiateProtocolVersion(String requested) {
        if (requested != null && SUPPORTED_PROTOCOL_VERSIONS.contains(requested)) {
            return requested;
        }
        return DEFAULT_PROTOCOL_VERSION;
    }

    public String summary() {
        return "enabled="
                + enabled
                + ", path="
                + path
                + ", requireAuthentication="
                + requireAuthentication
                + ", corsEnabled="
                + corsEnabled
                + ", maxListResults="
                + maxListResults;
    }
}
