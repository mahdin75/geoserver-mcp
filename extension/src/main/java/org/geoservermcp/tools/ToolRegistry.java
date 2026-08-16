package org.geoservermcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.geoservermcp.catalog.CatalogQueryService;

public class ToolRegistry {

    private final Map<String, McpTool> tools = new LinkedHashMap<>();

    public ToolRegistry(CatalogQueryService catalog) {
        register(new ListWorkspacesTool(catalog));
        register(new ListLayersTool(catalog));

        read(
                "get_layer_info",
                "Get detailed information about a layer.",
                Schemas.prop(
                        Schemas.prop(Schemas.object("workspace", "layer"), "workspace", "string", "The workspace containing the layer"),
                        "layer",
                        "string",
                        "The name of the layer"),
                args -> catalog.getLayerInfo(JsonArgs.requireText(args, "workspace"), JsonArgs.requireText(args, "layer")));

        read(
                "query_features",
                "Query features from a vector layer using CQL filter.",
                Schemas.prop(
                        Schemas.arrayProp(
                                Schemas.prop(
                                        Schemas.prop(
                                                Schemas.prop(
                                                        Schemas.object("workspace", "layer"),
                                                        "workspace",
                                                        "string",
                                                        "The workspace containing the layer"),
                                                "layer",
                                                "string",
                                                "The layer to query"),
                                        "filter",
                                        "string",
                                        "Optional CQL filter expression"),
                                "properties",
                                "string",
                                "Optional list of properties to return"),
                        "max_features",
                        "integer",
                        "Maximum number of features to return"),
                args -> catalog.queryFeatures(
                        JsonArgs.requireText(args, "workspace"),
                        JsonArgs.requireText(args, "layer"),
                        JsonArgs.optionalText(args, "filter"),
                        JsonArgs.optionalStringList(args, "properties"),
                        JsonArgs.optionalInt(args, "max_features", 10)));

        read(
                "generate_map",
                "Generate a map image using WMS GetMap.",
                Schemas.prop(
                        Schemas.prop(
                                Schemas.prop(
                                        Schemas.arrayProp(
                                                Schemas.arrayProp(
                                                        Schemas.arrayProp(Schemas.object("layers"), "layers", "string", "Layers to include (workspace:layer)"),
                                                        "styles",
                                                        "string",
                                                        "Optional styles to apply (one per layer)"),
                                                "bbox",
                                                "number",
                                                "Bounding box [minx, miny, maxx, maxy]"),
                                        "width",
                                        "integer",
                                        "Image width in pixels"),
                                "height",
                                "integer",
                                "Image height in pixels"),
                        "format",
                        "string",
                        "Image format (png, jpeg, gif, tiff, pdf)"),
                args -> catalog.generateMap(
                        JsonArgs.requireStringList(args, "layers"),
                        JsonArgs.optionalStringList(args, "styles"),
                        JsonArgs.optionalDoubleList(args, "bbox"),
                        JsonArgs.optionalInt(args, "width", 800),
                        JsonArgs.optionalInt(args, "height", 600),
                        defaultText(args, "format", "png")));

        read(
                "get_datastores",
                "List all datastores in the given workspace.",
                Schemas.prop(Schemas.object("workspace"), "workspace", "string", "Workspace"),
                args -> catalog.getDatastores(JsonArgs.requireText(args, "workspace")));
        read(
                "get_datastore",
                "Get a specific datastore by name.",
                Schemas.prop(
                        Schemas.prop(Schemas.object("workspace", "name"), "workspace", "string", "Workspace name"),
                        "name",
                        "string",
                        "Datastore name"),
                args -> catalog.getDatastore(JsonArgs.requireText(args, "workspace"), JsonArgs.requireText(args, "name")));
        read(
                "get_coveragestores",
                "Get all coveragestores in a workspace.",
                Schemas.prop(Schemas.object("workspace"), "workspace", "string", "Name of the workspace"),
                args -> catalog.getCoveragestores(JsonArgs.requireText(args, "workspace")));
        read(
                "get_coveragestore",
                "Get details about a single coveragestore.",
                Schemas.prop(
                        Schemas.prop(Schemas.object("workspace", "name"), "workspace", "string", "Workspace"),
                        "name",
                        "string",
                        "Coveragestore"),
                args -> catalog.getCoveragestore(JsonArgs.requireText(args, "workspace"), JsonArgs.requireText(args, "name")));
        read(
                "get_layergroups",
                "List all layer groups in a workspace.",
                Schemas.prop(Schemas.object("workspace"), "workspace", "string", "Workspace"),
                args -> catalog.getLayerGroups(JsonArgs.requireText(args, "workspace")));
        read(
                "get_layergroup",
                "Get a layer group from a workspace.",
                Schemas.prop(
                        Schemas.prop(Schemas.object("workspace", "name"), "workspace", "string", "Workspace to search"),
                        "name",
                        "string",
                        "Name of the group"),
                args -> catalog.getLayerGroup(JsonArgs.requireText(args, "workspace"), JsonArgs.requireText(args, "name")));
        read(
                "get_featuretypes",
                "List all feature types in a given store.",
                Schemas.prop(
                        Schemas.prop(Schemas.object("workspace", "store_name"), "workspace", "string", "Workspace"),
                        "store_name",
                        "string",
                        "Store name"),
                args -> catalog.getFeatureTypes(
                        JsonArgs.requireText(args, "workspace"), JsonArgs.requireText(args, "store_name")));
        read(
                "get_feature_attribute",
                "Get feature attribute schema/details.",
                Schemas.prop(
                        Schemas.prop(
                                Schemas.prop(
                                        Schemas.object("workspace", "store_name", "featuretype"),
                                        "workspace",
                                        "string",
                                        "Workspace"),
                                "store_name",
                                "string",
                                "Store containing layer"),
                        "featuretype",
                        "string",
                        "The layer or featuretype name"),
                args -> catalog.getFeatureAttributes(
                        JsonArgs.requireText(args, "workspace"),
                        JsonArgs.requireText(args, "store_name"),
                        JsonArgs.requireText(args, "featuretype")));
        read("get_version", "Fetch GeoServer version string.", Schemas.object(), args -> catalog.getVersion());

        write(
                "create_workspace",
                "Create a new workspace in GeoServer.",
                Schemas.prop(Schemas.object("workspace"), "workspace", "string", "Name of the workspace to create"),
                args -> catalog.createWorkspace(JsonArgs.requireText(args, "workspace")));
        write(
                "create_layer",
                "Create a new layer in GeoServer.",
                Schemas.prop(
                        Schemas.prop(
                                Schemas.prop(
                                        Schemas.prop(
                                                Schemas.object("workspace", "layer", "data_store", "source"),
                                                "workspace",
                                                "string",
                                                "The workspace for the new layer"),
                                        "layer",
                                        "string",
                                        "The name of the layer to create"),
                                "data_store",
                                "string",
                                "The data store to use"),
                        "source",
                        "string",
                        "The source data (file, table name, etc.)"),
                args -> catalog.createLayer(
                        JsonArgs.requireText(args, "workspace"),
                        JsonArgs.requireText(args, "layer"),
                        JsonArgs.requireText(args, "data_store"),
                        JsonArgs.requireText(args, "source")));
        write(
                "delete_resource",
                "Delete a resource from GeoServer.",
                Schemas.prop(
                        Schemas.prop(
                                Schemas.prop(
                                        Schemas.object("resource_type", "workspace", "name"),
                                        "resource_type",
                                        "string",
                                        "Type of resource to delete (workspace, layer, datastore, style, coverage)"),
                                "workspace",
                                "string",
                                "The workspace containing the resource"),
                        "name",
                        "string",
                        "The name of the resource"),
                args -> catalog.deleteResource(
                        JsonArgs.requireText(args, "resource_type"),
                        JsonArgs.requireText(args, "workspace"),
                        JsonArgs.requireText(args, "name")));
        write(
                "create_style",
                "Create a new SLD style in GeoServer.",
                Schemas.prop(
                        Schemas.prop(
                                Schemas.prop(Schemas.object("name", "sld"), "name", "string", "Name for the style"),
                                "sld",
                                "string",
                                "SLD XML content"),
                        "workspace",
                        "string",
                        "Optional workspace for the style"),
                args -> catalog.createStyle(
                        JsonArgs.requireText(args, "name"),
                        JsonArgs.requireText(args, "sld"),
                        JsonArgs.optionalText(args, "workspace")));
        write(
                "create_datastore",
                "Create a new datastore in the given workspace.",
                datastoreSchema(),
                args -> catalog.createDatastore(
                        JsonArgs.requireText(args, "workspace"),
                        JsonArgs.requireText(args, "name"),
                        JsonArgs.optionalStringMap(args, "params")));
        write(
                "create_featurestore",
                "Create a new featurestore in the given workspace.",
                datastoreSchema(),
                args -> catalog.createDatastore(
                        JsonArgs.requireText(args, "workspace"),
                        JsonArgs.requireText(args, "name"),
                        JsonArgs.optionalStringMap(args, "params")));
        write(
                "create_coveragestore",
                "Create a new coveragestore in a workspace.",
                datastoreSchema(),
                args -> catalog.createCoveragestore(
                        JsonArgs.requireText(args, "workspace"),
                        JsonArgs.requireText(args, "name"),
                        JsonArgs.optionalStringMap(args, "params")));
        write(
                "delete_coveragestore",
                "Delete a coveragestore from a workspace.",
                Schemas.prop(
                        Schemas.prop(Schemas.object("workspace", "name"), "workspace", "string", "Workspace name"),
                        "name",
                        "string",
                        "Coveragestore name"),
                args -> catalog.deleteCoveragestore(
                        JsonArgs.requireText(args, "workspace"), JsonArgs.requireText(args, "name")));
        write(
                "create_layergroup",
                "Create a new layer group with specific layers and (optionally) styles.",
                Schemas.arrayProp(
                        Schemas.arrayProp(
                                Schemas.prop(
                                        Schemas.prop(
                                                Schemas.object("workspace", "name", "layers"),
                                                "workspace",
                                                "string",
                                                "The workspace for the group"),
                                        "name",
                                        "string",
                                        "Name of the layer group"),
                                "layers",
                                "string",
                                "List of layers to include"),
                        "styles",
                        "string",
                        "List of styles for layers"),
                args -> catalog.createLayerGroup(
                        JsonArgs.requireText(args, "workspace"),
                        JsonArgs.requireText(args, "name"),
                        JsonArgs.requireStringList(args, "layers"),
                        JsonArgs.optionalStringList(args, "styles")));
        write(
                "add_layer_to_layergroup",
                "Add a specific layer to a layer group.",
                Schemas.prop(
                        Schemas.prop(
                                Schemas.prop(
                                        Schemas.prop(
                                                Schemas.object("layer_name", "layer_workspace", "layergroup_name"),
                                                "layer_name",
                                                "string",
                                                "Layer to add"),
                                        "layer_workspace",
                                        "string",
                                        "Workspace for the layer"),
                                "layergroup_name",
                                "string",
                                "Target group name"),
                        "layergroup_workspace",
                        "string",
                        "Workspace for the group"),
                args -> catalog.addLayerToLayerGroup(
                        JsonArgs.requireText(args, "layer_name"),
                        JsonArgs.requireText(args, "layer_workspace"),
                        JsonArgs.requireText(args, "layergroup_name"),
                        JsonArgs.optionalText(args, "layergroup_workspace")));
        write(
                "remove_layer_from_layergroup",
                "Remove a layer from a group.",
                Schemas.prop(
                        Schemas.prop(
                                Schemas.prop(
                                        Schemas.prop(
                                                Schemas.object("layer_name", "layer_workspace", "layergroup_name"),
                                                "layer_name",
                                                "string",
                                                "Layer"),
                                        "layer_workspace",
                                        "string",
                                        "Layer workspace"),
                                "layergroup_name",
                                "string",
                                "Group"),
                        "layergroup_workspace",
                        "string",
                        "Group workspace"),
                args -> catalog.removeLayerFromLayerGroup(
                        JsonArgs.requireText(args, "layer_name"),
                        JsonArgs.requireText(args, "layer_workspace"),
                        JsonArgs.requireText(args, "layergroup_name"),
                        JsonArgs.optionalText(args, "layergroup_workspace")));
        write(
                "delete_layergroup",
                "Delete a layer group from a workspace.",
                Schemas.prop(
                        Schemas.prop(Schemas.object("workspace", "name"), "workspace", "string", "Workspace"),
                        "name",
                        "string",
                        "Group to delete"),
                args -> catalog.deleteLayerGroup(
                        JsonArgs.requireText(args, "workspace"), JsonArgs.requireText(args, "name")));
        write(
                "update_layergroup",
                "Update a layer group's details and configuration.",
                Schemas.arrayProp(
                        Schemas.prop(
                                Schemas.prop(
                                        Schemas.prop(
                                                Schemas.object("layergroup_name"),
                                                "layergroup_name",
                                                "string",
                                                "The group to update"),
                                        "title",
                                        "string",
                                        "New title"),
                                "abstract_text",
                                "string",
                                "Abstract/description"),
                        "keywords",
                        "string",
                        "Associated keywords"),
                args -> catalog.updateLayerGroup(
                        JsonArgs.requireText(args, "layergroup_name"),
                        JsonArgs.optionalText(args, "title"),
                        JsonArgs.optionalText(args, "abstract_text"),
                        JsonArgs.optionalStringList(args, "keywords")));
        write(
                "publish_featurestore",
                "Publish an existing featurestore.",
                Schemas.objectProp(
                        Schemas.prop(
                                Schemas.prop(
                                        Schemas.object("workspace", "store_name", "params"),
                                        "workspace",
                                        "string",
                                        "Target workspace"),
                                "store_name",
                                "string",
                                "Featurestore name"),
                        "params",
                        "Publication settings (nativeName/table/name, title, srs)"),
                args -> catalog.publishFeaturestore(
                        JsonArgs.requireText(args, "workspace"),
                        JsonArgs.requireText(args, "store_name"),
                        JsonArgs.optionalStringMap(args, "params")));
        write(
                "edit_featuretype",
                "Edit the settings of a feature type in a store.",
                Schemas.objectProp(
                        Schemas.prop(
                                Schemas.prop(
                                        Schemas.prop(
                                                Schemas.object("workspace", "store_name", "featuretype"),
                                                "workspace",
                                                "string",
                                                "Workspace containing store"),
                                        "store_name",
                                        "string",
                                        "Store name"),
                                "featuretype",
                                "string",
                                "Feature type to modify"),
                        "updates",
                        "Updatable attributes (title, abstract, srs, enabled, advertised, nativeName)"),
                args -> catalog.editFeatureType(
                        JsonArgs.requireText(args, "workspace"),
                        JsonArgs.requireText(args, "store_name"),
                        JsonArgs.requireText(args, "featuretype"),
                        JsonArgs.optionalStringMap(args, "updates")));
        write(
                "publish_style",
                "Assign/publish a style to a layer.",
                Schemas.prop(
                        Schemas.prop(
                                Schemas.prop(
                                        Schemas.object("layer_name", "style_name", "workspace"),
                                        "layer_name",
                                        "string",
                                        "The target layer"),
                                "style_name",
                                "string",
                                "The style to apply"),
                        "workspace",
                        "string",
                        "Workspace context"),
                args -> catalog.publishStyle(
                        JsonArgs.requireText(args, "layer_name"),
                        JsonArgs.requireText(args, "style_name"),
                        JsonArgs.requireText(args, "workspace")));
    }

    public ToolRegistry(List<McpTool> tools) {
        for (McpTool tool : tools) {
            register(tool);
        }
    }

    public final void register(McpTool tool) {
        tools.put(tool.name(), tool);
    }

    public McpTool get(String name) {
        return tools.get(name);
    }

    public List<McpTool> list() {
        return new ArrayList<>(tools.values());
    }

    private void read(String name, String description, ObjectNode schema, Function<JsonNode, Object> handler) {
        register(new LambdaTool(name, description, schema, handler, false));
    }

    private void write(String name, String description, ObjectNode schema, Function<JsonNode, Object> handler) {
        register(new LambdaTool(name, description, schema, handler, true));
    }

    private static ObjectNode datastoreSchema() {
        return Schemas.objectProp(
                Schemas.prop(
                        Schemas.prop(Schemas.object("workspace", "name", "params"), "workspace", "string", "Workspace name"),
                        "name",
                        "string",
                        "Store name"),
                "params",
                "Connection parameters (host, dbtype, url, type, etc.)");
    }

    private static String defaultText(JsonNode args, String field, String fallback) {
        String value = JsonArgs.optionalText(args, field);
        return value == null ? fallback : value;
    }
}
