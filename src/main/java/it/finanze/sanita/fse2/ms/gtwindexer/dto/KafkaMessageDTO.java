/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package  it.finanze.sanita.fse2.ms.gtwindexer.dto;

import lombok.Builder;
import lombok.Getter;
/**
 * Content of a Kafka message
 */
@Getter
@Builder
public class KafkaMessageDTO extends AbstractDTO {

    /**
     * Message.
     */
    private String message;

         
}
