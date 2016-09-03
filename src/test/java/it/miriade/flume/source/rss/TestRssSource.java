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

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.flume.Channel;
import org.apache.flume.ChannelSelector;
import org.apache.flume.Context;
import org.apache.flume.Sink;
import org.apache.flume.SinkRunner;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.channel.MemoryChannel;
import org.apache.flume.channel.ReplicatingChannelSelector;
import org.apache.flume.conf.Configurables;
import org.apache.flume.sink.DefaultSinkProcessor;
import org.apache.flume.sink.LoggerSink;
import org.junit.Assert;
import org.junit.Test;

public class TestRssSource extends Assert implements RssConstants, TestParameters {

	private static final long RUNNING_TIME = 10000;

	@Test
	public void testFixedRateSetup() throws Exception {

		Context context = new Context();
		context.put(RSS_ENDPOINT_URL, url);
		context.put(RSS_ENDPOINT_POLLER_FIXEDRATE, String.valueOf(fixedrate));
		context.put(RSS_ENDPOINT_POLLER_TIMEUNIT, timeunit);
		context.put(RSS_ENDPOINT_POLLER_POOLSIZE, String.valueOf(poolsize));
		context.put("maxBatchSize", "1000");
		context.put("maxBatchDurationMillis", "1000");

		RssSource source = new RssSource();
		source.configure(context);

		Map<String, String> channelContext = new HashMap<String, String>();
		channelContext.put("capacity", "1000000");
		channelContext.put("keep-alive", "0"); // for faster tests
		Channel channel = new MemoryChannel();
		Configurables.configure(channel, new Context(channelContext));

		Sink sink = new LoggerSink();
		sink.setChannel(channel);
		sink.start();
		DefaultSinkProcessor proc = new DefaultSinkProcessor();
		proc.setSinks(Collections.singletonList(sink));
		SinkRunner sinkRunner = new SinkRunner(proc);
		sinkRunner.start();

		ChannelSelector rcs = new ReplicatingChannelSelector();
		rcs.setChannels(Collections.singletonList(channel));
		ChannelProcessor chp = new ChannelProcessor(rcs);
		source.setChannelProcessor(chp);
		source.start();

		Thread.sleep(RUNNING_TIME);
		source.stop();
		sinkRunner.stop();
		sink.stop();
	}

	@Test
	public void testCronExpressionSetup() throws Exception {

		Context context = new Context();
		context.put(RSS_ENDPOINT_URL, url);
		context.put(RSS_ENDPOINT_POLLER_CRONXPR, cronxpr);
		context.put(RSS_ENDPOINT_POLLER_POOLSIZE, String.valueOf(poolsize));
		context.put("maxBatchSize", "1000");
		context.put("maxBatchDurationMillis", "1000");

		RssSource source = new RssSource();
		source.configure(context);

		Map<String, String> channelContext = new HashMap<String, String>();
		channelContext.put("capacity", "1000000");
		channelContext.put("keep-alive", "0"); // for faster tests
		Channel channel = new MemoryChannel();
		Configurables.configure(channel, new Context(channelContext));

		Sink sink = new LoggerSink();
		sink.setChannel(channel);
		sink.start();
		DefaultSinkProcessor proc = new DefaultSinkProcessor();
		proc.setSinks(Collections.singletonList(sink));
		SinkRunner sinkRunner = new SinkRunner(proc);
		sinkRunner.start();

		ChannelSelector rcs = new ReplicatingChannelSelector();
		rcs.setChannels(Collections.singletonList(channel));
		ChannelProcessor chp = new ChannelProcessor(rcs);
		source.setChannelProcessor(chp);
		source.start();

		Thread.sleep(RUNNING_TIME);
		source.stop();
		sinkRunner.stop();
		sink.stop();
	}

	@Test
	public void testCarrotDateFormatBug() throws Exception {
		SimpleDateFormat formatterFrom = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
		formatterFrom.parse("Fri Oct 26 22:53:55 +0000 2012");
	}

}
