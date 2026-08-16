package org.geoservermcp.catalog;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.geoservermcp.config.McpExtensionConfig;
import org.geoservermcp.tools.McpToolException;

/** Catalog queries and writes with result caps and optional write gating. */
public class CatalogQueryService {

    private static final Set<String> MAP_FORMATS = Set.of("png", "jpeg", "gif", "tiff", "pdf");

    private final CatalogAccess catalogAccess;
    private final McpExtensionConfig config;

    public CatalogQueryService(CatalogAccess catalogAccess, McpExtensionConfig config) {
        this.catalogAccess = Objects.requireNonNull(catalogAccess, "catalogAccess");
        this.config = Objects.requireNonNull(config, "config");
    }

    public CatalogAccess catalogAccess() {
        return catalogAccess;
    }

    public List<String> listWorkspaces() {
        return cap(catalogAccess.workspaceNames());
    }

    public List<Map<String, Object>> listLayers(String workspace) {
        String filter = blankToNull(workspace);
        if (filter != null && !catalogAccess.workspaceExists(filter)) {
            throw new McpToolException("Workspace not found: " + filter);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (LayerView layer : catalogAccess.layers()) {
            if (filter != null && !filter.equals(layer.workspace())) {
                continue;
            }
            result.add(layer.toMap());
            if (result.size() >= config.getMaxListResults()) {
                break;
            }
        }
        return result;
    }

    public Map<String, Object> getLayerInfo(String workspace, String layer) {
        return catalogAccess.layerInfo(workspace, layer);
    }

    public List<Map<String, Object>> getDatastores(String workspace) {
        return cap(catalogAccess.datastores(workspace));
    }

    public Map<String, Object> getDatastore(String workspace, String name) {
        return catalogAccess.datastore(workspace, name);
    }

    public List<Map<String, Object>> getCoveragestores(String workspace) {
        return cap(catalogAccess.coveragestores(workspace));
    }

    public Map<String, Object> getCoveragestore(String workspace, String name) {
        return catalogAccess.coveragestore(workspace, name);
    }

    public List<Map<String, Object>> getLayerGroups(String workspace) {
        return cap(catalogAccess.layerGroups(workspace));
    }

    public Map<String, Object> getLayerGroup(String workspace, String name) {
        return catalogAccess.layerGroup(workspace, name);
    }

    public List<Map<String, Object>> getFeatureTypes(String workspace, String storeName) {
        return cap(catalogAccess.featureTypes(workspace, storeName));
    }

    public List<Map<String, Object>> getFeatureAttributes(String workspace, String storeName, String featureType) {
        return cap(catalogAccess.featureAttributes(workspace, storeName, featureType));
    }

    public Map<String, Object> queryFeatures(
            String workspace, String layer, String filter, List<String> properties, int maxFeatures) {
        int capped = Math.min(Math.max(maxFeatures, 1), config.getMaxFeatures());
        return catalogAccess.queryFeatures(workspace, layer, filter, properties, capped);
    }

    public String getVersion() {
        return catalogAccess.geoServerVersion();
    }

    public Map<String, Object> generateMap(
            List<String> layers, List<String> styles, List<Double> bbox, int width, int height, String format) {
        if (layers == null || layers.isEmpty()) {
            throw new McpToolException("At least one layer must be specified");
        }
        if (styles != null && !styles.isEmpty() && styles.size() != layers.size()) {
            throw new McpToolException("Number of styles must match number of layers");
        }
        List<Double> box = bbox == null || bbox.isEmpty() ? List.of(-180d, -90d, 180d, 90d) : bbox;
        if (box.size() != 4) {
            throw new McpToolException("Bounding box must have 4 coordinates: [minx, miny, maxx, maxy]");
        }
        if (width <= 0 || height <= 0) {
            throw new McpToolException("width and height must be positive");
        }
        String imageFormat = format == null || format.isBlank() ? "png" : format.toLowerCase(Locale.ROOT);
        if (!MAP_FORMATS.contains(imageFormat)) {
            throw new McpToolException("Invalid format. Must be one of: png, jpeg, gif, tiff, pdf");
        }
        String base = catalogAccess.serviceBaseUrl();
        if (base.isBlank()) {
            base = "http://localhost:8080/geoserver";
        }
        String bboxValue = box.get(0) + "," + box.get(1) + "," + box.get(2) + "," + box.get(3);
        StringBuilder url = new StringBuilder(base).append("/wms?");
        append(url, "service", "WMS", true);
        append(url, "version", "1.3.0", false);
        append(url, "request", "GetMap", false);
        append(url, "format", "image/" + imageFormat, false);
        append(url, "layers", String.join(",", layers), false);
        append(url, "width", Integer.toString(width), false);
        append(url, "height", Integer.toString(height), false);
        append(url, "crs", "EPSG:4326", false);
        append(url, "bbox", bboxValue, false);
        if (styles != null && !styles.isEmpty()) {
            append(url, "styles", String.join(",", styles), false);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", url.toString());
        result.put("width", width);
        result.put("height", height);
        result.put("format", imageFormat);
        result.put("layers", layers);
        result.put("styles", styles);
        result.put("bbox", box);
        return result;
    }

    public Map<String, Object> createWorkspace(String name) {
        requireWrites();
        return catalogAccess.createWorkspace(name);
    }

    public Map<String, Object> createLayer(String workspace, String layer, String dataStore, String source) {
        requireWrites();
        return catalogAccess.createLayer(workspace, layer, dataStore, source);
    }

    public Map<String, Object> deleteResource(String resourceType, String workspace, String name) {
        requireWrites();
        return catalogAccess.deleteResource(resourceType, workspace, name);
    }

    public Map<String, Object> createStyle(String name, String sld, String workspace) {
        requireWrites();
        return catalogAccess.createStyle(name, sld, workspace);
    }

    public Map<String, Object> createDatastore(String workspace, String name, Map<String, String> params) {
        requireWrites();
        return catalogAccess.createDatastore(workspace, name, params);
    }

    public Map<String, Object> createCoveragestore(String workspace, String name, Map<String, String> params) {
        requireWrites();
        return catalogAccess.createCoveragestore(workspace, name, params);
    }

    public Map<String, Object> deleteCoveragestore(String workspace, String name) {
        requireWrites();
        return catalogAccess.deleteCoveragestore(workspace, name);
    }

    public Map<String, Object> createLayerGroup(
            String workspace, String name, List<String> layers, List<String> styles) {
        requireWrites();
        return catalogAccess.createLayerGroup(workspace, name, layers, styles);
    }

    public Map<String, Object> addLayerToLayerGroup(
            String layerName, String layerWorkspace, String layerGroupName, String layerGroupWorkspace) {
        requireWrites();
        return catalogAccess.addLayerToLayerGroup(layerName, layerWorkspace, layerGroupName, layerGroupWorkspace);
    }

    public Map<String, Object> removeLayerFromLayerGroup(
            String layerName, String layerWorkspace, String layerGroupName, String layerGroupWorkspace) {
        requireWrites();
        return catalogAccess.removeLayerFromLayerGroup(
                layerName, layerWorkspace, layerGroupName, layerGroupWorkspace);
    }

    public Map<String, Object> deleteLayerGroup(String workspace, String name) {
        requireWrites();
        return catalogAccess.deleteLayerGroup(workspace, name);
    }

    public Map<String, Object> updateLayerGroup(
            String name, String title, String abstractText, List<String> keywords) {
        requireWrites();
        return catalogAccess.updateLayerGroup(name, title, abstractText, keywords);
    }

    public Map<String, Object> publishFeaturestore(String workspace, String storeName, Map<String, String> params) {
        requireWrites();
        return catalogAccess.publishFeaturestore(workspace, storeName, params);
    }

    public Map<String, Object> editFeatureType(
            String workspace, String storeName, String featureType, Map<String, String> updates) {
        requireWrites();
        return catalogAccess.editFeatureType(workspace, storeName, featureType, updates);
    }

    public Map<String, Object> publishStyle(String layerName, String styleName, String workspace) {
        requireWrites();
        return catalogAccess.publishStyle(layerName, styleName, workspace);
    }

    private void requireWrites() {
        if (!config.isAllowWrites()) {
            throw new McpToolException("Write tools are disabled. Set mcp.allowWrites=true to enable.");
        }
    }

    private <T> List<T> cap(List<T> values) {
        if (values == null) {
            return List.of();
        }
        int limit = config.getMaxListResults();
        if (values.size() <= limit) {
            return values;
        }
        return new ArrayList<>(values.subList(0, limit));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static void append(StringBuilder url, String name, String value, boolean first) {
        if (!first) {
            url.append('&');
        }
        url.append(URLEncoder.encode(name, StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"));
    }
}
