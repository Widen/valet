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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.widen.valet.Route53Driver;
import com.widen.valet.Zone;
import com.widen.valet.ZoneChangeStatus;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create all zones ending in .dns in zones directory. (e.g. "zones/domain.com.dns" --> domain.com)
 *
 * <p>Auto-creates zone if it does not exist in Route53.
 */
public class ImportBulkZones
{
	private static final int MAX_FILES_TO_PROCESS = 100;

	private static final String AWS_ACCESS_KEY = "";

	private static final String AWS_SECRET_KEY = "";

	private final Logger log = LoggerFactory.getLogger(ImportBulkZones.class);

	private final Route53Driver driver = new Route53Driver(AWS_ACCESS_KEY, AWS_SECRET_KEY);

	private List<Zone> existingZonesCache;

	public static void main(String[] args)
	{
		new ImportBulkZones().run();
	}

	private void run()
	{
		existingZonesCache = Collections.unmodifiableList(driver.listZones());

		File root = new File("src/main/java/com/widen/valet/importer/zones");

		int processCount = 0;

		for (File f : root.listFiles())
		{
			log.debug("listing file {}", f);

			try
			{
				processCount++;

				processZoneFile(f);

				if (processCount >= MAX_FILES_TO_PROCESS)
				{
					break;
				}
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	private Zone findZone(String domain)
	{
		for (Zone z : existingZonesCache)
		{
			if (StringUtils.equalsIgnoreCase(z.getName(), domain))
			{
				return z;
			}
		}

		log.warn("zone {} does not exist", domain);

		return null;
	}

	private void processZoneFile(File f) throws IOException
	{
		if (!f.getName().endsWith(".dns"))
		{
			log.warn("Skipping file {}", f.getAbsolutePath());
			return;
		}

		String zoneName = StringUtils.substringBefore(f.getName(), "dns");

		Zone zone = findZone(zoneName);

		if (zone != null)
		{
			log.info("zone {} = {}", zoneName, zone);
		}
		else
		{
			log.info("creating new zone for {}", zoneName);

			ZoneChangeStatus changeStatus = driver.createZone(zoneName, "Create zone " + zoneName);

			driver.waitForSync(changeStatus);

			zone = driver.zoneDetails(changeStatus.getZoneId());
		}

		ImportZone importZone = new ImportZone(zoneProperties(f.getAbsolutePath(), zone.getZoneId()));

		importZone.run();
	}

	private Properties zoneProperties(String fileName, String zoneId)
	{
		Properties properties = new Properties();

		properties.put("widen.valet.dry-run", "false");
		properties.put("widen.valet.aws-access-key", AWS_ACCESS_KEY);
		properties.put("widen.valet.aws-private-key", AWS_SECRET_KEY);
		properties.put("widen.valet.import-file", fileName);
		properties.put("widen.valet.aws-route53-zone-id", zoneId);
		properties.put("widen.valet.aws-name-server", "route53rrs");
		properties.put("widen.valet.default-ttl", "600");
		properties.put("widen.valet.clean-zone-by-deleteing-all-records", "true");

		return properties;
	}

}
