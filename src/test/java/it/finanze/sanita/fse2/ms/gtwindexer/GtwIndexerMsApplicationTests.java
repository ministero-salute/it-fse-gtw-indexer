package it.finanze.sanita.fse2.ms.gtwindexer;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import it.finanze.sanita.fse2.ms.gtwindexer.config.Constants;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ILogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.OperationLogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ResultLogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.logging.ElasticLoggerHelper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan(basePackages = {Constants.ComponentScan.BASE})
@ActiveProfiles(Constants.Profile.TEST)
class GtwIndexerMsApplicationTests {
	@Autowired
	private ElasticLoggerHelper elasticLogger;

	@Test
	void contextLoads() {
	    elasticLogger.info("messaggio per elk", OperationLogEnum.CALL_INI, ResultLogEnum.OK, new Date());
	    elasticLogger.debug("messaggio ko", OperationLogEnum.CALL_INI, ResultLogEnum.KO, new Date());
	    elasticLogger.warn("messaggio ok", OperationLogEnum.CALL_INI, ResultLogEnum.OK, new Date());
	    elasticLogger.error("messaggio errore", OperationLogEnum.CALL_INI, ResultLogEnum.OK, new Date(), new ILogEnum() {

			@Override
			public String getCode() {
				return null;
			}

			@Override
			public String getDescription() {
				return null;
			}

	    });

	}
}