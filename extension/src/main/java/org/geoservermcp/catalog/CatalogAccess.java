package org.geoservermcp.catalog;

import java.util.List;
import java.util.Map;

/** Catalog operations used by MCP tools. Implementations must use GeoServer's secured catalog. */
public interface CatalogAccess {

    List<String> workspaceNames();

    boolean workspaceExists(String name);

    List<LayerView> layers();

    Map<String, Object> layerInfo(String workspace, String layer);

    List<Map<String, Object>> datastores(String workspace);

    Map<String, Object> datastore(String workspace, String name);

    List<Map<String, Object>> coveragestores(String workspace);

    Map<String, Object> coveragestore(String workspace, String name);

    List<Map<String, Object>> layerGroups(String workspace);

    Map<String, Object> layerGroup(String workspace, String name);

    List<Map<String, Object>> featureTypes(String workspace, String storeName);

    List<Map<String, Object>> featureAttributes(String workspace, String storeName, String featureType);

    Map<String, Object> queryFeatures(
            String workspace, String layer, String filter, List<String> properties, int maxFeatures);

    String geoServerVersion();

    String serviceBaseUrl();

    Map<String, Object> createWorkspace(String name);

    Map<String, Object> createLayer(String workspace, String layer, String dataStore, String source);

    Map<String, Object> deleteResource(String resourceType, String workspace, String name);

    Map<String, Object> createStyle(String name, String sld, String workspace);

    Map<String, Object> createDatastore(String workspace, String name, Map<String, String> params);

    Map<String, Object> createCoveragestore(String workspace, String name, Map<String, String> params);

    Map<String, Object> deleteCoveragestore(String workspace, String name);

    Map<String, Object> createLayerGroup(
            String workspace, String name, List<String> layers, List<String> styles);

    Map<String, Object> addLayerToLayerGroup(
            String layerName, String layerWorkspace, String layerGroupName, String layerGroupWorkspace);

    Map<String, Object> removeLayerFromLayerGroup(
            String layerName, String layerWorkspace, String layerGroupName, String layerGroupWorkspace);

    Map<String, Object> deleteLayerGroup(String workspace, String name);

    Map<String, Object> updateLayerGroup(
            String name, String title, String abstractText, List<String> keywords);

    Map<String, Object> publishFeaturestore(String workspace, String storeName, Map<String, String> params);

    Map<String, Object> editFeatureType(
            String workspace, String storeName, String featureType, Map<String, String> updates);

    Map<String, Object> publishStyle(String layerName, String styleName, String workspace);
}
