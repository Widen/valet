package com.widen.valet.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.widen.valet.RecordType;
import com.widen.valet.Route53Driver;
import com.widen.valet.Zone;
import com.widen.valet.ZoneResource;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class NameQueryByRoute53APIService implements NameQueryService
{
	private final Route53Driver driver;

	private final Map<RecordKey, ZoneResource> zoneMap = new HashMap<RecordKey, ZoneResource>();

	public NameQueryByRoute53APIService(Route53Driver driver, Zone zone)
	{
		this.driver = driver;

		loadZone(zone);
	}

	private void loadZone(Zone zone)
	{
		List<ZoneResource> resources = driver.listZoneRecords(zone);

		for (ZoneResource resource : resources)
		{
			zoneMap.put(new RecordKey(resource.getName(), resource.getRecordType()), resource);
		}
	}

	@Override
	public LookupRecord lookup(String name, RecordType type)
	{
		ZoneResource resource = zoneMap.get(new RecordKey(name, type));

		if (resource == null)
		{
			return LookupRecord.NON_EXISTENT_RECORD;
		}

		return new LookupRecord(name, resource.getResourceRecords(), resource.getTtl(), true);
	}

	private final class RecordKey
	{
		final String name;
		final RecordType type;

		public RecordKey(String name, RecordType type)
		{
			this.name = name;
			this.type = type;
		}

		@Override
		public boolean equals(Object o)
		{
			RecordKey rhs = (RecordKey) o;
			return new EqualsBuilder().append(name, rhs.name).append(type, rhs.type).isEquals();
		}

		@Override
		public int hashCode()
		{
			return new HashCodeBuilder().append(name).append(type).toHashCode();
		}
	}
}
