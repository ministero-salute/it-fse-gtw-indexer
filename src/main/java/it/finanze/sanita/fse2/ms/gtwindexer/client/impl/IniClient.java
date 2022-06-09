package it.finanze.sanita.fse2.ms.gtwindexer.client.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import it.finanze.sanita.fse2.ms.gtwindexer.client.IIniClient;
import it.finanze.sanita.fse2.ms.gtwindexer.config.Constants;
import it.finanze.sanita.fse2.ms.gtwindexer.config.MicroservicesURLCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniPublicationResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.ConnectionRefusedException;
import lombok.extern.slf4j.Slf4j;

/**
 * Production implemention of Ini Client.
 * 
 * @author vincenzoingenito
 */
@Slf4j
@Component
@Profile("!" + Constants.Profile.DEV)
public class IniClient implements IIniClient {

    /**
	 * Serial version uid.
	 */
	private static final long serialVersionUID = -1470125906483650945L;

	@Autowired
    private transient RestTemplate restTemplate;
	
	@Autowired
	private MicroservicesURLCFG msUrlCFG;

	@Override
	public IniPublicationResponseDTO sendData(final String workflowInstanceId) {
		IniPublicationResponseDTO out = null;
		log.info("Calling ini client - START");
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "application/json");

		HttpEntity<?> entity = new HttpEntity<>(workflowInstanceId, headers);

		ResponseEntity<IniPublicationResponseDTO> response = null;
		try {
			response = restTemplate.exchange(msUrlCFG.getIniClientHost() + "/v1.0.0/ini-publish", HttpMethod.POST, entity, IniPublicationResponseDTO.class);
			out = response.getBody();
				log.info("{} status returned from Fhir mapping Client", response.getStatusCode());
		} catch(ResourceAccessException cex) {
			log.error("Connect error while call document reference ep :" + cex);
			throw new ConnectionRefusedException(msUrlCFG.getIniClientHost(),"Connection refused");
		} catch(Exception ex) {
			log.error("Generic error while call document reference ep :" + ex);
			throw new BusinessException("Generic error while call document reference ep :" + ex);
		}
		return out;
	}

}
