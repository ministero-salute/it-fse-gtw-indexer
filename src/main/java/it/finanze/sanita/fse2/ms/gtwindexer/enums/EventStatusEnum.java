package it.finanze.sanita.fse2.ms.gtwindexer.enums;

public enum EventStatusEnum {

	IN_QUEUE("IN_QUEUE"),
	SUCCESS("SUCCESS"),
	BLOCKING_ERROR("BLOCKING_ERROR"),
	NON_BLOCKING_ERROR("NON_BLOCKING_ERROR");

	private final String name;

	EventStatusEnum(String inName) {
		name = inName;
	}

	public String getName() {
		return name;
	}

}