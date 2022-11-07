/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.client;

import java.io.Serializable;

import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IniDeleteRequestDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniPublicationResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniTraceResponseDTO;

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
    IniPublicationResponseDTO sendReplaceData(String workflowInstanceId);

    /**
     * Execute the call to ini-client for deletion purpose
     * @param iniReq The delete request
     * @return Outcome of service call
     */
    IniTraceResponseDTO delete(IniDeleteRequestDTO iniReq);

}
