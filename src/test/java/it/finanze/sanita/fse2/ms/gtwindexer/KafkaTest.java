package it.finanze.sanita.fse2.ms.gtwindexer;

import com.google.gson.Gson;
import it.finanze.sanita.fse2.ms.gtwindexer.config.Constants;
import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaTopicCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IndexerValueDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniPublicationResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ProcessorOperationEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.ConnectionRefusedException;
import it.finanze.sanita.fse2.ms.gtwindexer.service.IKafkaSRV;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan(basePackages = {Constants.ComponentScan.BASE})
@ActiveProfiles(Constants.Profile.TEST)
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
class KafkaTest extends AbstractTest {
	
	@Autowired
	private IKafkaSRV kafkaSRV;

	@Autowired
	private KafkaTopicCFG kafkaTopicCFG;

	@SpyBean
	private RestTemplate restTemplate;

	@Test
	@Description("Publication - Success")
	void kafkaListenerPublicationSuccessTest() throws ExecutionException, InterruptedException {
		String topicLow = kafkaTopicCFG.getDispatcherIndexerLowPriorityTopic();
		String topicMedium = kafkaTopicCFG.getDispatcherIndexerMediumPriorityTopic();
		String topicHigh = kafkaTopicCFG.getDispatcherIndexerHighPriorityTopic();

		Map<String, Object> map = new HashMap<>();
		MessageHeaders headers = new MessageHeaders(map);

		Map<TopicPartition, List<ConsumerRecord<String, String>>> records = new LinkedHashMap<>();

		records.put(new TopicPartition(topicLow, 0), new ArrayList<>());
		records.put(new TopicPartition(topicMedium, 0), new ArrayList<>());
		records.put(new TopicPartition(topicHigh, 0), new ArrayList<>());
		
		final String kafkaValue = new Gson().toJson(new IndexerValueDTO(TestConstants.testWorkflowInstanceId, "String", ProcessorOperationEnum.PUBLISH));

		this.kafkaInit(topicLow, TestConstants.testWorkflowInstanceId, kafkaValue);
		this.kafkaInit(topicMedium, TestConstants.testWorkflowInstanceId, kafkaValue);
		this.kafkaInit(topicHigh, TestConstants.testWorkflowInstanceId, kafkaValue);

		ConsumerRecord<String, String> recordLow = new ConsumerRecord<String,String>(topicLow, 1, 0, topicLow, kafkaValue);
		ConsumerRecord<String, String> recordMedium = new ConsumerRecord<String,String>(topicMedium, 1, 0, topicMedium, kafkaValue);
		ConsumerRecord<String, String> recordHigh = new ConsumerRecord<String,String>(topicHigh, 1, 0, topicHigh, kafkaValue);

		IniPublicationResponseDTO responseDTO = new IniPublicationResponseDTO();
		responseDTO.setEsito(true);
		Mockito.doReturn(new ResponseEntity<>(responseDTO, HttpStatus.OK)).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(IniPublicationResponseDTO.class));

