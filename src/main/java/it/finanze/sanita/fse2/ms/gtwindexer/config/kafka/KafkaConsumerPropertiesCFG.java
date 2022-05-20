package it.finanze.sanita.fse2.ms.gtwindexer.config.kafka;

import java.io.Serializable;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 *	@author vincenzoingenito
 *
 *	Kafka consumer properties configuration.
 */
@Data
@Component
public class KafkaConsumerPropertiesCFG implements Serializable {

	/**
	 * Serial version uid.
	 */
	private static final long serialVersionUID = 4863316988401046567L;

	/**
	 * Client id.
	 */
	@Value("${kafka.consumer.client-id}")
	private String clientId;

	/**
	 * Group id consumer.
	 */
	@Value("${kafka.consumer.group-id}")
	private String consumerGroupId;

	/**
	 * Consumer key deserializer.
	 */
	@Value("${kafka.consumer.key-deserializer}")
	private String consumerKeyDeserializer;

	/**
	 * Consumer value deserializer.
	 */
	@Value("${kafka.consumer.value-deserializer}")
	private String consumerValueDeserializer;

	/**
	 * Consumer bootstrap server.
	 */
	@Value("${kafka.consumer.bootstrap-servers}")
	private String consumerBootstrapServers;

	/**
	 * Isolation level.
	 */
	@Value("${kafka.consumer.isolation.level}")
	private String isolationLevel;

	/**
	 * Flag autocommit.
	 */
	@Value("${kafka.consumer.auto-commit}")
	private String autoCommit;

	/**
	 * Flag auto offset reset.
	 */
	@Value("${kafka.consumer.auto-offset-reset}")
	private String autoOffsetReset;

	/**
	 * Eccezioni per dead letter.
	 */
	@Value("#{${kafka.consumer.dead-letter-exc}}")
	private List<String> deadLetterExceptions;

	/**
	 * Protocollo.
	 */
	@Value("${kafka.properties.security.protocol}")
	private String protocol;

	/**
	 * Meccanismo.
	 */
	@Value("${kafka.properties.sasl.mechanism}")
	private String mechanism;

	/**
	 * Config jaas.
	 */
	@Value("${kafka.properties.sasl.jaas.config}")
	private String configJaas;

	/**
	 * Truststore location.
	 */
	@Value("${kafka.properties.ssl.truststore.location}")
	private String trustoreLocation;

	/**
	 * Truststore password.
	 */
	@Value("${kafka.properties.ssl.truststore.password}")
	private transient char[] trustorePassword;

	/**
	 * Flag enable ssl.
	 */
	@Value("${kafka.enablessl}")
	private boolean enableSsl;

}
