package it.finanze.sanita.fse2.ms.gtwindexer.client.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import it.finanze.sanita.fse2.ms.gtwindexer.client.IIniClient;
import it.finanze.sanita.fse2.ms.gtwindexer.config.Constants;
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
 
 
	@Override
	public Boolean sendData(String transactionId) {
		log.info("Production implementation ini client");
		return null;
	}

}