		assertDoesNotThrow(() -> kafkaSRV.lowPriorityListener(recordLow, headers));
		assertDoesNotThrow(() -> kafkaSRV.mediumPriorityListener(recordMedium, headers));
		assertDoesNotThrow(() -> kafkaSRV.highPriorityListener(recordHigh, headers));
	}

	@Test
	@Description("Publication - Error tests")
	void kafkaListenerPublicationErrorsTest() throws ExecutionException, InterruptedException {
		String topicLow = kafkaTopicCFG.getDispatcherIndexerLowPriorityTopic();
		String topicMedium = kafkaTopicCFG.getDispatcherIndexerMediumPriorityTopic();
		String topicHigh = kafkaTopicCFG.getDispatcherIndexerHighPriorityTopic();

		Map<String, Object> map = new HashMap<>();
		MessageHeaders headers = new MessageHeaders(map);

		Map<TopicPartition, List<ConsumerRecord<String, String>>> records = new LinkedHashMap<>();

		records.put(new TopicPartition(topicLow, 0), new ArrayList<>());
		records.put(new TopicPartition(topicMedium, 0), new ArrayList<>());
		records.put(new TopicPartition(topicHigh, 0), new ArrayList<>());

		final String kafkaValue = new Gson().toJson(new IndexerValueDTO(TestConstants.testWorkflowInstanceId, "String", ProcessorOperationEnum.PUBLISH));

		this.kafkaInit(topicLow, TestConstants.testWorkflowInstanceId, kafkaValue);
		this.kafkaInit(topicMedium, TestConstants.testWorkflowInstanceId, kafkaValue);
		this.kafkaInit(topicHigh, TestConstants.testWorkflowInstanceId, kafkaValue);

		ConsumerRecord<String, String> recordLow = new ConsumerRecord<String,String>(topicLow, 1, 0, topicLow, kafkaValue);
		ConsumerRecord<String, String> recordMedium = new ConsumerRecord<String,String>(topicMedium, 1, 0, topicMedium, kafkaValue);
		ConsumerRecord<String, String> recordHigh = new ConsumerRecord<String,String>(topicHigh, 1, 0, topicHigh, kafkaValue);

		IniPublicationResponseDTO responseDTO = new IniPublicationResponseDTO();
		responseDTO.setEsito(false);

		// Connection refused
		Mockito.doThrow(new ConnectionRefusedException("uri", "error")).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(IniPublicationResponseDTO.class));

		assertThrows(ConnectionRefusedException.class, () -> kafkaSRV.lowPriorityListener(recordLow, headers));
		assertThrows(ConnectionRefusedException.class, () -> kafkaSRV.mediumPriorityListener(recordMedium, headers));
		assertThrows(ConnectionRefusedException.class, () -> kafkaSRV.highPriorityListener(recordHigh, headers));

		// ResourceAccessException
		Mockito.doThrow(new ResourceAccessException("uri")).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(IniPublicationResponseDTO.class));

		assertThrows(ResourceAccessException.class, () -> kafkaSRV.lowPriorityListener(recordLow, headers));
		assertThrows(ResourceAccessException.class, () -> kafkaSRV.mediumPriorityListener(recordMedium, headers));
		assertThrows(ResourceAccessException.class, () -> kafkaSRV.highPriorityListener(recordHigh, headers));

		// Generic exception
		Mockito.doThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY)).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(IniPublicationResponseDTO.class));

		assertThrows(BusinessException.class, () -> kafkaSRV.lowPriorityListener(recordLow, headers));
		assertThrows(BusinessException.class, () -> kafkaSRV.mediumPriorityListener(recordMedium, headers));
		assertThrows(BusinessException.class, () -> kafkaSRV.highPriorityListener(recordHigh, headers));

		// Http error
		Mockito.doReturn(new ResponseEntity<>(responseDTO, HttpStatus.OK)).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(IniPublicationResponseDTO.class));

		assertDoesNotThrow(() -> kafkaSRV.lowPriorityListener(recordLow, headers));
		assertDoesNotThrow(() -> kafkaSRV.mediumPriorityListener(recordMedium, headers));
		assertDoesNotThrow(() -> kafkaSRV.highPriorityListener(recordHigh, headers));
	}

	@Test
	@Description("Replace - Success")
	void kafkaListenerReplaceSuccessTest() {
		String topicLow = kafkaTopicCFG.getDispatcherIndexerLowPriorityTopic();

		Map<String, Object> map = new HashMap<>();
		MessageHeaders headers = new MessageHeaders(map);
		Map<TopicPartition, List<ConsumerRecord<String, String>>> records = new LinkedHashMap<>();
		records.put(new TopicPartition(topicLow, 0), new ArrayList<>());

		final String kafkaValue = new Gson().toJson(new IndexerValueDTO(TestConstants.testWorkflowInstanceId, "String", ProcessorOperationEnum.REPLACE));

		ConsumerRecord<String, String> recordLow = new ConsumerRecord<String,String>(topicLow, 1, 0, topicLow, kafkaValue);
		IniPublicationResponseDTO responseDTO = new IniPublicationResponseDTO();
		responseDTO.setEsito(true);

		Mockito.doReturn(new ResponseEntity<>(responseDTO, HttpStatus.OK)).when(restTemplate)
						.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniPublicationResponseDTO.class));

		assertDoesNotThrow(() -> kafkaSRV.lowPriorityListener(recordLow, headers));
	}

	@Test
	@Description("Replace - Error tests")
	void kafkaListenerReplaceErrorsTest() throws ExecutionException, InterruptedException {
		String topicLow = kafkaTopicCFG.getDispatcherIndexerLowPriorityTopic();

		Map<String, Object> map = new HashMap<>();
		MessageHeaders headers = new MessageHeaders(map);

		Map<TopicPartition, List<ConsumerRecord<String, String>>> records = new LinkedHashMap<>();

		records.put(new TopicPartition(topicLow, 0), new ArrayList<>());

		final String kafkaValue = new Gson().toJson(new IndexerValueDTO(TestConstants.testWorkflowInstanceId, "String", ProcessorOperationEnum.REPLACE));

		this.kafkaInit(topicLow, TestConstants.testWorkflowInstanceId, kafkaValue);

		ConsumerRecord<String, String> recordLow = new ConsumerRecord<String,String>(topicLow, 1, 0, topicLow, kafkaValue);

		IniPublicationResponseDTO responseDTO = new IniPublicationResponseDTO();
		responseDTO.setEsito(false);

		// Connection refused
		Mockito.doThrow(new ConnectionRefusedException("uri", "error")).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniPublicationResponseDTO.class));

		assertThrows(ConnectionRefusedException.class, () -> kafkaSRV.lowPriorityListener(recordLow, headers));

		// ResourceAccessException
		Mockito.doThrow(new ResourceAccessException("uri")).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniPublicationResponseDTO.class));

		assertThrows(ResourceAccessException.class, () -> kafkaSRV.lowPriorityListener(recordLow, headers));

		// Generic exception
		Mockito.doThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY)).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniPublicationResponseDTO.class));

		assertThrows(BusinessException.class, () -> kafkaSRV.lowPriorityListener(recordLow, headers));

		// Http error
		Mockito.doReturn(new ResponseEntity<>(responseDTO, HttpStatus.OK)).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniPublicationResponseDTO.class));

		assertDoesNotThrow(() -> kafkaSRV.lowPriorityListener(recordLow, headers));
	}
}
