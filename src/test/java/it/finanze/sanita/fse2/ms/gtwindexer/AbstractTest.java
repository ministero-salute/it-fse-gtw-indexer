/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer;

import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractTest {

    protected AbstractTest() {}

    protected ConsumerRecord<String, String> kafkaInit(
            String topic,
            String workflowInstanceId,
            String message
    ) throws ExecutionException, InterruptedException {
        MockProducer<String, String> mockProducer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());
        TestProducer testProducer = new TestProducer(mockProducer);
        testProducer.send(
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
