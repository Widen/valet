package com.widen.valet;

import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;

public class ZoneChangeStatus
{
	public enum Status
	{
		PENDING,
		INSYNC;
	}

	private final String zoneId;

	private final String changeId;

	private final Date submitDate;

	private final Status status;

	ZoneChangeStatus(String zoneId, String changeId, Status status, Date submitDate)
	{
		this.zoneId = zoneId;
		this.changeId = changeId;
		this.status = status;
		this.submitDate = submitDate;
	}

	public boolean isPending()
	{
		return Status.PENDING.equals(status);
	}

	public boolean isInSync()
	{
		return Status.INSYNC.equals(status);
	}

	@Override
	public String toString()
	{
		return ToStringBuilder.reflectionToString(this);
	}

	public String getZoneId()
	{
		return zoneId;
	}

	public String getChangeId()
	{
		return changeId;
	}

	public Date getSubmitDate()
	{
		return submitDate;
	}

	public Status getStatus()
	{
		return status;
	}
}
