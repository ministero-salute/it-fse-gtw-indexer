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
package it.finanze.sanita.fse2.ms.gtwindexer.config.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import it.finanze.sanita.fse2.ms.gtwindexer.config.Constants;
import it.finanze.sanita.fse2.ms.gtwindexer.utility.ProfileUtility;
import jakarta.annotation.PostConstruct;
import lombok.Data;

/**
 *
 *	Kafka topic configuration.
 */
@Data
@Component
public class KafkaTopicCFG {

	@Autowired
	private ProfileUtility profileUtility;

	/**
	 * Dispatcher indexer Topic.
	 */
	@Value("${kafka.dispatcher-indexer.base-topic}")
	private String dispatcherIndexerTopic;

	/**
	 * Dispatcher indexer Dead letter Topic. 
	 */
	@Value("${kafka.dispatcher-indexer.deadletter.topic}")
	private String dispatcherIndexerDeadLetterTopic;

	/**
	 * Dispatcher indexer retry topic (delete)
	 */
	@Value("${kafka.dispatcher-indexer.delete-retry-topic}")
	private String dispatcherIndexerDeleteRetryTopic;

	/**
	 * Dispatcher indexer retry topic (update)
	 */
	@Value("${kafka.dispatcher-indexer.update-retry-topic}")
	private String dispatcherIndexerUpdateRetryTopic;

	/**
	 * Indexer publisher Topic.
	 */
	@Value("${kafka.indexer-publisher.topic}")
	private String indexerPublisherTopic;

	/**
	 * Indexer status manager Topic.
	 */
	@Value("${kafka.statusmanager.topic}")
	private String statusManagerTopic;

	@PostConstruct
	public void afterInit() {
		if (profileUtility.isTestProfile()) {
			this.dispatcherIndexerTopic =
					Constants.Profile.TEST_PREFIX + this.dispatcherIndexerTopic;
			this.dispatcherIndexerDeadLetterTopic = Constants.Profile.TEST_PREFIX + this.dispatcherIndexerDeadLetterTopic;
			this.dispatcherIndexerDeleteRetryTopic =  Constants.Profile.TEST_PREFIX + this.dispatcherIndexerDeleteRetryTopic;
			this.dispatcherIndexerUpdateRetryTopic =  Constants.Profile.TEST_PREFIX + this.dispatcherIndexerUpdateRetryTopic;
			this.indexerPublisherTopic = Constants.Profile.TEST_PREFIX + this.indexerPublisherTopic;
			this.statusManagerTopic = Constants.Profile.TEST_PREFIX + this.statusManagerTopic;
		}
	}

}
