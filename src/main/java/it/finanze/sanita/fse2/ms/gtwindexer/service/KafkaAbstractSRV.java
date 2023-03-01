/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.service;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaTopicCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public abstract class KafkaAbstractSRV {
	/**
	 * Transactional producer.
	 */
	@Autowired
	@Qualifier("txkafkatemplate")
	protected KafkaTemplate<String, String> txKafkaTemplate;

	/**
	 * Not transactional producer.
	 */
	@Autowired
	@Qualifier("notxkafkatemplate")
	protected KafkaTemplate<String, String> notxKafkaTemplate;

	@Autowired
	protected KafkaTopicCFG topics;

	public RecordMetadata sendMessage(String topic, String key, String value, boolean trans) {
		RecordMetadata out;
		ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, key, value);
		try {
			out = kafkaSend(producerRecord, trans);
		} catch (Exception e) {
			log.error("Send failed.", e);
			throw new BusinessException(e);
		}
		return out;
	}

	protected RecordMetadata kafkaSend(ProducerRecord<String, String> producerRecord, boolean trans) {
		RecordMetadata out = null;
		SendResult<String, String> result = null;

		if (trans) {
			result = txKafkaTemplate.executeInTransaction(t -> {
				try {
					return t.send(producerRecord).get();
				} catch (InterruptedException e) {
					log.error("InterruptedException caught. Interrupting thread...");
					Thread.currentThread().interrupt();
					throw new BusinessException(e);
				} catch (Exception e) {
					throw new BusinessException(e);
				}
			});
		} else {
			notxKafkaTemplate.send(producerRecord);
		}

		if (result != null) {
			out = result.getRecordMetadata();
			log.debug("Kafka message sent successfully.");
		}

		return out;
	}
}
