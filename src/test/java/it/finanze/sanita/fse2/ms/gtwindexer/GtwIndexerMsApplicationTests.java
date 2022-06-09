package it.finanze.sanita.fse2.ms.gtwindexer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import it.finanze.sanita.fse2.ms.gtwindexer.config.Constants;

import it.finanze.sanita.fse2.ms.gtwindexer.config.Constants;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan(basePackages = {Constants.ComponentScan.BASE})
@ActiveProfiles(Constants.Profile.DEV)
class GtwIndexerMsApplicationTests {

	@Test
	void contextLoads() {
	}

}
