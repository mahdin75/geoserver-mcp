package org.geoservermcp.web;

import javax.servlet.http.HttpServletRequest;

/** Per-request GeoServer public URL, used to build WMS GetMap links. */
public final class RequestBaseUrl {

    private static final ThreadLocal<String> BASE = new ThreadLocal<>();

    private RequestBaseUrl() {}

    public static void set(HttpServletRequest request) {
        if (request == null) {
            BASE.remove();
            return;
        }
        StringBuilder url = new StringBuilder();
        url.append(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        if (port > 0 && port != 80 && port != 443) {
            url.append(':').append(port);
        }
        String context = request.getContextPath();
        if (context != null) {
            url.append(context);
        }
        BASE.set(url.toString());
    }

    public static void clear() {
        BASE.remove();
    }

    public static String get() {
        String value = BASE.get();
        return value == null || value.isBlank() ? "" : value;
    }
}
