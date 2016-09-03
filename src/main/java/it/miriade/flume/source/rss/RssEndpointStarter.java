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

import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.flume.Context;
import org.apache.flume.conf.Configurable;
import org.apache.flume.conf.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.Assert;

/**
 * 
 * @author svaponi
 *
 */
public class RssEndpointStarter implements Configurable, RssConstants {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	RssListener source;
	RssEndpoint endpoint;

	private String url;
	private String cronxpr;
	private int fixedrate;
	private String timeunit;
	private int poolsize;
	private boolean configured;
	private boolean useCronxpr = false;
	private boolean useFixedrate = false;

	public RssEndpointStarter(RssListener source) {
		super();
		this.source = source;
	}

	@Override
	public void configure(Context context) {

		if (!context.containsKey(RSS_ENDPOINT_URL))
			throw new ConfigurationException("Missing \"" + RSS_ENDPOINT_URL + "\" param");

		url = context.getString(RSS_ENDPOINT_URL);
		try {
			new URL(url);
		} catch (Exception e) {
			throw new ConfigurationException("Invalid \"" + RSS_ENDPOINT_URL + "\" param: " + e.getMessage());
		}
		log.info("\"{}\": {}", RSS_ENDPOINT_URL, url);

		if (context.containsKey(RSS_ENDPOINT_POLLER_CRONXPR)) {

			cronxpr = context.getString(RSS_ENDPOINT_POLLER_CRONXPR);
			try {
				new CronTrigger(cronxpr);
			} catch (Exception e) {
				throw new ConfigurationException(
						"Invalid \"" + RSS_ENDPOINT_POLLER_CRONXPR + "\" param: " + e.getMessage());
			}

			log.info("\"{}\": {}", RSS_ENDPOINT_POLLER_CRONXPR, cronxpr);
			useCronxpr = true;
		}

		if (context.containsKey(RSS_ENDPOINT_POLLER_FIXEDRATE)) {

			try {
				fixedrate = context.getInteger(RSS_ENDPOINT_POLLER_FIXEDRATE);
			} catch (Exception e) {
				throw new ConfigurationException(
						"Invalid \"" + RSS_ENDPOINT_POLLER_FIXEDRATE + "\" param: " + e.getMessage());
			}
			log.info("\"{}\": {}", RSS_ENDPOINT_POLLER_FIXEDRATE, fixedrate);

			if (!context.containsKey(RSS_ENDPOINT_POLLER_TIMEUNIT))
				throw new ConfigurationException("Missing \"" + RSS_ENDPOINT_POLLER_TIMEUNIT + "\" param");

			timeunit = context.getString(RSS_ENDPOINT_POLLER_TIMEUNIT);
			try {
				TimeUnit.valueOf(timeunit);
			} catch (Exception e) {
				throw new ConfigurationException("Invalid \"" + RSS_ENDPOINT_POLLER_TIMEUNIT + "\" param: [" + timeunit
						+ "] not in " + Arrays.deepToString(TimeUnit.values()));
			}

			log.info("\"{}\": {}", RSS_ENDPOINT_POLLER_TIMEUNIT, timeunit);
			useFixedrate = true;
		}

		if (!useFixedrate && !useCronxpr)
			throw new ConfigurationException("Missing poller interval config params, use \""
					+ RSS_ENDPOINT_POLLER_CRONXPR + "\" param or both [\"" + RSS_ENDPOINT_POLLER_FIXEDRATE + "\", \""
					+ RSS_ENDPOINT_POLLER_TIMEUNIT + "\"] params");

		if (!context.containsKey(RSS_ENDPOINT_POLLER_POOLSIZE))
			throw new ConfigurationException("Missing \"" + RSS_ENDPOINT_POLLER_POOLSIZE + "\" param");

		poolsize = context.getInteger(RSS_ENDPOINT_POLLER_POOLSIZE, poolsize);
		log.info("\"{}\": {}", RSS_ENDPOINT_POLLER_POOLSIZE, poolsize);

		configured = true;
	}

	public void startRssPoller() {

		if (!configured)
			throw new ConfigurationException("Missing configuration");

		System.setProperty(RSS_ENDPOINT_URL, url);
		if (useFixedrate) {
			System.setProperty(RSS_ENDPOINT_POLLER_FIXEDRATE, String.valueOf(fixedrate));
			System.setProperty(RSS_ENDPOINT_POLLER_TIMEUNIT, timeunit);
		}
		if (useCronxpr) {
			System.setProperty(RSS_ENDPOINT_POLLER_CRONXPR, cronxpr);
		}
		System.setProperty(RSS_ENDPOINT_POLLER_POOLSIZE, String.valueOf(poolsize));

		ApplicationContext ctx = null;
		if (useFixedrate)
			ctx = new ClassPathXmlApplicationContext(FIXEDRATE_CONTEXT_PATH);
		if (useCronxpr)
			ctx = new ClassPathXmlApplicationContext(CRON_CONTEXT_PATH);

		Assert.notNull(ctx, ApplicationContext.class.getSimpleName() + " should be not null");
		log.info("Spring Integration context loaded!");
		/*
		 * Una volta inizializzato il @MessageEndpoint comincierà a ricevere
		 * messaggi e rimarrà in ascolto. Ad ogni aggiornamento manderà un feed
		 * sul canale.
		 */
		endpoint = ctx.getBean(RssEndpoint.class);
		Assert.notNull(endpoint, RssEndpointStarter.class.getSimpleName() + " should be not null");
		log.info("RSS endpoint initialized!");

		endpoint.setListener(source);
	}

}
