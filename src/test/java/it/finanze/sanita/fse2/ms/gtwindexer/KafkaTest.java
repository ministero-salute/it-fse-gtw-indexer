package it.finanze.sanita.fse2.ms.gtwindexer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import it.finanze.sanita.fse2.ms.gtwindexer.config.Constants;
import it.finanze.sanita.fse2.ms.gtwindexer.service.IKafkaSRV;
import it.finanze.sanita.fse2.ms.gtwindexer.utility.EncryptDecryptUtility;
import lombok.Data;
import lombok.NoArgsConstructor;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan(basePackages = {Constants.ComponentScan.BASE})
@ActiveProfiles(Constants.Profile.DEV)
public class KafkaTest {
	
	@Autowired
	private IKafkaSRV kafkaSRV;

	@Test
	@DisplayName("Producer send")
	void kafkaProducerSend() {  
		String key = "1";
		String topic = "transactionEvents"; // kafkaCFG.getTopic();
 
		/*****************TOPIC**********************/ 
		for(int i=0; i<10; i++) { 
			KafkaMessageDTO msg = new KafkaMessageDTO();
			msg.setMessage("Messaggio numero : " + 1);
			String message = EncryptDecryptUtility.encryptObject("fse", msg);
			RecordMetadata output = kafkaSRV.sendMessage(topic,key, message, true);
			assertEquals(message.length(), output.serializedValueSize() , "Il value non coincide");
			assertEquals(topic,output.topic(), "Il topic non coincide");
		} 
		 
    }
 
	@Data
	@NoArgsConstructor
	class KafkaMessageDTO{
		String message;
		
	}
}
