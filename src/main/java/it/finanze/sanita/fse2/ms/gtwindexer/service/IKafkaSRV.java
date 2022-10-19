/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.service;

import java.io.Serializable;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.messaging.MessageHeaders;

import it.finanze.sanita.fse2.ms.gtwindexer.enums.EventStatusEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.EventTypeEnum;
 

public interface IKafkaSRV extends Serializable {

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
    void lowPriorityListener(ConsumerRecord<String, String> cr, MessageHeaders messageHeaders) throws InterruptedException;

	/**
	 * Kafka med priority listener
	 * @param cr
	 * @param messageHeaders
	 * @throws InterruptedException
	 */
	void mediumPriorityListener(ConsumerRecord<String, String> cr, MessageHeaders messageHeaders) throws InterruptedException;

	/**
	 * Kafka high priority listener
	 * @param cr
	 * @param messageHeaders
	 * @throws InterruptedException
	 */
	void highPriorityListener(ConsumerRecord<String, String> cr, MessageHeaders messageHeaders) throws InterruptedException;

	void sendStatusMessage(String workflowInstanceId, EventTypeEnum eventType, EventStatusEnum eventStatus, String exception);
	
}
