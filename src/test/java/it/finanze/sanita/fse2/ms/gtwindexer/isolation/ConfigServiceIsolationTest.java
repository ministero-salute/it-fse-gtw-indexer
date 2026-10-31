package it.finanze.sanita.fse2.ms.gtwindexer.isolation;

import static it.finanze.sanita.fse2.ms.gtwindexer.client.routes.base.ClientRoutes.Config.PROPS_NAME_REMOVE_EDS_ENABLE;
import static it.finanze.sanita.fse2.ms.gtwindexer.enums.ConfigItemTypeEnum.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import it.finanze.sanita.fse2.ms.gtwindexer.client.routes.IConfigClient;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.ConfigItemDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.service.impl.ConfigSRV;
import it.finanze.sanita.fse2.ms.gtwindexer.utility.ProfileUtility;

@Tag("ISOLATION")
class ConfigServiceIsolationTest {
    private ConfigSRV service;
    private IConfigClient client;
    private ProfileUtility profiles;

    @BeforeEach
    void setUp() {
        service = new ConfigSRV();
        client = mock(IConfigClient.class);
        profiles = mock(ProfileUtility.class);
        ReflectionTestUtils.setField(service, "client", client);
        ReflectionTestUtils.setField(service, "profiles", profiles);
        ReflectionTestUtils.setField(service, "refreshRate", Long.MAX_VALUE);
    }

    @Test
    void specificPropertiesOverrideGenericValuesAndAreCached() {
        when(client.getConfigurationItems(GENERIC)).thenReturn(items(Map.of(PROPS_NAME_REMOVE_EDS_ENABLE, "false")));
        when(client.getConfigurationItems(INDEXER)).thenReturn(items(Map.of(PROPS_NAME_REMOVE_EDS_ENABLE, "true")));
        service.postConstruct();
        assertTrue(service.isRemoveEds());
        assertEquals(Long.MAX_VALUE, service.getRefreshRate());
        verify(client, never()).getProps(anyString(), any(), any());
    }

    @Test
    void expiredCacheRefreshesUsingPreviousValue() {
        when(client.getConfigurationItems(any())).thenReturn(items(Map.of(PROPS_NAME_REMOVE_EDS_ENABLE, "true")));
        service.postConstruct();
        ReflectionTestUtils.setField(service, "refreshRate", 0L);
        when(client.getProps(PROPS_NAME_REMOVE_EDS_ENABLE, "true", INDEXER)).thenReturn("false");
        assertFalse(service.isRemoveEds());
        verify(client).getProps(PROPS_NAME_REMOVE_EDS_ENABLE, "true", INDEXER);
    }

    @Test
    void rejectsMissingRequiredProperty() {
        when(client.getConfigurationItems(any())).thenReturn(items(Map.of()));
        assertTrue(assertThrows(IllegalStateException.class, service::postConstruct)
                .getMessage().contains(PROPS_NAME_REMOVE_EDS_ENABLE));
    }

    @Test
    void skipsRemoteInitializationForTestProfile() {
        when(profiles.isTestProfile()).thenReturn(true);
        service.postConstruct();
        verifyNoInteractions(client);
    }

    @Test
    void propagatesInitializationFailure() {
        RuntimeException failure = new RuntimeException("configuration unavailable");
        when(client.getConfigurationItems(GENERIC)).thenThrow(failure);
        assertSame(failure, assertThrows(RuntimeException.class, service::postConstruct));
    }

    private ConfigItemDTO items(Map<String, String> values) {
        ConfigItemDTO dto = new ConfigItemDTO();
        ConfigItemDTO.ConfigDataItemDTO data = new ConfigItemDTO.ConfigDataItemDTO();
        data.setItems(values);
        dto.setConfigurationItems(values.isEmpty() ? List.of() : List.of(data));
        return dto;
    }
}
