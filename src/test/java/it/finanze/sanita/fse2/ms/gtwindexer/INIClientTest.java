package it.finanze.sanita.fse2.ms.gtwindexer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import it.finanze.sanita.fse2.ms.gtwindexer.client.IIniClient;
import it.finanze.sanita.fse2.ms.gtwindexer.config.Constants;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniPublicationResponseDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan(basePackages = {Constants.ComponentScan.BASE})
@ActiveProfiles(Constants.Profile.TEST)
@Disabled
public class INIClientTest {
	
	@Autowired
	private IIniClient iniClient;

	@Test
	@DisplayName("Send to INI Client Microservice")
	@Disabled
	void send() {
		String workflowInstanceId = "d870d05ba24f4b9891d22e461d71595b";
		IniPublicationResponseDTO res = iniClient.sendData(workflowInstanceId);
		assertNotNull(res.getErrorMessage());
		 
    }
}
