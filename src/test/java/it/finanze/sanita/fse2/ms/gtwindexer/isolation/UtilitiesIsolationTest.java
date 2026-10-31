package it.finanze.sanita.fse2.ms.gtwindexer.isolation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtwindexer.utility.ProfileUtility;
import it.finanze.sanita.fse2.ms.gtwindexer.utility.StringUtility;

@Tag("ISOLATION")
class UtilitiesIsolationTest {
    @ParameterizedTest
    @NullAndEmptySource
    void identifiesAbsentStrings(String value) {
        assertTrue(StringUtility.isNullOrEmpty(value));
    }

    @Test
    void doesNotTreatWhitespaceAsEmpty() {
        assertFalse(StringUtility.isNullOrEmpty(" "));
    }

    @Test
    void serializersRetainPayloadValues() throws Exception {
        var value = Map.of("workflow", "wif", "success", "true");
        var mapper = new ObjectMapper();
        assertEquals(mapper.valueToTree(value), mapper.readTree(StringUtility.toJSON(value)));
        assertEquals(mapper.valueToTree(value), mapper.readTree(StringUtility.toJSONJackson(value)));
    }

    @Test
    void wrapsSerializationFailure() {
        assertNotNull(assertThrows(BusinessException.class,
                () -> StringUtility.toJSONJackson(new FailingPayload())).getCause());
    }

    @ParameterizedTest
    @ValueSource(strings = {"test", "DEV", "docker", "production"})
    void identifiesSupportedProfiles(String active) {
        ProfileUtility utility = new ProfileUtility();
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(active);
        ReflectionTestUtils.setField(utility, "environment", environment);
        assertEquals(active.equals("test"), utility.isTestProfile());
        assertEquals(active.equals("DEV") || active.equals("docker"), utility.isDevOrDockerProfile());
    }

    @Test
    void nestedConsumerFailureIsClassifiedByOuterExceptionWithoutLosingCause() {
        var properties = new it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaConsumerPropertiesCFG();
        properties.setDeadLetterExceptions(java.util.List.of(IllegalArgumentException.class.getCanonicalName()));
        properties.setTemporaryExceptions(java.util.List.of(java.net.ConnectException.class.getCanonicalName()));
        var failure = new IllegalArgumentException("invalid payload", new java.io.IOException("source failed"));
        assertEquals(it.finanze.sanita.fse2.ms.gtwindexer.enums.EventStatusEnum.BLOCKING_ERROR,
                properties.asExceptionType(failure).orElseThrow());
        assertDoesNotThrow(() -> properties.deadLetterHelper(failure));
        assertEquals("source failed", failure.getCause().getMessage());
        assertTrue(properties.asExceptionType(new IllegalStateException("unknown")).isEmpty());
    }

    @Test
    void configRoutesExposeServiceIdentityAndWhoisEndpoint() {
        var routes = new it.finanze.sanita.fse2.ms.gtwindexer.client.routes.ConfigClientRoutes();
        ReflectionTestUtils.setField(routes, "configHost", "http://config.invalid");
        assertEquals("[CFG]", routes.identifier());
        assertEquals("cfg", routes.microservice());
        assertEquals("http://config.invalid/v1/whois", routes.whois());
    }

    @Test
    void missingOrEmptyEnvironmentHasNoSpecialProfile() {
        ProfileUtility utility = new ProfileUtility();
        assertFalse(utility.isTestProfile());
        assertFalse(utility.isDevOrDockerProfile());
        ReflectionTestUtils.setField(utility, "environment", new MockEnvironment());
        assertFalse(utility.isTestProfile());
        assertFalse(utility.isDevOrDockerProfile());
    }

    public static class FailingPayload {
        public String getValue() {
            throw new IllegalStateException("cannot serialize");
        }
    }
}
