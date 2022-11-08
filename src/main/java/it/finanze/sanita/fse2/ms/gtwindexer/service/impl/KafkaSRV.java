/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.service.impl;

import static it.finanze.sanita.fse2.ms.gtwindexer.enums.EventStatusEnum.BLOCKING_ERROR;
import static it.finanze.sanita.fse2.ms.gtwindexer.enums.EventStatusEnum.NON_BLOCKING_ERROR;
import static it.finanze.sanita.fse2.ms.gtwindexer.enums.EventStatusEnum.SUCCESS;
import static it.finanze.sanita.fse2.ms.gtwindexer.enums.EventTypeEnum.DESERIALIZE;
import static it.finanze.sanita.fse2.ms.gtwindexer.enums.EventTypeEnum.SEND_TO_INI;
import static it.finanze.sanita.fse2.ms.gtwindexer.utility.StringUtility.isNullOrEmpty;
import static it.finanze.sanita.fse2.ms.gtwindexer.utility.StringUtility.toJSONJackson;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import it.finanze.sanita.fse2.ms.gtwindexer.client.IIniClient;
import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaConsumerPropertiesCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.KafkaStatusManagerDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IndexerValueDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IniDeleteRequestDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IniMetadataUpdateReqDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniPublicationResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniTraceResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ErrorLogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.EventStatusEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.EventTypeEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.OperationLogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.PriorityTypeEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ProcessorOperationEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ResultLogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BlockingIniException;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtwindexer.service.IKafkaSRV;
import it.finanze.sanita.fse2.ms.gtwindexer.service.KafkaAbstractSRV;
import it.finanze.sanita.fse2.ms.gtwindexer.utility.ProfileUtility;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author vincenzoingenito
 *
 * Kafka management service.
 */
@Service
@Slf4j
public class KafkaSRV extends KafkaAbstractSRV implements IKafkaSRV {

	/**
	 * Serial version uid.
	 */
	private static final long serialVersionUID = 987723954716001270L;

	@Autowired
	private IIniClient iniClient;

	@Autowired
	private transient ProfileUtility profileUtility;
	
	@Autowired
	private KafkaConsumerPropertiesCFG kafkaConsumerPropCFG;

	@Override
	@KafkaListener(topics = "#{'${kafka.dispatcher-indexer.topic.low-priority}'}",  clientIdPrefix = "#{'${kafka.consumer.client-id.low}'}", containerFactory = "kafkaListenerDeadLetterContainerFactory", autoStartup = "${event.topic.auto.start}", groupId = "#{'${kafka.consumer.group-id}'}")
	public void lowPriorityListener(final ConsumerRecord<String, String> cr, final MessageHeaders messageHeaders) throws InterruptedException {
		genericListener(cr, PriorityTypeEnum.LOW);
	}

	@Override
	@KafkaListener(topics = "#{'${kafka.dispatcher-indexer.topic.medium-priority}'}",  clientIdPrefix = "#{'${kafka.consumer.client-id.medium}'}", containerFactory = "kafkaListenerDeadLetterContainerFactory", autoStartup = "${event.topic.auto.start}", groupId = "#{'${kafka.consumer.group-id}'}")
	public void mediumPriorityListener(final ConsumerRecord<String, String> cr, final MessageHeaders messageHeaders) throws InterruptedException {
		genericListener(cr, PriorityTypeEnum.MEDIUM);
	}

	@Override
	@KafkaListener(topics = "#{'${kafka.dispatcher-indexer.topic.high-priority}'}",  clientIdPrefix = "#{'${kafka.consumer.client-id.high}'}", containerFactory = "kafkaListenerDeadLetterContainerFactory", autoStartup = "${event.topic.auto.start}", groupId = "#{'${kafka.consumer.group-id}'}")
	public void highPriorityListener(final ConsumerRecord<String, String> cr, final MessageHeaders messageHeaders) throws InterruptedException {
		genericListener(cr, PriorityTypeEnum.HIGH);
	}

