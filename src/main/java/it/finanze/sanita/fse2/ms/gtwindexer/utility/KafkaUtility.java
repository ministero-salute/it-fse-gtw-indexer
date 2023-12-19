package it.finanze.sanita.fse2.ms.gtwindexer.utility;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.util.Optional;

public class KafkaUtility {

    public static final String SPAN_CONTEXT_HEADER = "b3";

    public static Optional<Header> getTraceContext(ConsumerRecord<?, ?> cr){
        return Optional.ofNullable(cr.headers().lastHeader(SPAN_CONTEXT_HEADER));
    }
}
