/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum AccreditamentoPrefixEnum {

	CRASH_INI("CRASH_INI");
	
	private String prefix;
	 
	
	public static AccreditamentoPrefixEnum get(String inPrefix) {
		AccreditamentoPrefixEnum out = null;
		for (AccreditamentoPrefixEnum v: AccreditamentoPrefixEnum.values()) {
			if (v.getPrefix().equalsIgnoreCase(inPrefix)) {
				out = v;
				break;
			}
		}
		return out;
	}

	public static AccreditamentoPrefixEnum getStartWith(String inPrefix) {
		AccreditamentoPrefixEnum out = null;
		for (AccreditamentoPrefixEnum v: AccreditamentoPrefixEnum.values()) {
			if(inPrefix.startsWith(v.getPrefix())) {
				out = v;
				break;
				}
			}
		return out;
	}

}