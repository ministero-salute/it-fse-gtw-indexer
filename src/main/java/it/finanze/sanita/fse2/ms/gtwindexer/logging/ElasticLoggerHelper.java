package it.finanze.sanita.fse2.ms.gtwindexer.logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import it.finanze.sanita.fse2.ms.gtwindexer.dto.LogDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ILogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ResultLogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;

/** 
 * 
 * @author: Guido Rocco - IBM 
 */ 
@Service
@Slf4j
public class ElasticLoggerHelper {
	private static final String OPERATION = "operation";
	private static final String OP_RESULT = "op_result";
	private static final String OP_TIMESTAMP_START = "op-timestamp-start";
	private static final String OP_TIMESTAMP_END = "op-timestamp-end";
	private static final String OP_ERROR = "op-error";
	private static final String OP_ERROR_DESCRIPTION = "op-error-description";

	Logger elasticLog = LoggerFactory.getLogger("elastic-logger"); 


	/* 
	 * Specify here the format for the dates 
	 */
	private DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS"); 

	@Value("${log.elastic-search.enable}")
	private boolean elasticLogEnable;

	/* 
	 * Implements structured logs, at all logging levels
	 */
	public void trace(String message, ILogEnum operation, 
			ResultLogEnum result, Date startDateOperation) {

		LogDTO logDTO = LogDTO.builder().
				message(message).
				operation(operation.getCode()).
				op_result(result.getCode()).
				op_timestamp_start(dateFormat.format(startDateOperation)).
				op_timestamp_end(dateFormat.format(new Date())).
				build();
		log.trace(StringUtility.toJSON(logDTO));

		if(elasticLogEnable) {
			elasticLog.trace(message,  
					StructuredArguments.kv(OPERATION, operation.getCode()), 
					StructuredArguments.kv(OP_RESULT, result.getCode()),
					StructuredArguments.kv(OP_TIMESTAMP_START, dateFormat.format(startDateOperation)),
					StructuredArguments.kv(OP_TIMESTAMP_END, dateFormat.format(new Date()))); 
		}
	} 

	public void debug(String message,  ILogEnum operation,  
			ResultLogEnum result, Date startDateOperation) {

		LogDTO logDTO = LogDTO.builder().
				message(message).
				operation(operation.getCode()).
				op_result(result.getCode()).
				op_timestamp_start(dateFormat.format(startDateOperation)).
				op_timestamp_end(dateFormat.format(new Date())).
				build();
		log.debug(StringUtility.toJSON(logDTO));

		if(elasticLogEnable) {
			elasticLog.debug(message,  
					StructuredArguments.kv(OPERATION, operation.getCode()), 
					StructuredArguments.kv(OP_RESULT, result.getCode()),
					StructuredArguments.kv(OP_TIMESTAMP_START, dateFormat.format(startDateOperation)),
					StructuredArguments.kv(OP_TIMESTAMP_END, dateFormat.format(new Date()))); 
		}
	} 

	public void info(String message, ILogEnum operation,  
			ResultLogEnum result, Date startDateOperation) {

		LogDTO logDTO = LogDTO.builder().
				message(message).
				operation(operation.getCode()).
				op_result(result.getCode()).
				op_timestamp_start(dateFormat.format(startDateOperation)).
				op_timestamp_end(dateFormat.format(new Date())).
				build();
		log.info(StringUtility.toJSON(logDTO));

		if(elasticLogEnable) {
			elasticLog.info(message,  
					StructuredArguments.kv(OPERATION, operation.getCode()), 
					StructuredArguments.kv(OP_RESULT, result.getCode()),
					StructuredArguments.kv(OP_TIMESTAMP_START, dateFormat.format(startDateOperation)),
					StructuredArguments.kv(OP_TIMESTAMP_END, dateFormat.format(new Date()))); 
		}
	} 

	public void warn(String message, ILogEnum operation,  
			ResultLogEnum result, Date startDateOperation) {

		LogDTO logDTO = LogDTO.builder().
				message(message).
				operation(operation.getCode()).
				op_result(result.getCode()).
				op_timestamp_start(dateFormat.format(startDateOperation)).
				op_timestamp_end(dateFormat.format(new Date())).
				build();
		log.warn(StringUtility.toJSON(logDTO));

		if(elasticLogEnable) {
			elasticLog.warn(message,  
					StructuredArguments.kv(OPERATION, operation.getCode()), 
					StructuredArguments.kv(OP_RESULT, result.getCode()),
					StructuredArguments.kv(OP_TIMESTAMP_START, dateFormat.format(startDateOperation)),
					StructuredArguments.kv(OP_TIMESTAMP_END, dateFormat.format(new Date())));
		}
	} 

	public void error(String message, ILogEnum operation,  
			ResultLogEnum result, Date startDateOperation,
			ILogEnum error) {

		LogDTO logDTO = LogDTO.builder().
				message(message).
				operation(operation.getCode()).
				op_result(result.getCode()).
				op_timestamp_start(dateFormat.format(startDateOperation)).
				op_timestamp_end(dateFormat.format(new Date())).
				build();
		log.error(StringUtility.toJSON(logDTO));

		if(elasticLogEnable) {
			elasticLog.error(message,  
					StructuredArguments.kv(OPERATION, operation.getCode()), 
					StructuredArguments.kv(OP_RESULT, result.getCode()),
					StructuredArguments.kv(OP_TIMESTAMP_START, dateFormat.format(startDateOperation)),
					StructuredArguments.kv(OP_TIMESTAMP_END, dateFormat.format(new Date())),
					StructuredArguments.kv(OP_ERROR, error.getCode()),
					StructuredArguments.kv(OP_ERROR_DESCRIPTION, error.getDescription()));
		}
	}


}