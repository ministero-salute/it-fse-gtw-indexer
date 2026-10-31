package it.finanze.sanita.fse2.ms.gtwindexer.isolation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BlockingIniException;
import it.finanze.sanita.fse2.ms.gtwindexer.service.impl.AccreditamentoSimulationSRV;

@Tag("ISOLATION")
class SimulationIsolationTest {
    @Test
    void crashPrefixProducesBlockingFailure() {
        var service = new AccreditamentoSimulationSRV();
        assertEquals("Crash ini simulated exception", assertThrows(BlockingIniException.class,
                () -> service.runSimulation("CRASH_INI-document")).getMessage());
    }

    @Test
    void ordinaryDocumentDoesNotSimulateFailure() {
        assertNull(new AccreditamentoSimulationSRV().runSimulation("document"));
    }
}
