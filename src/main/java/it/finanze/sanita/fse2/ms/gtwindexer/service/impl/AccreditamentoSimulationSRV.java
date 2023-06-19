/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.service.impl;

import org.springframework.stereotype.Service;

import it.finanze.sanita.fse2.ms.gtwindexer.dto.AccreditamentoSimulationDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.AccreditamentoPrefixEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BlockingIniException;
import it.finanze.sanita.fse2.ms.gtwindexer.service.IAccreditamentoSimulationSRV;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AccreditamentoSimulationSRV implements IAccreditamentoSimulationSRV {

	
	@Override
	public AccreditamentoSimulationDTO runSimulation(final String idDocumento) {
		AccreditamentoSimulationDTO output = null;
		AccreditamentoPrefixEnum prefixEnum = AccreditamentoPrefixEnum.getStartWith(idDocumento);
		if(prefixEnum!=null) {
			switch (prefixEnum) {
			case CRASH_INI:
				simulateCrashIni();
				break;
			default:
				break;
			}
		}
		return output;
	}
 

	private void simulateCrashIni() {
		log.info("Crash ini simulated");
		throw new BlockingIniException("Crash ini simulated exception");
	}

}
