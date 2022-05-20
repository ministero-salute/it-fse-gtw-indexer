package it.finanze.sanita.fse2.ms.gtwindexer.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.finanze.sanita.fse2.ms.gtwindexer.client.IIniClient;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtwindexer.repository.entity.IniEdsInvocationETY;
import it.finanze.sanita.fse2.ms.gtwindexer.repository.mongo.impl.IniInvocationRepo;
import it.finanze.sanita.fse2.ms.gtwindexer.service.IIniInvocationSRV;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class IniInvocationSRV implements IIniInvocationSRV {

	/**
	 * Serial version uid.
	 */
	private static final long serialVersionUID = -8674806764400269288L;

	@Autowired
	private IniInvocationRepo iniInvocationRepo;
	
	@Autowired
	private IIniClient iniClient;
	
	@Override
	public Boolean findAndSendToIniByTransactionId(String transactionId) {
		Boolean out = false;
		try {
			IniEdsInvocationETY iniInvocationETY = iniInvocationRepo.findByTransactionId(transactionId);
			iniClient.sendData(transactionId);
			out = true;
			//TODO - Ricostruire soap
		} catch(Exception ex) {
			log.error("Error while running find and send to ini by transaction id : " , ex);
			throw new BusinessException(ex);
		}
		return out;
	}

}
