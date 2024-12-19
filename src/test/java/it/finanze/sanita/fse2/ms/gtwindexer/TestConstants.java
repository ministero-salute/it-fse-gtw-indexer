
/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * 
 * Copyright (C) 2023 Ministero della Salute
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
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
