/*
 * Copyright 2010 Widen Enterprises, Inc.
 * Madison, Wisconsin USA -- www.widen.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.widen.valet.importer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.widen.valet.RecordType;
import com.widen.valet.Route53Driver;
import com.widen.valet.Zone;
import com.widen.valet.ZoneChangeStatus;
import com.widen.valet.ZoneResource;
import com.widen.valet.ZoneUpdateAction;
import com.widen.valet.util.ListUtil;
import com.widen.valet.util.NameQueryByRoute53APIService;
import com.widen.valet.util.NameQueryService;
import com.widen.valet.util.NameQueryServiceImpl;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Smart sync zone from file.
 *
 * <p><b>Only tested using Windows 2003 DNS server files!</b>
 *
 * <p>Resources will be one-way synced with file. Route53 entries will be created or modified to match
 * zone file. Additional records present in Route53 will not be deleted.
 *
 * <p>Zones are <b>not</b> auto-created. Use ImportBulkZones to auto-create Route53 hosted zones.
 *
 * <p>Uses <b>importdns.properties</b>; see importdns.properties.sample for details.
 */
public class ImportZone
{
	private final Logger log = LoggerFactory.getLogger(ImportZone.class);

	private final String awsAccessKey;

	private final String awsPrivateKey;

	private final String importFile;

	private final String route53ZoneId;

	private final int defaultTTL;

	private final boolean dryRun;

	private final boolean cleanZone;

	private final String nameServer;

	private NameQueryService queryService;

	public static void main(String[] args) throws IOException
	{
		Properties properties = new Properties();

		InputStream stream = ImportZone.class.getResourceAsStream("importdns.properties");

		if (stream == null)
		{
			throw new RuntimeException("File importdns.properties not found!");
		}

		properties.load(stream);

		new ImportZone(properties).run();
	}

	public ImportZone(Properties properties)
	{
		awsAccessKey = getAndVerifyProperty("widen.valet.aws-access-key", properties);
		awsPrivateKey = getAndVerifyProperty("widen.valet.aws-private-key", properties);
		importFile = getAndVerifyProperty("widen.valet.import-file", properties);
		route53ZoneId = getAndVerifyProperty("widen.valet.aws-route53-zone-id", properties);
		defaultTTL = Integer.parseInt(getAndVerifyProperty("widen.valet.default-ttl", properties));
		dryRun = Boolean.parseBoolean(getAndVerifyProperty("widen.valet.dry-run", properties));
		nameServer = getAndVerifyProperty("widen.valet.aws-name-server", properties);
		cleanZone = Boolean.parseBoolean(getAndVerifyProperty("widen.valet.clean-zone-by-deleteing-all-records", properties));
	}

	private String getAndVerifyProperty(String key, Properties properties)
	{
		String prop = properties.getProperty(key);

		if (StringUtils.isBlank(prop))
		{
			throw new RuntimeException("Property key " + key + " cannot be blank.");
		}

		return prop;
	}

	public void run() throws IOException
	{
		Route53Driver driver = new Route53Driver(awsAccessKey, awsPrivateKey);

		Zone zone = driver.zoneDetails(route53ZoneId);

		if (cleanZone)
		{
			List<ZoneUpdateAction> deletes = new ArrayList<ZoneUpdateAction>();

			for (ZoneResource resource : driver.listZoneRecords(zone))
			{
				if (!Arrays.asList(RecordType.NS, RecordType.SOA).contains(resource.getRecordType()))
				{
					ZoneUpdateAction zua = new ZoneUpdateAction.Builder().fromZoneResource(resource).buildDeleteAction();
					deletes.add(zua);
				}
			}

			for (List<ZoneUpdateAction> deleteChunk : ListUtil.split(deletes, 100))
			{
				ZoneChangeStatus status = driver.updateZone(zone, "Clean all zone records before re-import", deleteChunk);

				driver.waitForSync(status);
			}
		}

		if ("route53rrs".equals(nameServer))
		{
			queryService = new NameQueryByRoute53APIService(driver, zone);
		}
		else
		{
			queryService = new NameQueryServiceImpl(nameServer);
		}

		final List<ZoneUpdateAction> actions = createZoneUpdateActions(zone);

		log.info("Zone Update Actions Created:\n{}", StringUtils.join(actions, "\n"));

		if (dryRun)
		{
			log.info("Dry run complete.");
			return;
		}

		int batch = 0;

		final int MAX_CHANGE_RECORDS_ALLOWED = 100;

		for (List<ZoneUpdateAction> processingList : ListUtil.split(actions, MAX_CHANGE_RECORDS_ALLOWED))
		{
			batch++;

			ZoneChangeStatus sync = driver.updateZone(zone, "Sync zone records from file" + importFile + " batch " + batch, processingList);

			driver.waitForSync(sync);
		}

		log.info("Zone import complete and INSYNC!");
	}

	private List<ZoneUpdateAction> createZoneUpdateActions(Zone zone) throws IOException
	{
		List<String> lines = IOUtils.readLines(new FileInputStream(importFile));

		boolean foundZoneRecords = false;

		List<ZoneUpdateAction> actions = new ArrayList<ZoneUpdateAction>();

		for (String l : lines)
		{
			if (!foundZoneRecords)
			{
				if (StringUtils.contains(l, ";  Zone records"))
				{
					foundZoneRecords = true;
				}

				continue;
			}

			if (StringUtils.isNotBlank(l) && !l.startsWith(";"))
			{
				log.debug("processing line: {}", l);

				List<ZoneUpdateAction> actionForRecord = parseRecord(l, zone.getName(), actions);

				actions.addAll(actionForRecord);
			}
		}

		return actions;
	}

	private List<ZoneUpdateAction> parseRecord(String record, String zone, List<ZoneUpdateAction> existing)
	{
		final List<String> split = ZoneFileLineSplitter.splitLine(record);

		final String rawName = split.get(0);

		final String name;

		if ("@".equals(rawName))
		{
			name = zone.toLowerCase();
		}
		else
		{
			name = String.format("%s.%s", rawName, zone).toLowerCase();
		}

		final RecordType type = RecordType.valueOf(split.get(1));

		final String value = split.get(2).toLowerCase();

		NameQueryService.LookupRecord lookupRecord = queryService.lookup(name, type);

		if (!lookupRecord.exists)
		{
			ZoneUpdateAction action = new ZoneUpdateAction.Builder().withData(name, type, Arrays.asList(value)).withTtl(defaultTTL).buildCreateAction();

			return Arrays.asList(mergeAction(action, existing));
		}
		else if (lookupRecord.valueEqual(value))
		{
			log.debug("{} {} {} is current", new Object[]{name, type, lookupRecord.values});

			return Collections.emptyList();
		}
		else
		{
			ZoneUpdateAction delete = new ZoneUpdateAction.Builder().withData(name, type, lookupRecord.values).withTtl(lookupRecord.ttl).buildDeleteAction();

			ZoneUpdateAction create = new ZoneUpdateAction.Builder().withData(name, type, Arrays.asList(value)).withTtl(defaultTTL).buildCreateAction();

			return Arrays.asList(mergeAction(delete, existing), mergeAction(create, existing));
		}
	}

	ZoneUpdateAction mergeAction(ZoneUpdateAction action, List<ZoneUpdateAction> existing)
	{
		int itemToMerge = Collections.binarySearch(existing, action);

		if (itemToMerge >= 0)
		{
			List<String> resources = existing.get(itemToMerge).getResourceRecords();

			existing.remove(itemToMerge);

			return ZoneUpdateAction.mergeResources(action, resources);
		}
		else
		{
			return action;
		}
	}

}
