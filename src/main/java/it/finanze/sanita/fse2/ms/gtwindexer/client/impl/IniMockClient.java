package it.finanze.sanita.fse2.ms.gtwindexer.client.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import it.finanze.sanita.fse2.ms.gtwindexer.client.IIniClient;
import it.finanze.sanita.fse2.ms.gtwindexer.config.Constants;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniPublicationResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.ResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;


/**
 * Test implemention of Ini Client.
 * 
 * @author vincenzoingenito
 */
@Slf4j
@Component
@Profile(Constants.Profile.DEV)
public class IniMockClient implements IIniClient {

    /**
	 * Serial version uid.
	 */
	private static final long serialVersionUID = -1094030146435617088L;

	private static final String INI_FORCE_EXCEPTION = "ini_force_exception";
	
	@Override
	public IniPublicationResponseDTO sendData(final String workflowInstanceId) {
		log.warn("ATTENZIONE : Si sta chiamando il client mockato . Assicurarsi che sia voluto");
		if(workflowInstanceId!=null && workflowInstanceId.trim().contains(INI_FORCE_EXCEPTION)) {
			throw new BusinessException("Eccezione di test");
		}
		return new IniPublicationResponseDTO();
	}
	
 

}