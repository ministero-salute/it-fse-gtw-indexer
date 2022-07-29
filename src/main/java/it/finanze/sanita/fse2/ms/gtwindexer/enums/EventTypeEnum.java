package it.finanze.sanita.fse2.ms.gtwindexer.enums;

public enum EventTypeEnum {

	SEND_TO_INI("SEND_TO_INI");

	private String name;

	private EventTypeEnum(String inName) {
		name = inName;
	}

	public String getName() {
		return name;
	}

}