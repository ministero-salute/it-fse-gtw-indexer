package it.finanze.sanita.fse2.ms.gtwindexer.isolation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import it.finanze.sanita.fse2.ms.gtwindexer.controller.LivenessCheckCTL;

@Tag("ISOLATION")
@ActiveProfiles("ISOLATION")
@SpringJUnitConfig(LivenessCheckCTL.class)
class LivenessIsolationTest {
    @Autowired
    private LivenessCheckCTL controller;

    @Test
    void statusEndpointReturnsUpWithoutInfrastructure() throws Exception {
        MockMvcBuilders.standaloneSetup(controller).build().perform(get("/status"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}
