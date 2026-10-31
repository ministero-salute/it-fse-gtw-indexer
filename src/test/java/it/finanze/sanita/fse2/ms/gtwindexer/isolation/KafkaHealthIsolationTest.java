package it.finanze.sanita.fse2.ms.gtwindexer.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.util.ReflectionTestUtils;

import it.finanze.sanita.fse2.ms.gtwindexer.config.health.KafkaHealthIndicator;

@Tag("ISOLATION")
class KafkaHealthIsolationTest {
    private KafkaHealthIndicator indicator;
    private AdminClient admin;

    @BeforeEach
    void setUp() {
        indicator = new KafkaHealthIndicator();
        admin = mock(AdminClient.class);
        ReflectionTestUtils.setField(indicator, "client", admin);
    }

    @Test
    void reportsUpWhenTopicListingSucceeds() {
        ListTopicsResult result = mock(ListTopicsResult.class);
        when(admin.listTopics()).thenReturn(result);
        when(result.listings()).thenReturn(KafkaFuture.completedFuture(List.<TopicListing>of()));
        assertEquals(Status.UP, indicator.health().getStatus());
        verify(admin).listTopics();
    }

    @Test
    void reportsDownWithDiagnosticWhenAdminFails() {
        when(admin.listTopics()).thenThrow(new IllegalStateException("broker offline"));
        var health = indicator.health();
        assertEquals(Status.DOWN, health.getStatus());
        assertTrue(health.getDetails().get("error").toString().contains("broker offline"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void restoresInterruptFlagWhenTopicListingIsInterrupted() throws Exception {
        ListTopicsResult result = mock(ListTopicsResult.class);
        KafkaFuture<java.util.Collection<TopicListing>> future = mock(KafkaFuture.class);
        when(admin.listTopics()).thenReturn(result);
        when(result.listings()).thenReturn(future);
        when(future.get()).thenThrow(new InterruptedException("cancelled"));
        try {
            assertEquals(Status.DOWN, indicator.health().getStatus());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
