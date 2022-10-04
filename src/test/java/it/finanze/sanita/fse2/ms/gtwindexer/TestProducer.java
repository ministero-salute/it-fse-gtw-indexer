package it.finanze.sanita.fse2.ms.gtwindexer;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.concurrent.Future;


public class TestProducer {

    
    
    private final Producer<String, String> producer;

    public TestProducer(Producer<String, String> producer) {
        this.producer = producer;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Future<RecordMetadata> send(String topic, String key, String value) {
        ProducerRecord record = new ProducerRecord(topic, key, value);
        return producer.send(record);
    }
}