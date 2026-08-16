package org.geoservermcp.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;

/**
 * Adapts the GeoServer {@link Catalog} (the secured bean) to {@link CatalogAccess}.
 *
 * <p>Layer visibility is whatever {@code SecureCatalogImpl} already applied for the current user.
 */
public class GeoServerCatalogAccess implements CatalogAccess {

    private final Catalog catalog;

    public GeoServerCatalogAccess(Catalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
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
}
