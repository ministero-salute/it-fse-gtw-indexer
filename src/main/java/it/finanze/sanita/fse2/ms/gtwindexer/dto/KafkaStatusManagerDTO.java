package it.finanze.sanita.fse2.ms.gtwindexer.dto;

import java.util.Date;

import it.finanze.sanita.fse2.ms.gtwindexer.enums.EventStatusEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.EventTypeEnum;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper=true)
public class KafkaStatusManagerDTO extends AbstractDTO {

	/**
	 * Serial version uid.
	 */
	private static final long serialVersionUID = 7080680277816570116L;
	
	private EventTypeEnum eventType;
	
	private Date eventDate;
	
	private EventStatusEnum eventStatus;
	
	private String message;
}
 