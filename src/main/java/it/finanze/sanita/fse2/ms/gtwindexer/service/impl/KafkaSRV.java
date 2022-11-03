/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.service.impl;

import java.util.Date;

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
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniPublicationResponseDTO;
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
import it.finanze.sanita.fse2.ms.gtwindexer.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author vincenzoingenito
 *
 * Kafka management service.
 */
@Service
@Slf4j
public class KafkaSRV extends KafkaAbstractSRV implements IKafkaSRV{

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
		abstractListener(cr, PriorityTypeEnum.LOW);
	}

	@Override
	@KafkaListener(topics = "#{'${kafka.dispatcher-indexer.topic.medium-priority}'}",  clientIdPrefix = "#{'${kafka.consumer.client-id.medium}'}", containerFactory = "kafkaListenerDeadLetterContainerFactory", autoStartup = "${event.topic.auto.start}", groupId = "#{'${kafka.consumer.group-id}'}")
	public void mediumPriorityListener(final ConsumerRecord<String, String> cr, final MessageHeaders messageHeaders) throws InterruptedException {
		abstractListener(cr, PriorityTypeEnum.MEDIUM);
	}

	@Override
	@KafkaListener(topics = "#{'${kafka.dispatcher-indexer.topic.high-priority}'}",  clientIdPrefix = "#{'${kafka.consumer.client-id.high}'}", containerFactory = "kafkaListenerDeadLetterContainerFactory", autoStartup = "${event.topic.auto.start}", groupId = "#{'${kafka.consumer.group-id}'}")
	public void highPriorityListener(final ConsumerRecord<String, String> cr, final MessageHeaders messageHeaders) throws InterruptedException {
		abstractListener(cr, PriorityTypeEnum.HIGH);
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
			String json = StringUtility.toJSONJackson(statusManagerMessage);
			sendMessage(kafkaTopicCFG.getStatusManagerTopic(), workflowInstanceId, json, true);
		} catch(Exception ex) {
			log.error("Error while send status message on indexer : " , ex);
			throw new BusinessException(ex);
		}
	}
	 
	private void abstractListener(final ConsumerRecord<String, String> cr, PriorityTypeEnum priorityType) {
		log.debug("Message priority: {}", priorityType.getDescription());
		final Date startDateOperation = new Date();
		IndexerValueDTO valueInfo = new IndexerValueDTO();

		EventTypeEnum eventStepEnum = EventTypeEnum.SEND_TO_INI;

		boolean esito = false;
		int counter = 0;

		while(Boolean.FALSE.equals(esito) && counter<=kafkaConsumerPropCFG.getNRetry()) {
			try {
				String key = cr.key();
				log.debug("Consuming Transaction Event - Message received with key {}", cr.key());
				valueInfo = new Gson().fromJson(cr.value(), IndexerValueDTO.class);

				IniPublicationResponseDTO response = sendToIniClient(valueInfo, esito);

				if (Boolean.TRUE.equals(response.getEsito()) || isHandledPerMock(response)) {
					log.debug("Successfully sent data to INI for workflow instance id" + valueInfo.getWorkflowInstanceId() + " with response: true", OperationLogEnum.CALL_INI, ResultLogEnum.OK, startDateOperation);
					esito = response.getEsito();
					String destTopic = kafkaTopicCFG.getIndexerPublisherTopic() + priorityType.getQueue();
					sendMessage(destTopic, key, cr.value(), true);
				} else {
					throw new BlockingIniException(response.getErrorMessage());
				} 
				//				final String errorMessage = response != null ? response.getErrorMessage() : null;
				sendStatusMessage(valueInfo.getWorkflowInstanceId(), eventStepEnum, EventStatusEnum.SUCCESS, null);
			} catch (Exception e) {
				String errorMessage = StringUtility.isNullOrEmpty(e.getMessage()) ? "Errore generico durante l'invocazione del client di ini" : e.getMessage();
				log.error("Error sending data to INI " + valueInfo.getWorkflowInstanceId() , OperationLogEnum.CALL_INI, ResultLogEnum.KO, startDateOperation, ErrorLogEnum.KO_INI);
				deadLetterHelper(e);
				if(kafkaConsumerPropCFG.getDeadLetterExceptions().contains(e.getClass().getName())) {
					sendStatusMessage(valueInfo.getWorkflowInstanceId(), eventStepEnum, EventStatusEnum.BLOCKING_ERROR, errorMessage);
				} else if(kafkaConsumerPropCFG.getTemporaryExceptions().contains(e.getClass().getName())){
					sendStatusMessage(valueInfo.getWorkflowInstanceId(), eventStepEnum, EventStatusEnum.NON_BLOCKING_ERROR, errorMessage);
				} else {
					counter++;
					if(counter==kafkaConsumerPropCFG.getNRetry()) {
						sendStatusMessage(valueInfo.getWorkflowInstanceId(), eventStepEnum, EventStatusEnum.BLOCKING_ERROR, "Massimo numero di retry raggiunto :" + errorMessage);
					}
				}
				throw e;
			}
		}
	}

	private IniPublicationResponseDTO sendToIniClient(final IndexerValueDTO valueInfo,final boolean callIni) {
		IniPublicationResponseDTO response = null; 
		if(Boolean.FALSE.equals(callIni)) {
			if (valueInfo.getEdsDPOperation().equals(ProcessorOperationEnum.PUBLISH)) {
				response = iniClient.sendPublicationData(valueInfo.getWorkflowInstanceId());
			} else if (valueInfo.getEdsDPOperation().equals(ProcessorOperationEnum.REPLACE)) {
				response = iniClient.sendReplaceData(valueInfo);
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

		boolean isIpConfigurationError = response != null && !StringUtility.isNullOrEmpty(response.getErrorMessage()) && response.getErrorMessage().contains("Invalid region ip");
		return (profileUtility.isTestProfile() || profileUtility.isDevOrDockerProfile()) && isIpConfigurationError;
	}

}
