/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IniMetadataUpdateReqDTO {
	
    private String marshallData;
    
    private JWTPayloadDTO token;
    
}
