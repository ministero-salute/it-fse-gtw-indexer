package it.finanze.sanita.fse2.ms.gtwindexer.service.impl;

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

import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaPropertiesCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaTopicCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtwindexer.service.IIniInvocationSRV;
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
	private IIniInvocationSRV iniInvocationSRV;
	
	@Autowired
	private KafkaTopicCFG kafkaTopicCFG;

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
		String message = cr.value();
		log.info("Consuming Transaction Event - Message received with key {}", cr.key());

		try {
			String transactionId = EncryptDecryptUtility.decryptObject(kafkaPropCFG.getCrypto(), message, String.class);
			
			if(!StringUtility.isNullOrEmpty(transactionId)) {
				System.out.println("TRANSACTION ID FROM DISPATCHER : " + transactionId);
				Boolean sendToIniCompleted = iniInvocationSRV.findAndSendToIniByTransactionId(transactionId);
				if(Boolean.TRUE.equals(sendToIniCompleted)) {
					String cryptoMessage = EncryptDecryptUtility.encryptObject(kafkaPropCFG.getCrypto(), transactionId);
					sendMessage(kafkaTopicCFG.getIndexerPublisherTopic(), "key", cryptoMessage, true);
				}
			} else {
				log.warn("Error consuming Validation Event with key {}: null received", cr.key());
			}

		} catch (Exception e) {
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


}
