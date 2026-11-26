package it.finanze.sanita.fse2.ms.gtwindexer.isolation;

import static it.finanze.sanita.fse2.ms.gtwindexer.enums.EventStatusEnum.BLOCKING_ERROR;
import static it.finanze.sanita.fse2.ms.gtwindexer.enums.EventStatusEnum.BLOCKING_ERROR_MAX_RETRY;
import static it.finanze.sanita.fse2.ms.gtwindexer.enums.EventStatusEnum.NON_BLOCKING_ERROR;
import static it.finanze.sanita.fse2.ms.gtwindexer.enums.EventStatusEnum.SUCCESS;
import static it.finanze.sanita.fse2.ms.gtwindexer.enums.ProcessorOperationEnum.DELETE;
import static it.finanze.sanita.fse2.ms.gtwindexer.enums.ProcessorOperationEnum.PUBLISH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import it.finanze.sanita.fse2.ms.gtwindexer.client.IIniClient;
import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaConsumerCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaConsumerPropertiesCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaProducerPropertiesCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaTopicCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IndexerValueDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IniDeleteRequestDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IniMetadataUpdateReqDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniTraceResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.EventStatusEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.EventTypeEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ProcessorOperationEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BlockingIniException;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.TransientTimeoutIniException;
import it.finanze.sanita.fse2.ms.gtwindexer.service.IConfigSRV;
import it.finanze.sanita.fse2.ms.gtwindexer.service.impl.KafkaSRV;

