package it.finanze.sanita.fse2.ms.gtwindexer.enums;

public enum CurrentApplicationLogEnum {
	
	
	DISPATCHER("gtw-dispatcher", "Gateway FSE - Dispatcher"),
	INDEXER("gtw-indexer", "Gateway FSE - Indexer"),
	PUBLISHER("gtw-publisher", "Gateway FSE - Publisher"),
	RULES_MANAGER("gtw-rules-manager", "Gateway FSE - Rules Manager"),
	VALIDATOR("gtw-validator", "Gateway FSE - Validator"),
	FHIR_MAPPING("gtw-fhir-mapping", "Gateway FSE - FHIR Mapping");


	private String code;
	
	public String getCode() {
		return code;
	}

	private String description;

	private CurrentApplicationLogEnum(String inCode, String inDescription) {
		code = inCode;
		description = inDescription;
	}

	public String getDescription() {
		return description;
	}
	
}
