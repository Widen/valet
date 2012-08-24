package com.widen.valet.internal;

import java.util.Map;

/**
 * The pilot executes "in-flight" requests -- typically HTTP communication to the AWS Route53 endpoint.
 *
 * There is also a pilot for running unit tests the simply returns canned XML responses.
 */
public interface Route53Pilot
{

	String executeResourceRecordSetGet(String zone, Map<String, String> query);

	String executeResourceRecordSetsPost(String zone, String payload);

	String executeHostedZoneGet();

	String executeHostedZoneGet(String zone);

	String executeHostedZonePost(String payload);

	String executeHostedZoneDelete(String zone);

	String executeChangeInfoGet(String changeId);

}
