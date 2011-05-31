package com.widen.valet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.mycila.xmltool.XMLTag;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Creates XML "Change" element for Route53 Zone modifications
 */
public class ZoneUpdateAction implements Comparable<ZoneUpdateAction>
{
	private final String action;

	private final String name;

	private final RecordType type;

	private final int ttl;

	private final String setIdentifier;

	private final int weight;

	private final List<String> resourceRecords;

	private ZoneUpdateAction(String action, String name, RecordType type, int ttl, List<String> resourceRecords, String setIdentifier, int weight)
	{
		this.action = action;
		this.name = name;
		this.type = type;
		this.ttl = ttl;
		this.setIdentifier = setIdentifier;
		this.weight = weight;

		Collections.sort(resourceRecords);
		this.resourceRecords = Collections.unmodifiableList(resourceRecords);
	}

	private ZoneUpdateAction(String action, String name, RecordType type, int ttl, List<String> resourceRecords)
	{
		this(action, name, type, ttl, resourceRecords, null, 0);
	}

	/**
	 * Append additional resource records to update action.
	 *
	 * @return
	 * 		New ZoneUpdateAction instance with merged resource records.
	 */
	public static ZoneUpdateAction mergeResources(ZoneUpdateAction action, List<String> resources)
	{
		List<String> mergedResources = new ArrayList<String>();
		mergedResources.addAll(action.resourceRecords);
		mergedResources.addAll(resources);

		return new ZoneUpdateAction(action.action, action.name, action.type, action.ttl, mergedResources);
	}

	/**
	 * Construct a "CREATE" action. If the resources are pre-existing a "DELETE" action must be used.
	 *
	 * @return
	 * 		An action that the Route53Driver can execute
	 */
	public static ZoneUpdateAction createAction(String name, RecordType type, int ttl, String... resource)
	{
		return new ZoneUpdateAction("CREATE", name, type, ttl, Arrays.asList(resource));
	}

	/**
	 * Construct a "CREATE" action that includes Round Robin attributes. If the resources are pre-existing a "DELETE" action must be used.
	 *
	 * @return
	 * 		An action that the Route53Driver can execute
	 */
	public static ZoneUpdateAction createRoundRobinAction(String name, RecordType type, int ttl, String setIdentifier, int weight, String... resource)
	{
		return new ZoneUpdateAction("CREATE", name, type, ttl, Arrays.asList(resource), setIdentifier, weight);
	}

	/**
	 * Construct a "DELETE" action.
	 *
	 * @return
	 * 		An action that the Route53Driver can execute
	 */
	public static ZoneUpdateAction deleteAction(String name, RecordType type, int ttl, String... resource)
	{
		return new ZoneUpdateAction("DELETE", name, type, ttl, Arrays.asList(resource));
	}

	/**
	 * Construct a "DELETE" action.
	 *
	 * @return An action that the Route53Driver can execute
	 */
	public static ZoneUpdateAction deleteRoundRobinAction(String name, RecordType type, int ttl, String setIdentifier, int weight, String... resource)
	{
		return new ZoneUpdateAction("DELETE", name, type, ttl, Arrays.asList(resource), setIdentifier, weight);
	}

	/**
	 * Construct a "DELETE" action from an existing ZoneResource
	 *
	 * @return
	 * 		An action that the Route53Driver can execute
	 */
	public static ZoneUpdateAction deleteAction(ZoneResource resource)
	{
		return deleteRoundRobinAction(resource.getName(), resource.getRecordType(), resource.getTtl(), resource.getSetIdentifier(), resource.getWeight(), resource.getResourceRecords().toArray(new String[] {}));
	}

	void addChangeTag(XMLTag xml)
	{
		xml.addTag("Change")
				.addTag("Action").addText(action)
				.addTag("ResourceRecordSet")
				.addTag("Name").addText(name)
				.addTag("Type").addText(type.name());

		if (StringUtils.isNotBlank(setIdentifier))
		{
			xml.addTag("SetIdentifier").addText(setIdentifier);
			xml.addTag("Weight").addText(Integer.toString(weight));
		}

		xml.addTag("TTL").addText(String.valueOf(ttl));

		xml.addTag("ResourceRecords");

		for (String resource : resourceRecords)
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

	public List<String> getResourceRecords()
	{
		return resourceRecords;
	}
}
