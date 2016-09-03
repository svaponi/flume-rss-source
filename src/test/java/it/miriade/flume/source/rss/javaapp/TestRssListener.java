package it.miriade.flume.source.rss.javaapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.sun.syndication.feed.synd.SyndEntry;

import it.miriade.flume.source.rss.RssListener;

public class TestRssListener implements RssListener {

	Logger logger = LoggerFactory.getLogger(getClass());
	int counter;

	public TestRssListener() {
		super();
		logger.info("Initialize {}", TestRssListener.class.getName());
	}

	@Override
	public void send(SyndEntry feed) {
		Assert.notNull(feed, SyndEntry.class.getSimpleName() + " should be not null");
		logger.info(" {} > {}", ++counter, feed);
	}
}