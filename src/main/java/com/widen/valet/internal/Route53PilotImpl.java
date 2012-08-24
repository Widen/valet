package com.widen.valet.internal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * Route53 Pilot is responsible for communicating with the AWS Route53 REST endpoint.
 *
 * At minimum, the pilot requires the AWS access and secret key.
 *
 * <p>You may use an alternate constructor to inject the {@link HttpClient} instance to use.
 * This is useful if your environment requires proxy configuration to access the Route53 endpoint.
 */
public class Route53PilotImpl implements Route53Pilot
{
	private static final String ROUTE_53_ENDPOINT = "https://route53.amazonaws.com/2011-05-05/";

	private static final String HOSTED_ZONE_ENDPOINT = ROUTE_53_ENDPOINT + "hostedzone";

	private final String awsAccessKey;

	private final String awsSecret;

	private final HttpClient httpClient;

	public Route53PilotImpl(String awsAccessKey, String awsSecret, HttpClient httpClient)
	{
		Defense.notBlank(awsAccessKey, "awsAccessKey");
		Defense.notBlank(awsSecret, "awsSecret");

		this.awsAccessKey = awsAccessKey;
		this.awsSecret = awsSecret;
		this.httpClient = httpClient;
	}

	public Route53PilotImpl(String awsAccessKey, String awsSecret)
	{
		this(awsAccessKey, awsSecret, new DefaultHttpClient());
	}

	public String executeHostedZoneGet()
	{
		return executeHostedZoneGet(null);
	}

	public String executeHostedZoneGet(String zone)
	{
		String uri = HOSTED_ZONE_ENDPOINT;

		if (StringUtils.isNotBlank(zone))
		{
			uri = String.format("%s/%s", uri, zone);
		}

		HttpGet httpget = new HttpGet(uri);

		return execute(httpget);
	}

	public String executeHostedZonePost(String payload)
	{
		HttpPost post = new HttpPost(HOSTED_ZONE_ENDPOINT);

		try
		{
			post.setEntity(new StringEntity(payload));
		}
		catch (UnsupportedEncodingException e)
		{
			throw new RuntimeException(e);
		}

		return execute(post);
	}

	@Override
	public String executeHostedZoneDelete(String zone)
	{
		Defense.notBlank(zone, "zone");

		String uri = String.format("%s/%s", HOSTED_ZONE_ENDPOINT, zone);

		HttpDelete delete = new HttpDelete(uri);

		return execute(delete);
	}

	public String executeChangeInfoGet(String changeId)
	{
		HttpGet get = new HttpGet(ROUTE_53_ENDPOINT + "change/" + changeId);

		return execute(get);
	}

	public String executeResourceRecordSetGet(String zone, Map<String, String> query)
	{
		HttpGet get = new HttpGet(recordSetUri(zone, query));

		return execute(get);
	}

	public String executeResourceRecordSetsPost(String zone, String payload)
	{
		HttpPost post = new HttpPost(recordSetUri(zone, Collections.EMPTY_MAP));

		try
		{
			post.setEntity(new StringEntity(payload));
		}
		catch (UnsupportedEncodingException e)
		{
			throw new RuntimeException(e);
		}

		return execute(post);
	}

	private String recordSetUri(String zone, Map<String, String> query)
	{
		StringBuilder q = new StringBuilder();

		if (query != null && !query.isEmpty())
        {
            q.append("?");

            for (Map.Entry<String, String> entry : query.entrySet())
            {
                q.append(String.format("%s=%s", encodeQueryParam(entry.getKey()), encodeQueryParam(entry.getValue())));
            }
        }

		return String.format("%s/%s/rrset%s", HOSTED_ZONE_ENDPOINT, zone, q);
	}

    private String encodeQueryParam(String in)
    {
        try
        {
            return URLEncoder.encode(in, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("UTF-8 encoding not supported");
        }
    }

	private String execute(HttpRequestBase request)
	{
		String date = new SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss zzz", java.util.Locale.US).format(new Date());

		String signature = sign(date, awsSecret);

		request.addHeader("Date", date);

		request.addHeader("X-Amzn-Authorization", String.format("AWS3-HTTPS AWSAccessKeyId=%s,Algorithm=HmacSHA1,Signature=%s", awsAccessKey, signature));

		request.addHeader("Content-Type", "text/plain");

		String content = "<root><nonset/></root>";

		try
		{
			HttpResponse response = httpClient.execute(request);

			HttpEntity entity = response.getEntity();

			if (entity != null)
			{
				content = EntityUtils.toString(entity);
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}

		return content;
	}

	/**
	 * Computes RFC 2104-compliant HMAC signature.
	 */
	private static String sign(String data, String key)
	{
		try
		{
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(new SecretKeySpec(key.getBytes(), "HmacSHA1"));
			return Base64.encodeBytes(mac.doFinal(data.getBytes("UTF-8")));
		}
		catch (Exception e)
		{
			throw new RuntimeException(new SignatureException("Failed to generate signature: " + e.getMessage(), e));
		}
	}

}
