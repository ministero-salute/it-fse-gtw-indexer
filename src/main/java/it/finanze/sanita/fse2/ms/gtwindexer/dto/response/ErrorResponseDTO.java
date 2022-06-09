package it.finanze.sanita.fse2.ms.gtwindexer.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.AbstractDTO;
import lombok.Data;


/**
 * The Class ErrorResponseDTO.
 *
 * @author vincenzoingenito
 * 
 * 	Error response.
 */
@Data
public class ErrorResponseDTO extends AbstractDTO {

	/**
	 * Serial version uid.
	 */
	private static final long serialVersionUID = 26011590310238806L;

	/**
	 * Trace id log.
	 */
	@Schema(description = "Indentificativo univoco della richiesta dell'utente")
	private String traceID;
	
	/**
	 * Span id log.
	 */
	@Schema(description = "Indentificativo univoco di un task della richiesta dell'utente (differisce dal traceID solo in caso di chiamate sincrone in cascata)")
	private String spanID;

	@Schema(description = "Identificativo del problema verificatosi")
	private String type;
	
	@Schema(description = "Sintesi del problema (invariante per occorrenze diverse dello stesso problema)")
	private String title;

	@Schema(description = "Descrizione del problema")
	private String detail;

	@Schema(description = "Stato http")
	private Integer status;
	
	@Schema(description = "URI che potrebbe fornire ulteriori informazioni riguardo l'occorrenza del problema")
	private String instance;

	public ErrorResponseDTO(final LogTraceInfoDTO traceInfo, final String inType, final String inTitle, final String inDetail, final Integer inStatus, final String inInstance) {
		traceID = traceInfo.getTraceID();
		spanID = traceInfo.getSpanID();
		type = inType;
		title = inTitle;
		detail = inDetail;
		status = inStatus;
		instance = inInstance;
	}

	public ErrorResponseDTO(final LogTraceInfoDTO traceInfo) {
		traceID = traceInfo.getTraceID();
		spanID = traceInfo.getSpanID(); 
	}

}
