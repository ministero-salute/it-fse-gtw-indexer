/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.config.kafka;

import java.util.Properties;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import it.finanze.sanita.fse2.ms.gtwindexer.utility.ProfileUtility;
import lombok.Data;

/**
 *	Kafka properties configuration.
 */
@Data
@Component
public class KafkaPropertiesCFG {
  
	/**
	 *  Boostrap server.
	 */
	@Value("${kafka.bootstrap-servers}")
	private String producerBootstrapServers;
	 
	
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
	 
	@Autowired
	private ProfileUtility profileUtility;

	@Bean
	public AdminClient client() {
		Properties configProperties = new Properties();
    	configProperties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, producerBootstrapServers);
    	if(!profileUtility.isDevOrDockerProfile() && !profileUtility.isTestProfile()) {
    		configProperties.put("security.protocol", protocol);
    		configProperties.put("sasl.mechanism", mechanism);
    		configProperties.put("sasl.jaas.config", configJaas);
    		configProperties.put("ssl.truststore.location", trustoreLocation);  
    		configProperties.put("ssl.truststore.password", String.valueOf(trustorePassword)); 
		}
		return AdminClient.create(configProperties);
	}
}
