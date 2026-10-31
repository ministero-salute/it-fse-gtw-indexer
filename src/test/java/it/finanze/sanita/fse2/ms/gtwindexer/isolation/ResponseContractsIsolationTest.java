package it.finanze.sanita.fse2.ms.gtwindexer.isolation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.*;

@Tag("ISOLATION")
class ResponseContractsIsolationTest {
    @Test
    void errorResponseSerializesProblemAndTraceFields() throws Exception {
        var trace = new LogTraceInfoDTO("span", "trace");
        var response = new ErrorResponseDTO(trace, "urn:error", "Rejected", "Invalid document", 400, "/documents/1");
        var json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(response));
        assertEquals("trace", json.get("traceID").asText());
        assertEquals("span", json.get("spanID").asText());
        assertEquals("urn:error", json.get("type").asText());
        assertEquals("Rejected", json.get("title").asText());
        assertEquals("Invalid document", json.get("detail").asText());
        assertEquals(400, json.get("status").asInt());
        assertEquals("/documents/1", json.get("instance").asText());
        var minimal = new ErrorResponseDTO(trace);
        assertEquals("trace", minimal.getTraceID());
        assertEquals("span", minimal.getSpanID());
        assertNull(minimal.getDetail());
    }

    @Test
    void iniResponseCarriesTraceResultAndMessage() {
        var trace = new LogTraceInfoDTO("span", "trace");
        var response = new IniTraceResponseDTO(trace, false, "rejected");
        assertEquals("trace", response.getTraceID());
        assertEquals("span", response.getSpanID());
        assertFalse(response.getEsito());
        assertEquals("rejected", response.getMessage());
        assertNull(response.getMockEds());
    }
}
