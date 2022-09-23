package it.finanze.sanita.fse2.ms.gtwindexer;

import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaPropertiesCFG;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ExecutionException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractTest {
    @Autowired
    private KafkaPropertiesCFG kafkaPropCFG;

    private TestProducer testProducer;
    protected AbstractTest() {}

    protected ConsumerRecord<String, String> kafkaInit(
            String topic,
            String workflowInstanceId,
            String message
    ) throws ExecutionException, InterruptedException {
        MockProducer<String, String> mockProducer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());
        TestProducer testProducer = new TestProducer(mockProducer);
        RecordMetadata recordMetadataFuture = testProducer.send(
                topic,
                workflowInstanceId,
                message
        ).get();

        return new ConsumerRecord<>(
                topic,
                1,
                0,
                workflowInstanceId,
                message
        );
    }
}
