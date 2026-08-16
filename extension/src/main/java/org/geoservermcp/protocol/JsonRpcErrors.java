package org.geoservermcp.protocol;

/** JSON-RPC 2.0 error codes plus HTTP status used by the Streamable HTTP binding. */
public final class JsonRpcErrors {

    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    private JsonRpcErrors() {}
}
