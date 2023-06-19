/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * 
 * Copyright (C) 2023 Ministero della Salute
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package it.finanze.sanita.fse2.ms.gtwindexer.client;

import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IniDeleteRequestDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IniMetadataUpdateReqDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniTraceResponseDTO;

/**
 * Interface of Ini client.
 */
public interface IIniClient {

    /**
     * Executes the call to ini-client for creation purpose.
     * 
     * @param workflowInstanceId Identifier of workflow instance that consents to retrieve data of creation.
     * @return Outcome of service call.
     */
	IniTraceResponseDTO sendPublicationData(String workflowInstanceId);

    /**
     * Executes the call to ini-client for update purpose.
     * 
     * @param updateInfo Information of document to update.
     * @return Outcome of service call.
     */
	IniTraceResponseDTO sendReplaceData(String workflowInstanceId);

    /**
     * Execute the call to ini-client for deletion purpose
     * @param iniReq The delete request
     * @return Outcome of service call
     */
    IniTraceResponseDTO delete(IniDeleteRequestDTO iniReq);
    
    /**
     * Execute the call to ini-client for deletion purpose
     * @param iniReq The delete request
     * @return Outcome of service call
     */
    IniTraceResponseDTO sendUpdateData(IniMetadataUpdateReqDTO iniReq);

}
