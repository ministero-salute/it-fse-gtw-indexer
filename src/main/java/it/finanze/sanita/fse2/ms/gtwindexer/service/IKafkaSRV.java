/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.service;

import it.finanze.sanita.fse2.ms.gtwindexer.enums.EventStatusEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.EventTypeEnum;
import org.apache.kafka.clients.consumer.ConsumerRecord;
 

public interface IKafkaSRV {

	void lowPriorityListener(ConsumerRecord<String, String> cr, int delivery) throws Exception;

	void mediumPriorityListener(final ConsumerRecord<String, String> cr, int delivery) throws Exception;

	void highPriorityListener(final ConsumerRecord<String, String> cr, int delivery) throws Exception;

	void retryDeleteListener(ConsumerRecord<String, String> cr, int delivery) throws Exception;
	
	void retryUpdateListener(ConsumerRecord<String, String> cr, int delivery) throws Exception;

	void sendStatusMessage(String workflowInstanceId, EventTypeEnum eventType, EventStatusEnum eventStatus, String exception);
}
