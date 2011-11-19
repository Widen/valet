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

	private final String wrrSetIdentifier;

	private final int wrrWeight;

	private final String aliasZoneId;

	private final String aliasDnsName;

	private final List<String> resourceRecords;

	/**
	 * Internal usage for 'normal' resources
	 */
	ZoneResource(String name, RecordType recordType, int ttl, List<String> resourceRecords)
	{
		this(name, recordType, ttl, resourceRecords, null, 0, null, null);
	}

	/**
	 * Internal usage for 'weighted round-robin' and 'alias' resources
	 */
	ZoneResource(String name, RecordType recordType, int ttl, List<String> resourceRecords, String wrrSetIdentifier, int wrrWeight, String aliasZoneId, String aliasDnsName)
	{
		this.name = name;
		this.recordType = recordType;
		this.ttl = ttl;
		this.resourceRecords = Collections.unmodifiableList(resourceRecords);
		this.wrrSetIdentifier = wrrSetIdentifier;
		this.wrrWeight = wrrWeight;
		this.aliasZoneId = aliasZoneId;
		this.aliasDnsName = aliasDnsName;
	}

	public String getFirstResource()
	{
		return resourceRecords.iterator().next();
	}

	public final ZoneUpdateAction createAction()
	{
		return new ZoneUpdateAction.Builder().withData(name, recordType, resourceRecords).withTtl(ttl).addRoundRobinData(wrrSetIdentifier, wrrWeight).addAliasData(aliasZoneId, aliasDnsName).buildCreateAction();
	}

	public final ZoneUpdateAction deleteAction()
	{
		return new ZoneUpdateAction.Builder().withData(name, recordType, resourceRecords).withTtl(ttl).addRoundRobinData(wrrSetIdentifier, wrrWeight).addAliasData(aliasZoneId, aliasDnsName).buildDeleteAction();
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

	public String getWrrSetIdentifier()
	{
		return wrrSetIdentifier;
	}

	public int getWrrWeight()
	{
		return wrrWeight;
	}

	public String getAliasZoneId()
	{
		return aliasZoneId;
	}

	public String getAliasDnsName()
	{
		return aliasDnsName;
	}
}
