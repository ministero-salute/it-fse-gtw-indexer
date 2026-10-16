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
package it.finanze.sanita.fse2.ms.gtwindexer;

import static it.finanze.sanita.fse2.ms.gtwindexer.TestConstants.CRF1_RESPONSE_INI_DTO;
import static it.finanze.sanita.fse2.ms.gtwindexer.TestConstants.EMPTY_JSON;
import static it.finanze.sanita.fse2.ms.gtwindexer.TestConstants.FAILURE_RESPONSE_INI_DTO;
import static it.finanze.sanita.fse2.ms.gtwindexer.TestConstants.R224_RESPONSE_INI_DTO;
import static it.finanze.sanita.fse2.ms.gtwindexer.TestConstants.SUCCESS_RESPONSE_INI_DTO;
import static it.finanze.sanita.fse2.ms.gtwindexer.TestConstants.getFakeDeleteRequest;
import static it.finanze.sanita.fse2.ms.gtwindexer.TestConstants.getFakeRetryRequest;
import static it.finanze.sanita.fse2.ms.gtwindexer.TestConstants.testWorkflowInstanceId;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;

import it.finanze.sanita.fse2.ms.gtwindexer.client.impl.IniClient;
import it.finanze.sanita.fse2.ms.gtwindexer.config.Constants;
import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaTopicCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IndexerValueDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IniDeleteRequestDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniTraceResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ProcessorOperationEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BlockingIniException;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.TransientTimeoutIniException;
import it.finanze.sanita.fse2.ms.gtwindexer.service.IConfigSRV;
import it.finanze.sanita.fse2.ms.gtwindexer.service.IKafkaSRV;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.Profile.TEST)
@EmbeddedKafka
class KafkaTest extends AbstractTest {
	
	@MockitoSpyBean
	private IKafkaSRV kafkaSRV;

	@Autowired
	private KafkaTopicCFG kafkaTopicCFG;

	@MockitoSpyBean
	private IniClient iniClient;

	@MockitoSpyBean
	private RestTemplate restTemplate;

	@MockitoBean
	private IConfigSRV config;

