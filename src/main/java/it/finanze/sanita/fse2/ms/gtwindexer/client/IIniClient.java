package it.finanze.sanita.fse2.ms.gtwindexer.client;

import java.io.Serializable;

import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IndexerValueDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniPublicationResponseDTO;

/**
 * Interface of Ini client.
 * 
 * @author vincenzoingenito
 */
public interface IIniClient extends Serializable {

    /**
     * Executes the call to ini-client for creation purpose.
     * 
     * @param workflowInstanceId Identifier of workflow instance that consents to retrieve data of creation.
     * @return Outcome of service call.
     */
    IniPublicationResponseDTO sendPublicationData(String workflowInstanceId);

    /**
     * Executes the call to ini-client for update purpose.
     * 
     * @param updateInfo Information of document to update.
     * @return Outcome of service call.
     */
    IniPublicationResponseDTO sendReplaceData(IndexerValueDTO updateInfo);

}
