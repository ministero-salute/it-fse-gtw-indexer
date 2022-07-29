package it.finanze.sanita.fse2.ms.gtwindexer.enums;

public enum PriorityTypeEnum {
    LOW("bassa"),
    MEDIUM("media"),
    HIGH("alta");

    private final String description;

    PriorityTypeEnum(String code) {
        this.description = code;
    }

    public String getCode() {
        return this.description;
    }
}
