package com.widen.valet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.widen.valet.internal.Route53Pilot;
import com.widen.valet.internal.Route53PilotMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class ValetTest
{
	private final Logger log = LoggerFactory.getLogger(ValetTest.class);

	private Route53Pilot getPilot()
	{
		return new Route53PilotMock();
	}

	@Test
	public void testZoneInfo()
	{
		Zone zone = new Route53Driver(getPilot()).zoneDetails("Z18VDLRK3QY0Z4");

		assertEquals("Z18VDLRK3QY0Z4", zone.getExistentZoneId());

		assertEquals("6a1dd118-8019-478b-9d06-6ae7158d82e1", zone.getCallerReference());

		assertEquals("uriahacarpenter.com.", zone.getName());

		assertEquals("Create zone uriahacarpenter.com", zone.getComment());

		assertEquals(Arrays.asList("ns-1333.awsdns-38.org", "ns-1940.awsdns-50.co.uk", "ns-240.awsdns-30.com", "ns-604.awsdns-11.net"), zone.getNameServers());
	}

	@Test
	public void testListZones()
	{
		List<Zone> zones = new Route53Driver(getPilot()).listZones();

		assertEquals(zones.size(), 2);

		assertEquals("Z18VDLRK3QY0Z4", zones.get(0).getZoneId());

		assertEquals("ZNI0MR764YEWF", zones.get(1).getZoneId());
	}

	@Test
	public void testAddRecords()
	{
		List<ZoneUpdateAction> updates = new ArrayList<ZoneUpdateAction>();

		updates.add(new ZoneUpdateAction.Builder().withData("uriahacarpenter.com", RecordType.A, Arrays.asList("127.0.0.1")).buildDeleteAction());
		updates.add(new ZoneUpdateAction.Builder().withData("uriahacarpenter.com", RecordType.A, Arrays.asList("127.0.0.1")).buildCreateAction());

		Zone zone = new Zone("Z1234", "uriahcarpenter.com.", "", "", Collections.<String>emptyList());

		ZoneChangeStatus status = new Route53Driver(getPilot()).updateZone(zone, "update comment", updates);

		assertEquals("Z1234", status.getZoneId());
		assertEquals("C34NBUXNVUM7LE", status.getChangeId());
		assertEquals("PENDING", status.getStatus().toString());
		assertEquals("Wed Feb 23 08:35:42 CST 2011", status.getSubmitDate().toString());
	}

	@Test
	public void testChangeStatus()
	{
		ZoneChangeStatus oldStatus = new ZoneChangeStatus("C34NBUXNVUM7LE", "change-id", ZoneChangeStatus.Status.PENDING, new Date());

		ZoneChangeStatus newStatus = new Route53Driver(getPilot()).queryChangeStatus(oldStatus);

		assertEquals("C34NBUXNVUM7LE", newStatus.getChangeId());
		assertEquals("INSYNC", newStatus.getStatus().toString());
		assertEquals("Wed Feb 23 08:35:42 CST 2011", newStatus.getSubmitDate().toString());
		assertEquals("C34NBUXNVUM7LE", newStatus.getZoneId());
		assertEquals(true, newStatus.isInSync());
	}

	@Test
	public void testListRecords()
	{
		Zone zone = new Zone("Z1234", "uriahcarpenter.com.", "", "", Collections.<String>emptyList());

		List<ZoneResource> resources = new Route53Driver(getPilot()).listZoneRecords(zone);

		assertEquals(4, resources.size());

	}

}
