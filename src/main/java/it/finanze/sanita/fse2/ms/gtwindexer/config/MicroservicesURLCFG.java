/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

/**
 *  Microservices URL.
 */
@Configuration
@Getter
public class MicroservicesURLCFG {

    /** 
     *  Validator host.
     */
	@Value("${ms.url.gtw-ini-client-service}")
	private String iniClientHost;

}