	@Override
	@KafkaListener(topics = "#{'${kafka.dispatcher-indexer.delete-retry-topic}'}",  clientIdPrefix = "#{'${kafka.consumer.client-id.retry-delete}'}", containerFactory = "kafkaListenerDeadLetterContainerFactory", autoStartup = "${event.topic.auto.start}", groupId = "#{'${kafka.consumer.group-id}'}")
	public void retryDeleteListener(ConsumerRecord<String, String> cr, MessageHeaders messageHeaders) {

		// ====================
		// Deserialize request
		// ====================
		// Retrieve request body
		String wif = cr.key(), request = cr.value();
		IniDeleteRequestDTO req;
		boolean exit = false;
		// Convert to delete request
		try {
			// Get object
			req = new Gson().fromJson(request, IniDeleteRequestDTO.class);
			// Require not null
			Objects.requireNonNull(req, "The request payload cannot be null");
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
				IniTraceResponseDTO res = iniClient.delete(req);
				// Everything has been resolved
				if (Boolean.TRUE.equals(res.getEsito())) {
					sendStatusMessage(wif, SEND_TO_INI, SUCCESS, new Gson().toJson(res));
				} else {
					throw new BlockingIniException(res.getErrorMessage());
				}
				// Quit flag
				exit = true;
			}catch (Exception e) {
				// Assign
				ex = e;
				// Display help
				deadLetterHelper(e);
				// Try to identify the exception type
				Optional<EventStatusEnum> type = kafkaConsumerPropCFG.asExceptionType(e);
				// If we found it, we are good to make an action, otherwise, let's retry
				if(type.isPresent()) {
					// Get type [BLOCKING or NON_BLOCKING_ERROR]
					EventStatusEnum status = type.get();
					// Send to kafka
					sendStatusMessage(wif, SEND_TO_INI, status, e.getMessage());
					// We are going re-process it
					throw e;
				}
			}
		}

