/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * 
 * Copyright (C) 2023 Ministero della Salute
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
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

import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.oauth2.CustomAuthenticateCallbackHandler;
import it.finanze.sanita.fse2.ms.gtwindexer.utility.StringUtility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class KafkaConsumerCFG {

	public static final int MAX_ATTEMPT = 5;

	/**
	 *	Kafka consumer properties.
	 */
	@Autowired
	private KafkaConsumerPropertiesCFG kafkaConsumerPropCFG;
	
	@Autowired
	private KafkaPropertiesCFG kafkaPropCFG;

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

		props.put(ConsumerConfig.CLIENT_ID_CONFIG, kafkaConsumerPropCFG.getClientId());
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaPropCFG.getBootstrapServers());
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, kafkaConsumerPropCFG.getConsumerKeyDeserializer());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, kafkaConsumerPropCFG.getConsumerValueDeserializer());
		props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, kafkaConsumerPropCFG.getIsolationLevel());
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaConsumerPropCFG.getAutoCommit());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaConsumerPropCFG.getAutoOffsetReset());

		if(!StringUtility.isNullOrEmpty(kafkaConsumerPropCFG.getProtocol())) {
			props.put("security.protocol", kafkaConsumerPropCFG.getProtocol());
		}

		if(!StringUtility.isNullOrEmpty(kafkaConsumerPropCFG.getMechanism())) {
			props.put("sasl.mechanism", kafkaConsumerPropCFG.getMechanism());
		}

		if(!StringUtility.isNullOrEmpty(kafkaConsumerPropCFG.getConfigJaas())) {
			props.put("sasl.jaas.config", kafkaConsumerPropCFG.getConfigJaas());
		}

		if(!StringUtility.isNullOrEmpty(kafkaConsumerPropCFG.getTrustoreLocation())) {
			props.put("ssl.truststore.location", kafkaConsumerPropCFG.getTrustoreLocation());
		}

		if(kafkaConsumerPropCFG.getTrustorePassword()!=null && kafkaConsumerPropCFG.getTrustorePassword().length>0) {
			props.put("ssl.truststore.password", String.valueOf(kafkaConsumerPropCFG.getTrustorePassword()));
		}
		
		if("OAUTHBEARER".equals(kafkaPropCFG.getMechanism())) {
			props.put("sasl.login.callback.handler.class", CustomAuthenticateCallbackHandler.class);
			props.put("kafka.oauth.tenantId", kafkaPropCFG.getTenantId());	
			props.put("kafka.oauth.appId", kafkaPropCFG.getAppId());
			props.put("kafka.oauth.pfxPathName", kafkaPropCFG.getPfxPathName());
			props.put("kafka.oauth.pwd", kafkaPropCFG.getPwd());	
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
	 * @param dlt
	 * @return	factory
	 */
	@Bean
	public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> kafkaListenerDeadLetterContainerFactory(final @Qualifier("notxkafkadeadtemplate") KafkaTemplate<Object, Object> dlt) {

		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory());
		factory.getContainerProperties().setDeliveryAttemptHeader(true);

		// Definizione nome topic deadLetter
		log.debug("TOPIC: " + kafkaTopicCFG.getDispatcherIndexerDeadLetterTopic());
		DeadLetterPublishingRecoverer dlpr = new DeadLetterPublishingRecoverer(
				dlt, (consumerRecord, ex) -> new TopicPartition(kafkaTopicCFG.getDispatcherIndexerDeadLetterTopic(), -1));

		// Set classificazione errori da gestire per la deadLetter.
		DefaultErrorHandler policy = new DefaultErrorHandler(dlpr, new FixedBackOff());

		log.debug("setClassification - kafkaListenerDeadLetterContainerFactory: ");
		setClassification(policy);

		// da eliminare se non si volesse gestire la dead letter
		factory.setCommonErrorHandler(policy);

		return factory;
	}

	private void setClassification(final DefaultErrorHandler sceh) {
		List<Class<? extends Exception>> out = getExceptionsConfig();

		for (Class<? extends Exception> ex : out) {
			log.warn("Found non retryable exception: {}", ex);
			sceh.addNotRetryableExceptions(ex);
		}
	}

	/**
	 * @return	exceptions list
	 */
	@SuppressWarnings("unchecked")
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
