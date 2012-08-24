/*
 * Copyright 2010 Widen Enterprises, Inc.
 * Madison, Wisconsin USA -- www.widen.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.widen.valet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mycila.xmltool.XMLDoc;
import com.mycila.xmltool.XMLDocumentException;
import com.mycila.xmltool.XMLTag;
import com.widen.valet.internal.DateUtil;
import com.widen.valet.internal.Defense;
import com.widen.valet.internal.Route53Pilot;
import com.widen.valet.internal.Route53PilotImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Primary interface to interact with Route53.  See {@link com.widen.examples.ValetExample} for usage example.
 */
public class Route53Driver
{
	private Logger log = LoggerFactory.getLogger(Route53Driver.class);

	private static final String ROUTE53_XML_NAMESPACE = "https://route53.amazonaws.com/doc/2011-05-05/";

	private final Route53Pilot pilot;

	/**
	 * Construct driver using AWS user/secret keys.
	 * @param awsUserKey
	 * @param awsSecretKey
	 */
	public Route53Driver(String awsUserKey, String awsSecretKey)
	{
		this.pilot = new Route53PilotImpl(awsUserKey, awsSecretKey);
	}

	/**
	 * Construct driver using AWS user/secret keys with custom {@link HttpClient} instance.
	 * @param awsUserKey
	 * @param awsSecretKey
	 * @param httpClient
	 */
	public Route53Driver(String awsUserKey, String awsSecretKey, HttpClient httpClient)
	{
		this.pilot = new Route53PilotImpl(awsUserKey, awsSecretKey, httpClient);
	}

	/**
	 * Use specific pilot for driver.
	 *
	 * @param pilot
	 */
	public Route53Driver(Route53Pilot pilot)
	{
		this.pilot = pilot;
	}

	/**
	 * Submit ordered list of commands to Route53.
	 * @param zone
	 * @param comment
	 * @param actions
	 * @return
	 */
	public ZoneChangeStatus updateZone(final Zone zone, final String comment, ZoneUpdateAction... actions)
	{
		return updateZone(zone, comment, Arrays.asList(actions));
	}

	/**
	 * Submit ordered list of commands to Route53.
	 *
	 * @param zone
	 * @param comment
	 * @param updateActions
	 * @return
	 * @thorws
	 *      ValetException if Route53 rejects the transaction block
	 */
	public ZoneChangeStatus updateZone(final Zone zone, final String comment, final List<ZoneUpdateAction> updateActions)
	{
		if (updateActions.isEmpty())
		{
			return new ZoneChangeStatus(zone.getExistentZoneId(), "no-change-submitted", ZoneChangeStatus.Status.INSYNC, new Date());
		}

		if (updateActions.size() > 100)
		{
			throw new ValetException("Route53 will only process 100 actions per request. Use com.widen.util.ListUtil:split() to make multiple requests.");
		}

		String commentXml = StringUtils.defaultIfEmpty(comment, String.format("Modify %s records.", updateActions.size()));

		XMLTag xml = XMLDoc.newDocument(false)
				.addDefaultNamespace(ROUTE53_XML_NAMESPACE)
				.addRoot("ChangeResourceRecordSetsRequest")
				.addTag("ChangeBatch")
				.addTag("Comment").addText(commentXml)
				.addTag("Changes");

		String ns = xml.getPefix(ROUTE53_XML_NAMESPACE);

		for (ZoneUpdateAction updateAction : updateActions)
		{
			updateAction.addChangeTag(xml);
			xml.gotoTag("//%s:Changes", ns);
		}

		String payload = xml.toString();

		log.trace("Update Zone Post Payload:\n{}", payload);

		String responseText = pilot.executeResourceRecordSetsPost(zone.getExistentZoneId(), payload);

		XMLTag result = XMLDoc.from(responseText, true);

		log.trace("Update Zone Response:\n{}", result);

		if (result.hasTag("Error"))
		{
			throw parseErrorResponse(result);
		}

		return parseChangeResourceRecordSetsResponse(zone.getExistentZoneId(), result);
	}

	/**
	 * Use old ZoneChangeStatus to query for current status of a ZoneChange
	 * @param oldStatus
	 * @return
	 */
	public ZoneChangeStatus queryChangeStatus(ZoneChangeStatus oldStatus)
	{
		String response = pilot.executeChangeInfoGet(oldStatus.getChangeId());

		XMLTag xml = XMLDoc.from(response, true);

		return parseChangeResourceRecordSetsResponse(oldStatus.getZoneId(), xml);
	}

