package it.finanze.sanita.fse2.ms.gtwindexer.logging;

import ch.qos.logback.core.spi.ContextAwareBase;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BusinessException;

import org.apache.kafka.clients.producer.BufferExhaustedException;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
 
public class BlockingDeliveryStrategy extends ContextAwareBase implements DeliveryStrategy {

    private long timeout = 0L;

    @Override
    public <K, V, E> boolean send(Producer<K, V> producer, ProducerRecord<K, V> record, E event, FailedDeliveryCallback<E> failureCallback) {
        try {
            final Future<RecordMetadata> future = producer.send(record);
            if (timeout > 0L) future.get(timeout, TimeUnit.MILLISECONDS);
            else if (timeout == 0) future.get();
            return true;
        } catch (InterruptedException e) { 
			Thread.currentThread().interrupt();
			throw new BusinessException(e); 
        } catch (BufferExhaustedException | ExecutionException | CancellationException | TimeoutException e) {
            failureCallback.onFailedDelivery(event, e);
        }
        return false;
    }

    public long getTimeout() {
        return timeout;
    }

    /**
     * Sets the timeout for waits on full consumers.
     * <ul>
     *     <li>{@code timeout > 0}: Wait for {@code timeout} milliseconds</li>
     *     <li>{@code timeout == 0}: Wait infinitely
     * </ul>
     * @param timeout a timeout in {@link TimeUnit#MILLISECONDS}.
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}