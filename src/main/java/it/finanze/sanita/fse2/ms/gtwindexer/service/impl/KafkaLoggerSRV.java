package it.finanze.sanita.fse2.ms.gtwindexer.service.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.finanze.sanita.fse2.ms.gtwindexer.dto.LogDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ILogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ResultLogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.service.ILogSRV;
import it.finanze.sanita.fse2.ms.gtwindexer.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;

/** 
 * 
 * @author: Guido Rocco - IBM 
 */ 

@Service
@Slf4j
public class KafkaLoggerSRV {

	@Autowired
	private ILogSRV logSRV;

	/* 
	 * Specify here the format for the dates 
	 */
	private DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS"); 

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

		final String jsonObject = StringUtility.toJSON(logDTO);
		log.trace(jsonObject);
		logSRV.sendLoggerStatus(jsonObject);
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
				
		final String jsonObject = StringUtility.toJSON(logDTO);
		log.debug(jsonObject);
		logSRV.sendLoggerStatus(jsonObject);
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
				
		final String jsonObject = StringUtility.toJSON(logDTO);
		log.info(jsonObject);
		logSRV.sendLoggerStatus(jsonObject);
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

		final String jsonObject = StringUtility.toJSON(logDTO);
		log.warn(jsonObject);
		logSRV.sendLoggerStatus(jsonObject);
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

		final String jsonObject = StringUtility.toJSON(logDTO);
		log.error(jsonObject);
		logSRV.sendLoggerStatus(jsonObject);
	}

}
