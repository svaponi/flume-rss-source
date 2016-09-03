package it.miriade.flume.source.rss;

public interface TestParameters {

	String url = "http://meta.stackoverflow.com/feeds";
	String cronxpr = "0/2 * * * * *";
	String fixedrate = "2";
	String timeunit = "SECONDS";
	String poolsize = "1";
}
