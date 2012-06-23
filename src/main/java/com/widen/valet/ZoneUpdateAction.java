package com.widen.valet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

	private final String aliasZoneId;

	private final String aliasDnsName;

	private ZoneUpdateAction(String action, String name, RecordType type, int ttl, String setIdentifier, int weight, List<String> resourceRecords, String aliasZoneId, String aliasDnsName)
	{
		this.action = action;
		this.name = name;
		this.type = type;
		this.ttl = ttl;
		this.setIdentifier = setIdentifier;
		this.weight = weight;
		this.aliasZoneId = aliasZoneId;
		this.aliasDnsName = aliasDnsName;

		Collections.sort(resourceRecords);
		this.resourceRecords = Collections.unmodifiableList(resourceRecords);
	}

	public static class Builder
	{
		private String name;
		private RecordType type;
		private int ttl = 600;
		private List<String> resourceRecords = new ArrayList<String>();
		private String setIdentifier = null;
		private int weight = 0;
		private String aliasZoneId;
		private String aliasDnsName;

		public ZoneUpdateAction buildCreateAction()
		{
			return build("CREATE");
		}

		public ZoneUpdateAction buildDeleteAction()
		{
			return build("DELETE");
		}

		private ZoneUpdateAction build(String action)
		{
			return new ZoneUpdateAction(action, name, type, ttl, setIdentifier, weight, resourceRecords, aliasZoneId, aliasDnsName);
		}

		/**
		 * Create zone resource using 'simple' name (e.g. 'www') -- without the zone name pre-appended
		 *
		 * @param resourceName
		 * @param zone
		 * @param type
		 * @param resourceValues
		 * @return
		 */
		public Builder withData(String resourceName, Zone zone, RecordType type, String... resourceValues)
        {
            String hostName = "";

            if (resourceName != null && !resourceName.equals(""))
            {
                hostName = resourceName + ".";
            }

            return withData(String.format("%s%s", hostName, zone.getName()), type, Arrays.asList(resourceValues));
        }

		public Builder withData(String name, RecordType type, Collection<String> resourceValues)
		{
			this.type = type;
			this.name = name;
			this.resourceRecords.addAll(resourceValues);
			return this;
		}

		public Builder withData(String name, RecordType type)
		{
			this.name = name;
			this.type = type;
			return this;
		}

		public Builder withTtl(int ttl)
		{
			this.ttl = ttl;
			return this;
		}

		public Builder addResourceRecords(List<String> resourceRecords)
		{
			this.resourceRecords.addAll(resourceRecords);
			return this;
		}

		public Builder addResourceRecords(String... resourceRecords)
		{
			this.resourceRecords.addAll(Arrays.asList(resourceRecords));
			return this;
		}

		public Builder addRoundRobinData(String setIdentifier, int weight)
		{
			this.setIdentifier = setIdentifier;
			this.weight = weight;
			return this;
		}

		/**
		 * Add Alias resource record.
		 *
		 * <p>Details in the Route53 <a href="http://docs.amazonwebservices.com/Route53/latest/DeveloperGuide/index.html?CreatingAliasRRSets.html">documentation</a></a>
		 * @param zoneId
		 * @param dnsName
		 * @return
		 */
		public Builder addAliasData(String zoneId, String dnsName)
		{
			this.aliasZoneId = zoneId;
			this.aliasDnsName = dnsName;
			return this;
		}

		public Builder fromZoneResource(ZoneResource resource)
		{
			name = resource.getName();
			type = resource.getRecordType();
			ttl = resource.getTtl();
			resourceRecords.addAll(resource.getResourceRecords());
			setIdentifier = resource.getWrrSetIdentifier();
			weight = resource.getWrrWeight();
			aliasZoneId = resource.getAliasZoneId();
			aliasDnsName = resource.getAliasDnsName();
			return this;
		}
	}

	/**
	 * Append additional resource records to an existing update action.
	 *
	 * @return
	 * 		New ZoneUpdateAction instance with merged resource records.
	 */
	public static ZoneUpdateAction mergeResources(ZoneUpdateAction action, List<String> resources)
	{
		List<String> mergedResources = new ArrayList<String>();
		mergedResources.addAll(action.resourceRecords);
		mergedResources.addAll(resources);

		return new ZoneUpdateAction(action.action, action.name, action.type, action.ttl, action.getSetIdentifier(), action.getWeight(), mergedResources, action.aliasZoneId, action.aliasDnsName);
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

		if (StringUtils.isNotBlank(aliasZoneId))
		{
			xml.addTag("AliasTarget");
			xml.addTag("HostedZoneId").addText(aliasZoneId);
			xml.addTag("DNSName").addText(aliasDnsName);
		}
		else
		{
			xml.addTag("TTL").addText(String.valueOf(ttl));

			xml.addTag("ResourceRecords");

			for (String resource : resourceRecords)
			{
				String value = resource;

				xml.addTag("ResourceRecord").addTag("Value").addText(value);

				xml.gotoParent();
			}
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
		return new EqualsBuilder().append(action, rhs.action).append(name, rhs.name).append(type, rhs.type).append(setIdentifier, rhs.setIdentifier).isEquals();
	}

	@Override
	public int hashCode()
	{
		return new HashCodeBuilder().append(action).append(name).append(type).append(setIdentifier).toHashCode();
	}

	@Override
	public int compareTo(ZoneUpdateAction rhs)
	{
		return new CompareToBuilder().append(action, rhs.action).append(name, rhs.name).append(type, rhs.type).append(setIdentifier, rhs.setIdentifier).toComparison();
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

	public String getSetIdentifier()
	{
		return setIdentifier;
	}

	public int getWeight()
	{
		return weight;
	}

	public List<String> getResourceRecords()
	{
		return resourceRecords;
	}
}
