package it.finanze.sanita.fse2.ms.gtwindexer.service;

import java.io.Serializable;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.messaging.MessageHeaders;
 

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
	 * Kafka listener
	 * @param cr
	 * @param messageHeaders
	 */
	void listener(ConsumerRecord<String, String> cr, MessageHeaders messageHeaders);
 
	
}
