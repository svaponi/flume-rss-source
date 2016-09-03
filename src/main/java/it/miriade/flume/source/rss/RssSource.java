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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.annotations.InterfaceAudience;
import org.apache.flume.annotations.InterfaceStability;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.source.AbstractSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndEntry;

import it.miriade.commons.utils.StringHandler;

/**
 * 
 * @author svaponi
 *
 */
@InterfaceAudience.Private
@InterfaceStability.Unstable
public class RssSource extends AbstractSource implements EventDrivenSource, Configurable, RssListener {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private RssEndpointStarter starter;
	private Schema avroSchema;
	private long docCount = 0;
	private long startTime = 0;
	private long exceptionCount = 0;
	private long totalTextIndexed = 0;
	private long skippedDocs = 0;
	private long batchEndTime = 0;
	private final List<Record> docs = new ArrayList<Record>();
	private final ByteArrayOutputStream serializationBuffer = new ByteArrayOutputStream();
	private DataFileWriter<GenericRecord> dataFileWriter;

	private String name;
	private int maxBatchSize = 1000;
	private int maxBatchDurationMillis = 1000;

	// Fri May 14 02:52:55 +0000 2010
	private SimpleDateFormat formatterTo = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	private DecimalFormat numFormatter = new DecimalFormat("###,###.###");

	private static int REPORT_INTERVAL = 100;
	private static int STATS_INTERVAL = REPORT_INTERVAL * 10;

	public RssSource() {
		super();
		starter = new RssEndpointStarter(this);
	}

	@Override
	public synchronized String getName() {
		return name;
	}

	@Override
	public void configure(Context context) {

		avroSchema = createAvroSchema();
		dataFileWriter = new DataFileWriter<GenericRecord>(new GenericDatumWriter<GenericRecord>(avroSchema));

		name = context.getString("name", RssSource.class.getSimpleName());
		maxBatchSize = context.getInteger("maxBatchSize", maxBatchSize);
		maxBatchDurationMillis = context.getInteger("maxBatchDurationMillis", maxBatchDurationMillis);

		starter.configure(context);
		starter.startRssPoller();
	}

	@Override
	public synchronized void start() {
		log.info("Starting Flume RSS source {} ...", this);
		docCount = 0;
		startTime = System.currentTimeMillis();
		exceptionCount = 0;
		totalTextIndexed = 0;
		skippedDocs = 0;
		batchEndTime = System.currentTimeMillis() + maxBatchDurationMillis;
		log.info("Flume RSS source {} started.", getName());
		super.start();
	}

	@Override
	public synchronized void stop() {
		log.info("Flume RSS source {} stopping...", getName());
		super.stop();
		log.info("Flume RSS source {} stopped.", getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.miriade.flume.source.rss.RssListener#send(com.sun.syndication.feed.
	 * synd.SyndEntry)
	 */
	@Override
	public void send(SyndEntry feed) {
		log.info("Total docs indexed: {}, total skipped docs: {}", numFormatter.format(docCount),
				numFormatter.format(skippedDocs));

		Record doc = extractRecord(avroSchema, feed);
		if (doc == null) {
			return; // skip
		}
		docs.add(doc);
		if (docs.size() >= maxBatchSize || System.currentTimeMillis() >= batchEndTime) {
			batchEndTime = System.currentTimeMillis() + maxBatchDurationMillis;
			byte[] bytes;
			try {
				bytes = serializeToAvro(avroSchema, docs);
			} catch (IOException e) {
				log.error("Exception while serializing tweet", e);
				return; // skip
			}
			Event event = EventBuilder.withBody(bytes);
			getChannelProcessor().processEvent(event); // send event to the
														// flume sink
			docs.clear();
		}
		docCount++;
		if ((docCount % REPORT_INTERVAL) == 0) {
			log.info("Processed {} docs", numFormatter.format(docCount));
		}
		if ((docCount % STATS_INTERVAL) == 0) {
			logStats();
		}
	}

	private Schema createAvroSchema() {
		Schema avroSchema = Schema.createRecord("Doc", "adoc", null, false);
		List<Field> fields = new ArrayList<Field>();
		fields.add(new Field("id", Schema.create(Type.LONG), null, null));
		fields.add(new Field("published_at", createOptional(Schema.create(Type.STRING)), null, null));
		fields.add(new Field("updated_at", createOptional(Schema.create(Type.STRING)), null, null));
		fields.add(new Field("title", createOptional(Schema.create(Type.STRING)), null, null));
		fields.add(new Field("author", createOptional(Schema.create(Type.STRING)), null, null));
		fields.add(new Field("content", createOptional(Schema.create(Type.STRING)), null, null));
		fields.add(new Field("content_type", createOptional(Schema.create(Type.STRING)), null, null));
		fields.add(new Field("link", createOptional(Schema.create(Type.STRING)), null, null));
		fields.add(new Field("uri", createOptional(Schema.create(Type.STRING)), null, null));
		avroSchema.setFields(fields);
		return avroSchema;
	}

	private Record extractRecord(Schema avroSchema, SyndEntry e) {
		Record doc = new Record(avroSchema);
		String tmpid = StringHandler.join("#", // separator
				formatterTo.format(e.getPublishedDate()), // published
				formatterTo.format(e.getUpdatedDate()), // updated
				e.getTitle()); // title
		doc.put("id", tmpid.hashCode() & Long.MAX_VALUE);
		addString(doc, "published_at", formatterTo.format(e.getPublishedDate()));
		addString(doc, "updated_at", formatterTo.format(e.getUpdatedDate()));
		addString(doc, "title", e.getTitle());
		addString(doc, "author", e.getAuthor());
		addString(doc, "content", e.getDescription().getValue());
		addString(doc, "content_mode", e.getDescription().getMode());
		addString(doc, "content_type", e.getDescription().getType());
		addString(doc, "link", e.getLink());
		addString(doc, "uri", e.getUri());
		return doc;
	}

	private byte[] serializeToAvro(Schema avroSchema, List<Record> docList) throws IOException {
		serializationBuffer.reset();
		dataFileWriter.create(avroSchema, serializationBuffer);
		for (Record doc2 : docList) {
			dataFileWriter.append(doc2);
		}
		dataFileWriter.close();
		return serializationBuffer.toByteArray();
	}

	private Schema createOptional(Schema schema) {
		return Schema.createUnion(Arrays.asList(new Schema[] { schema, Schema.create(Type.NULL) }));
	}

	private void addString(Record doc, String avroField, String val) {
		if (val == null) {
			return;
		}
		doc.put(avroField, val);
		totalTextIndexed += val.length();
	}

	private void logStats() {
		double mbIndexed = totalTextIndexed / (1024 * 1024.0);
		long seconds = (System.currentTimeMillis() - startTime) / 1000;
		seconds = Math.max(seconds, 1);
		log.info("Total docs indexed: {}, total skipped docs: {}", numFormatter.format(docCount),
				numFormatter.format(skippedDocs));
		log.info("    {} docs/second", numFormatter.format(docCount / seconds));
		log.info("Run took {} seconds and processed:", numFormatter.format(seconds));
		log.info("    {} MB/sec sent to index",
				numFormatter.format(((float) totalTextIndexed / (1024 * 1024)) / seconds));
		log.info("    {} MB text sent to index", numFormatter.format(mbIndexed));
		log.info("There were {} exceptions ignored: ", numFormatter.format(exceptionCount));
	}

}
