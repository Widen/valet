package com.widen.valet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegrationTest
{
	private final Logger log = LoggerFactory.getLogger(IntegrationTest.class);

	private Route53Driver driver;

	public static void main(String[] args)
	{
		new IntegrationTest().run();
	}

	private void run()
	{
		setupDriver();

		final Zone zone = createZone(String.format("valet-test-zone-%s.net.", System.currentTimeMillis()));

		addResources(zone);

		addRoundRobinResources(zone);

		deleteZone(zone);
	}

	private void addRoundRobinResources(Zone zone)
	{
		List<ZoneUpdateAction> actions = new ArrayList<ZoneUpdateAction>();

		actions.add(new ZoneUpdateAction.Builder().withData("wwwrr", zone, RecordType.A, "127.0.0.1").withRoundRobinData("set1", 1).buildCreate());
		actions.add(new ZoneUpdateAction.Builder().withData("wwwrr", zone, RecordType.A, "127.0.0.2").withRoundRobinData("set2", 2).buildCreate());
		actions.add(new ZoneUpdateAction.Builder().withData("wwwrr", zone, RecordType.A, "127.0.0.3").withRoundRobinData("set3", 3).buildCreate());

		final ZoneChangeStatus status = driver.updateZone(zone, "add rr resources", actions);

		driver.waitForSync(status);
	}

	private void addResources(Zone zone)
	{
		List<ZoneUpdateAction> actions = new ArrayList<ZoneUpdateAction>();

		actions.add(new ZoneUpdateAction.Builder().withData("www", zone, RecordType.A, "127.0.0.1").buildCreate());

		actions.add(new ZoneUpdateAction.Builder().withData(zone.getName(), RecordType.MX, Arrays.asList("10 mail10.example.com", "20 mail20.example.com", "30 mail30.example.com")).buildCreate());

		final ZoneChangeStatus status = driver.updateZone(zone, "add resources", actions);

		driver.waitForSync(status);
	}

	private Zone createZone(String domain)
	{
		final ZoneChangeStatus status = driver.createZone(domain, "Valet integration test on " + new Date());

		driver.waitForSync(status);

		return driver.zoneDetails(status.getZoneId());
	}

	private void deleteZone(Zone zone)
	{
		List<RecordType> keepTypes = Arrays.asList(RecordType.SOA, RecordType.NS);

		final List<ZoneResource> resources = driver.listZoneRecords(zone);

		List<ZoneUpdateAction> deleteActions = new ArrayList<ZoneUpdateAction>();

		for (ZoneResource resource : resources)
		{
			if (!keepTypes.contains(resource.getRecordType()))
			{
				deleteActions.add(new ZoneUpdateAction.Builder().fromZoneResource(resource).buildDelete());
			}
		}

		ZoneChangeStatus status = driver.updateZone(zone, "Delete all resources for zone deletion", deleteActions);

		driver.waitForSync(status);

		status = driver.deleteZone(zone, "Delete integration test zone");

		driver.waitForSync(status);
	}

	private void setupDriver()
	{
		final Properties properties = new Properties();

		try
		{
			properties.load(getClass().getResourceAsStream("IntegrationTest.properties"));
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}

		driver = new Route53Driver(properties.getProperty("aws-access-key"), properties.getProperty("aws-secret-key"));
	}

}
