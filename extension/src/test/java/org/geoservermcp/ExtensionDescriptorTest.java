package org.geoservermcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class ExtensionDescriptorTest {

    @Test
    void applicationContextDeclaresFilterAndModuleStatus() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/applicationContext.xml")) {
            assertNotNull(in, "applicationContext.xml must be on the classpath so GeoServer can load the extension");
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
            assertEquals("beans", document.getDocumentElement().getNodeName());
            assertTrue(document.getElementsByTagName("bean").getLength() >= 5);
            String xml = new String(getClass().getResourceAsStream("/applicationContext.xml").readAllBytes());
            assertTrueContains(xml, "mcpGeoServerFilter");
            assertTrueContains(xml, "org.geoservermcp.web.McpGeoServerFilter");
            assertTrueContains(xml, "mcpCatalogAccess");
            assertTrueContains(xml, "org.geoservermcp.catalog.GeoServerCatalogAccess");
            assertTrueContains(xml, "ref=\"catalog\"");
            assertTrueContains(xml, "ref=\"geoServer\"");
            assertTrueContains(xml, "gs-mcp-status");
            assertTrueContains(xml, "org.geoserver.platform.ModuleStatusImpl");
        }
    }

    private static void assertTrueContains(String xml, String value) {
        if (!xml.contains(value)) {
            throw new AssertionError("applicationContext.xml should contain: " + value);
        }
    }
}
