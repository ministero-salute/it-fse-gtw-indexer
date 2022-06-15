package it.finanze.sanita.fse2.ms.gtwindexer.service.impl;

import java.util.Date;

import org.apache.commons.lang3.exception.ExceptionUtils;
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

import it.finanze.sanita.fse2.ms.gtwindexer.client.IIniClient;
import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaPropertiesCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaTopicCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.KafkaStatusManagerDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.ResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ErrorLogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.EventStatusEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.EventTypeEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.OperationLogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ResultLogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtwindexer.logging.ElasticLoggerHelper;
import it.finanze.sanita.fse2.ms.gtwindexer.service.IKafkaSRV;
import it.finanze.sanita.fse2.ms.gtwindexer.utility.EncryptDecryptUtility;
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
public class KafkaSRV implements IKafkaSRV {

	/**
	 * Serial version uid.
	 */
	private static final long serialVersionUID = 987723954716001270L;


	/**
	 * Transactional producer.
	 */
	@Autowired
	@Qualifier("txkafkatemplate")
	private KafkaTemplate<String, String> txKafkaTemplate;

	/**
	 * Not transactional producer.
	 */
	@Autowired
	@Qualifier("notxkafkatemplate")
	private KafkaTemplate<String, String> notxKafkaTemplate;

	@Autowired
	private KafkaPropertiesCFG kafkaPropCFG;
	
	@Autowired
	private KafkaTopicCFG kafkaTopicCFG;

	@Autowired
	private IIniClient iniClient;

	@Autowired
	private ElasticLoggerHelper elasticLogger;

	@Override
	public RecordMetadata sendMessage(String topic, String key, String value, boolean trans) {
		RecordMetadata out = null;
		ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value); 
		try { 
			out = kafkaSend(record, trans);
		} catch (Exception e) {
			log.error("Send failed.", e); 
			throw new BusinessException(e);
		}   
		return out;
	} 

	private RecordMetadata kafkaSend(ProducerRecord<String, String> record, boolean trans) {
		RecordMetadata out = null;
		Object result = null;

		if (trans) {  
			result = txKafkaTemplate.executeInTransaction(t -> { 
				try {
					return t.send(record).get();
				} catch (Exception e) {
					throw new BusinessException(e);
				}  
			});  
		} else { 
			notxKafkaTemplate.send(record);
		} 

		if(result != null) {
			SendResult<String,String> sendResult = (SendResult<String,String>) result;
			out = sendResult.getRecordMetadata();
			log.info("Send success.");
		}

		return out;
	}

	@Override
	@KafkaListener(topics = "#{'${kafka.dispatcher-indexer.topic}'}",  clientIdPrefix = "#{'${kafka.consumer.client-id}'}", containerFactory = "kafkaListenerDeadLetterContainerFactory", autoStartup = "${event.topic.auto.start}", groupId = "#{'${kafka.consumer.group-id}'}")
	public void listener(final ConsumerRecord<String, String> cr, final MessageHeaders messageHeaders) {

		Date startDateOperation = new Date();
		String transactionId = "";

		EventTypeEnum eventStepEnum = null;
		try {
			boolean sendStatusManagerMessage = true;
			String message = cr.value();
			log.info("Consuming Transaction Event - Message received with key {}", cr.key());
			transactionId = EncryptDecryptUtility.decryptObject(kafkaPropCFG.getCrypto(), message, String.class);

			if(!StringUtility.isNullOrEmpty(transactionId)) {
				log.info("TRANSACTION ID FROM DISPATCHER : " + transactionId);
				ResponseDTO response = iniClient.sendData(transactionId);
				if(response != null) {
					String cryptoMessage = EncryptDecryptUtility.encryptObject(kafkaPropCFG.getCrypto(), transactionId);
					sendMessage(kafkaTopicCFG.getIndexerPublisherTopic(), "key", cryptoMessage, true);
					eventStepEnum = EventTypeEnum.SEND_TO_INI;
				}
			} else {
				log.warn("Error consuming Validation Event with key {}: null received", cr.key());
				elasticLogger.error("Error consuming Validation Event with key " + cr.key() + " null received", OperationLogEnum.CALL_INI, ResultLogEnum.KO, startDateOperation, ErrorLogEnum.KO_INI);
				sendStatusManagerMessage = false;
			}

			if(Boolean.TRUE.equals(sendStatusManagerMessage)) {
				sendStatusMessage(transactionId, eventStepEnum, EventStatusEnum.SUCCESS,null);
			}

			elasticLogger.info("Successfully sent data to INI for transactionId " + transactionId, OperationLogEnum.CALL_INI, ResultLogEnum.OK, startDateOperation);

		} catch (Exception e) {
			if(eventStepEnum==null) {
				eventStepEnum = EventTypeEnum.GENERIC_ERROR;
			}

			elasticLogger.error("Error sending data to INI", OperationLogEnum.CALL_INI, ResultLogEnum.KO, startDateOperation, ErrorLogEnum.KO_INI);

			sendStatusMessage(transactionId, eventStepEnum, EventStatusEnum.ERROR,ExceptionUtils.getStackTrace(e));
			deadLetterHelper(e);
			throw new BusinessException(e);
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

		log.error("{}", sb.toString());
	}

	@Override
	public void sendStatusMessage(final String transactionId,final EventTypeEnum eventType,
			final EventStatusEnum eventStatus, String exception) {
		try {
			KafkaStatusManagerDTO statusManagerMessage = KafkaStatusManagerDTO.builder().
					eventType(eventType).
					eventDate(new Date()).
					eventStatus(eventStatus).
					exception(exception).
					build();
			String json = StringUtility.toJSONJackson(statusManagerMessage);
			String cryptoMessage = EncryptDecryptUtility.encryptObject(kafkaPropCFG.getCrypto(), json);
			sendMessage(kafkaTopicCFG.getStatusManagerTopic(), transactionId, cryptoMessage, true);
		} catch(Exception ex) {
			log.error("Error while send status message on indexer : " , ex);
			throw new BusinessException(ex);
		}
	}
}