	@Test
	@Description("Publication - Success")
	void kafkaListenerPublicationSuccessTest() throws ExecutionException, InterruptedException {
		String topic = kafkaTopicCFG.getDispatcherIndexerTopic();
		
		final String kafkaValue = new Gson().toJson(new IndexerValueDTO(testWorkflowInstanceId, "String", ProcessorOperationEnum.PUBLISH, Boolean.FALSE));

		this.kafkaInit(topic, testWorkflowInstanceId, kafkaValue);

		ConsumerRecord<String, String> record =
				new ConsumerRecord<>(topic, 1, 0, topic, kafkaValue);

		IniTraceResponseDTO responseDTO = new IniTraceResponseDTO();
		responseDTO.setEsito(true);
		doReturn(responseDTO).when(restTemplate)
				.postForObject(anyString(), any(HttpEntity.class), eq(IniTraceResponseDTO.class));

		when(config.isRemoveEds()).thenReturn(false);
		assertDoesNotThrow(() -> kafkaSRV.publishedDocListener(record, 0));
	}

	 
	@Test
	@Description("Replace - Success")
	void kafkaListenerReplaceSuccessTest() {
		String topic = kafkaTopicCFG.getDispatcherIndexerTopic();
		
		final String kafkaValue = new Gson().toJson(new IndexerValueDTO(testWorkflowInstanceId, "String", ProcessorOperationEnum.REPLACE, Boolean.TRUE));

		ConsumerRecord<String, String> recordLow =
				new ConsumerRecord<>(topic, 1, 0, topic, kafkaValue);
		IniTraceResponseDTO responseDTO = new IniTraceResponseDTO();
		responseDTO.setEsito(true);

		doReturn(new ResponseEntity<>(responseDTO, HttpStatus.OK)).when(restTemplate)
						.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniTraceResponseDTO.class));

		when(config.isRemoveEds()).thenReturn(false);
		assertDoesNotThrow(() -> kafkaSRV.publishedDocListener(recordLow, 0));
	}

	@Test
	void retryTestUpdateSuccess() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
				kafkaTopicCFG.getDispatcherIndexerDeleteRetryTopic(),
				getFakeDeleteRequest()
		);
		// Provide mock knowledge
		doReturn(new ResponseEntity<>(SUCCESS_RESPONSE_INI_DTO, HttpStatus.OK)).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), anyString());
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		assertNotNull(key);
		assertDoesNotThrow(() -> kafkaSRV.retryUpdateListener(key, 0));
	}

	@Test
	void retryDeleteTestSuccess() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
			kafkaTopicCFG.getDispatcherIndexerDeleteRetryTopic(),
			getFakeDeleteRequest()
		);
		// Provide mock knowledge
		doReturn(new ResponseEntity<>(SUCCESS_RESPONSE_INI_DTO, HttpStatus.OK)).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), anyString());
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		assertNotNull(key);
		assertDoesNotThrow(() -> kafkaSRV.retryDeleteListener(key, 0));
	}

	@Test
	void retryDeleteTestFailure() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
			kafkaTopicCFG.getDispatcherIndexerDeleteRetryTopic(),
			getFakeDeleteRequest()
		);
		// Provide mock knowledge
		doReturn(FAILURE_RESPONSE_INI_DTO).when(iniClient).delete(any(IniDeleteRequestDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), anyString());
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		assertNotNull(key);
		assertThrows(BlockingIniException.class, () -> kafkaSRV.retryDeleteListener(key, 0));
	}

	@Test
	void retryDeleteTestWithInvalidPayload() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
			kafkaTopicCFG.getDispatcherIndexerDeleteRetryTopic(),
			EMPTY_JSON
		);
		// Provide mock knowledge
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), anyString());
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		assertNotNull(key);
		assertThrows(BlockingIniException.class,() -> kafkaSRV.retryDeleteListener(key,  0));
	}

	@Test
	void retryDeleteTestWithBlockingError() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
			kafkaTopicCFG.getDispatcherIndexerDeleteRetryTopic(),
			getFakeDeleteRequest()
		);
		// Provide mock knowledge
		doThrow(NullPointerException.class).when(iniClient).delete(any(IniDeleteRequestDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), nullable(String.class));
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		assertNotNull(key);
		assertThrows(NullPointerException.class, () -> kafkaSRV.retryDeleteListener(key,  0));
	}

	@Test
	void retryDeleteTestWithResourceAccessBlockingError() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
			kafkaTopicCFG.getDispatcherIndexerDeleteRetryTopic(),
			getFakeDeleteRequest()
		);
		// Provide mock knowledge
		doThrow(ResourceAccessException.class).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), nullable(String.class));
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		assertNotNull(key);
		assertThrows(ResourceAccessException .class, () -> kafkaSRV.retryDeleteListener(key,  0));
	}

	@Test
	void retryDeleteTestWithHttpClientBlockingError() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
				kafkaTopicCFG.getDispatcherIndexerDeleteRetryTopic(),
				getFakeDeleteRequest()
		);
		// Provide mock knowledge
		doThrow(HttpClientErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, EMPTY_JSON, null, null, null)).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), nullable(String.class));
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		assertNotNull(key);
		assertThrows(BlockingIniException.class, () -> kafkaSRV.retryDeleteListener(key,  0));
	}

	@Test
	void retryDeleteTestWithUnknownError() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
			kafkaTopicCFG.getDispatcherIndexerDeleteRetryTopic(),
			getFakeDeleteRequest()
		);
		// Provide mock knowledge
		doThrow(RuntimeException.class).when(iniClient).delete(any(IniDeleteRequestDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), nullable(String.class));
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		assertNotNull(key);
		assertThrows(BlockingIniException.class, () -> kafkaSRV.retryDeleteListener(key,  0));
	}

	@Test
	@Description("Publication - CRF1 response is classified as transient and throws TransientTimeoutIniException")
	void kafkaListenerPublicationCrf1IsTransientTest() throws ExecutionException, InterruptedException {
		String topic = kafkaTopicCFG.getDispatcherIndexerTopic();
		final String kafkaValue = new Gson().toJson(new IndexerValueDTO(testWorkflowInstanceId, "String", ProcessorOperationEnum.PUBLISH, Boolean.FALSE));
		ConsumerRecord<String, String> record = new ConsumerRecord<>(topic, 1, 0, topic, kafkaValue);

		// INI-client returns esito=false with a CRF1 error code in the message
		doReturn(CRF1_RESPONSE_INI_DTO).when(restTemplate)
				.postForObject(anyString(), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), nullable(String.class));

		// Must throw TransientTimeoutIniException, NOT BlockingIniException
		assertThrows(TransientTimeoutIniException.class, () -> kafkaSRV.publishedDocListener(record, 1));
	}

	@Test
	@Description("Publication - delivery=2, INI replies R224 (doc already indexed after prior CRF1 timeout): must be treated as SUCCESS")
	void kafkaListenerR224OnRetryAfterCrf1IsSuccessTest() {
		String topic = kafkaTopicCFG.getDispatcherIndexerTopic();
		final String kafkaValue = new Gson().toJson(
				new IndexerValueDTO(testWorkflowInstanceId, "String", ProcessorOperationEnum.PUBLISH, Boolean.FALSE));
		// delivery=2: Kafka re-delivers the message after the first attempt timed out with CRF1.
		// INI now responds R224 because it already indexed the doc during that timed-out attempt.
		ConsumerRecord<String, String> record = new ConsumerRecord<>(topic, 1, 0, topic, kafkaValue);

		doReturn(R224_RESPONSE_INI_DTO).when(iniClient).sendPublicationData(anyString());
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), nullable(String.class));

		// Must complete without any exception (treated as success)
		assertDoesNotThrow(() -> kafkaSRV.publishedDocListener(record, 2));
	}

	@Test
	@Description("Publication - delivery=1, INI replies R224 without any prior CRF1: must remain a BlockingIniException")
	void kafkaListenerR224OnFirstDeliveryIsBlockingErrorTest() {
		String topic = kafkaTopicCFG.getDispatcherIndexerTopic();
		final String kafkaValue = new Gson().toJson(
				new IndexerValueDTO(testWorkflowInstanceId, "String", ProcessorOperationEnum.PUBLISH, Boolean.FALSE));
		// delivery=1: first attempt, R224 with no prior timeout — genuine blocking error
		ConsumerRecord<String, String> record = new ConsumerRecord<>(topic, 1, 0, topic, kafkaValue);

		doReturn(R224_RESPONSE_INI_DTO).when(iniClient).sendPublicationData(anyString());
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), nullable(String.class));

		assertThrows(BlockingIniException.class, () -> kafkaSRV.publishedDocListener(record, 1));
	}

	@Test
	@Description("Delete retry - CRF1 response is classified as transient and throws TransientTimeoutIniException")
	void retryDeleteCrf1IsTransientTest() {
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
			kafkaTopicCFG.getDispatcherIndexerDeleteRetryTopic(),
			getFakeDeleteRequest()
		);
		// INI-client returns esito=false with a CRF1 error code in the message
		doReturn(CRF1_RESPONSE_INI_DTO).when(iniClient).delete(any(IniDeleteRequestDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), nullable(String.class));

		ConsumerRecord<String, String> key = req.getKey();
		assertNotNull(key);
		// Must throw TransientTimeoutIniException, NOT BlockingIniException
		assertThrows(TransientTimeoutIniException.class, () -> kafkaSRV.retryDeleteListener(key, 1));
	}

	@Test
	void retryUpdateTestWithResourceAccessBlockingError() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
				kafkaTopicCFG.getDispatcherIndexerUpdateRetryTopic(),
				"{\"key\":\"value\"}"
		);
		// Provide mock knowledge
		doThrow(ResourceAccessException.class).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), nullable(String.class));
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		assertNotNull(key);
		assertThrows(ResourceAccessException .class, () -> kafkaSRV.retryUpdateListener(key,  0));
	}

	@Test
	void retryUpdateTestWithHttpClientBlockingError() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
				kafkaTopicCFG.getDispatcherIndexerUpdateRetryTopic(),
				"{\"key\":\"value\"}"
		);
		// Provide mock knowledge
		doThrow(HttpClientErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, EMPTY_JSON, null, null, null)).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), nullable(String.class));
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		assertNotNull(key);
		assertThrows(BlockingIniException.class, () -> kafkaSRV.retryUpdateListener(key,  0));
	}

}
