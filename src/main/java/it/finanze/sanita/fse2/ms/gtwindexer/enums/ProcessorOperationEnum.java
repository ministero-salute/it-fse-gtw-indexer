package it.finanze.sanita.fse2.ms.gtwindexer.enums;

import lombok.Getter;

@Getter
public enum ProcessorOperationEnum {

	PUBLISH("PUBLISH"),
	DELETE("DELETE"),
	REPLACE("REPLACE"),
	UPDATE("UPDATE"),
	READ("READ");

	private String name;

	ProcessorOperationEnum(String pname) {
		name = pname;
	}
}