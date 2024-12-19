
/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * 
 * Copyright (C) 2023 Ministero della Salute
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package it.finanze.sanita.fse2.ms.gtwindexer.service.impl;

import com.google.gson.Gson;
import it.finanze.sanita.fse2.ms.gtwindexer.client.IIniClient;
import it.finanze.sanita.fse2.ms.gtwindexer.client.base.ClientCallback;
import it.finanze.sanita.fse2.ms.gtwindexer.config.AccreditationSimulationCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaConsumerCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaConsumerPropertiesCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.KafkaStatusManagerDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IndexerValueDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IniDeleteRequestDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IniMetadataUpdateReqDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniTraceResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.*;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BlockingIniException;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtwindexer.service.IAccreditamentoSimulationSRV;
import it.finanze.sanita.fse2.ms.gtwindexer.service.IConfigSRV;
import it.finanze.sanita.fse2.ms.gtwindexer.service.IKafkaSRV;
import it.finanze.sanita.fse2.ms.gtwindexer.service.KafkaAbstractSRV;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static it.finanze.sanita.fse2.ms.gtwindexer.config.Constants.Logs.MESSAGE_PRIORITY;
import static it.finanze.sanita.fse2.ms.gtwindexer.enums.EventStatusEnum.*;
import static it.finanze.sanita.fse2.ms.gtwindexer.enums.EventTypeEnum.DESERIALIZE;
import static it.finanze.sanita.fse2.ms.gtwindexer.enums.EventTypeEnum.SEND_TO_INI;
import static it.finanze.sanita.fse2.ms.gtwindexer.enums.PriorityTypeEnum.*;
import static it.finanze.sanita.fse2.ms.gtwindexer.utility.KafkaUtility.getTraceContext;
import static it.finanze.sanita.fse2.ms.gtwindexer.utility.StringUtility.toJSONJackson;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Kafka management service.
 */
@Service
@Slf4j
public class KafkaSRV extends KafkaAbstractSRV implements IKafkaSRV {

	@Autowired
	private IIniClient iniClient;

	@Autowired
	private IAccreditamentoSimulationSRV accreditamentoSRV;

	@Autowired
	private KafkaConsumerPropertiesCFG kafkaConsumerPropCFG;

	@Autowired
	private AccreditationSimulationCFG accreditamentoSimulationCFG;

	@Value("${spring.application.name}")
	private String msName;
	
	@Autowired
	private IConfigSRV configSRV;

	@Override
	@KafkaListener(topics = "#{'${kafka.dispatcher-indexer.topic.low-priority}'}",  clientIdPrefix = "#{'${kafka.consumer.client-id.low}'}", containerFactory = "kafkaListenerDeadLetterContainerFactory", autoStartup = "${event.topic.auto.start}", groupId = "#{'${kafka.consumer.group-id}'}")
	public void lowPriorityListener( ConsumerRecord<String, String> cr, @Header(KafkaHeaders.DELIVERY_ATTEMPT) int delivery) throws Exception {
		log.debug(MESSAGE_PRIORITY, LOW.getDescription());
		loop(cr, IndexerValueDTO.class, req -> publishAndReplace(cr, topics.getIndexerPublisherTopic(LOW), new Date(), req) , delivery, IndexerValueDTO::getWorkflowInstanceId);
	}

	@Override
	@KafkaListener(topics = "#{'${kafka.dispatcher-indexer.topic.medium-priority}'}",  clientIdPrefix = "#{'${kafka.consumer.client-id.medium}'}", containerFactory = "kafkaListenerDeadLetterContainerFactory", autoStartup = "${event.topic.auto.start}", groupId = "#{'${kafka.consumer.group-id}'}")
	public void mediumPriorityListener( ConsumerRecord<String, String> cr, @Header(KafkaHeaders.DELIVERY_ATTEMPT) int delivery) throws Exception {
		log.debug(MESSAGE_PRIORITY, MEDIUM.getDescription());
		loop(cr, IndexerValueDTO.class, req -> publishAndReplace(cr, topics.getIndexerPublisherTopic(MEDIUM), new Date(), req) , delivery, IndexerValueDTO::getWorkflowInstanceId);
	}

