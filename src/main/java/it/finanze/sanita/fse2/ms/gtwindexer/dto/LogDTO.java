package it.finanze.sanita.fse2.ms.gtwindexer.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LogDTO {

	final String log_type = "gateway-structured-log";
	
	private String message;
	
	private String operation;
	
	private String op_result;
	
	private String op_timestamp_start;
	
	private String op_timestamp_end;
	
	private String op_error;
	
	private String op_error_description;
	
}
