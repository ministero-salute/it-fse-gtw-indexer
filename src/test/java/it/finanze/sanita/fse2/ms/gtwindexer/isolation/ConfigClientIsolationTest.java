package it.finanze.sanita.fse2.ms.gtwindexer.isolation;

import static it.finanze.sanita.fse2.ms.gtwindexer.enums.ConfigItemTypeEnum.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import it.finanze.sanita.fse2.ms.gtwindexer.client.impl.ConfigClient;
import it.finanze.sanita.fse2.ms.gtwindexer.client.routes.ConfigClientRoutes;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.ConfigItemDTO;

@Tag("ISOLATION")
class ConfigClientIsolationTest {
    private ConfigClient client;
    private ConfigClientRoutes routes;
    private RestTemplate rest;

    @BeforeEach
    void setUp() {
        client = new ConfigClient();
        routes = new ConfigClientRoutes();
        rest = mock(RestTemplate.class);
        ReflectionTestUtils.setField(routes, "configHost", "http://config.invalid");
        ReflectionTestUtils.setField(client, "routes", routes);
        ReflectionTestUtils.setField(client, "client", rest);
    }

    @Test
    void retrievesTypedConfigurationUsingRealRoutes() {
        ConfigItemDTO expected = new ConfigItemDTO();
        String url = "http://config.invalid/v1/config-items?type=INDEXER";
        assertEquals(url, routes.getConfigItems(INDEXER));
        when(rest.getForObject(url, ConfigItemDTO.class)).thenReturn(expected);
        assertSame(expected, client.getConfigurationItems(INDEXER));
        verify(rest).getForObject(url, ConfigItemDTO.class);
    }

    @Test
    void prefersSpecificPropertyWithoutGenericLookup() {
        when(rest.getForObject(routes.getConfigItem(INDEXER, "key"), String.class)).thenReturn("specific");
        assertEquals("specific", client.getProps("key", "old", INDEXER));
        verify(rest, never()).getForObject(routes.getConfigItem(GENERIC, "key"), String.class);
    }

    @Test
    void fallsBackToGenericProperty() {
        when(rest.getForObject(routes.getConfigItem(GENERIC, "key"), String.class)).thenReturn("generic");
        assertEquals("generic", client.getProps("key", "old", INDEXER));
        verify(rest).getForObject(routes.getConfigItem(INDEXER, "key"), String.class);
        verify(rest).getForObject(routes.getConfigItem(GENERIC, "key"), String.class);
    }

    @Test
    void keepsPreviousValueWhenConfigIsOffline() {
        when(rest.getForEntity(routes.status(), String.class)).thenThrow(new ResourceAccessException("offline"));
        assertEquals("cached", client.getProps("key", "cached", INDEXER));
        verify(rest, never()).getForObject(anyString(), eq(String.class));
    }

    @Test
    void returnsNullWhenNeitherScopeDefinesProperty() {
        assertNull(client.getProps("missing", "old", INDEXER));
    }
}
