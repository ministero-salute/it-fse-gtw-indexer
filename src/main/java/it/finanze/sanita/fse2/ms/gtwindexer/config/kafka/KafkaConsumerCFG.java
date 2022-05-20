package it.finanze.sanita.fse2.ms.gtwindexer.config.kafka;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class KafkaConsumerCFG {

	/**
	 *	Kafka properties.
	 */
	@Autowired
	private KafkaPropertiesCFG kafkaPropCFG;

	/**
	 *	Kafka consumer properties.
	 */
	@Autowired
	private KafkaConsumerPropertiesCFG kafkaConsumerPropCFG;


	@Autowired
	private KafkaTopicCFG kafkaTopicCFG;
	
	/**
	 * Configurazione consumer.
	 * 
	 * @return	configurazione consumer
	 */
	@Bean
	public Map<String, Object> consumerConfigs() {
		Map<String, Object> props = new HashMap<>();
		
		log.info("CLIENT_ID_CONFIG: " + kafkaConsumerPropCFG.getClientId());
		props.put(ConsumerConfig.CLIENT_ID_CONFIG, kafkaConsumerPropCFG.getClientId());
		
		log.info("BOOTSTRAP_SERVERS_CONFIG: " + kafkaConsumerPropCFG.getConsumerBootstrapServers());
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConsumerPropCFG.getConsumerBootstrapServers());
		
		log.info("GROUP_ID_CONFIG: " + kafkaConsumerPropCFG.getConsumerGroupId());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConsumerPropCFG.getConsumerGroupId());
		
		log.info("KEY_DESERIALIZER_CLASS_CONFIG: " + kafkaConsumerPropCFG.getConsumerKeyDeserializer());
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, kafkaConsumerPropCFG.getConsumerKeyDeserializer());
		
		log.info("VALUE_DESERIALIZER_CLASS_CONFIG: " + kafkaConsumerPropCFG.getConsumerValueDeserializer());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, kafkaConsumerPropCFG.getConsumerValueDeserializer());
		
		log.info("ISOLATION_LEVEL_CONFIG: " + kafkaConsumerPropCFG.getIsolationLevel());
		props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, kafkaConsumerPropCFG.getIsolationLevel());
		
		log.info("ENABLE_AUTO_COMMIT_CONFIG: " + kafkaConsumerPropCFG.getAutoCommit());
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaConsumerPropCFG.getAutoCommit());
		
		log.info("AUTO_OFFSET_RESET_CONFIG: " + kafkaConsumerPropCFG.getAutoOffsetReset());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaConsumerPropCFG.getAutoOffsetReset());
		
		//SSL
		if (kafkaConsumerPropCFG.isEnableSsl()) { 
			props.put("security.protocol", kafkaConsumerPropCFG.getProtocol());
			props.put("sasl.mechanism", kafkaConsumerPropCFG.getMechanism());
			props.put("sasl.jaas.config", kafkaConsumerPropCFG.getConfigJaas());
			props.put("ssl.truststore.location", kafkaConsumerPropCFG.getTrustoreLocation());
			props.put("ssl.truststore.password", String.valueOf(kafkaConsumerPropCFG.getTrustorePassword()));
		}
		return props;
	}


	/**
	 * Consumer factory.
	 * 
	 * @return	factory
	 */
	@Bean
	public ConsumerFactory<String, String> consumerFactory() {
		return new DefaultKafkaConsumerFactory<>(consumerConfigs());
	}

	/**
	 * Factory with dead letter configuration.
	 * 
	 * @param deadLetterKafkaTemplate
	 * @return	factory
	 */
	@Bean
	public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> kafkaListenerDeadLetterContainerFactory(final @Qualifier("notxkafkadeadtemplate") KafkaTemplate<Object, Object> deadLetterKafkaTemplate) {
		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory());
		
		// Definizione nome topic deadLetter
		log.info("TOPIC: " + kafkaTopicCFG.getDispatcherIndexerDeadLetterTopic());
		DeadLetterPublishingRecoverer dlpr = new DeadLetterPublishingRecoverer(deadLetterKafkaTemplate, (record, ex) -> new TopicPartition(kafkaTopicCFG.getDispatcherIndexerDeadLetterTopic(), -1));
		
		// Set classificazione errori da gestire per la deadLetter.
		DefaultErrorHandler sceh = new DefaultErrorHandler(dlpr, new FixedBackOff(FixedBackOff.DEFAULT_INTERVAL, FixedBackOff.UNLIMITED_ATTEMPTS));
		
		log.info("setClassification - kafkaListenerDeadLetterContainerFactory: ");
		setClassification(sceh);
		
		// da eliminare se non si volesse gestire la dead letter
		factory.setCommonErrorHandler(sceh); 

		return factory;
	}
	
	private void setClassification(final DefaultErrorHandler sceh) {
		List<Class<? extends Exception>> out = getExceptionsConfig();

		for (Class<? extends Exception> ex : out) {
			log.info("addNotRetryableException: " + ex);
			sceh.addNotRetryableExceptions(ex);
		}
		
	}

	/**
	 * @return	exceptions list
	 */
	private List<Class<? extends Exception>> getExceptionsConfig() {
		List<Class<? extends Exception>> out = new ArrayList<>();
		String temp = null;
		try {
			for (String excs : kafkaConsumerPropCFG.getDeadLetterExceptions()) {
				temp = excs;
				Class<? extends Exception> s = (Class<? extends Exception>) Class.forName(excs, false, Thread.currentThread().getContextClassLoader());
				out.add(s);
			}
		} catch (Exception e) {
			log.error("Error retrieving the exception with fully qualified name: <{}>", temp);
			log.error("Error : ", e);
		}
		
		return out;
	}
	
	/**
	 * Default Container factory.
	 * 
	 * @param kafkaTemplate	templete
	 * @return				factory
	 */
	@Bean
	public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> kafkaListenerContainerFactory(final @Qualifier("notxkafkatemplate") KafkaTemplate<String, String> kafkaTemplate) {
		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory());
		
		return factory;
	}
}
