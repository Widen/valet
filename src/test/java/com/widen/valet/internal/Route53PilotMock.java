package com.widen.valet.internal;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Returns static values for Route53 requests.
 */
public class Route53PilotMock implements Route53Pilot
{
	public String executeHostedZonePost(String payload)
	{
		try
		{
			return IOUtils.toString(getClass().getResourceAsStream("create-zone-post-result.xml"));
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public String executeHostedZoneDelete(String zone)
	{
		return "<delete xml>";
	}

	public String executeChangeInfoGet(String changeId)
	{
		try
		{
			return IOUtils.toString(getClass().getResourceAsStream("change-info-status-response.xml"));
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public String executeResourceRecordSetGet(String zone, Map<String, String> query)
	{
		try
		{
			return IOUtils.toString(getClass().getResourceAsStream("list-zone-resource-records.xml"));
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public String executeResourceRecordSetsPost(String zone, String payload)
	{
		try
		{
			return IOUtils.toString(getClass().getResourceAsStream("update-resource-records-response.xml"));
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public String executeHostedZoneGet()
	{
		return executeHostedZoneGet(null);
	}

	public String executeHostedZoneGet(String zone)
	{
		String response;

		if (StringUtils.isBlank(zone))
		{
			response = "list-hosted-zones-response.xml";
		}
		else
		{
			response = "hosted-zone-details-response.xml";
		}

		try
		{
			return IOUtils.toString(getClass().getResourceAsStream(response));
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
