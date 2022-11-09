/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer;

import com.google.gson.Gson;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IniDeleteRequestDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniTraceResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.LogTraceInfoDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.messaging.MessageHeaders;

import java.util.AbstractMap;
import java.util.HashMap;

public class TestConstants {
    public final static String testWorkflowInstanceId = "yYuj7thDLqRbaXC7eYcLz57ui5m5PfQwcBDTjKgHcuhKkxrnB7HqJ3lfXpKS//iboyzEFP/CsTVrtStjyZCLtletgD3GhzP3e3RbUBn7fWuQNzPO2/ndTt4TnIr0UalGC3uLEfqWHSQGm8QVx+Tr2YOWbedciKX3+wTwmkz3hm5Jck7CdSzA2ecP5i6U9oFs/uKLMz8lGk1CM9B2ESk5hCPBU+9lTScY3NsNUZLJwWiJijIt2cM3qtOKYxqYj70Z";
    public final static String testDocumentId = "2.16.840.1.113883.2.9.2.110.4.4";

    public final static String EMPTY_JSON = "";

    public final static IniTraceResponseDTO SUCCESS_RESPONSE_INI_DTO = new IniTraceResponseDTO(
        new LogTraceInfoDTO("test", "test"),
        true,
        ""
    );

    public final static IniTraceResponseDTO FAILURE_RESPONSE_INI_DTO = new IniTraceResponseDTO(
        new LogTraceInfoDTO("test", "test"),
        false,
        "KaBoom!"
    );

    public final static IniDeleteRequestDTO DELETE_REQUEST_DTO = new IniDeleteRequestDTO(
        testDocumentId,
        "uuid",
        "integrity:S1#110201234567XX",
        "SSSMNN75B01F257L^^^&amp;2.16.840.1.113883.2.9.4.3.2&amp;ISO",
        "110",
        "Regione Marche",
        "110",
        "APR",
        "GTWGWY82B42G920M",
        "TREATMENT",
        "('11502-2^^2.16.840.1.113883.6.1')",
        "READ",
        true
    );

    public static AbstractMap.SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> getFakeRetryRequest(String topic, String json) {
        return new AbstractMap.SimpleImmutableEntry<>(
            new ConsumerRecord<>(
                topic,
                1,
                0,
                testWorkflowInstanceId,
                json
            ),
            new MessageHeaders(new HashMap<>())
        );
    }

    public static String getFakeDeleteRequest() {
        return new Gson().toJson(DELETE_REQUEST_DTO);
    }

    private TestConstants() {}
}
