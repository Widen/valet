package com.widen.examples;

import com.widen.valet.Route53Driver;
import com.widen.valet.Zone;
import com.widen.valet.ZoneResource;
import com.widen.valet.internal.Route53PilotImpl;

public class ListZone
{
	private static final String AWS_ACCESS_KEY = "";

	private static final String AWS_SECRET_KEY = "";

	private Route53Driver driver = new Route53Driver(new Route53PilotImpl(AWS_ACCESS_KEY, AWS_SECRET_KEY));

	public static void main(String[] args)
	{
		new ListZone().run();
	}

	private void run()
	{
		Zone zone = driver.zoneDetailsForDomain("mydomain.com.");

		System.out.println(zone);

		for (ZoneResource zr : driver.listZoneRecords(zone))
		{
			System.out.println(zr);
		}
	}

}
