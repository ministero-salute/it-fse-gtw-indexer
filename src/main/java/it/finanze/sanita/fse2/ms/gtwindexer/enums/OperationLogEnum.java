package it.finanze.sanita.fse2.ms.gtwindexer.enums;

public enum OperationLogEnum implements ILogEnum {

	CALL_INI("CALL-INI", "Chiamata a INI"),
	TRAS_KAFKA("TRAS-KAFKA", "Invio topic su Kafka");

	private String code;
	
	public String getCode() {
		return code;
	}

	private String description;

	private OperationLogEnum(String inCode, String inDescription) {
		code = inCode;
		description = inDescription;
	}

	public String getDescription() {
		return description;
	}

}

