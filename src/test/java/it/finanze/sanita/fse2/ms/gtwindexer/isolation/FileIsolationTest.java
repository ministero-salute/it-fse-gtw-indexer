package it.finanze.sanita.fse2.ms.gtwindexer.isolation;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import it.finanze.sanita.fse2.ms.gtwindexer.utility.FileUtility;

@Tag("ISOLATION")
class FileIsolationTest {
    @Test
    void readsMultipleChunksAndClosesResource() {
        byte[] bytes = new byte[40000];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) i;
        AtomicBoolean closed = new AtomicBoolean();
        withResource(new ByteArrayInputStream(bytes) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        }, () -> assertArrayEquals(bytes, FileUtility.getFileFromInternalResources("payload")));
        assertTrue(closed.get());
    }

    @Test
    void missingResourceReturnsNull() {
        withResource(null, () -> assertNull(FileUtility.getFileFromInternalResources("missing")));
    }

    @Test
    void readFailureReturnsNullAndClosesResource() {
        AtomicBoolean closed = new AtomicBoolean();
        withResource(new InputStream() {
            @Override
            public int read() throws IOException { throw new IOException("read failure"); }
            @Override
            public void close() { closed.set(true); }
        }, () -> assertNull(FileUtility.getFileFromInternalResources("broken")));
        assertTrue(closed.get());
    }

    private void withResource(InputStream stream, Runnable assertion) {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new ClassLoader(previous) {
            @Override
            public InputStream getResourceAsStream(String name) { return stream; }
        });
        try { assertion.run(); }
        finally { Thread.currentThread().setContextClassLoader(previous); }
    }
}