@Tag("ISOLATION")
class KafkaServiceIsolationTest {
    private KafkaSRV service;
    private IIniClient ini;
    private IConfigSRV config;
    private KafkaConsumerPropertiesCFG consumer;
    private KafkaTemplate<String, String> producer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new KafkaSRV();
        ini = mock(IIniClient.class);
        config = mock(IConfigSRV.class);
        producer = mock(KafkaTemplate.class);
        consumer = new KafkaConsumerPropertiesCFG();
        consumer.setNRetry(1);
        consumer.setDeadLetterExceptions(List.of(BlockingIniException.class.getCanonicalName()));
        consumer.setTemporaryExceptions(List.of(TransientTimeoutIniException.class.getCanonicalName()));
        KafkaTopicCFG topics = mock(KafkaTopicCFG.class);
        when(topics.getIndexerPublisherTopic()).thenReturn("publisher");
        when(topics.getStatusManagerTopic()).thenReturn("status");
        ReflectionTestUtils.setField(service, "iniClient", ini);
        ReflectionTestUtils.setField(service, "configSRV", config);
        ReflectionTestUtils.setField(service, "kafkaConsumerPropCFG", consumer);
        ReflectionTestUtils.setField(service, "topics", topics);
        ReflectionTestUtils.setField(service, "kafkaProducerCFG", mock(KafkaProducerPropertiesCFG.class));
        ReflectionTestUtils.setField(service, "notxKafkaTemplate", producer);
        ReflectionTestUtils.setField(service, "msName", "indexer-isolation");
    }

    @ParameterizedTest
    @CsvSource({"PUBLISH,false,false,true,true", "REPLACE,false,false,true,true",
            "PUBLISH,true,false,true,false", "PUBLISH,false,true,true,false",
            "PUBLISH,false,false,false,false"})
    void publishesOnlyEligibleEdsDocuments(ProcessorOperationEnum operation, boolean removeEds,
            boolean mockEds, boolean edsPublished, boolean forwarded) throws Exception {
        when(config.isRemoveEds()).thenReturn(removeEds);
        IniTraceResponseDTO response = response(true, "accepted");
        response.setMockEds(mockEds);
        when(ini.sendPublicationData("workflow")).thenReturn(response);
        when(ini.sendReplaceData("workflow")).thenReturn(response);
        ConsumerRecord<String, String> record = publication(operation, edsPublished);
        service.publishedDocListener(record, 1);
        if (operation == PUBLISH) verify(ini).sendPublicationData("workflow");
        else verify(ini).sendReplaceData("workflow");
        List<ProducerRecord<String, String>> messages = messages();
        assertEquals(forwarded ? 2 : 1, messages.size());
        if (forwarded) {
            assertEquals("publisher", messages.get(0).topic());
            assertEquals(record.value(), messages.get(0).value());
            assertEquals("record-key", messages.get(0).key());
        }
        assertStatus(messages.getLast(), "workflow", SUCCESS, "SEND_TO_INI");
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{broken"})
    void rejectsInvalidPayloadAndReportsDeserializationFailure(String payload) throws Exception {
        assertThrows(BlockingIniException.class,
                () -> service.publishedDocListener(record(payload), 1));
        verifyNoInteractions(ini);
        assertStatus(messages().getFirst(), "record-key", BLOCKING_ERROR, "DESERIALIZE");
    }

    @ParameterizedTest
    @ValueSource(strings = {"CRF1 timeout", "R123 rejected"})
    void classifiesIniFailureForRetryOrDeadLetter(String message) throws Exception {
        when(ini.sendPublicationData("workflow")).thenReturn(response(false, message));
        Class<? extends RuntimeException> expected = message.startsWith("CRF1")
                ? TransientTimeoutIniException.class : BlockingIniException.class;
        assertEquals(message, assertThrows(expected,
                () -> service.publishedDocListener(publication(PUBLISH, true), 1)).getMessage());
        assertStatus(messages().getFirst(), "workflow",
                message.startsWith("CRF1") ? NON_BLOCKING_ERROR : BLOCKING_ERROR, "SEND_TO_INI");
        verify(ini, times(1)).sendPublicationData("workflow");
    }

    @Test
    void nullIniOutcomeIsRejectedWithoutForwardingDocument() throws Exception {
        var response = response(true, "indeterminate outcome");
        response.setEsito(null);
        when(ini.sendPublicationData("workflow")).thenReturn(response);
        assertThrows(BlockingIniException.class,
                () -> service.publishedDocListener(publication(PUBLISH, true), 1));
        assertEquals(1, messages().size());
        assertStatus(messages().getFirst(), "workflow", BLOCKING_ERROR, "SEND_TO_INI");
    }

    @Test
    void unsupportedPublicationOperationExhaustsRetriesWithoutCallingIni() throws Exception {
        var failure = assertThrows(BlockingIniException.class,
                () -> service.publishedDocListener(publication(DELETE, true), 1));
        assertEquals("Unsupported INI operation", failure.getMessage());
        verifyNoInteractions(ini);
        assertStatus(messages().getFirst(), "workflow", BLOCKING_ERROR_MAX_RETRY, "SEND_TO_INI");
    }

    @Test
    void retryUpdateSuccessUsesRecordKeyForStatus() throws Exception {
        when(ini.sendUpdateData(any())).thenReturn(response(true, "updated"));
        service.retryUpdateListener(record("{}"), 1);
        verify(ini).sendUpdateData(any(IniMetadataUpdateReqDTO.class));
        assertStatus(messages().getFirst(), "record-key", SUCCESS, "SEND_TO_INI");
    }

    @ParameterizedTest
    @ValueSource(strings = {"CRF1 timeout", "rejected"})
    void retryDeleteClassifiesUnsuccessfulResponses(String message) throws Exception {
        when(ini.delete(any())).thenReturn(response(false, message));
        Class<? extends RuntimeException> expected = message.startsWith("CRF1")
                ? TransientTimeoutIniException.class : BlockingIniException.class;
        assertThrows(expected,
                () -> service.retryDeleteListener(record("{}"), 1));
        assertStatus(messages().getFirst(), "record-key",
                message.startsWith("CRF1") ? NON_BLOCKING_ERROR : BLOCKING_ERROR, "SEND_TO_INI");
    }

    @Test
    void nullFailureMessageIsReportedAsBlocking() throws Exception {
        when(ini.delete(any())).thenReturn(response(false, null));
        assertThrows(BlockingIniException.class, () -> service.retryDeleteListener(record("{}"), 1));
        assertStatus(messages().getFirst(), "record-key", BLOCKING_ERROR, "SEND_TO_INI");
    }

    @Test
    void publisherFailureIsConvertedToBlockingError() throws Exception {
        when(ini.sendPublicationData("workflow")).thenReturn(response(true, "accepted"));
        when(producer.send(any(ProducerRecord.class))).thenAnswer(invocation -> {
            ProducerRecord<?, ?> message = invocation.getArgument(0);
            if (message.topic().equals("publisher")) throw new IllegalStateException("publisher offline");
            return CompletableFuture.completedFuture(null);
        });
        var failure = assertThrows(BlockingIniException.class,
                () -> service.publishedDocListener(publication(PUBLISH, true), 1));
        assertInstanceOf(BusinessException.class, failure.getCause());
        assertStatus(messages().getLast(), "workflow", BLOCKING_ERROR, "SEND_TO_INI");
    }

    @Test
    void retriesUnclassifiedFailureThenReportsSuccess() throws Exception {
        when(ini.delete(any())).thenThrow(new IllegalStateException("temporary"))
                .thenReturn(response(true, "deleted"));
        service.retryDeleteListener(record("{}"), 1);
        verify(ini, times(2)).delete(any(IniDeleteRequestDTO.class));
        assertStatus(messages().getFirst(), "record-key", SUCCESS, "SEND_TO_INI");
    }

    @Test
    void reportsExhaustedRetryBudget() throws Exception {
        when(ini.sendUpdateData(any())).thenThrow(new IllegalStateException("unclassified"));
        assertThrows(BlockingIniException.class, () -> service.retryUpdateListener(record("{}"), 1));
        verify(ini, times(2)).sendUpdateData(any(IniMetadataUpdateReqDTO.class));
        assertStatus(messages().getFirst(), "record-key", BLOCKING_ERROR_MAX_RETRY, "SEND_TO_INI");
        assertTrue(messages().getFirst().value().contains("unclassified"));
    }

    @ParameterizedTest
    @CsvSource({"PUBLISH,false,false,true,true", "REPLACE,false,false,true,true",
            "PUBLISH,true,false,true,false", "REPLACE,true,false,true,false",
            "PUBLISH,false,true,true,false", "REPLACE,false,true,true,false",
            "PUBLISH,false,false,false,false", "REPLACE,false,false,false,false"})
    void duplicateResponseOnRedeliverySucceedsAndRespectsEdsGuards(ProcessorOperationEnum operation,
            boolean removeEds, boolean mockEds, boolean edsPublished, boolean forwarded) throws Exception {
        when(config.isRemoveEds()).thenReturn(removeEds);
        IniTraceResponseDTO response = response(false, "R224 duplicate");
        response.setMockEds(mockEds);
        when(ini.sendPublicationData("workflow")).thenReturn(response);
        when(ini.sendReplaceData("workflow")).thenReturn(response);
        ConsumerRecord<String, String> record = publication(operation, edsPublished);

        assertDoesNotThrow(() -> service.publishedDocListener(record, 2));

        if (operation == PUBLISH) verify(ini).sendPublicationData("workflow");
        else verify(ini).sendReplaceData("workflow");
        List<ProducerRecord<String, String>> messages = messages();
        assertEquals(forwarded ? 2 : 1, messages.size());
        if (forwarded) {
            assertEquals("publisher", messages.getFirst().topic());
            assertEquals(record.key(), messages.getFirst().key());
            assertEquals(record.value(), messages.getFirst().value());
        }
        assertStatus(messages.getLast(), "workflow", SUCCESS, "SEND_TO_INI");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUBLISH", "REPLACE"})
    void duplicateResponseOnFirstDeliveryRemainsBlocking(ProcessorOperationEnum operation) throws Exception {
        when(ini.sendPublicationData("workflow")).thenReturn(response(false, "R224 duplicate"));
        when(ini.sendReplaceData("workflow")).thenReturn(response(false, "R224 duplicate"));

        var failure = assertThrows(BlockingIniException.class,
                () -> service.publishedDocListener(publication(operation, true), 1));

        assertEquals("R224 duplicate", failure.getMessage());
        assertEquals(1, messages().size());
        assertStatus(messages().getFirst(), "workflow", BLOCKING_ERROR, "SEND_TO_INI");
    }

    @Test
    void thrownDuplicateExceptionOnRedeliveryRemainsBlockingWithoutForwarding() throws Exception {
        BlockingIniException failure = new BlockingIniException("R224 duplicate");
        when(ini.sendPublicationData("workflow")).thenThrow(failure);
        assertSame(failure, assertThrows(BlockingIniException.class,
                () -> service.publishedDocListener(publication(PUBLISH, true), 2)));
        assertEquals(1, messages().size());
        assertStatus(messages().getFirst(), "workflow", BLOCKING_ERROR, "SEND_TO_INI");
    }

    @Test
    void skipsFailureNotificationAfterMaximumDeliveryAttempt() {
        when(ini.delete(any())).thenThrow(new BlockingIniException("rejected"));
        assertThrows(BlockingIniException.class,
                () -> service.retryDeleteListener(record("{}"), KafkaConsumerCFG.MAX_ATTEMPT + 1));
        verifyNoInteractions(producer);
    }

    @Test
    @SuppressWarnings("unchecked")
    void transactionalSendExecutesCallbackAndReturnsMetadata() {
        KafkaProducerPropertiesCFG properties = new KafkaProducerPropertiesCFG();
        properties.setTransactionalId("isolation-tx");
        ReflectionTestUtils.setField(service, "kafkaProducerCFG", properties);
        ReflectionTestUtils.setField(service, "txKafkaTemplate", producer);
        RecordMetadata metadata = mock(RecordMetadata.class);
        SendResult<String, String> result = mock(SendResult.class);
        when(result.getRecordMetadata()).thenReturn(metadata);
        when(producer.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(result));
        when(producer.executeInTransaction(any())).thenAnswer(invocation -> {
            org.springframework.kafka.core.KafkaOperations.OperationsCallback<String, String, SendResult<String, String>> callback
                    = invocation.getArgument(0);
            return callback.doInOperations(producer);
        });
        assertSame(metadata, service.sendMessage("topic", "key", "payload"));
        verify(producer).executeInTransaction(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void transactionalFailurePreservesCauseAndInterruptState() throws Exception {
        KafkaProducerPropertiesCFG properties = new KafkaProducerPropertiesCFG();
        properties.setTransactionalId("isolation-tx");
        ReflectionTestUtils.setField(service, "kafkaProducerCFG", properties);
        ReflectionTestUtils.setField(service, "txKafkaTemplate", producer);
        CompletableFuture<SendResult<String, String>> future = mock(CompletableFuture.class);
        when(producer.send(any(ProducerRecord.class))).thenReturn(future);
        when(producer.executeInTransaction(any())).thenAnswer(invocation -> {
            org.springframework.kafka.core.KafkaOperations.OperationsCallback<String, String, SendResult<String, String>> callback
                    = invocation.getArgument(0);
            return callback.doInOperations(producer);
        });
        InterruptedException interrupted = new InterruptedException("cancelled");
        when(future.get()).thenThrow(interrupted);
        try {
            var failure = assertThrows(BusinessException.class, () -> service.sendMessage("topic", "key", "payload"));
            assertSame(interrupted, failure.getCause().getCause());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
        var rejected = new java.util.concurrent.ExecutionException(new IllegalStateException("rejected"));
        doThrow(rejected).when(future).get();
        var failure = assertThrows(BusinessException.class, () -> service.sendMessage("topic", "key", "payload"));
        assertSame(rejected, failure.getCause().getCause());
        assertFalse(Thread.currentThread().isInterrupted());
    }

    @Test
    void producerFailureIsWrappedWithOriginalCause() {
        IllegalStateException failure = new IllegalStateException("producer unavailable");
        when(producer.send(any(ProducerRecord.class))).thenThrow(failure);
        assertSame(failure, assertThrows(BusinessException.class,
                () -> service.sendMessage("topic", "key", "value")).getCause());
        assertThrows(BusinessException.class,
                () -> service.sendStatusMessage("workflow", EventTypeEnum.SEND_TO_INI, SUCCESS, "done"));
    }

    private IniTraceResponseDTO response(boolean success, String message) {
        IniTraceResponseDTO response = new IniTraceResponseDTO();
        response.setEsito(success);
        response.setMockEds(false);
        response.setMessage(message);
        return response;
    }

    private ConsumerRecord<String, String> publication(ProcessorOperationEnum operation, boolean published) {
        return record(new Gson().toJson(new IndexerValueDTO("workflow", "document", operation, published)));
    }

    private ConsumerRecord<String, String> record(String payload) {
        return new ConsumerRecord<>("input", 0, 1L, "record-key", payload);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<ProducerRecord<String, String>> messages() {
        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass((Class) ProducerRecord.class);
        verify(producer, atLeastOnce()).send(captor.capture());
        return captor.getAllValues();
    }

    private void assertStatus(ProducerRecord<String, String> message, String workflow,
            EventStatusEnum status, String event) throws Exception {
        assertEquals("status", message.topic());
        assertEquals(workflow, message.key());
        var json = new ObjectMapper().readTree(message.value());
        assertEquals(status.name(), json.get("eventStatus").asText());
        assertEquals(event, json.get("eventType").asText());
        assertEquals("indexer-isolation", json.get("microserviceName").asText());
    }
}
