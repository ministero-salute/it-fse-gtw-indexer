package it.finanze.sanita.fse2.ms.gtwindexer.client.base;

@FunctionalInterface
public interface ClientCallback<K, V> {
    V request(K value) throws Exception;
}
