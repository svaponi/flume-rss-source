package it.miriade.flume.source.rss.javaapp;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Assert;

import it.miriade.commons.utils.StringHandler;
import it.miriade.flume.source.rss.RssConstants;
import it.miriade.flume.source.rss.RssEndpoint;
import it.miriade.flume.source.rss.RssEndpointStarter;
import it.miriade.flume.source.rss.TestParameters;

public class MainCronxprRssEndpoint implements TestParameters, RssConstants {

	static final Logger LOGGER = LoggerFactory.getLogger(MainCronxprRssEndpoint.class);

	static RssEndpoint endpoint;
	static ApplicationContext ctx;

	public static void main(String[] args) {

		Properties props = System.getProperties();
		props.put(RSS_ENDPOINT_URL, url);
		props.put(RSS_ENDPOINT_POLLER_CRONXPR, cronxpr);
		props.put(RSS_ENDPOINT_POLLER_FIXEDRATE, Integer.parseInt(fixedrate));
		props.put(RSS_ENDPOINT_POLLER_TIMEUNIT, timeunit);
		props.put(RSS_ENDPOINT_POLLER_POOLSIZE, Integer.parseInt(poolsize));
		System.setProperties(props);
		LOGGER.info("System properties: ");
		for (Object param : System.getProperties().keySet())
			if (StringHandler.toString(param).startsWith("rss.endpoint"))
				LOGGER.info("{} = {}", param, System.getProperties().get(param));

		ctx = new ClassPathXmlApplicationContext(FIXEDRATE_CONTEXT_PATH);
		Assert.notNull(ctx, ApplicationContext.class.getSimpleName() + " should be not null");
		LOGGER.info("Spring Integration context loaded!");

		/*
		 * Una volta inizializzato il @MessageEndpoint comincierà a ricevere
		 * messaggi e rimarrà in ascolto. Ad ogni aggiornamento manderà un feed
		 * sul canale.
		 */
		endpoint = ctx.getBean(RssEndpoint.class);
		Assert.notNull(endpoint, RssEndpointStarter.class.getSimpleName() + " should be not null");
		endpoint.setListener(new TestRssListener());

		try {
			Thread.sleep(10000);
			Assert.isTrue(true, "Waiting for feeds to come...");
		} catch (Exception e) {
			Assert.isTrue(false, e.getMessage());
		}

	}
}
