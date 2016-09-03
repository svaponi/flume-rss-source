package it.miriade.flume.source.rss;

public interface RssConstants {

	public static final String RSS_ENDPOINT_URL = "rss.endpoint.url";
	public static final String RSS_ENDPOINT_POLLER_CRONXPR = "rss.endpoint.poller.cronxpr";
	public static final String RSS_ENDPOINT_POLLER_FIXEDRATE = "rss.endpoint.poller.fixedrate";
	public static final String RSS_ENDPOINT_POLLER_TIMEUNIT = "rss.endpoint.poller.timeunit";
	public static final String RSS_ENDPOINT_POLLER_POOLSIZE = "rss.endpoint.poller.poolsize";
	public static final String FIXEDRATE_CONTEXT_PATH = "classpath:integration-context-fixedrate.xml";
	public static final String CRON_CONTEXT_PATH = "classpath:integration-context-cronxpr.xml";
}