	private ZoneChangeStatus parseChangeResourceRecordSetsResponse(String zoneId, XMLTag xml)
	{
		XMLTag changeInfo;

		try
		{
			changeInfo = xml.gotoChild("ChangeInfo");
		}
		catch (XMLDocumentException xmlde)
		{
			// No 'ChangeInfo' element
			return new ZoneChangeStatus(zoneId, null, null, null);
		}

		String changeId = StringUtils.substringAfter(changeInfo.getText("Id"), "/change/");

		ZoneChangeStatus.Status status = ZoneChangeStatus.Status.valueOf(changeInfo.getText("Status"));

		Date date = DateUtil.fromZulu(changeInfo.getText("SubmittedAt"));

		return new ZoneChangeStatus(zoneId, changeId, status, date);
	}

	private ValetException parseErrorResponse(XMLTag xml)
	{
		XMLTag error = xml.gotoChild("Error");

		String type = error.getText("Type");
		String code = error.getText("Code");
		String message = error.getText("Message");

		return new ValetException(String.format("%s: %s", code, message));
	}

	/**
	 * Block until ZoneChangeStatus return INSYNC from Route53
	 * @param oldStatus
	 */
	public void waitForSync(ZoneChangeStatus oldStatus)
	{
		boolean inSync = oldStatus.isInSync();

		while (!inSync)
		{
			ZoneChangeStatus current = queryChangeStatus(oldStatus);

			if (current.isInSync())
			{
				inSync = true;

				log.debug("Zone ID {} is now INSYNC", current.getZoneId());
			}
			else
			{
				try
				{
					log.debug("Waiting for INSYNC...");
					Thread.sleep(2000);
				}
				catch (InterruptedException e)
				{
				}
			}
		}
	}

	/**
	 * Query for all Resources in Zone.
	 * May make multiple Route53 calls to retrieve all the resources.
	 * @param zone
	 * @return
	 *      List of Zone Resources
	 */
	public List<ZoneResource> listZoneRecords(final Zone zone)
	{
		Set<ZoneResource> zoneResources = new HashSet<ZoneResource>();

		boolean readMore = true;

		Map<String, String> query = new HashMap<String, String>();

		while (readMore)
		{
			String result = pilot.executeResourceRecordSetGet(zone.getExistentZoneId(), query);

			XMLTag xml = XMLDoc.from(result, true);

			log.trace("List Zone Records:\n{}", xml);

			if (xml.hasTag("Error"))
			{
				throw parseErrorResponse(xml);
			}

			if (xml.getText("//IsTruncated").equals("false"))
			{
				readMore = false;
			}

			String lastName = "";

			for (XMLTag record : xml.getChilds("//ResourceRecordSet"))
			{
				String name = record.getText("Name");
				String type = record.getText("Type");

				String weight = null;
				String setIdentifier = null;
				if (record.hasTag("SetIdentifier"))
				{
					setIdentifier = record.getText("SetIdentifier");
					weight = record.getText("Weight");
				}

				String ttl = null;
				String aliasZoneId = null;
				String aliasDnsName = null;
				List<String> values = new ArrayList<String>();

				if (record.hasTag("AliasTarget"))
				{
					aliasZoneId = record.getText("AliasTarget/HostedZoneId");
					aliasDnsName = record.getText("AliasTarget/DNSName");
				}
				else
				{
					ttl = record.getText("TTL");

					for (XMLTag resource : record.getChilds("ResourceRecords/ResourceRecord"))
					{
						values.add(resource.getText("Value"));
					}

					Collections.sort(values);
				}

				zoneResources.add((new ZoneResource(name, RecordType.valueOf(type), parseIntWithDefault(ttl, 0), values, setIdentifier, parseIntWithDefault(weight, 0), aliasZoneId, aliasDnsName)));

				lastName = name;
			}

            query.put("name", lastName);
		}

		List<ZoneResource> list = new ArrayList<ZoneResource>();

		list.addAll(zoneResources);

		return list;
	}

	private int parseIntWithDefault(String s, int defaultValue)
	{
		if (StringUtils.isEmpty(s))
		{
			return defaultValue;
		}

		return Integer.parseInt(s);
	}

	/**
	 * Query for all Zones assigned to AWS Access Key.
	 *
	 * <p>Zones returned from this method do <b>NOT</b> include name servers.
	 * Reload zone using zoneDetail(Zone z) if name server addresses are needed.
	 *
	 * @return
	 */
	public List<Zone> listZones()
	{
		String result = pilot.executeHostedZoneGet("");

		XMLTag xml = XMLDoc.from(result, true);

		if (xml.hasTag("Error"))
		{
			throw parseErrorResponse(xml);
		}

		ArrayList<Zone> zones = new ArrayList<Zone>();

		for (XMLTag tag : xml.getChilds("//HostedZone"))
		{
			zones.add(buildZone(tag));
		}

		return zones;
	}

	/**
	 * Query for named Route53 zone ID (e.g. Z123ABC456DEF)
	 *
	 * @param zoneId
	 * @return
	 */
	public Zone zoneDetails(final String zoneId)
	{
		Zone zone = new Zone(zoneId, "", "", "", Collections.<String>emptyList());

		return zoneDetails(zone);
	}

