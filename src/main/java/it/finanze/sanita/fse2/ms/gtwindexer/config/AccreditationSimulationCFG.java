package it.finanze.sanita.fse2.ms.gtwindexer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

/**
 *  Configuration accreditation simulation.
 */
@Configuration
@Getter
public class AccreditationSimulationCFG {

    /** 
     *  Flag che permette di accendere e spegnere il meccanismo di accreditamento.
     */
	@Value("${accreditation.enable-check}")
	private boolean enableCheck;

}
