package it.finanze.sanita.fse2.ms.gtwindexer.enums;

public enum EventTypeEnum {

	SEND_TO_INI("Send to ini"), 
	GENERIC_ERROR("Generic error from indexer");

	private String name;

	private EventTypeEnum(String inName) {
		name = inName;
	}

	public String getName() {
		return name;
	}

}