	/**
	 * Query for named domain in Route53 (e.g. "foodomain.com.")
	 * @param domain
	 * @return
	 */
	public Zone zoneDetailsForDomain(final String domain)
	{
		List<Zone> zones = listZones();

		for (Zone zone : zones)
		{
			if (StringUtils.equalsIgnoreCase(zone.getName(), domain))
			{
				return zone;
			}
		}

		return Zone.NON_EXISTENT_ZONE;
	}

	/**
	 * Load detailed information for named Zone.
	 *
	 * @param zone
	 * @return
	 */
	public Zone zoneDetails(final Zone zone)
	{
		String result = pilot.executeHostedZoneGet(zone.getExistentZoneId());

		XMLTag xml = XMLDoc.from(result, true);

		if (xml.hasTag("Error"))
		{
			throw parseErrorResponse(xml);
		}

		return buildZone(xml.gotoChild("HostedZone"));
	}

	private Zone buildZone(final XMLTag xml)
	{
		String id = StringUtils.substringAfter(xml.getText("Id"), "/hostedzone/");

		String name = xml.getText("Name");

		String callerReference = xml.getText("CallerReference");

		String comment = "";

		try
		{
			comment = xml.getText("Config/Comment");
		}
		catch (XMLDocumentException e)
		{
		}

		List<String> nameServers = new ArrayList<String>();

		for (XMLTag nsTag : xml.getChilds("//NameServer"))
		{
			nameServers.add(nsTag.getTextOrCDATA());
		}
		
		return new Zone(id, name, callerReference, comment, nameServers);
	}

	/**
	 * Create a Zone in Route53
	 *
	 * @param domainName
	 * @return
	 * @throws IllegalArgumentException
	 *      if domainName is null, blank, starts/ends with period.
	 */
	public ZoneChangeStatus createZone(final String domainName, final String comment)
	{
		checkDomainName(domainName);

		ensureDomainNameNotAlreadyCreated(domainName);

		String payload = XMLDoc.newDocument(false)
				.addDefaultNamespace(ROUTE53_XML_NAMESPACE)
				.addRoot("CreateHostedZoneRequest")
				.addTag("Name").addText(domainName)
				.addTag("CallerReference").addText(UUID.randomUUID().toString())
				.addTag("HostedZoneConfig")
				.addTag("Comment").addText(comment)
				.toString();

		log.debug("Create Zone Post Payload:\n{}", payload);

		String result = pilot.executeHostedZonePost(payload);

		XMLTag xml = XMLDoc.from(result, true);

		log.debug("Create Zone Response:\n{}", xml);

		if (xml.hasTag("Error"))
		{
			throw parseErrorResponse(xml);
		}

		xml.gotoChild("HostedZone");

		Zone zone = buildZone(xml);

		xml.gotoRoot();

		return parseChangeResourceRecordSetsResponse(zone.getExistentZoneId(), xml);
	}

	/**
	 * Delete a Route53 hosted zone. Route53 requires that all resource records (except NS and SOA) be already be removed from the zone.
	 */
	public ZoneChangeStatus deleteZone(final Zone zone, final String comment)
	{
		log.trace("Delete ZoneId {} ({})", zone.getZoneId(), comment);

		final String result = pilot.executeHostedZoneDelete(zone.getZoneId());

		final XMLTag xml = XMLDoc.from(result, true);

		log.debug("Delete Zone Response:\n{}", xml);

		if (xml.hasTag("Error"))
		{
			throw parseErrorResponse(xml);
		}

		return parseChangeResourceRecordSetsResponse(zone.getZoneId(), xml);
	}

	private void ensureDomainNameNotAlreadyCreated(String domainName)
	{
		List<Zone> zones = listZones();

		for (Zone zone : zones)
		{
			if (zone.getName().equals(domainName))
			{
				throw new IllegalArgumentException("Domain name '" + domainName + "' is already hosted by Route53.");
			}
		}
	}

	/**
	 * Query for existence of named domain in Zones available to AWS Access Key
	 */
	public boolean zoneDomainExists(final String domainName)
	{
		checkDomainName(domainName);

		List<Zone> zones = listZones();

		for (Zone zone : zones)
		{
			if (StringUtils.equalsIgnoreCase(domainName, zone.getName()))
			{
				return true;
			}
		}

		return false;
	}

	private void checkDomainName(String name)
	{
		Defense.notBlank(name, "name");

		if (StringUtils.startsWith(name, ".") || !StringUtils.endsWith(name, "."))
		{
			throw new IllegalArgumentException("Domain name '" + name + "' is invalid. Name can not start with a period and must end with a period.");
		}
	}

}
