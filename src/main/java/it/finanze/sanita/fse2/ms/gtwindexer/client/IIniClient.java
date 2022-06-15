package it.finanze.sanita.fse2.ms.gtwindexer.client;

import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.ResponseDTO;

/**
 * Interface of Ini client.
 * 
 * @author vincenzoingenito
 */
public interface IIniClient {

    ResponseDTO sendData(String transactionId);
}
