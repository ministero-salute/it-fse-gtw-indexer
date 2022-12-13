/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.client.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import it.finanze.sanita.fse2.ms.gtwindexer.client.IIniClient;
import it.finanze.sanita.fse2.ms.gtwindexer.config.MicroservicesURLCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IniDeleteRequestDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IniMetadataUpdateReqDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniPublicationResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniTraceResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BlockingIniException;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.NoRecordFoundException;
import lombok.extern.slf4j.Slf4j;

/**
 * Production implemention of Ini Client.
 */
@Slf4j
@Component 
public class IniClient implements IIniClient {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private MicroservicesURLCFG msUrlCFG;

	
	@Override
	public IniPublicationResponseDTO sendPublicationData(final String workflowInstanceId) {
		log.debug("INI Client - Sending publication data to INI");

		HttpEntity<?> entity = buildHeader(workflowInstanceId);
		IniPublicationResponseDTO response = new IniPublicationResponseDTO();
		try {
			final String endpoint = msUrlCFG.getIniClientHost() + "/v1/ini-publish";
			response = restTemplate.postForObject(endpoint, entity, IniPublicationResponseDTO.class);
		} catch (ResourceAccessException rax) {
			log.error("Connect error while call ini client send publication :" + rax);
			throw rax;
		} catch (HttpServerErrorException | HttpClientErrorException error) {
			errorHandler(error);
		}  

		return response;
	}

	@Override
	public IniPublicationResponseDTO sendReplaceData(final String workflowInstanceId) {
		log.debug("INI Client - Sending update data to INI to update document with wii: {}", workflowInstanceId);
		IniPublicationResponseDTO out = new IniPublicationResponseDTO();

		HttpEntity<?> entity = buildHeader(workflowInstanceId);

		ResponseEntity<IniPublicationResponseDTO> response = null;
		try {
			final String endpoint = msUrlCFG.getIniClientHost() + "/v1/ini-replace";
			response = restTemplate.exchange(endpoint, HttpMethod.PUT, entity, IniPublicationResponseDTO.class);
			out = response.getBody();
		} catch (ResourceAccessException rax) {
			log.error("Connection error while calling ini client send replace:", rax);
			throw rax;
		} catch(HttpServerErrorException | HttpClientErrorException error) {
			errorHandler(error);
		}  
		return out;
	}

	@Override
	public IniTraceResponseDTO delete(IniDeleteRequestDTO iniReq) {
		log.debug("INI Client - Calling Ini to execute delete operation");
		IniTraceResponseDTO output = new IniTraceResponseDTO();
		try {
			HttpEntity<Object> entity = new HttpEntity<>(iniReq, null);
			final String endpoint = msUrlCFG.getIniClientHost() + "/v1/ini-delete";
			ResponseEntity<IniTraceResponseDTO> restExchange = restTemplate.exchange(endpoint, HttpMethod.DELETE, entity, IniTraceResponseDTO.class);
			output = restExchange.getBody();
		} catch (ResourceAccessException rax) {
			log.error("Connection error while calling ini client send delete:", rax);
			throw rax;
		} catch(HttpClientErrorException clientError) {
			errorHandler(clientError);
		} 
		return output;
	}

	@Override
	public IniTraceResponseDTO sendUpdateData(IniMetadataUpdateReqDTO iniReq) {
		log.debug("INI Client - Calling INI to execute update metadati");
		IniTraceResponseDTO out = new IniTraceResponseDTO();
		try {
			HttpEntity<Object> entity = new HttpEntity<>(iniReq, null);

			final String endpoint = msUrlCFG.getIniClientHost() + "/v1/ini-update";
			ResponseEntity<IniTraceResponseDTO> restExchange = restTemplate.exchange(endpoint, HttpMethod.PUT, entity, IniTraceResponseDTO.class);
			out = restExchange.getBody();
		} catch (ResourceAccessException rax) {
			log.error("Connection error while calling ini client send update:", rax);
			throw rax;
		} catch(HttpClientErrorException clientError) {
			errorHandler(clientError);
		} 

		return out;
	}
	
	private HttpEntity<?> buildHeader(Object body) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "application/json");
		return new HttpEntity<>(body, headers);
	}

	private void errorHandler(final HttpStatusCodeException ex) {
		if (HttpStatus.NOT_FOUND.equals(ex.getStatusCode())) {
			throw new NoRecordFoundException(ex.getMessage(), ex);
		} else if (HttpStatus.INTERNAL_SERVER_ERROR.equals(ex.getStatusCode())) {
			throw new BlockingIniException(ex.getMessage(), ex);
		} else {
			throw new BusinessException(ex.getMessage(), ex);
		}
	}
}
