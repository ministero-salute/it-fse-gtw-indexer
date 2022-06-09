package it.finanze.sanita.fse2.ms.gtwindexer.enums;

public enum EventStatusEnum {

	SUCCESS("Success"), 
	ERROR("Error");

	private String name;

	private EventStatusEnum(String inName) {
		name = inName;
	}

	public String getName() {
		return name;
	}

}