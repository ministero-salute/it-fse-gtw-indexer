package it.finanze.sanita.fse2.ms.gtwindexer.dto.response;

import it.finanze.sanita.fse2.ms.gtwindexer.dto.AbstractDTO;
import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author CPIERASC
 *
 *	Base response.
 */
@Getter
@Setter
public class ResponseDTO extends AbstractDTO {

	/**
	 * Serial version uid.
	 */
	private static final long serialVersionUID = 3473312771318529328L;

	/**
	 * Trace id log.
	 */
	private String traceID;
	
	/**
	 * Span id log.
	 */
	private String spanID;
	
	/**
	 * Instantiates a new response DTO.
	 */
	public ResponseDTO() {
	}

	/**
	 * Instantiates a new response DTO.
	 *
	 * @param traceInfo the trace info
	 */
	public ResponseDTO(final LogTraceInfoDTO traceInfo) {
		traceID = traceInfo.getTraceID();
		spanID = traceInfo.getSpanID(); 
	}
	
}
