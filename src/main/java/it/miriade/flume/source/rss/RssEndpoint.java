/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package it.miriade.flume.source.rss;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.util.Assert;

import com.sun.syndication.feed.synd.SyndEntry;

/**
 * 
 * @author svaponi
 *
 */
@MessageEndpoint
public class RssEndpoint {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private List<RssListener> listeners;
	private int total = 0;
	private int sent = 0;
	private int errors = 0;

	public void setListener(RssListener listener) {
		Assert.notNull(listener, "RssListener should not be null");
		log.info("Add RssListener {}", listener.getClass());
		if (this.listeners == null)
			this.listeners = new ArrayList<>();
		this.listeners.add(listener);
	}

	/*
	 * Metodi del RSS Activator
	 */

	/**
	 * Metodo dell'Activator che gestisce il salvataggio di una lista di feeds.
	 * 
	 * @param list
	 */
	@ServiceActivator
	public void process(List<SyndEntry> list) {
		for (SyndEntry e : list)
			this.process(e);
	}

	/**
	 * Metodo dell'Activator che provvede all'effettivo salvataggio del feed
	 * (solo se non già presente).
	 * 
	 * @param entry
	 */
	@ServiceActivator
	public void process(SyndEntry entry) {
		total++;
		if (!exists(entry))
			try {
				for (RssListener listener : listeners)
					listener.send(entry);
				sent++;
				log.debug("Processed entry no. {} and sent to {} listeners", total, listeners.size());
			} catch (Exception e) {
				errors++;
				log.error("Error processing entry no. {}: {}", total, e.getMessage());
			}
	}

	/**
	 * Controlla se il feed è già passato
	 * 
	 * @param feed
	 * @return
	 */
	private boolean exists(SyndEntry feed) {
		return false;
	}

	/**
	 * Torna il numero di entry spedite ai listeners
	 * 
	 * @return
	 */
	public int getSent() {
		return sent;
	}

	/**
	 * Torna il numero di entry andate in errore
	 * 
	 * @return
	 */
	public int getErrors() {
		return errors;
	}

}
