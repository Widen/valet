package com.widen.valet.internal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

public class Route53PilotImpl implements Route53Pilot
{
	private static final String ROUTE_53_ENDPOINT = "https://route53.amazonaws.com/2011-05-05/";

	private static final String HOSTED_ZONE_ENDPOINT = ROUTE_53_ENDPOINT + "hostedzone";

	private final String awsAccessKey;

	private final String awsSecret;

	public Route53PilotImpl(String awsAccessKey, String awsSecret)
	{
		Defense.notBlank(awsAccessKey, "awsAccessKey");
		Defense.notBlank(awsSecret, "awsSecret");

		this.awsAccessKey = awsAccessKey;
		this.awsSecret = awsSecret;
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

	public String executeResourceRecordSetGet(String zone, String query)
	{
		HttpGet get = new HttpGet(recordSetUri(zone, query));

		return execute(get);
	}

	public String executeResourceRecordSetsPost(String zone, String payload)
	{
		HttpPost post = new HttpPost(recordSetUri(zone, ""));

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

	private String recordSetUri(String zone, String query)
	{
		String q = "";

		if (StringUtils.isNotBlank(query))
		{
			q = "?" + query;
		}

		return String.format("%s/%s/rrset%s", HOSTED_ZONE_ENDPOINT, zone, q);
	}

	private String execute(HttpRequestBase request)
	{
		String date = new SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss zzz", java.util.Locale.US).format(new Date());

		HttpClient httpclient = new DefaultHttpClient();

		String signature = sign(date, awsSecret);

		request.addHeader("Date", date);

		request.addHeader("X-Amzn-Authorization", String.format("AWS3-HTTPS AWSAccessKeyId=%s,Algorithm=HmacSHA1,Signature=%s", awsAccessKey, signature));

		request.addHeader("Content-Type", "text/plain");

		String content = "<root><nonset/></root>";

		try
		{
			HttpResponse response = httpclient.execute(request);

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