	@Override
	@KafkaListener(topics = "#{'${kafka.dispatcher-indexer.topic.high-priority}'}",  clientIdPrefix = "#{'${kafka.consumer.client-id.high}'}", containerFactory = "kafkaListenerDeadLetterContainerFactory", autoStartup = "${event.topic.auto.start}", groupId = "#{'${kafka.consumer.group-id}'}")
	public void highPriorityListener( ConsumerRecord<String, String> cr, @Header(KafkaHeaders.DELIVERY_ATTEMPT) int delivery) throws Exception {
		log.debug(MESSAGE_PRIORITY, HIGH.getDescription());
		loop(cr, IndexerValueDTO.class, req -> publishAndReplace(cr, topics.getIndexerPublisherTopic(HIGH), new Date(), req) , delivery, IndexerValueDTO::getWorkflowInstanceId);
	}

	@Override
	@KafkaListener(topics = "#{'${kafka.dispatcher-indexer.delete-retry-topic}'}",  clientIdPrefix = "#{'${kafka.consumer.client-id.retry-delete}'}", containerFactory = "kafkaListenerDeadLetterContainerFactory", autoStartup = "${event.topic.auto.start}", groupId = "#{'${kafka.consumer.group-id}'}")
	public void retryDeleteListener(ConsumerRecord<String, String> cr, @Header(KafkaHeaders.DELIVERY_ATTEMPT) int delivery) throws Exception {
		log.debug("Retry delete listener");
		loop(cr, IniDeleteRequestDTO.class, req -> iniClient.delete(req), delivery, null);
	}

	@Override
	@KafkaListener(topics = "#{'${kafka.dispatcher-indexer.update-retry-topic}'}",  clientIdPrefix = "#{'${kafka.consumer.client-id.retry-update}'}", containerFactory = "kafkaListenerDeadLetterContainerFactory", autoStartup = "${event.topic.auto.start}", groupId = "#{'${kafka.consumer.group-id}'}")
	public void retryUpdateListener(ConsumerRecord<String, String> cr, @Header(KafkaHeaders.DELIVERY_ATTEMPT) int delivery) throws Exception {
		log.debug("Retry update listener");
		loop(cr, IniMetadataUpdateReqDTO.class, req -> iniClient.sendUpdateData(req), delivery, null);
	}


	@Override
	public void sendStatusMessage( String workflowInstanceId, EventTypeEnum eventType,
			 EventStatusEnum eventStatus, String message) {
		try {
			KafkaStatusManagerDTO statusManagerMessage = KafkaStatusManagerDTO.builder().
					eventType(eventType).
					eventDate(new Date()).
					eventStatus(eventStatus).
					message(message).
					microserviceName(msName).
					build();
			String json = toJSONJackson(statusManagerMessage);
			sendMessage(topics.getStatusManagerTopic(), workflowInstanceId, json);
		} catch(Exception ex) {
			log.error("Error while send status message on indexer : " , ex);
			throw new BusinessException(ex);
		}
	}
 
	private IniTraceResponseDTO publishAndReplace(ConsumerRecord<String, String> cr, String destTopic, Date startDateOperation, IndexerValueDTO valueInfo) {

		if(accreditamentoSimulationCFG.isEnableCheck()) accreditamentoSRV.runSimulation(valueInfo.getIdDoc());
		
		IniTraceResponseDTO response = sendToIniClient(valueInfo);

		if (Boolean.TRUE.equals(response.getEsito()) && !configSRV.isRemoveEds()) { 
			log.debug("Successfully sent data to INI for workflow instance id" + valueInfo.getWorkflowInstanceId() + " with response: true", OperationLogEnum.CALL_INI, ResultLogEnum.OK, startDateOperation);
			try {
				sendMessage(destTopic, cr.key(), cr.value());
			}catch(Exception ex) {
				throw new BlockingIniException(ex);
			}
		} else if (Boolean.FALSE.equals(response.getEsito())) {
			throw new BlockingIniException(response.getMessage());
		}  
		return response;
	}

