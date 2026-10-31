package it.finanze.sanita.fse2.ms.gtwindexer.isolation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import it.finanze.sanita.fse2.ms.gtwindexer.config.Constants;
import it.finanze.sanita.fse2.ms.gtwindexer.config.OpenTelemetryConfig;

@Tag("ISOLATION")
class TelemetryIsolationTest {
    @Test
    void registersConfiguredSdkAndUsesApplicationInstrumentationName() {
        var sdk = mock(OpenTelemetrySdk.class);
        var auto = mock(AutoConfiguredOpenTelemetrySdk.class);
        var tracer = mock(Tracer.class);
        when(auto.getOpenTelemetrySdk()).thenReturn(sdk);
        var config = new OpenTelemetryConfig();
        ReflectionTestUtils.setField(config, "autoConfiguredOpenTelemetrySdk", auto);
        try (var global = mockStatic(GlobalOpenTelemetry.class)) {
            global.when(() -> GlobalOpenTelemetry.getTracer(Constants.Properties.MS_NAME)).thenReturn(tracer);
            config.init();
            global.verify(() -> GlobalOpenTelemetry.set(sdk));
            assertSame(tracer, config.tracer());
        }
    }
}
