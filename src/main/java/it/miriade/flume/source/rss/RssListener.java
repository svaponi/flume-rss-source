package it.miriade.flume.source.rss;

import com.sun.syndication.feed.synd.SyndEntry;

public interface RssListener {

	void send(SyndEntry feed);

}