package it.finanze.sanita.fse2.ms.gtwindexer.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.*;

import it.finanze.sanita.fse2.ms.gtwindexer.client.impl.IniClient;
import it.finanze.sanita.fse2.ms.gtwindexer.config.MicroservicesURLCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IniDeleteRequestDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IniMetadataUpdateReqDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniTraceResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.*;

@Tag("ISOLATION")
class IniClientIsolationTest {
    private IniClient client;
    private RestTemplate rest;
    private IniTraceResponseDTO response;

    @BeforeEach
    void setUp() {
        client = new IniClient();
        rest = mock(RestTemplate.class);
        MicroservicesURLCFG urls = mock(MicroservicesURLCFG.class);
        when(urls.getIniClientHost()).thenReturn("http://ini.invalid");
        ReflectionTestUtils.setField(client, "restTemplate", rest);
        ReflectionTestUtils.setField(client, "msUrlCFG", urls);
        response = new IniTraceResponseDTO();
        response.setEsito(true);
    }

    @Test
    void publicationPostsWorkflowAsJson() {
        when(rest.postForObject(eq("http://ini.invalid/v1/ini-publish"), any(), eq(IniTraceResponseDTO.class)))
                .thenReturn(response);
        assertSame(response, client.sendPublicationData("workflow"));
        ArgumentCaptor<HttpEntity<?>> entity = ArgumentCaptor.forClass(HttpEntity.class);
        verify(rest).postForObject(eq("http://ini.invalid/v1/ini-publish"), entity.capture(), eq(IniTraceResponseDTO.class));
        assertEquals("workflow", entity.getValue().getBody());
        assertEquals(MediaType.APPLICATION_JSON, entity.getValue().getHeaders().getContentType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"replace", "delete", "update"})
    void sendsCorrectMethodEndpointAndBody(String operation) {
        HttpMethod method = operation.equals("delete") ? HttpMethod.DELETE : HttpMethod.PUT;
        String endpoint = "http://ini.invalid/v1/ini-" + operation;
        when(rest.exchange(eq(endpoint), eq(method), any(HttpEntity.class), eq(IniTraceResponseDTO.class)))
                .thenReturn(ResponseEntity.ok(response));
        Object body;
        IniTraceResponseDTO actual;
        if (operation.equals("replace")) {
            body = "workflow";
            actual = client.sendReplaceData((String) body);
        } else if (operation.equals("delete")) {
            body = new IniDeleteRequestDTO();
            actual = client.delete((IniDeleteRequestDTO) body);
        } else {
            body = new IniMetadataUpdateReqDTO("document", null);
            actual = client.sendUpdateData((IniMetadataUpdateReqDTO) body);
        }
        assertSame(response, actual);
        ArgumentCaptor<HttpEntity<?>> entity = ArgumentCaptor.forClass(HttpEntity.class);
        verify(rest).exchange(eq(endpoint), eq(method), entity.capture(), eq(IniTraceResponseDTO.class));
        assertSame(body, entity.getValue().getBody());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 500})
    void mapsHttpErrorsAndPreservesCause(int status) {
        HttpStatusCodeException failure = status == 500
                ? new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)
                : new HttpClientErrorException(HttpStatus.valueOf(status));
        when(rest.postForObject(anyString(), any(), eq(IniTraceResponseDTO.class))).thenThrow(failure);
        Class<? extends RuntimeException> expected = status == 404 ? NoRecordFoundException.class
                : status == 500 ? BlockingIniException.class : BusinessException.class;
        assertSame(failure, assertThrows(expected, () -> client.sendPublicationData("workflow")).getCause());
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 503})
    void replacementMapsServerErrors(int status) {
        HttpServerErrorException failure = new HttpServerErrorException(HttpStatus.valueOf(status));
        when(rest.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniTraceResponseDTO.class)))
                .thenThrow(failure);
        Class<? extends RuntimeException> expected = status == 500 ? BlockingIniException.class : BusinessException.class;
        assertSame(failure, assertThrows(expected, () -> client.sendReplaceData("workflow")).getCause());
    }

    @ParameterizedTest
    @ValueSource(strings = {"replace", "delete", "update"})
    void mapsExchangeNotFoundErrors(String operation) {
        HttpClientErrorException failure = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        when(rest.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(IniTraceResponseDTO.class)))
                .thenThrow(failure);
        assertSame(failure, assertThrows(NoRecordFoundException.class, () -> {
            switch (operation) {
                case "replace" -> client.sendReplaceData("workflow");
                case "delete" -> client.delete(new IniDeleteRequestDTO());
                default -> client.sendUpdateData(new IniMetadataUpdateReqDTO("document", null));
            }
        }).getCause());
    }

    @ParameterizedTest
    @ValueSource(strings = {"publish", "replace", "delete", "update"})
    void propagatesConnectionFailure(String operation) {
        ResourceAccessException failure = new ResourceAccessException("offline");
        if (operation.equals("publish")) {
            when(rest.postForObject(anyString(), any(), eq(IniTraceResponseDTO.class))).thenThrow(failure);
        } else {
            when(rest.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(IniTraceResponseDTO.class)))
                    .thenThrow(failure);
        }
        assertSame(failure, assertThrows(ResourceAccessException.class, () -> {
            switch (operation) {
                case "publish" -> client.sendPublicationData("workflow");
                case "replace" -> client.sendReplaceData("workflow");
                case "delete" -> client.delete(new IniDeleteRequestDTO());
                default -> client.sendUpdateData(new IniMetadataUpdateReqDTO("document", null));
            }
        }));
    }
}
