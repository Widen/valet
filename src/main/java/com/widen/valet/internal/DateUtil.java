package com.widen.valet.internal;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import static java.util.TimeZone.getTimeZone;

/**
 * Utilities specific to Route53 date handling.
 */
public class DateUtil
{
	/**
	 * Parse ISO-8601 string into java.util.Date
	 *
	 * <p/>This method tries multiple date formats as the Route53 API does not always include milliseconds.
	 *
	 * @throws RuntimeException
	 * 		if no date formats match input string
	 */
	public static final Date fromZulu(String input)
	{
		String[] formats = { "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'" };

		for (String format : formats)
		{
			try
			{
				SimpleDateFormat sdf = new SimpleDateFormat(format);
				sdf.setLenient(false);
				sdf.setTimeZone(getTimeZone("Zulu"));
				return sdf.parse(input);
			}
			catch (ParseException e)
			{
				//try next
			}
		}

		throw new RuntimeException(String.format("'%s' is not parsable from format %s", input, StringUtils.join(formats, " or ")));
	}

}
