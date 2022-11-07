/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.client.impl;

import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IniDeleteRequestDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniTraceResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;

import it.finanze.sanita.fse2.ms.gtwindexer.client.IIniClient;
import it.finanze.sanita.fse2.ms.gtwindexer.config.MicroservicesURLCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IndexerValueDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniPublicationResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.ConnectionRefusedException;
import lombok.extern.slf4j.Slf4j;

/**
 * Production implemention of Ini Client.
 * 
 * @author vincenzoingenito
 */
@Slf4j
@Component 
public class IniClient implements IIniClient {

	/**
	 * Serial version uid.
	 */
	private static final long serialVersionUID = -1470125906483650945L;

	@Autowired
	private transient RestTemplate restTemplate;

	@Autowired
	private transient MicroservicesURLCFG msUrlCFG;

	@Override
	public IniPublicationResponseDTO sendPublicationData(final String workflowInstanceId) {
		IniPublicationResponseDTO out = new IniPublicationResponseDTO();
		log.debug("INI Client - Sending publication data to INI");
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "application/json");

		HttpEntity<?> entity = new HttpEntity<>(workflowInstanceId, headers);

		ResponseEntity<IniPublicationResponseDTO> response = null;
		try {
			response = restTemplate.exchange(msUrlCFG.getIniClientHost() + "/v1/ini-publish", HttpMethod.POST, entity, IniPublicationResponseDTO.class);
			out = response.getBody();
			log.debug("{} status returned from INI client while executing publish document, with body: {}", response.getStatusCode(), response.getBody());
		} catch (ResourceAccessException cex) {
			log.error("Connect error while call ini client ep :" + cex);
			throw cex;
		} catch(Exception ex) {
			log.error("Generic error while call ini client ep :" + ex);
			throw new BusinessException("Generic error while call ini client ep :" + ex);
		}
		return out;
	}

	@Override
	public IniPublicationResponseDTO sendReplaceData(final String workflowInstanceId) {

		IniPublicationResponseDTO out = new IniPublicationResponseDTO();
		log.debug("INI Client - Sending update data to INI to update document with wii: {}", workflowInstanceId);

		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "application/json");

		HttpEntity<?> entity = new HttpEntity<>(workflowInstanceId, headers);

		ResponseEntity<IniPublicationResponseDTO> response = null;
		try {
			response = restTemplate.exchange(msUrlCFG.getIniClientHost() + "/v1/ini-replace", HttpMethod.PUT, entity, IniPublicationResponseDTO.class);
			out = response.getBody();
			log.debug("{} status returned from INI client while executing update of document, with body: {}", response.getStatusCode(), response.getBody());
		} catch (ResourceAccessException | ConnectionRefusedException cex) {
			log.error("Connection error while calling ini client ep:", cex);
			throw cex;
		} catch(Exception ex) {
			log.error("Generic error while calling ini endpoint to execute update: ", ex);
			throw new BusinessException("Generic error while calling ini endpoint to execute update: ", ex);
		}
		return out;
	}

	@Override
	public IniTraceResponseDTO delete(IniDeleteRequestDTO iniReq) {
		IniTraceResponseDTO output;
		try {
			log.debug("INI Client - Calling Ini to execute delete operation");
			// Build headers.
			HttpEntity<Object> entity = new HttpEntity<>(iniReq, null);
			// Build endpoint e call.
			String endpoint = msUrlCFG.getIniClientHost() + "/v1/ini-delete";
			ResponseEntity<IniTraceResponseDTO> restExchange = restTemplate.exchange(endpoint, HttpMethod.DELETE, entity, IniTraceResponseDTO.class);
			// Gestione response
			output = restExchange.getBody();
		} catch (Exception e) {
			log.error("Errore durante l'invocazione di INI dell'API delete(). ", e);
			throw e;
		}
		return output;
	}

}
