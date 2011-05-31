package com.widen.valet;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class Zone implements Comparable<Zone>
{
	public static final Zone NON_EXISTENT_ZONE = new Zone("Z_NON_EXISTENT", "non-existent-zone.com.", "", "", Collections.<String>emptyList());

	private final String zoneId;

	private final String name;

	private final String callerReference;

	private final String comment;

	private final List<String> nameServers;

	Zone(String zoneId, String name, String callerReference, String comment, List<String> nameServers)
	{
		this.zoneId = zoneId;
		this.name = name;
		this.callerReference = callerReference;
		this.comment = comment;
		this.nameServers = Collections.unmodifiableList(nameServers);
	}

	@Override
	public String toString()
	{
		return ToStringBuilder.reflectionToString(this);
	}

	public String getExistentZoneId()
	{
		if (this == NON_EXISTENT_ZONE)
		{
			throw new UnsupportedOperationException("Cannot perform action on non-existent zone.");
		}

		return zoneId;
	}

	@Override
	public int compareTo(Zone rhs)
	{
		return new CompareToBuilder().append(name, rhs.name).append(zoneId, rhs.zoneId).toComparison();
	}

	public String getZoneId()
	{
		return zoneId;
	}

	public String getName()
	{
		return name;
	}

	public String getCallerReference()
	{
		return callerReference;
	}

	public String getComment()
	{
		return comment;
	}

	public List<String> getNameServers()
	{
		return nameServers;
	}

}
