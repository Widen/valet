package com.widen.valet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.mycila.xmltool.XMLTag;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class ZoneUpdateAction implements Comparable<ZoneUpdateAction>
{
	private final String action;

	private final String name;

	private final RecordType type;

	private final int ttl;

	private final List<String> resourceRecord;

	private ZoneUpdateAction(String action, String name, RecordType type, int ttl, List<String> resourceRecords)
	{
		this.action = action;
		this.name = name;
		this.type = type;
		this.ttl = ttl;

		Collections.sort(resourceRecords);
		this.resourceRecord = Collections.unmodifiableList(resourceRecords);
	}

	public static ZoneUpdateAction mergeResources(ZoneUpdateAction action, List<String> resources)
	{
		List<String> mergedResources = new ArrayList<String>();
		mergedResources.addAll(action.resourceRecord);
		mergedResources.addAll(resources);

		return new ZoneUpdateAction(action.action, action.name, action.type, action.ttl, mergedResources);
	}

	public static ZoneUpdateAction createAction(String name, RecordType type, int ttl, String... resource)
	{
		return new ZoneUpdateAction("CREATE", name, type, ttl, Arrays.asList(resource));
	}

	public static ZoneUpdateAction deleteAction(String name, RecordType type, int ttl, String... resource)
	{
		return new ZoneUpdateAction("DELETE", name, type, ttl, Arrays.asList(resource));
	}

	void addChangeTag(XMLTag xml)
	{
		xml.addTag("Change")
				.addTag("Action").addText(action)
				.addTag("ResourceRecordSet")
				.addTag("Name").addText(name)
				.addTag("Type").addText(type.name())
				.addTag("TTL").addText(String.valueOf(ttl))
				.addTag("ResourceRecords");

		for (String resource : resourceRecord)
		{
			String value = resource;

			xml.addTag("ResourceRecord").addTag("Value").addText(value);

			xml.gotoParent();
		}
	}

	@Override
	public String toString()
	{
		return ToStringBuilder.reflectionToString(this);
	}

	/**
	 * Actions are equal if action, name, and type are the same.
	 *
	 * @param obj
	 * @return
	 */
	@Override
	public boolean equals(Object obj)
	{
		ZoneUpdateAction rhs = (ZoneUpdateAction) obj;
		return new EqualsBuilder().append(action, rhs.action).append(name, rhs.name).append(type, rhs.type).isEquals();
	}

	@Override
	public int hashCode()
	{
		return new HashCodeBuilder().append(action).append(name).append(type).toHashCode();
	}

	@Override
	public int compareTo(ZoneUpdateAction rhs)
	{
		return new CompareToBuilder().append(action, rhs.action).append(name, rhs.name).append(type, rhs.type).toComparison();
	}

	public String getAction()
	{
		return action;
	}

	public String getName()
	{
		return name;
	}

	public RecordType getType()
	{
		return type;
	}

	public int getTtl()
	{
		return ttl;
	}

	public List<String> getResourceRecord()
	{
		return resourceRecord;
	}
}
