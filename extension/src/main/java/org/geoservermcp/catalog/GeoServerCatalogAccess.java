package org.geoservermcp.catalog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoservermcp.tools.McpToolException;
import org.geoservermcp.web.RequestBaseUrl;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.filter.text.ecql.ECQL;

/**
 * Adapts the GeoServer {@link Catalog} (the secured bean) to {@link CatalogAccess}.
 *
 * <p>Visibility and writes are whatever {@code SecureCatalogImpl} already applied for the current
 * user. Connection secrets are redacted on the way out.
 */
public class GeoServerCatalogAccess implements CatalogAccess {

    private static final Set<String> SECRET_KEYS =
            Set.of(
                    "password",
                    "passwd",
                    "pass",
                    "pwd",
                    "user",
                    "username",
                    "secret",
                    "token",
                    "accesskey",
                    "secretkey",
                    "apikey",
                    "api_key");

    private final Catalog catalog;
    private final GeoServer geoServer;

    public GeoServerCatalogAccess(Catalog catalog) {
        this(catalog, null);
    }

    public GeoServerCatalogAccess(Catalog catalog, GeoServer geoServer) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.geoServer = geoServer;
    }

    @Override
    public List<String> workspaceNames() {
        List<String> names = new ArrayList<>();
        for (WorkspaceInfo workspace : catalog.getWorkspaces()) {
            if (workspace != null && workspace.getName() != null) {
                names.add(workspace.getName());
            }
        }
        return names;
    }

    @Override
    public boolean workspaceExists(String name) {
        return name != null && catalog.getWorkspaceByName(name) != null;
    }

    @Override
    public List<LayerView> layers() {
        List<LayerView> views = new ArrayList<>();
        List<LayerInfo> layers = catalog.getLayers();
        if (layers == null) {
            return views;
        }
        for (LayerInfo layer : layers) {
            if (layer != null) {
                views.add(toView(layer));
            }
        }
        return views;
    }

    @Override
    public Map<String, Object> layerInfo(String workspace, String layer) {
        LayerInfo info = requireLayer(workspace, layer);
        Map<String, Object> map = toView(info).toMap();
        map.put("advertised", info.isAdvertised());
        map.put("title", info.getTitle());
        map.put("abstract", info.getAbstract());
        StyleInfo style = info.getDefaultStyle();
        map.put("defaultStyle", style == null ? null : style.prefixedName());
        List<String> styles = new ArrayList<>();
        if (info.getStyles() != null) {
            for (StyleInfo extra : info.getStyles()) {
                if (extra != null) {
                    styles.add(extra.prefixedName());
                }
            }
        }
        map.put("styles", styles);
        ResourceInfo resource = info.getResource();
        if (resource != null) {
            map.put("resource", resourceMap(resource));
            Object bbox = resource.getLatLonBoundingBox();
            if (bbox != null) {
                map.put("latLonBoundingBox", bbox.toString());
            }
        }
        return map;
    }

    @Override
    public List<Map<String, Object>> datastores(String workspace) {
        requireWorkspace(workspace);
        List<Map<String, Object>> result = new ArrayList<>();
        for (DataStoreInfo store : catalog.getDataStoresByWorkspace(workspace)) {
            result.add(storeMap(store));
        }
        return result;
    }

    @Override
    public Map<String, Object> datastore(String workspace, String name) {
        DataStoreInfo store = catalog.getDataStoreByName(workspace, name);
        if (store == null) {
            throw new McpToolException("Datastore not found: " + workspace + ":" + name);
        }
        return storeMap(store);
    }

    @Override
    public List<Map<String, Object>> coveragestores(String workspace) {
        requireWorkspace(workspace);
        List<Map<String, Object>> result = new ArrayList<>();
        for (CoverageStoreInfo store : catalog.getCoverageStoresByWorkspace(workspace)) {
            result.add(coverageStoreMap(store));
        }
        return result;
    }

    @Override
    public Map<String, Object> coveragestore(String workspace, String name) {
        CoverageStoreInfo store = catalog.getCoverageStoreByName(workspace, name);
        if (store == null) {
            throw new McpToolException("Coveragestore not found: " + workspace + ":" + name);
        }
        return coverageStoreMap(store);
    }

    @Override
    public List<Map<String, Object>> layerGroups(String workspace) {
        requireWorkspace(workspace);
        List<Map<String, Object>> result = new ArrayList<>();
        for (LayerGroupInfo group : catalog.getLayerGroupsByWorkspace(workspace)) {
            result.add(layerGroupMap(group));
        }
        return result;
    }

    @Override
    public Map<String, Object> layerGroup(String workspace, String name) {
        LayerGroupInfo group = findLayerGroup(workspace, name);
        if (group == null) {
            throw new McpToolException("Layer group not found: " + qualified(workspace, name));
        }
        return layerGroupMap(group);
    }

    @Override
    public List<Map<String, Object>> featureTypes(String workspace, String storeName) {
        DataStoreInfo store = requireDataStore(workspace, storeName);
        List<Map<String, Object>> result = new ArrayList<>();
        for (FeatureTypeInfo ft : catalog.getFeatureTypesByDataStore(store)) {
            result.add(featureTypeMap(ft));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> featureAttributes(String workspace, String storeName, String featureType) {
        requireDataStore(workspace, storeName);
        FeatureTypeInfo ft = catalog.getFeatureTypeByName(workspace, featureType);
        if (ft == null) {
            throw new McpToolException("Feature type not found: " + workspace + ":" + featureType);
        }
        List<Map<String, Object>> attributes = new ArrayList<>();
        List<AttributeTypeInfo> declared = ft.getAttributes();
        if (declared != null) {
            for (AttributeTypeInfo attribute : declared) {
                if (attribute != null) {
                    attributes.add(attributeMap(attribute));
                }
            }
        }
        if (attributes.isEmpty()) {
            try {
                for (AttributeTypeInfo attribute : ft.attributes()) {
                    attributes.add(attributeMap(attribute));
                }
            } catch (IOException e) {
                throw new McpToolException("Failed to read feature attributes: " + e.getMessage());
            }
        }
        return attributes;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map<String, Object> queryFeatures(
            String workspace, String layer, String filter, List<String> properties, int maxFeatures) {
        FeatureTypeInfo ft = catalog.getFeatureTypeByName(workspace, layer);
        if (ft == null) {
            LayerInfo layerInfo = requireLayer(workspace, layer);
            ResourceInfo resource = layerInfo.getResource();
            if (!(resource instanceof FeatureTypeInfo featureType)) {
                throw new McpToolException("Layer is not a vector feature type: " + workspace + ":" + layer);
            }
            ft = featureType;
        }
        try {
            FeatureSource source = ft.getFeatureSource(null, null);
            Query query = new Query();
            query.setMaxFeatures(maxFeatures);
            if (filter != null && !filter.isBlank()) {
                query.setFilter(ECQL.toFilter(filter));
            }
            if (properties != null && !properties.isEmpty()) {
                query.setPropertyNames(properties);
            }
            FeatureCollection collection = source.getFeatures(query);
            List<Map<String, Object>> features = new ArrayList<>();
            try (FeatureIterator iterator = collection.features()) {
                while (iterator.hasNext()) {
                    features.add(featureMap(iterator.next()));
                }
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", "FeatureCollection");
            result.put("features", features);
            result.put("numberReturned", features.size());
            return result;
        } catch (McpToolException e) {
            throw e;
        } catch (Exception e) {
            throw new McpToolException("Failed to query features: " + e.getMessage());
        }
    }

    @Override
    public String geoServerVersion() {
        Package catalogPackage = Catalog.class.getPackage();
        if (catalogPackage != null && catalogPackage.getImplementationVersion() != null) {
            return catalogPackage.getImplementationVersion();
        }
        return "2.28";
    }

    @Override
    public String serviceBaseUrl() {
        String request = RequestBaseUrl.get();
        if (!request.isBlank()) {
            return request;
        }
        if (geoServer != null && geoServer.getSettings() != null) {
            String proxy = geoServer.getSettings().getProxyBaseUrl();
            if (proxy != null && !proxy.isBlank()) {
                return trimSlash(proxy);
            }
        }
        return "";
    }

    @Override
    public Map<String, Object> createWorkspace(String name) {
        if (catalog.getWorkspaceByName(name) != null) {
            return status("info", "Workspace '" + name + "' already exists", "workspace", name);
        }
        WorkspaceInfo workspace = catalog.getFactory().createWorkspace();
        workspace.setName(name);
        NamespaceInfo namespace = catalog.getFactory().createNamespace();
        namespace.setPrefix(name);
        namespace.setURI("http://geoserver.org/" + name);
        catalog.add(namespace);
        catalog.add(workspace);
        return status("success", "Workspace '" + name + "' created successfully", "workspace", name);
    }

    @Override
    public Map<String, Object> createLayer(String workspace, String layer, String dataStore, String source) {
        DataStoreInfo store = requireDataStore(workspace, dataStore);
        if (findLayer(workspace, layer) != null) {
            throw new McpToolException("Layer already exists: " + workspace + ":" + layer);
        }
        try {
            CatalogBuilder builder = new CatalogBuilder(catalog);
            builder.setStore(store);
            FeatureTypeInfo featureType = builder.buildFeatureType(new NameImpl(source));
            featureType.setName(layer);
            featureType.setNativeName(source);
            featureType.setEnabled(true);
            featureType.setAdvertised(true);
            try {
                builder.setupBounds(featureType);
            } catch (Exception ignored) {
                // native bounds are optional if the store cannot compute them
            }
            LayerInfo layerInfo = builder.buildLayer(featureType);
            catalog.add(featureType);
            catalog.add(layerInfo);
        } catch (McpToolException e) {
            throw e;
        } catch (Exception e) {
            FeatureTypeInfo featureType = catalog.getFactory().createFeatureType();
            featureType.setStore(store);
            featureType.setNamespace(catalog.getNamespaceByPrefix(workspace));
            featureType.setName(layer);
            featureType.setNativeName(source);
            featureType.setEnabled(true);
            featureType.setAdvertised(true);
            catalog.add(featureType);
            LayerInfo layerInfo = catalog.getFactory().createLayer();
            layerInfo.setResource(featureType);
            layerInfo.setEnabled(true);
            layerInfo.setAdvertised(true);
            catalog.add(layerInfo);
        }
        Map<String, Object> result =
                status("success", "Layer '" + layer + "' created successfully in workspace '" + workspace + "'");
        result.put("name", layer);
        result.put("workspace", workspace);
        result.put("data_store", dataStore);
        result.put("source", source);
        return result;
    }

    @Override
    public Map<String, Object> deleteResource(String resourceType, String workspace, String name) {
        String type = resourceType.toLowerCase(Locale.ROOT);
        switch (type) {
            case "workspace" -> deleteWorkspace(name);
            case "layer" -> deleteLayer(workspace, name);
            case "datastore" -> deleteDatastore(workspace, name);
            case "style" -> deleteStyle(workspace, name);
            case "coverage" -> deleteCoverage(workspace, name);
            default -> throw new McpToolException(
                    "Invalid resource type. Must be one of: workspace, layer, datastore, style, coverage");
        }
        Map<String, Object> result =
                status("success", capitalize(type) + " '" + name + "' deleted successfully", "name", name);
        result.put("type", resourceType);
        result.put("workspace", workspace == null || workspace.isBlank() ? "global" : workspace);
        return result;
    }

    @Override
    public Map<String, Object> createStyle(String name, String sld, String workspace) {
        StyleInfo existing =
                workspace == null ? catalog.getStyleByName(name) : catalog.getStyleByName(workspace, name);
        if (existing != null) {
            throw new McpToolException("Style already exists: " + qualified(workspace, name));
        }
        StyleInfo style = catalog.getFactory().createStyle();
        style.setName(name);
        style.setFilename(name + ".sld");
        style.setFormat("sld");
        if (workspace != null) {
            style.setWorkspace(requireWorkspace(workspace));
        }
        catalog.add(style);
        try (ByteArrayInputStream in = new ByteArrayInputStream(sld.getBytes(StandardCharsets.UTF_8))) {
            catalog.getResourcePool().writeStyle(style, in);
        } catch (IOException e) {
            catalog.remove(style);
            throw new McpToolException("Failed to write SLD: " + e.getMessage());
        }
        String message =
                workspace == null
                        ? "Global style '" + name + "' created"
                        : "Style '" + name + "' created in workspace '" + workspace + "'";
        Map<String, Object> result = status("success", message, "name", name);
        result.put("workspace", workspace == null ? "global" : workspace);
        return result;
    }

    @Override
    public Map<String, Object> createDatastore(String workspace, String name, Map<String, String> params) {
        requireWorkspace(workspace);
        if (catalog.getDataStoreByName(workspace, name) != null) {
            throw new McpToolException("Datastore already exists: " + workspace + ":" + name);
        }
        DataStoreInfo store = catalog.getFactory().createDataStore();
        store.setName(name);
        store.setWorkspace(requireWorkspace(workspace));
        store.setEnabled(true);
        copyParams(store.getConnectionParameters(), params);
        catalog.add(store);
        Map<String, Object> result =
                status("success", "Datastore '" + name + "' created in workspace '" + workspace + "'");
        result.put("workspace", workspace);
        result.put("name", name);
        result.put("connectionParameters", redact(store.getConnectionParameters()));
        return result;
    }

    @Override
    public Map<String, Object> createCoveragestore(String workspace, String name, Map<String, String> params) {
        requireWorkspace(workspace);
        if (catalog.getCoverageStoreByName(workspace, name) != null) {
            throw new McpToolException("Coveragestore already exists: " + workspace + ":" + name);
        }
        CoverageStoreInfo store = catalog.getFactory().createCoverageStore();
        store.setName(name);
        store.setWorkspace(requireWorkspace(workspace));
        store.setEnabled(true);
        if (params.get("type") != null) {
            store.setType(params.get("type"));
        }
        String url = firstNonBlank(params.get("url"), fileUrl(params.get("file")));
        if (url != null) {
            store.setURL(url);
        }
        copyParams(store.getConnectionParameters(), params);
        catalog.add(store);
        Map<String, Object> result =
                status("success", "Coveragestore '" + name + "' created in workspace '" + workspace + "'");
        result.put("workspace", workspace);
        result.put("name", name);
        result.putAll(coverageStoreMap(store));
        return result;
    }

    @Override
    public Map<String, Object> deleteCoveragestore(String workspace, String name) {
        CoverageStoreInfo store = catalog.getCoverageStoreByName(workspace, name);
        if (store == null) {
            throw new McpToolException("Coveragestore not found: " + workspace + ":" + name);
        }
        removeStore(store);
        return status(
                "success",
                "Coveragestore '" + name + "' deleted from workspace '" + workspace + "'",
                "workspace",
                workspace,
                "name",
                name);
    }

    @Override
    public Map<String, Object> createLayerGroup(
            String workspace, String name, List<String> layers, List<String> styles) {
        requireWorkspace(workspace);
        if (findLayerGroup(workspace, name) != null) {
            throw new McpToolException("Layer group already exists: " + workspace + ":" + name);
        }
        if (layers == null || layers.isEmpty()) {
            throw new McpToolException("layers is required");
        }
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(name);
        group.setWorkspace(requireWorkspace(workspace));
        for (int i = 0; i < layers.size(); i++) {
            group.getLayers().add(resolvePublished(workspace, layers.get(i)));
            StyleInfo style = null;
            if (styles != null && i < styles.size()) {
                style = resolveStyle(workspace, styles.get(i));
            }
            group.getStyles().add(style);
        }
        catalog.add(group);
        Map<String, Object> result =
                status("success", "Layer group '" + name + "' created in workspace '" + workspace + "'");
        result.putAll(layerGroupMap(group));
        return result;
    }

    @Override
    public Map<String, Object> addLayerToLayerGroup(
            String layerName, String layerWorkspace, String layerGroupName, String layerGroupWorkspace) {
        LayerGroupInfo group = requireLayerGroup(layerGroupWorkspace, layerGroupName);
        LayerInfo layer = requireLayer(layerWorkspace, layerName);
        group.getLayers().add(layer);
        group.getStyles().add(null);
        catalog.save(group);
        return status(
                "success",
                "Layer '" + layerName + "' added to layer group '" + layerGroupName + "'",
                "layer",
                layerName,
                "layergroup",
                layerGroupName);
    }

    @Override
    public Map<String, Object> removeLayerFromLayerGroup(
            String layerName, String layerWorkspace, String layerGroupName, String layerGroupWorkspace) {
        LayerGroupInfo group = requireLayerGroup(layerGroupWorkspace, layerGroupName);
        List<PublishedInfo> published = group.getLayers();
        boolean removed = false;
        for (int i = published.size() - 1; i >= 0; i--) {
            PublishedInfo item = published.get(i);
            if (item != null && layerName.equals(item.getName())) {
                String itemWorkspace = workspaceOfPublished(item);
                if (layerWorkspace == null || layerWorkspace.equals(itemWorkspace)) {
                    published.remove(i);
                    if (i < group.getStyles().size()) {
                        group.getStyles().remove(i);
                    }
                    removed = true;
                }
            }
        }
        if (!removed) {
            throw new McpToolException("Layer '" + layerName + "' is not in layer group '" + layerGroupName + "'");
        }
        catalog.save(group);
        return status(
                "success",
                "Layer '" + layerName + "' removed from layer group '" + layerGroupName + "'",
                "layer",
                layerName,
                "layergroup",
                layerGroupName);
    }

    @Override
    public Map<String, Object> deleteLayerGroup(String workspace, String name) {
        LayerGroupInfo group = requireLayerGroup(workspace, name);
        catalog.remove(group);
        return status("success", "Layer group '" + name + "' deleted", "workspace", workspace, "name", name);
    }

    @Override
    public Map<String, Object> updateLayerGroup(
            String name, String title, String abstractText, List<String> keywords) {
        LayerGroupInfo group = catalog.getLayerGroupByName(name);
        if (group == null) {
            throw new McpToolException("Layer group not found: " + name);
        }
        if (title != null) {
            group.setTitle(title);
        }
        if (abstractText != null) {
            group.setAbstract(abstractText);
        }
        if (keywords != null) {
            @SuppressWarnings("unchecked")
            List<KeywordInfo> values = group.getKeywords();
            values.clear();
            for (String keyword : keywords) {
                if (keyword != null && !keyword.isBlank()) {
                    values.add(new Keyword(keyword));
                }
            }
        }
        catalog.save(group);
        Map<String, Object> result = status("success", "Layer group '" + name + "' updated", "name", name);
        result.putAll(layerGroupMap(group));
        return result;
    }

    @Override
    public Map<String, Object> publishFeaturestore(String workspace, String storeName, Map<String, String> params) {
        String nativeName =
                firstNonBlank(params.get("nativeName"), params.get("native_name"), params.get("table"), params.get("name"));
        if (nativeName == null) {
            throw new McpToolException("params.nativeName (or table/name) is required");
        }
        String layerName = firstNonBlank(params.get("name"), nativeName);
        return createLayer(workspace, layerName, storeName, nativeName);
    }

    @Override
    public Map<String, Object> editFeatureType(
            String workspace, String storeName, String featureType, Map<String, String> updates) {
        requireDataStore(workspace, storeName);
        FeatureTypeInfo ft = catalog.getFeatureTypeByName(workspace, featureType);
        if (ft == null) {
            throw new McpToolException("Feature type not found: " + workspace + ":" + featureType);
        }
        if (updates.containsKey("title")) {
            ft.setTitle(updates.get("title"));
        }
        if (updates.containsKey("abstract")) {
            ft.setAbstract(updates.get("abstract"));
        }
        String srs = firstNonBlank(updates.get("srs"), updates.get("SRS"));
        if (srs != null) {
            ft.setSRS(srs);
        }
        if (updates.containsKey("enabled")) {
            ft.setEnabled(Boolean.parseBoolean(updates.get("enabled")));
        }
        if (updates.containsKey("advertised")) {
            ft.setAdvertised(Boolean.parseBoolean(updates.get("advertised")));
        }
        if (updates.containsKey("nativeName")) {
            ft.setNativeName(updates.get("nativeName"));
        }
        catalog.save(ft);
        Map<String, Object> result =
                status("success", "Feature type '" + featureType + "' updated", "name", featureType);
        result.putAll(featureTypeMap(ft));
        return result;
    }

    @Override
    public Map<String, Object> publishStyle(String layerName, String styleName, String workspace) {
        LayerInfo layer = requireLayer(workspace, layerName);
        StyleInfo style = catalog.getStyleByName(workspace, styleName);
        if (style == null) {
            style = catalog.getStyleByName(styleName);
        }
        if (style == null) {
            throw new McpToolException("Style not found: " + qualified(workspace, styleName));
        }
        layer.setDefaultStyle(style);
        catalog.save(layer);
        return status(
                "success",
                "Style '" + styleName + "' published on layer '" + layerName + "'",
                "layer",
                layerName,
                "style",
                styleName,
                "workspace",
                workspace);
    }

    static LayerView toView(LayerInfo layer) {
        String name = layer.getName();
        String workspace = workspaceOf(layer);
        return new LayerView(name, workspace, prefixedName(layer, workspace, name), layer.isEnabled(), typeName(layer.getType()));
    }

    static String workspaceOf(LayerInfo layer) {
        ResourceInfo resource = layer.getResource();
        if (resource == null) {
            return null;
        }
        StoreInfo store = resource.getStore();
        if (store == null || store.getWorkspace() == null) {
            return null;
        }
        return store.getWorkspace().getName();
    }

    static String prefixedName(LayerInfo layer, String workspace, String name) {
        String prefixed = layer.prefixedName();
        if (prefixed != null && !prefixed.isBlank()) {
            return prefixed;
        }
        if (workspace != null && name != null) {
            return workspace + ":" + name;
        }
        return name;
    }

    static String typeName(PublishedType type) {
        return type == null ? null : type.name();
    }

    private WorkspaceInfo requireWorkspace(String name) {
        WorkspaceInfo workspace = catalog.getWorkspaceByName(name);
        if (workspace == null) {
            throw new McpToolException("Workspace not found: " + name);
        }
        return workspace;
    }

    private DataStoreInfo requireDataStore(String workspace, String name) {
        requireWorkspace(workspace);
        DataStoreInfo store = catalog.getDataStoreByName(workspace, name);
        if (store == null) {
            throw new McpToolException("Datastore not found: " + workspace + ":" + name);
        }
        return store;
    }

    private LayerInfo requireLayer(String workspace, String name) {
        LayerInfo layer = findLayer(workspace, name);
        if (layer == null) {
            throw new McpToolException("Layer not found: " + workspace + ":" + name);
        }
        return layer;
    }

    private LayerInfo findLayer(String workspace, String name) {
        LayerInfo layer = catalog.getLayerByName(workspace + ":" + name);
        if (layer == null) {
            layer = catalog.getLayerByName(name);
        }
        return layer;
    }

    private LayerGroupInfo requireLayerGroup(String workspace, String name) {
        LayerGroupInfo group = findLayerGroup(workspace, name);
        if (group == null) {
            throw new McpToolException("Layer group not found: " + qualified(workspace, name));
        }
        return group;
    }

    private LayerGroupInfo findLayerGroup(String workspace, String name) {
        if (workspace != null && !workspace.isBlank()) {
            LayerGroupInfo group = catalog.getLayerGroupByName(workspace, name);
            if (group != null) {
                return group;
            }
        }
        return catalog.getLayerGroupByName(name);
    }

    private PublishedInfo resolvePublished(String workspace, String ref) {
        if (ref == null || ref.isBlank()) {
            throw new McpToolException("Layer name is required");
        }
        String layerWorkspace = workspace;
        String layerName = ref;
        int colon = ref.indexOf(':');
        if (colon > 0) {
            layerWorkspace = ref.substring(0, colon);
            layerName = ref.substring(colon + 1);
        }
        LayerInfo layer = findLayer(layerWorkspace, layerName);
        if (layer != null) {
            return layer;
        }
        LayerGroupInfo group = findLayerGroup(layerWorkspace, layerName);
        if (group != null) {
            return group;
        }
        throw new McpToolException("Layer not found: " + ref);
    }

    private StyleInfo resolveStyle(String workspace, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        StyleInfo style = catalog.getStyleByName(workspace, name);
        return style != null ? style : catalog.getStyleByName(name);
    }

    private void deleteWorkspace(String name) {
        WorkspaceInfo workspace = requireWorkspace(name);
        for (StoreInfo store : catalog.getStoresByWorkspace(workspace, StoreInfo.class)) {
            removeStore(store);
        }
        for (LayerGroupInfo group : catalog.getLayerGroupsByWorkspace(workspace)) {
            catalog.remove(group);
        }
        for (StyleInfo style : catalog.getStylesByWorkspace(workspace)) {
            catalog.remove(style);
        }
        NamespaceInfo namespace = catalog.getNamespaceByPrefix(name);
        if (namespace != null) {
            catalog.remove(namespace);
        }
        catalog.remove(workspace);
    }

    private void deleteLayer(String workspace, String name) {
        LayerInfo layer = requireLayer(workspace, name);
        ResourceInfo resource = layer.getResource();
        catalog.remove(layer);
        if (resource != null) {
            try {
                catalog.remove(resource);
            } catch (RuntimeException ignored) {
                // another published layer may still own the resource
            }
        }
    }

    private void deleteDatastore(String workspace, String name) {
        removeStore(requireDataStore(workspace, name));
    }

    private void deleteStyle(String workspace, String name) {
        StyleInfo style =
                workspace == null || workspace.isBlank()
                        ? catalog.getStyleByName(name)
                        : catalog.getStyleByName(workspace, name);
        if (style == null) {
            style = catalog.getStyleByName(name);
        }
        if (style == null) {
            throw new McpToolException("Style not found: " + qualified(workspace, name));
        }
        catalog.remove(style);
    }

    private void deleteCoverage(String workspace, String name) {
        CoverageInfo coverage = catalog.getCoverageByName(workspace, name);
        if (coverage == null) {
            throw new McpToolException("Coverage not found: " + workspace + ":" + name);
        }
        LayerInfo layer = findLayer(workspace, name);
        if (layer != null) {
            catalog.remove(layer);
        }
        catalog.remove(coverage);
    }

    private void removeStore(StoreInfo store) {
        List<ResourceInfo> resources = catalog.getResourcesByStore(store, ResourceInfo.class);
        for (ResourceInfo resource : resources) {
            LayerInfo layer = catalog.getLayerByName(resource.prefixedName());
            if (layer != null) {
                catalog.remove(layer);
            }
            catalog.remove(resource);
        }
        catalog.remove(store);
    }

    private Map<String, Object> resourceMap(ResourceInfo resource) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", resource.getName());
        map.put("nativeName", resource.getNativeName());
        map.put("title", resource.getTitle());
        map.put("abstract", resource.getAbstract());
        map.put("srs", resource.getSRS());
        map.put("namespace", resource.getNamespace() == null ? null : resource.getNamespace().getPrefix());
        StoreInfo store = resource.getStore();
        map.put("store", store == null ? null : store.getName());
        map.put("storeType", resource instanceof CoverageInfo ? "coverage" : "featureType");
        return map;
    }

    private Map<String, Object> storeMap(DataStoreInfo store) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", store.getName());
        map.put("workspace", store.getWorkspace() == null ? null : store.getWorkspace().getName());
        map.put("type", store.getType());
        map.put("enabled", store.isEnabled());
        map.put("description", store.getDescription());
        map.put("connectionParameters", redact(store.getConnectionParameters()));
        return map;
    }

    private Map<String, Object> coverageStoreMap(CoverageStoreInfo store) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", store.getName());
        map.put("workspace", store.getWorkspace() == null ? null : store.getWorkspace().getName());
        map.put("type", store.getType());
        map.put("enabled", store.isEnabled());
        map.put("description", store.getDescription());
        map.put("url", store.getURL());
        map.put("connectionParameters", redact(store.getConnectionParameters()));
        return map;
    }

    private Map<String, Object> layerGroupMap(LayerGroupInfo group) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", group.getName());
        map.put("workspace", group.getWorkspace() == null ? null : group.getWorkspace().getName());
        map.put("title", group.getTitle());
        map.put("abstract", group.getAbstract());
        map.put("mode", group.getMode() == null ? null : group.getMode().name());
        List<String> layers = new ArrayList<>();
        if (group.getLayers() != null) {
            for (PublishedInfo published : group.getLayers()) {
                layers.add(published == null ? null : published.prefixedName());
            }
        }
        map.put("layers", layers);
        List<String> styles = new ArrayList<>();
        if (group.getStyles() != null) {
            for (StyleInfo style : group.getStyles()) {
                styles.add(style == null ? null : style.prefixedName());
            }
        }
        map.put("styles", styles);
        return map;
    }

    private Map<String, Object> featureTypeMap(FeatureTypeInfo ft) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", ft.getName());
        map.put("nativeName", ft.getNativeName());
        map.put("title", ft.getTitle());
        map.put("abstract", ft.getAbstract());
        map.put("srs", ft.getSRS());
        map.put("enabled", ft.isEnabled());
        map.put("advertised", ft.isAdvertised());
        map.put("namespace", ft.getNamespace() == null ? null : ft.getNamespace().getPrefix());
        map.put("store", ft.getStore() == null ? null : ft.getStore().getName());
        return map;
    }

    private Map<String, Object> attributeMap(AttributeTypeInfo attribute) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", attribute.getName());
        map.put("minOccurs", attribute.getMinOccurs());
        map.put("maxOccurs", attribute.getMaxOccurs());
        map.put("nillable", attribute.isNillable());
        Class<?> binding = attribute.getBinding();
        map.put("binding", binding == null ? null : binding.getName());
        return map;
    }

    private Map<String, Object> featureMap(Feature feature) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "Feature");
        map.put("id", feature.getIdentifier() == null ? null : feature.getIdentifier().getID());
        Map<String, Object> properties = new LinkedHashMap<>();
        Object geometry = null;
        if (feature instanceof SimpleFeature simple) {
            geometry = simple.getDefaultGeometry();
        }
        for (Property property : feature.getProperties()) {
            if (property == null || property.getName() == null) {
                continue;
            }
            String name = property.getName().getLocalPart();
            Object value = property.getValue();
            if (geometry != null && value == geometry) {
                continue;
            }
            if (value != null && value.getClass().getName().contains("geom")) {
                if (geometry == null) {
                    geometry = value;
                    continue;
                }
            }
            properties.put(name, scalar(value));
        }
        map.put("geometry", geometry == null ? null : geometry.toString());
        map.put("properties", properties);
        return map;
    }

    private static Object scalar(Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean || value instanceof String) {
            return value;
        }
        return String.valueOf(value);
    }

    private static Map<String, Object> redact(Map<String, Serializable> params) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (params == null) {
            return out;
        }
        for (Map.Entry<String, Serializable> entry : params.entrySet()) {
            String key = entry.getKey();
            if (key != null && SECRET_KEYS.contains(key.toLowerCase(Locale.ROOT))) {
                out.put(key, "***");
            } else {
                out.put(key, entry.getValue() == null ? null : String.valueOf(entry.getValue()));
            }
        }
        return out;
    }

    private static void copyParams(Map<String, Serializable> target, Map<String, String> params) {
        if (params == null) {
            return;
        }
        for (Map.Entry<String, String> entry : params.entrySet()) {
            target.put(entry.getKey(), entry.getValue());
        }
    }

    private static Map<String, Object> status(String status, String message, Object... extra) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", status);
        map.put("message", message);
        for (int i = 0; i + 1 < extra.length; i += 2) {
            map.put(String.valueOf(extra[i]), extra[i + 1]);
        }
        return map;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String fileUrl(String file) {
        if (file == null || file.isBlank()) {
            return null;
        }
        return file.startsWith("file:") ? file : "file:" + file;
    }

    private static String qualified(String workspace, String name) {
        return workspace == null || workspace.isBlank() ? name : workspace + ":" + name;
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String workspaceOfPublished(PublishedInfo published) {
        if (published instanceof LayerInfo layer) {
            return workspaceOf(layer);
        }
        if (published instanceof LayerGroupInfo group && group.getWorkspace() != null) {
            return group.getWorkspace().getName();
        }
        return null;
    }
}
