package com.widen.valet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.widen.valet.internal.Defense;
import com.widen.valet.util.ListUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegrationTest
{
	private final Logger log = LoggerFactory.getLogger(IntegrationTest.class);

	private Properties testProperties;

	private Route53Driver driver;

	public static void main(String[] args) throws IOException
    {
		//new IntegrationTest().runTest();

        new IntegrationTest().runDeleteZone("valet-test-zone-1345827982884.net.");
	}

    private void runDeleteZone(String domain) throws IOException
    {
        setupForTest();
        deleteZone(driver.zoneDetailsForDomain(domain));
    }

	private void runTest() throws IOException
    {
		setupForTest();

		final Zone zone = createZone(String.format("valet-test-zone-%s.net.", System.currentTimeMillis()));

        try
        {
            addTxtResources(zone);

            addResources(zone);

            addRoundRobinResources(zone);

            addMassNumberOfRecords(zone);

            //addAliasResources(zone);

            readZone(zone);
        }
        catch (Exception e)
        {
            log.error("Error running integration test", e);
        }
        finally
        {
            deleteZone(zone);
        }
	}

    private void addMassNumberOfRecords(Zone zone)
    {
        int zonesToCreate = 501;

        List<ZoneUpdateAction> actions = new ArrayList<ZoneUpdateAction>();

        for (int i = 0; i < zonesToCreate; i++)
        {
            actions.add(new ZoneUpdateAction.Builder().withData("hostname-" + i, zone, RecordType.A, "127.0.0.1").buildCreateAction());
        }

        List<List<ZoneUpdateAction>> split = ListUtil.split(actions, 100);

        for (List<ZoneUpdateAction> list : split)
        {
            final ZoneChangeStatus status = driver.updateZone(zone, "add a mass number of resources", list);

            driver.waitForSync(status);
        }
    }

    private void readZone(Zone zone)
    {
        List<ZoneResource> resources = driver.listZoneRecords(zone);

        log.info("Listing {} zone resources", resources.size());

        for (ZoneResource resource : resources)
        {
            log.info("{}", resource);
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

	private void addAliasResources(Zone zone)
	{
        String elbZoneId = testProperties.getProperty("elb-hosted-zone-id");
        String elbDnsName = testProperties.getProperty("elb-dns-name");

        Defense.notBlank(elbZoneId, "elb-hosted-zone-id");
        Defense.notBlank(elbDnsName, "elb-dns-name");

        List<ZoneUpdateAction> actions = new ArrayList<ZoneUpdateAction>();

        actions.add(new ZoneUpdateAction.Builder().withData("elb-alias", zone, RecordType.A).addAliasData(elbZoneId, elbDnsName).buildCreateAction());

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

        actions.add(new ZoneUpdateAction.Builder().withData("", zone, RecordType.A, "127.0.0.1").buildCreateAction());

        actions.add(new ZoneUpdateAction.Builder().withData("*", zone, RecordType.A, "127.0.0.1").buildCreateAction());

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

        List<List<ZoneUpdateAction>> split = ListUtil.split(deleteActions, 100);

        for (List<ZoneUpdateAction> actions : split)
        {
            ZoneChangeStatus status = driver.updateZone(zone, "Delete all resources for zone deletion", actions);
            driver.waitForSync(status);
        }

        ZoneChangeStatus status = driver.deleteZone(zone, "Delete integration test zone");

        driver.waitForSync(status);
    }

	private void setupForTest() throws IOException
    {
		testProperties = new Properties();

        InputStream stream = getClass().getResourceAsStream("IntegrationTest.properties");

        if (stream == null)
        {
            throw new FileNotFoundException("You must create a properties file named src/test/resources/com/widen/valet/IntegrationTest.properties with the keys 'aws-access-key' and 'aws-secret-key'");
        }

        testProperties.load(stream);


        String accessKey = testProperties.getProperty("aws-access-key");
        String secretKey = testProperties.getProperty("aws-secret-key");

        Defense.notBlank(accessKey, "aws-access-key");
        Defense.notBlank(secretKey, "aws-secret-key");

        driver = new Route53Driver(accessKey, secretKey);
	}

}
