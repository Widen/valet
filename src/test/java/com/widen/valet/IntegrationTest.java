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

	private Properties testProperteis;

	private Route53Driver driver;

	public static void main(String[] args)
	{
		new IntegrationTest().runTest();
	}

	private void runTest()
	{
		loadProperties();

		driver = new Route53Driver(testProperteis.getProperty("aws-access-key"), testProperteis.getProperty("aws-secret-key"));

		final Zone zone = createZone(String.format("valet-test-zone-%s.net.", System.currentTimeMillis()));

		try
        {
            addTxtResources(zone);

            addResources(zone);

            addRoundRobinResources(zone);

            addAliasResources(zone);
        }
		finally
		{
			deleteZone(zone);
		}
	}

    private void addTxtResources(Zone zone)
    {
        List<ZoneUpdateAction> actions = new ArrayList<ZoneUpdateAction>();

        actions.add(new ZoneUpdateAction.Builder().withData("foohost", zone, RecordType.TXT, "\"foo text\"").buildCreateAction());
        actions.add(new ZoneUpdateAction.Builder().withData("", zone, RecordType.TXT, "\"bar text\"").buildCreateAction());

        final ZoneChangeStatus status = driver.updateZone(zone, "add txt resources", actions);

        driver.waitForSync(status);
    }

    private void runDeleteZone(String domain)
	{
		loadProperties();

		deleteZone(driver.zoneDetailsForDomain(domain));
	}

	private void addAliasResources(Zone zone)
	{
		List<ZoneUpdateAction> actions = new ArrayList<ZoneUpdateAction>();

		actions.add(new ZoneUpdateAction.Builder().withData(zone.getName(), RecordType.A).addAliasData(testProperteis.getProperty("elb-hosted-zone-id"), testProperteis.getProperty("elb-dns-name")).buildCreateAction());

		final ZoneChangeStatus status = driver.updateZone(zone, "add alias resources", actions);

		driver.waitForSync(status);
	}

	private void addRoundRobinResources(Zone zone)
	{
		List<ZoneUpdateAction> actions = new ArrayList<ZoneUpdateAction>();

		actions.add(new ZoneUpdateAction.Builder().withData("wwwrr", zone, RecordType.A, "127.0.0.1").addRoundRobinData("set1", 1).buildCreateAction());
		actions.add(new ZoneUpdateAction.Builder().withData("wwwrr", zone, RecordType.A, "127.0.0.2").addRoundRobinData("set2", 2).buildCreateAction());
		actions.add(new ZoneUpdateAction.Builder().withData("wwwrr", zone, RecordType.A, "127.0.0.3").addRoundRobinData("set3", 3).buildCreateAction());

		final ZoneChangeStatus status = driver.updateZone(zone, "add rr resources", actions);

		driver.waitForSync(status);
	}

	private void addResources(Zone zone)
	{
		List<ZoneUpdateAction> actions = new ArrayList<ZoneUpdateAction>();

		actions.add(new ZoneUpdateAction.Builder().withData("www", zone, RecordType.A, "127.0.0.1").buildCreateAction());

		actions.add(new ZoneUpdateAction.Builder().withData(zone.getName(), RecordType.MX, Arrays.asList("10 mail10.example.com", "20 mail20.example.com", "30 mail30.example.com")).buildCreateAction());

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
		log.info("Deleting integration test zone {} ({})", zone.getName(), zone.getZoneId());

		List<RecordType> keepTypes = Arrays.asList(RecordType.SOA, RecordType.NS);

		final List<ZoneResource> resources = driver.listZoneRecords(zone);

		List<ZoneUpdateAction> deleteActions = new ArrayList<ZoneUpdateAction>();

		for (ZoneResource resource : resources)
		{
			if (!keepTypes.contains(resource.getRecordType()))
			{
				deleteActions.add(new ZoneUpdateAction.Builder().fromZoneResource(resource).buildDeleteAction());
			}
		}

		ZoneChangeStatus status = driver.updateZone(zone, "Delete all resources for zone deletion", deleteActions);

		driver.waitForSync(status);

		status = driver.deleteZone(zone, "Delete integration test zone");

		driver.waitForSync(status);
	}

	private void loadProperties()
	{
		testProperteis = new Properties();

		try
		{
			testProperteis.load(getClass().getResourceAsStream("IntegrationTest.properties"));
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

}