	private IniTraceResponseDTO sendToIniClient( IndexerValueDTO valueInfo) {
		IniTraceResponseDTO response = new IniTraceResponseDTO();
		response.setEsito(true);
		if (valueInfo.getEdsDPOperation().equals(ProcessorOperationEnum.PUBLISH)) {
			response = iniClient.sendPublicationData(valueInfo.getWorkflowInstanceId());
		} else if (valueInfo.getEdsDPOperation().equals(ProcessorOperationEnum.REPLACE)) {
			response = iniClient.sendReplaceData(valueInfo.getWorkflowInstanceId());
		} else {
			throw new BusinessException("Unsupported INI operation");
		}
		return response;
	}


	private <T> void loop(ConsumerRecord<String, String> cr,Class<T> clazz,ClientCallback<T, IniTraceResponseDTO> cb,int delivery,Function<T, String> extractor) throws Exception {

		getTraceContext(cr).ifPresent(hd -> log.info("Logging transaction with context {}", new String(hd.value(), UTF_8)));

		// ====================
		// Deserialize request
		// ====================
		// Retrieve request body
		String wif = cr.key();
		String request = cr.value();
		T req;
		boolean exit = false;
		// Convert request
		try {
			// Get object
			req = new Gson().fromJson(request, clazz);
			// Require not null
			Objects.requireNonNull(req, "The request payload cannot be null");
			// Extract wif if provided
			if (extractor != null) {
				wif = extractor.apply(req);
			}
		} catch (Exception e) {
			log.error("Unable to deserialize request with wif {} due to: {}", wif, e.getMessage());
			sendStatusMessage(wif, DESERIALIZE, BLOCKING_ERROR, request);
			throw new BlockingIniException(e.getMessage());
		}

		// ====================
		// Retry iterations
		// ====================
		Exception ex = new Exception("Errore generico durante l'invocazione del client di ini");
		// Iterate
		for (int i = 0; i <= kafkaConsumerPropCFG.getNRetry() && !exit; ++i) {
			try {
				// Execute request
				IniTraceResponseDTO res = cb.request(req);
				// Everything has been resolved
				if (Boolean.TRUE.equals(res.getEsito())) {
					sendStatusMessage(wif, SEND_TO_INI, SUCCESS, res.getMessage());
				} else {
					throw new BlockingIniException(res.getMessage());
				}
				// Quit flag
				exit = true;
			} catch (Exception e) {
				// Assign
				ex = e;
				// Display help
				kafkaConsumerPropCFG.deadLetterHelper(e);
				// Try to identify the exception type
				Optional<EventStatusEnum> type = kafkaConsumerPropCFG.asExceptionType(e);
				// If we found it, we are good to make an action, otherwise, let's retry
				if(type.isPresent()) {
					// Get type [BLOCKING or NON_BLOCKING_ERROR]
					EventStatusEnum status = type.get();
					// Send to kafka
					if (delivery <= KafkaConsumerCFG.MAX_ATTEMPT) {
						// Send to kafka
						sendStatusMessage(wif, SEND_TO_INI, status, e.getMessage());
					}
					// We are going re-process it
					throw e;
				}
			}
		}

		// We didn't exit properly from the loop,
		// We reached the max amount of retries
		if(!exit) {
			sendStatusMessage(wif, SEND_TO_INI, BLOCKING_ERROR_MAX_RETRY, "Massimo numero di retry raggiunto: " + ex.getMessage());
			throw new BlockingIniException(ex.getMessage());
		}

	}

}
