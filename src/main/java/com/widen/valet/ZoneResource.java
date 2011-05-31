package com.widen.valet;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class ZoneResource
{
	private final String name;

	private final RecordType recordType;

	private final int ttl;

	private final String setIdentifier;

	private final int weight;

	private final List<String> resourceRecords;

	ZoneResource(String name, RecordType recordType, int ttl, List<String> resourceRecords)
	{
		this(name, recordType, ttl, resourceRecords, null, 0);
	}

	ZoneResource(String name, RecordType recordType, int ttl, List<String> resourceRecords, String setIdentifier, int weight)
	{
		this.name = name;
		this.recordType = recordType;
		this.ttl = ttl;
		this.resourceRecords = Collections.unmodifiableList(resourceRecords);
		this.setIdentifier = setIdentifier;
		this.weight = weight;
	}

	public String getFirstResource()
	{
		return resourceRecords.iterator().next();
	}

	public final ZoneUpdateAction createAction()
	{
		return ZoneUpdateAction.createAction(name, recordType, ttl, resourceRecords.toArray(new String[0]));
	}

	public final ZoneUpdateAction deleteAction()
	{
		return ZoneUpdateAction.deleteAction(name, recordType, ttl, resourceRecords.toArray(new String[0]));
	}

	@Override
	public String toString()
	{
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public boolean equals(Object obj)
	{
		ZoneResource rhs = (ZoneResource) obj;
		return new EqualsBuilder().append(name, rhs.name).append(recordType, rhs.recordType).append(ttl, rhs.ttl).append(resourceRecords, rhs.resourceRecords).isEquals();
	}

	@Override
	public int hashCode()
	{
		return new HashCodeBuilder().append(name).append(recordType).append(ttl).append(resourceRecords).toHashCode();
	}

	public String getName()
	{
		return name;
	}

	public RecordType getRecordType()
	{
		return recordType;
	}

	public int getTtl()
	{
		return ttl;
	}

	public List<String> getResourceRecords()
	{
		return resourceRecords;
	}

	public String getSetIdentifier()
	{
		return setIdentifier;
	}

	public int getWeight()
	{
		return weight;
	}
}
