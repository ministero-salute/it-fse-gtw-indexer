package it.finanze.sanita.fse2.ms.gtwindexer.service.impl;

import java.util.Date;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import it.finanze.sanita.fse2.ms.gtwindexer.client.IIniClient;
import it.finanze.sanita.fse2.ms.gtwindexer.config.Constants;
import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaConsumerPropertiesCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaTopicCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.KafkaStatusManagerDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IndexerValueDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniPublicationResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ErrorLogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.EventStatusEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.EventTypeEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.OperationLogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.PriorityTypeEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ResultLogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtwindexer.logging.ElasticLoggerHelper;
import it.finanze.sanita.fse2.ms.gtwindexer.service.IKafkaSRV;
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
public class KafkaSRV implements IKafkaSRV{

	/**
	 * Serial version uid.
	 */
	private static final long serialVersionUID = 987723954716001270L;


	/**
	 * Transactional producer.
	 */
	@Autowired
	@Qualifier("txkafkatemplate")
	private transient KafkaTemplate<String, String> txKafkaTemplate;

	/**
	 * Not transactional producer.
	 */
	@Autowired
	@Qualifier("notxkafkatemplate")
	private transient KafkaTemplate<String, String> notxKafkaTemplate;

	@Autowired
	private transient KafkaTopicCFG kafkaTopicCFG;

	@Autowired
	private IIniClient iniClient;

	@Autowired
	private transient ElasticLoggerHelper elasticLogger;

	@Autowired
	private transient ProfileUtility profileUtility;
	
	@Autowired
	private KafkaConsumerPropertiesCFG kafkaConsumerPropCFG;

	@Override
	public RecordMetadata sendMessage(String topic, String key, String value, boolean trans) {
		RecordMetadata out = null;
		ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, key, value);
		try { 
			out = kafkaSend(producerRecord, trans);
		} catch (Exception e) {
			log.error("Send failed.", e); 
			throw new BusinessException(e);
		}   
		return out;
	} 

	@SuppressWarnings("unchecked")
	private RecordMetadata kafkaSend(ProducerRecord<String, String> producerRecord, boolean trans) {
		RecordMetadata out = null;
		Object result = null;

		if (trans) {  
			result = txKafkaTemplate.executeInTransaction(t -> { 
				try {
					return t.send(producerRecord).get();
				} catch (Exception e) {
					throw new BusinessException(e);
				}  
			});  
		} else { 
			notxKafkaTemplate.send(producerRecord);
		} 

		if(result != null) {
			SendResult<String,String> sendResult = (SendResult<String,String>) result;
			out = sendResult.getRecordMetadata();
			log.info("Send success.");
		}

		return out;
	}

	@Override
	@KafkaListener(topics = "#{'${kafka.dispatcher-indexer.topic.low-priority}'}",  clientIdPrefix = "#{'${kafka.consumer.client-id.low}'}", containerFactory = "kafkaListenerDeadLetterContainerFactory", autoStartup = "${event.topic.auto.start}", groupId = "#{'${kafka.consumer.group-id}'}")
	public void lowPriorityListener(final ConsumerRecord<String, String> cr, final MessageHeaders messageHeaders) throws InterruptedException {
		this.abstractListener(cr, PriorityTypeEnum.LOW);
	}

	@Override
	@KafkaListener(topics = "#{'${kafka.dispatcher-indexer.topic.medium-priority}'}",  clientIdPrefix = "#{'${kafka.consumer.client-id.medium}'}", containerFactory = "kafkaListenerDeadLetterContainerFactory", autoStartup = "${event.topic.auto.start}", groupId = "#{'${kafka.consumer.group-id}'}")
	public void mediumPriorityListener(final ConsumerRecord<String, String> cr, final MessageHeaders messageHeaders) throws InterruptedException {
		this.abstractListener(cr, PriorityTypeEnum.MEDIUM);
	}

	@Override
	@KafkaListener(topics = "#{'${kafka.dispatcher-indexer.topic.high-priority}'}",  clientIdPrefix = "#{'${kafka.consumer.client-id.high}'}", containerFactory = "kafkaListenerDeadLetterContainerFactory", autoStartup = "${event.topic.auto.start}", groupId = "#{'${kafka.consumer.group-id}'}")
	public void highPriorityListener(final ConsumerRecord<String, String> cr, final MessageHeaders messageHeaders) throws InterruptedException {
		this.abstractListener(cr, PriorityTypeEnum.HIGH);
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
		log.info("Messaggio con priorità: {}", priorityType.getCode());
		final Date startDateOperation = new Date();
		IndexerValueDTO valueInfo = new IndexerValueDTO();

		EventTypeEnum eventStepEnum = EventTypeEnum.SEND_TO_INI;
		try {
			String key = cr.key();
			log.info("Consuming Transaction Event - Message received with key {}", cr.key());
			valueInfo = new Gson().fromJson(cr.value(), IndexerValueDTO.class);

			IniPublicationResponseDTO response = null;
			if (StringUtility.isNullOrEmpty(valueInfo.getIdentificativoDocUpdate())) {
				response = iniClient.sendPublicationData(valueInfo.getWorkflowInstanceId());
			} else {
				response = iniClient.sendReplaceData(valueInfo);
			}

			if ((response != null && Boolean.TRUE.equals(response.getEsito())) || profileUtility.isTestProfile() || profileUtility.isDevProfile()) {
				elasticLogger.info("Successfully sent data to INI for workflow instance id" + valueInfo.getWorkflowInstanceId() + " with response:" + response.getEsito(), OperationLogEnum.CALL_INI, ResultLogEnum.OK, startDateOperation);
				String destTopic = kafkaTopicCFG.getIndexerPublisherTopic();
				switch (priorityType) {
					case LOW:
						destTopic += Constants.Misc.LOW_PRIORITY;
						break;
					case MEDIUM:
						destTopic += Constants.Misc.MEDIUM_PRIORITY;
						break;
					case HIGH:
						destTopic += Constants.Misc.HIGH_PRIORITY;
						break;
					default:
						break;
				}
				
				sendStatusMessage(valueInfo.getWorkflowInstanceId(), eventStepEnum, EventStatusEnum.SUCCESS, null);
				sendMessage(destTopic, key, cr.value(), true);
			}  
		} catch (Exception e) {
			String errorMessage = StringUtility.isNullOrEmpty(e.getMessage()) ? "Errore generico durante l'invocazione del client di ini" : e.getMessage();
			elasticLogger.error("Error sending data to INI " + valueInfo.getWorkflowInstanceId() , OperationLogEnum.CALL_INI, ResultLogEnum.KO, startDateOperation, ErrorLogEnum.KO_INI);
			deadLetterHelper(e);
			if(!kafkaConsumerPropCFG.getDeadLetterExceptions().contains(e.getClass().getName())) {
				sendStatusMessage(valueInfo.getWorkflowInstanceId(), eventStepEnum, EventStatusEnum.NON_BLOCKING_ERROR, errorMessage);
			} else {
				sendStatusMessage(valueInfo.getWorkflowInstanceId(), eventStepEnum, EventStatusEnum.BLOCKING_ERROR, errorMessage);
			} 
			throw e;
		}
	}
 
}
