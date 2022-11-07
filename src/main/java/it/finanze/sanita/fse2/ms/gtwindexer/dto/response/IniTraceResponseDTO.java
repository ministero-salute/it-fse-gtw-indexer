/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IniTraceResponseDTO extends ResponseDTO {

	/**
	 * Serial version uid.
	 */
	private static final long serialVersionUID = 5457503502983726876L;

	private Boolean esito;

	private String errorMessage;

	public IniTraceResponseDTO() {
		super();
	}

	public IniTraceResponseDTO(final LogTraceInfoDTO traceInfo, final Boolean inEsito, final String inErrorMessage) {
		super(traceInfo);
		esito = inEsito;
		errorMessage = inErrorMessage;
	}

}
