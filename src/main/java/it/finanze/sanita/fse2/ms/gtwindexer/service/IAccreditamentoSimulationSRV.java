/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.service;

import it.finanze.sanita.fse2.ms.gtwindexer.dto.AccreditamentoSimulationDTO;

public interface IAccreditamentoSimulationSRV {

	AccreditamentoSimulationDTO runSimulation(String idDocumento);
}
