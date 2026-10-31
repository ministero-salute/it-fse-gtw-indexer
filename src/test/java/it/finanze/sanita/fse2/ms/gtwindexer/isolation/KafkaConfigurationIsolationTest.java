package it.finanze.sanita.fse2.ms.gtwindexer.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import it.finanze.sanita.fse2.ms.gtwindexer.config.Constants;
import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.*;
import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.oauth2.CustomAuthenticateCallbackHandler;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BlockingIniException;
import it.finanze.sanita.fse2.ms.gtwindexer.utility.ProfileUtility;

@Tag("ISOLATION")
class KafkaConfigurationIsolationTest {
    private KafkaPropertiesCFG common;
    private KafkaProducerPropertiesCFG producer;
    private KafkaConsumerPropertiesCFG consumer;
    private KafkaProducerCFG producers;
    private KafkaConsumerCFG consumers;
    private KafkaTopicCFG topics;

    @BeforeEach
    void setUp() {
        common = new KafkaPropertiesCFG();
        common.setBootstrapServers("broker.invalid:9092");
        producer = new KafkaProducerPropertiesCFG();
        producer.setProducerBootstrapServers(common.getBootstrapServers());
        producer.setClientId("indexer");
        producer.setRetries(3);
        producer.setKeySerializer(StringSerializer.class.getName());
        producer.setValueSerializer(StringSerializer.class.getName());
        producer.setTransactionalId("tx");
        producer.setAck("all");
        producer.setIdempotence(true);
        consumer = new KafkaConsumerPropertiesCFG();
        consumer.setClientId("reader");
        consumer.setConsumerKeyDeserializer(StringDeserializer.class.getName());
        consumer.setConsumerValueDeserializer(StringDeserializer.class.getName());
        consumer.setIsolationLevel("read_committed");
        consumer.setAutoCommit("false");
        consumer.setAutoOffsetReset("earliest");
        consumer.setRetryIntervalMs(0);
        consumer.setNRetry(0);
        consumer.setDeadLetterExceptions(List.of(BlockingIniException.class.getName()));
        topics = new KafkaTopicCFG();
        topics.setDispatcherIndexerDeadLetterTopic("dead-letter");
        producers = new KafkaProducerCFG();
        consumers = new KafkaConsumerCFG();
        ReflectionTestUtils.setField(producers, "kafkaPropCFG", common);
        ReflectionTestUtils.setField(producers, "kafkaProducerPropCFG", producer);
        ReflectionTestUtils.setField(consumers, "kafkaPropCFG", common);
        ReflectionTestUtils.setField(consumers, "kafkaConsumerPropCFG", consumer);
        ReflectionTestUtils.setField(consumers, "kafkaTopicCFG", topics);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void producerMapsTransactionAndSecurityOptions(boolean secured) throws Exception {
        if (secured) security();
        InetAddress host = InetAddress.getLoopbackAddress();
        try (var dns = mockStatic(InetAddress.class)) {
            dns.when(InetAddress::getLocalHost).thenReturn(host);
            Map<String, Object> tx = producers.producerConfigs();
            Map<String, Object> plain = producers.producerWithoutTransactionConfigs();
            assertEquals(host + "-tx", tx.get("transactional.id"));
            assertEquals("indexer-tx-" + host, tx.get("client.id"));
            assertEquals("all", tx.get("acks"));
            assertEquals(true, tx.get("enable.idempotence"));
            assertFalse(plain.containsKey("transactional.id"));
            assertEquals("indexer-noTx", plain.get("client.id"));
            for (var config : List.of(tx, plain)) {
                assertEquals("broker.invalid:9092", config.get("bootstrap.servers"));
                assertEquals(3, config.get("retries"));
                assertEquals(StringSerializer.class.getName(), config.get("key.serializer"));
                assertEquals(StringSerializer.class.getName(), config.get("value.serializer"));
                assertSecurity(config, secured);
            }
        }
    }

    @Test
    void unresolvedLocalHostStillBuildsProducerConfiguration() throws Exception {
        try (var dns = mockStatic(InetAddress.class)) {
            dns.when(InetAddress::getLocalHost).thenThrow(new java.net.UnknownHostException("unavailable"));
            assertEquals("null-tx", producers.producerConfigs().get("transactional.id"));
        }
    }

    @Test
    void producerFactoriesAreLazyAndHaveExpectedTransactionCapability() {
        assertTrue(producers.producerFactory().transactionCapable());
        assertFalse(producers.producerFactoryWithoutTransaction().transactionCapable());
        assertFalse(producers.producerDeadFactory().transactionCapable());
        assertTrue(producers.txKafkaTemplate().getProducerFactory().transactionCapable());
        assertFalse(producers.notxKafkaTemplate().getProducerFactory().transactionCapable());
        assertFalse(producers.noTxKafkaDeadTemplate().getProducerFactory().transactionCapable());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void consumerMapsSecurityAndOffsetOptions(boolean secured) {
        if (secured) security();
        var config = consumers.consumerConfigs();
        assertEquals("reader", config.get("client.id"));
        assertEquals("broker.invalid:9092", config.get("bootstrap.servers"));
        assertEquals("read_committed", config.get("isolation.level"));
        assertEquals("false", config.get("enable.auto.commit"));
        assertEquals("earliest", config.get("auto.offset.reset"));
        assertEquals(StringDeserializer.class.getName(), config.get("key.deserializer"));
        assertEquals(StringDeserializer.class.getName(), config.get("value.deserializer"));
        assertSecurity(config, secured);
        assertEquals(config, consumers.consumerFactory().getConfigurationProperties());
    }

    @Test
    void emptyTruststorePasswordIsOmittedForConsumer() {
        consumer.setTrustorePassword(new char[0]);
        assertFalse(consumers.consumerConfigs().containsKey("ssl.truststore.password"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listenerFactoriesBuildStoppedContainersAndEnableAttemptHeadersForDeadLetter() {
        KafkaTemplate<Object, Object> dead = mock(KafkaTemplate.class);
        var factory = consumers.kafkaListenerDeadLetterContainerFactory(dead);
        ConcurrentMessageListenerContainer<String, String> container = factory.createContainer("input");
        assertFalse(container.isRunning());
        assertTrue(container.getContainerProperties().isDeliveryAttemptHeader());
        assertTrue(container.getCommonErrorHandler().deliveryAttemptHeader());
        var policy = (org.springframework.kafka.listener.DefaultErrorHandler) container.getCommonErrorHandler();
        assertEquals(Boolean.FALSE, policy.removeClassification(BlockingIniException.class));
        var plain = consumers.kafkaListenerContainerFactory(mock(KafkaTemplate.class)).createContainer("input");
        assertFalse(plain.isRunning());
        assertFalse(plain.getContainerProperties().isDeliveryAttemptHeader());
        verify(dead, never()).send(any(org.apache.kafka.clients.producer.ProducerRecord.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void deadLetterResolverUsesConfiguredTopicAndUnspecifiedPartition() {
        KafkaTemplate<Object, Object> template = mock(KafkaTemplate.class);
        try (var recoverers = mockConstruction(org.springframework.kafka.listener.DeadLetterPublishingRecoverer.class,
                (recoverer, context) -> {
                    java.util.function.BiFunction<org.apache.kafka.clients.consumer.ConsumerRecord<?, ?>, Exception,
                            org.apache.kafka.common.TopicPartition> resolver =
                            (java.util.function.BiFunction<org.apache.kafka.clients.consumer.ConsumerRecord<?, ?>, Exception,
                                    org.apache.kafka.common.TopicPartition>) context.arguments().get(1);
                    var destination = resolver.apply(new org.apache.kafka.clients.consumer.ConsumerRecord<>(
                            "source", 3, 10, "key", "payload"), new BlockingIniException("failed"));
                    assertEquals("dead-letter", destination.topic());
                    assertEquals(-1, destination.partition());
                })) {
            consumers.kafkaListenerDeadLetterContainerFactory(template);
            assertEquals(1, recoverers.constructed().size());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void invalidExceptionClassDoesNotPreventFactoryCreation() {
        consumer.setDeadLetterExceptions(List.of("missing.exception.Type"));
        assertNotNull(consumers.kafkaListenerDeadLetterContainerFactory(mock(KafkaTemplate.class)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"production", "dev", "test"})
    void adminClientUsesSecurityOnlyInProduction(String profile) {
        security();
        ProfileUtility profiles = mock(ProfileUtility.class);
        when(profiles.isDevOrDockerProfile()).thenReturn(profile.equals("dev"));
        when(profiles.isTestProfile()).thenReturn(profile.equals("test"));
        common.setProfileUtility(profiles);
        AdminClient admin = mock(AdminClient.class);
        try (var factory = mockStatic(AdminClient.class)) {
            factory.when(() -> AdminClient.create(any(Properties.class))).thenReturn(admin);
            assertSame(admin, common.client());
            factory.verify(() -> AdminClient.create(argThat((Properties p) ->
                    "broker.invalid:9092".equals(p.get("bootstrap.servers"))
                    && (profile.equals("production") ? "SASL_SSL".equals(p.get("security.protocol"))
                            && "secret".equals(p.get("ssl.truststore.password")) : p.size() == 1))));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void topicNamesArePrefixedOnlyForTestProfile(boolean test) {
        ProfileUtility profiles = mock(ProfileUtility.class);
        when(profiles.isTestProfile()).thenReturn(test);
        topics.setProfileUtility(profiles);
        topics.setDispatcherIndexerTopic("input");
        topics.setDispatcherIndexerDeleteRetryTopic("delete");
        topics.setDispatcherIndexerUpdateRetryTopic("update");
        topics.setIndexerPublisherTopic("publisher");
        topics.setStatusManagerTopic("status");
        topics.afterInit();
        String prefix = test ? Constants.Profile.TEST_PREFIX : "";
        assertEquals(prefix + "input", topics.getDispatcherIndexerTopic());
        assertEquals(prefix + "delete", topics.getDispatcherIndexerDeleteRetryTopic());
        assertEquals(prefix + "update", topics.getDispatcherIndexerUpdateRetryTopic());
        assertEquals(prefix + "publisher", topics.getIndexerPublisherTopic());
        assertEquals(prefix + "status", topics.getStatusManagerTopic());
        assertEquals(prefix + "dead-letter", topics.getDispatcherIndexerDeadLetterTopic());
    }

    private void security() {
        common.setProtocol("SASL_SSL");
        common.setMechanism("OAUTHBEARER");
        common.setConfigJaas("oauth required;");
        common.setTrustoreLocation("trust.jks");
        common.setTrustorePassword("secret".toCharArray());
        common.setTenantId("tenant");
        common.setAppId("app");
        common.setPfxPathName("client.pfx");
        common.setPwd("certificate-secret");
        common.setCallbackHandlerClass("custom.Callback");
        consumer.setProtocol(common.getProtocol());
        consumer.setMechanism(common.getMechanism());
        consumer.setConfigJaas(common.getConfigJaas());
        consumer.setTrustoreLocation(common.getTrustoreLocation());
        consumer.setTrustorePassword(common.getTrustorePassword());
    }

    private void assertSecurity(Map<String, Object> config, boolean enabled) {
        if (!enabled) {
            assertFalse(config.containsKey("security.protocol"));
            assertFalse(config.containsKey("sasl.mechanism"));
            assertFalse(config.containsKey("ssl.truststore.password"));
            assertFalse(config.containsKey("sasl.login.callback.handler.class"));
            return;
        }
        assertEquals("SASL_SSL", config.get("security.protocol"));
        assertEquals("OAUTHBEARER", config.get("sasl.mechanism"));
        assertEquals("oauth required;", config.get("sasl.jaas.config"));
        assertEquals("trust.jks", config.get("ssl.truststore.location"));
        assertEquals("secret", config.get("ssl.truststore.password"));
        assertEquals(CustomAuthenticateCallbackHandler.class, config.get("sasl.login.callback.handler.class"));
        assertEquals("tenant", config.get("kafka.oauth.tenantId"));
        assertEquals("app", config.get("kafka.oauth.appId"));
        assertEquals("client.pfx", config.get("kafka.oauth.pfxPathName"));
        assertEquals("certificate-secret", config.get("kafka.oauth.pwd"));
        assertEquals("custom.Callback", config.get("sasl.client.callback.handler.class"));
    }
}
