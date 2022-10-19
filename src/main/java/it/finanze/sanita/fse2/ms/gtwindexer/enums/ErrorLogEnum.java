/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.enums;

import lombok.Getter;

@Getter
public enum ErrorLogEnum implements ILogEnum {

	KO_INI("KO-VAL", "Errore nella chiamata a INI"),
	KO_INVALID_DATA("KO-INV-DATA", "Errore: dati di input non validi");

	private String code;
	
	public String getCode() {
		return code;
	}

	private String description;

	private ErrorLogEnum(String inCode, String inDescription) {
		code = inCode;
		description = inDescription;
	}

	public String getDescription() {
		return description;
	}

}

