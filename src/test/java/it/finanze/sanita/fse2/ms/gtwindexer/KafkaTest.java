package it.finanze.sanita.fse2.ms.gtwindexer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Description;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.ActiveProfiles;

import com.google.gson.Gson;

import it.finanze.sanita.fse2.ms.gtwindexer.client.IIniClient;
import it.finanze.sanita.fse2.ms.gtwindexer.config.Constants;
import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaTopicCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IndexerValueDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniPublicationResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtwindexer.service.IKafkaSRV;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan(basePackages = {Constants.ComponentScan.BASE})
@ActiveProfiles(Constants.Profile.TEST)
class KafkaTest {
	
	@Autowired
	private IKafkaSRV kafkaSRV;

	@Autowired
	private KafkaTopicCFG kafkaTopicCFG;

	@MockBean
	private IIniClient iniClient;
	
	
	@DisplayName("Producer send")
	void kafkaProducerSendTest() {  
		String key = "1";
		String topic = "transactionEvents"; 
 
	/*****************TOPIC**********************/ 
		String message = "Messaggio numero : " + 1;
		RecordMetadata output = kafkaSRV.sendMessage(topic,key, message, true);
		assertEquals(message.length(), output.serializedValueSize() , "Il value non coincide");
		assertEquals(topic,output.topic(), "Il topic non coincide");
		
    }
	

	@Test
	@Description("Error in listening messages on topic")
	void kafkaListenerErrorTest() {

		String value = "{\"workflowInstanceId\":\"wii1\",\"identificativoDocUpdate\":\"id1\"}";
		String topic = "topic";
		Map<String, Object> map = new HashMap<>();
		MessageHeaders headers = new MessageHeaders(map);
	    
		Map<TopicPartition, List<ConsumerRecord<String, String>>> records = new LinkedHashMap<>();

	    records.put(new TopicPartition(topic, 0), new ArrayList<>());
		ConsumerRecord<String, String> record = new ConsumerRecord<>(topic, 1, 0, 0L, TimestampType.CREATE_TIME, 0L, 0, 0, topic, value);


	    map.put("test", value);

		BDDMockito.given(iniClient.sendPublicationData(anyString())).willThrow(BusinessException.class);
		BDDMockito.given(iniClient.sendReplaceData(Mockito.any(IndexerValueDTO.class))).willThrow(BusinessException.class);

		assertThrows(BusinessException.class, ()->kafkaSRV.lowPriorityListener(record, headers));
		assertThrows(BusinessException.class, ()->kafkaSRV.mediumPriorityListener(record, headers));
		assertThrows(BusinessException.class, ()->kafkaSRV.highPriorityListener(record, headers));
	}

	@Test
	@Description("Success")
	void kafkaListenerSuccessTest() {

		int value = 123;
		String topicLow = kafkaTopicCFG.getDispatcherIndexerLowPriorityTopic();
		String topicMedium = kafkaTopicCFG.getDispatcherIndexerMediumPriorityTopic();
		String topicHigh = kafkaTopicCFG.getDispatcherIndexerHighPriorityTopic();

		Map<String, Object> map = new HashMap<>();
		MessageHeaders headers = new MessageHeaders(map);

		Map<TopicPartition, List<ConsumerRecord<String, String>>> records = new LinkedHashMap<>();

		records.put(new TopicPartition(topicLow, 0), new ArrayList<>());
		records.put(new TopicPartition(topicMedium, 0), new ArrayList<>());
		records.put(new TopicPartition(topicHigh, 0), new ArrayList<>());
		
		final String kafkaValue = new Gson().toJson(new IndexerValueDTO(TestConstants.testWorkflowInstanceId, null));
		
		ConsumerRecord<String, String> recordLow = new ConsumerRecord<>(topicLow, 1, 0, 0L, TimestampType.CREATE_TIME, 0L, 0, 0, topicLow, kafkaValue);
		ConsumerRecord<String, String> recordMedium = new ConsumerRecord<>(topicMedium, 1, 0, 0L, TimestampType.CREATE_TIME, 0L, 0, 0, topicMedium, kafkaValue);
		ConsumerRecord<String, String> recordHigh = new ConsumerRecord<>(topicHigh, 1, 0, 0L, TimestampType.CREATE_TIME, 0L, 0, 0, topicHigh, kafkaValue);

		map.put("test", value);

		IniPublicationResponseDTO responseDTO = new IniPublicationResponseDTO();
		responseDTO.setEsito(true);
		BDDMockito.given(iniClient.sendPublicationData(anyString())).willReturn(responseDTO);

		assertDoesNotThrow(() -> kafkaSRV.lowPriorityListener(recordLow, headers));
		assertDoesNotThrow(() -> kafkaSRV.mediumPriorityListener(recordMedium, headers));
		assertDoesNotThrow(() -> kafkaSRV.highPriorityListener(recordHigh, headers));
	}
  
}
