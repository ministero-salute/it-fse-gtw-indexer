package it.finanze.sanita.fse2.ms.gtwindexer.config.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 *	@author vincenzoingenito
 *
 *	Kafka topic configuration.
 */
@Data
@Component
public class KafkaTopicCFG {

	/**
	 * Dispatcher indexer Topic.
	 */
	@Value("${kafka.dispatcher-indexer.topic}")
	private String dispatcherIndexerTopic;
	
	/**
	 * Dispatcher indexer Dead letter Topic. 
	 */
	@Value("${kafka.dispatcher-indexer.deadletter.topic}")
	private String dispatcherIndexerDeadLetterTopic;
	
	/**
	 * Indexer publisher Topic.
	 */
	@Value("${kafka.indexer-publisher.topic}")
	private String indexerPublisherTopic;
	
	/**
	 * Indexer status manager Topic.
	 */
	@Value("${kafka.statusmanager.topic}")
	private String statusManagerTopic;
	
}