		// We didn't exit properly from the loop,
		// We reached the max amount of retries
		if(!exit) {
			sendStatusMessage(wif, SEND_TO_INI, BLOCKING_ERROR, "Massimo numero di retry raggiunto: " + ex.getMessage());
			throw new BlockingIniException(ex.getMessage());
		}

	}
	
	@Override
	@KafkaListener(topics = "#{'${kafka.dispatcher-indexer.update-retry-topic}'}",  clientIdPrefix = "#{'${kafka.consumer.client-id.retry-delete}'}", containerFactory = "kafkaListenerDeadLetterContainerFactory", autoStartup = "${event.topic.auto.start}", groupId = "#{'${kafka.consumer.group-id}'}")
	public void retryUpdateListener(ConsumerRecord<String, String> cr, MessageHeaders messageHeaders) {
		String wif = cr.key();
		String request = cr.value();
		IniMetadataUpdateReqDTO req;
		boolean exit = false;

		// Convert to delete request
		try {
			// Get object
			req = new Gson().fromJson(request, IniMetadataUpdateReqDTO.class);
			Objects.requireNonNull(req, "The request payload cannot be null");
		} catch (Exception e) {
			log.error("Unable to deserialize request with wif {} due to: {}", wif, e.getMessage());
			sendStatusMessage(wif, DESERIALIZE, BLOCKING_ERROR, request);
			throw new BlockingIniException(e.getMessage());
		}

		Exception ex = new Exception("Errore generico durante l'invocazione del client di ini");
		for (int i = 0; i <= kafkaConsumerPropCFG.getNRetry() && !exit; ++i) {
			try {
				IniTraceResponseDTO res = iniClient.sendUpdateData(req);
				if (Boolean.TRUE.equals(res.getEsito())) {
					sendStatusMessage(wif, SEND_TO_INI, SUCCESS, new Gson().toJson(res));
				} else {
					throw new BlockingIniException(res.getErrorMessage());
				}
				exit = true;
			}catch (Exception e) {
				ex = e;
				deadLetterHelper(e);
				Optional<EventStatusEnum> type = kafkaConsumerPropCFG.asExceptionType(e);
				if(type.isPresent()) {
					EventStatusEnum status = type.get();
					sendStatusMessage(wif, SEND_TO_INI, status, e.getMessage());
					throw e;
				}
			}
		}

		if(!exit) {
			sendStatusMessage(wif, SEND_TO_INI, BLOCKING_ERROR, "Massimo numero di retry raggiunto: " + ex.getMessage());
			throw new BlockingIniException(ex.getMessage());
		}

	}

	/**
	 * @param e
	 */
	private void deadLetterHelper(Exception e) {
		StringBuilder sb = new StringBuilder("LIST OF USEFUL EXCEPTIONS TO MOVE TO DEADLETTER OFFSET 'kafka.consumer.dead-letter-exc'. ");
		boolean continua = true;
		Throwable excTmp = e;
		Throwable excNext = null;

		while (continua) {
			if (excNext != null) {
				excTmp = excNext;
				sb.append(", ");
			}

			sb.append(excTmp.getClass().getCanonicalName());
			excNext = excTmp.getCause();

			if (excNext == null) {
				continua = false;
			}

		}

		log.error("{}", sb);
	}

	@Override
	public void sendStatusMessage(final String workflowInstanceId,final EventTypeEnum eventType,
			final EventStatusEnum eventStatus, String message) {
		try {
			KafkaStatusManagerDTO statusManagerMessage = KafkaStatusManagerDTO.builder().
					eventType(eventType).
					eventDate(new Date()).
					eventStatus(eventStatus).
					message(message).
					build();
			String json = toJSONJackson(statusManagerMessage);
			sendMessage(kafkaTopicCFG.getStatusManagerTopic(), workflowInstanceId, json, true);
		} catch(Exception ex) {
			log.error("Error while send status message on indexer : " , ex);
			throw new BusinessException(ex);
		}
	}

	private void genericListener(final ConsumerRecord<String, String> cr, PriorityTypeEnum priorityType) {
		log.debug("Message priority: {}", priorityType.getDescription());
		final Date startDateOperation = new Date();
		IndexerValueDTO valueInfo = new IndexerValueDTO();

		EventTypeEnum eventStepEnum = SEND_TO_INI;

		boolean esito = false;
		int counter = 0;

		boolean callIni = true;
		boolean sendMessageToPublisher = true;
		while(Boolean.FALSE.equals(esito) && counter<=kafkaConsumerPropCFG.getNRetry()) {
			try {
				String key = cr.key();
				log.debug("Consuming Transaction Event - Message received with key {}", cr.key());
				valueInfo = new Gson().fromJson(cr.value(), IndexerValueDTO.class);

				IniPublicationResponseDTO response = sendToIniClient(valueInfo, callIni);

				if (Boolean.TRUE.equals(response.getEsito()) || isHandledPerMock(response)) {
					log.debug("Successfully sent data to INI for workflow instance id" + valueInfo.getWorkflowInstanceId() + " with response: true", OperationLogEnum.CALL_INI, ResultLogEnum.OK, startDateOperation);
					callIni = false;
					
					if(sendMessageToPublisher) {
						String destTopic = kafkaTopicCFG.getIndexerPublisherTopic() + priorityType.getQueue();
						sendMessage(destTopic, key, cr.value(), true);
						sendMessageToPublisher = false;
					}
				} else {
					throw new BlockingIniException(response.getErrorMessage());
				} 
			 
				sendStatusMessage(valueInfo.getWorkflowInstanceId(), eventStepEnum, SUCCESS, null);
				esito = true;
			} catch (Exception e) {
				String errorMessage = isNullOrEmpty(e.getMessage()) ? "Errore generico durante l'invocazione del client di ini" : e.getMessage();
				log.error("Error sending data to INI " + valueInfo.getWorkflowInstanceId() , OperationLogEnum.CALL_INI, ResultLogEnum.KO, startDateOperation, ErrorLogEnum.KO_INI);
				deadLetterHelper(e);
				if(kafkaConsumerPropCFG.getDeadLetterExceptions().contains(ExceptionUtils.getRootCause(e).getClass().getCanonicalName())) {
					sendStatusMessage(valueInfo.getWorkflowInstanceId(), eventStepEnum, BLOCKING_ERROR, errorMessage);
					throw e;
				} else if(kafkaConsumerPropCFG.getTemporaryExceptions().contains(ExceptionUtils.getRootCause(e).getClass().getCanonicalName())){
					sendStatusMessage(valueInfo.getWorkflowInstanceId(), eventStepEnum, NON_BLOCKING_ERROR, errorMessage);
					throw e;
				} else {
					counter++;
					if(counter==kafkaConsumerPropCFG.getNRetry()) {
						sendStatusMessage(valueInfo.getWorkflowInstanceId(), eventStepEnum, BLOCKING_ERROR, "Massimo numero di retry raggiunto :" + errorMessage);
					}
				}
			}
		}
	}

	private IniPublicationResponseDTO sendToIniClient(final IndexerValueDTO valueInfo,final boolean callIni) {
		IniPublicationResponseDTO response = new IniPublicationResponseDTO();
		response.setEsito(true);
		if(Boolean.TRUE.equals(callIni)) {
			if (valueInfo.getEdsDPOperation().equals(ProcessorOperationEnum.PUBLISH)) {
				response = iniClient.sendPublicationData(valueInfo.getWorkflowInstanceId());
			} else if (valueInfo.getEdsDPOperation().equals(ProcessorOperationEnum.REPLACE)) {
				response = iniClient.sendReplaceData(valueInfo.getWorkflowInstanceId());
			} else {
				throw new BusinessException("Unsupported INI operation");
			}
		}
		return response;
	}
	 
	/**
	 * Returns {@code true} if the response is handled as a success for mock purposes.
	 * 
	 * @param response The response returnd from Ini Client
	 * @return {@code true} if the response is handled as a success for mock purposes, {@code false} otherwise
	 */
	private boolean isHandledPerMock(IniPublicationResponseDTO response) {

		boolean isIpConfigurationError = response != null && !isNullOrEmpty(response.getErrorMessage()) && response.getErrorMessage().contains("Invalid region ip");
		return (profileUtility.isTestProfile() || profileUtility.isDevOrDockerProfile()) && isIpConfigurationError;
	}

}
