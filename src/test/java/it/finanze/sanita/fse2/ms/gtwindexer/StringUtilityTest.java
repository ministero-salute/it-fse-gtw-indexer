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
package it.finanze.sanita.fse2.ms.gtwindexer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import it.finanze.sanita.fse2.ms.gtwindexer.config.Constants;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniTraceResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.CurrentApplicationLogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ErrorLogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.EventStatusEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.EventTypeEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.OperationLogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ResultLogEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtwindexer.utility.StringUtility;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.Profile.TEST)
class StringUtilityTest {
    
    @Test
    void toJsonJacksonErrorTest() {
        Map<String, Object> map = new HashMap<>();
        map.put(null, "null");
        Assertions.assertThrows(BusinessException.class, () -> StringUtility.toJSONJackson(map));
    }
    
    @Test
    void toJSONTest() {
    	IniTraceResponseDTO obj = new IniTraceResponseDTO();
    	obj.setErrorMessage(null);
    	obj.setEsito(null);
    	
        Assertions.assertDoesNotThrow(() -> StringUtility.toJSON(obj));
    }
    
    @Test
	@DisplayName("enumeration test ")
	void currentApplicationLogEnum() {			
			for(CurrentApplicationLogEnum entry : Arrays.asList(CurrentApplicationLogEnum.values())) {
				assertNotNull(entry.getCode());
				assertNotNull(entry.getDescription());
				}
			
			for(ErrorLogEnum entry : Arrays.asList(ErrorLogEnum.values())) {
				assertNotNull(entry.getCode());
				assertNotNull(entry.getDescription());
				}
			
			for(EventTypeEnum entry : Arrays.asList(EventTypeEnum.values())) {
				assertNotNull(entry.getName());
				}
			
			for(OperationLogEnum entry : Arrays.asList(OperationLogEnum.values())) {
				assertNotNull(entry.getCode());
				assertNotNull(entry.getDescription());
				}
			
			for(ResultLogEnum entry : Arrays.asList(ResultLogEnum.values())) {
				assertNotNull(entry.getCode());
				assertNotNull(entry.getDescription());
				}
			
			for(EventStatusEnum entry : Arrays.asList(EventStatusEnum.values())) {
				assertNotNull(entry.getName());
				}
		}
    
    @Test
	@DisplayName("enumeration test ")
	void stringCheckTest() {						
    	String stringTest = "string";
    	String stringTestEmpty = null;
    	
		assertTrue(StringUtility.isNullOrEmpty(stringTestEmpty));
		assertFalse(StringUtility.isNullOrEmpty(stringTest));	

		}
     

}
