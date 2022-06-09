package it.finanze.sanita.fse2.ms.gtwindexer.client;

import java.io.Serializable;

import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniPublicationResponseDTO;

/**
 * Interface of Ini client.
 * 
 * @author vincenzoingenito
 */
public interface IIniClient extends Serializable {

    IniPublicationResponseDTO sendData(String workflowInstanceId);
}
