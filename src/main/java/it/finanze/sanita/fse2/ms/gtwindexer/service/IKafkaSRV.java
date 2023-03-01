/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.service;

import it.finanze.sanita.fse2.ms.gtwindexer.enums.EventStatusEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.EventTypeEnum;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Header;
 

public interface IKafkaSRV {

	/**
	 * Send message over kafka topic
	 * @param topic
	 * @param key
	 * @param value
	 * @param trans
	 * @return
	 */
	RecordMetadata sendMessage(String topic, String key, String value, boolean trans);

	/**
	 * Kafka low priority listener
	 * @param cr
	 * @param messageHeaders
	 * @throws InterruptedException
	 */
	void lowPriorityListener(ConsumerRecord<String, String> cr, MessageHeaders messageHeaders, @Header(KafkaHeaders.DELIVERY_ATTEMPT) int delivery) throws Exception;

	/**
	 * Kafka med priority listener
	 * @param cr
	 * @param messageHeaders
	 * @throws InterruptedException
	 */
	void mediumPriorityListener(final ConsumerRecord<String, String> cr, final MessageHeaders messageHeaders, @Header(KafkaHeaders.DELIVERY_ATTEMPT) int delivery) throws Exception;

	/**
	 * Kafka high priority listener
	 * @param cr
	 * @param messageHeaders
	 * @throws InterruptedException
	 */
	void highPriorityListener(final ConsumerRecord<String, String> cr, final MessageHeaders messageHeaders, @Header(KafkaHeaders.DELIVERY_ATTEMPT) int delivery) throws Exception;
	
	void sendStatusMessage(String workflowInstanceId, EventTypeEnum eventType, EventStatusEnum eventStatus, String exception);

	void retryDeleteListener(ConsumerRecord<String, String> cr, MessageHeaders messageHeaders, int delivery) throws Exception;
	
	void retryUpdateListener(ConsumerRecord<String, String> cr, MessageHeaders messageHeaders, int delivery) throws Exception;

}
