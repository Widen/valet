package com.widen.valet.internal;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Test;

public class DateUtilTest
{

	@Test
	public void testWithMillis()
	{
		Date parsed = DateUtil.fromZulu("2011-02-23T14:35:42.005Z");

		Assert.assertEquals(setupCalendar().getTime(), parsed);
	}

	@Test
	public void testWithSeconds()
	{
		Date parsed = DateUtil.fromZulu("2011-02-23T14:35:42Z");

		Calendar calendar = setupCalendar();
		calendar.clear(Calendar.MILLISECOND);

		Assert.assertEquals(calendar.getTime(), parsed);
	}

	@Test(expected = RuntimeException.class)
	public void testInvalidDate()
	{
		DateUtil.fromZulu("2011-02-23T14:35:42.005");
	}

	private Calendar setupCalendar()
	{
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Zulu"));
		calendar.clear();
		calendar.set(2011, 1, 23, 14, 35, 42);
		calendar.set(Calendar.MILLISECOND, 5);
		return calendar;
	}
}
