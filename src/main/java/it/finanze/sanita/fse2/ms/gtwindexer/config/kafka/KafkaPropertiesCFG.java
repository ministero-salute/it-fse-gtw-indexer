package it.finanze.sanita.fse2.ms.gtwindexer.config.kafka;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 *	@author vincenzoingenito
 *
 *	Kafka properties configuration.
 */
@Data
@Component
public class KafkaPropertiesCFG implements Serializable {
  
	/**
	 *  Serial version uid
	 */
	private static final long serialVersionUID = -7936473659737067416L;

	/**
	 *  Boostrap server.
	 */
	@Value("${kafka.bootstrap-servers}")
	private String producerBootstrapServers;
	
	/**
	 * Crypto.
	 */
	@Value("${kafka.crypto}")
	private String crypto;
	
	/**
	 * Security protocol.
	 */
	@Value("${kafka.properties.security.protocol}")
	private String protocol;
	
	/**
	 * Sasl mechanism.
	 */
	@Value("${kafka.properties.sasl.mechanism}")
	private String mechanism;
	
	/**
	 * Jaas config.
	 */
	@Value("${kafka.properties.sasl.jaas.config}")
	private String configJaas;
	
	/**
	 * Trustore location.
	 */
	@Value("${kafka.properties.ssl.truststore.location}")
	private String trustoreLocation;
	
	/**
	 * Trustore password.
	 */
	@Value("${kafka.properties.ssl.truststore.password}")
	private transient char[] trustorePassword;
	 
	/**
	 * Enable Ssl flag.
	 */
	@Value("${kafka.enablessl}")
	private boolean enableSSL;
	
}
