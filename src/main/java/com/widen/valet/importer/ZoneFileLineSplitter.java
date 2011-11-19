package com.widen.valet.importer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class ZoneFileLineSplitter
{
	public static void main(String[] args)
	{
		System.out.println(splitLine("@        A       10.0.0.0"));
		System.out.println(splitLine("foo   MX    10  mail-server.foo.com."));
		System.out.println(splitLine("foo   TXT    ( \"Windows DNS puts parens around txt values\""));
		System.out.println(splitLine("      A  10.0.0.1"));
	}

	public static List<String> splitLine(String l)
	{
		StringBuilder[] builders = new StringBuilder[] { new StringBuilder(), new StringBuilder(), new StringBuilder() } ;

		int segment = 0;

		boolean seeking = false;

		for (int i = 0; i < l.length(); i++)
		{
			char next = l.charAt(i);

			if (Character.isWhitespace(next) && segment < 2)
			{
				if (builders[segment].toString().length() > 0 && builders[segment + 1].toString().length() == 0)
				{
					seeking = true;
					continue;
				}
			}
			else
			{
				if (seeking)
				{
					seeking = false;
					segment++;
				}

				builders[segment].append(next);
			}
		}

		String chunk1 = cleanup(builders[0]);
		String chunk2 = cleanup(builders[1]);
		String chunk3 = cleanup(builders[2]);

		//'apex zone' record is blank, but when parsing it's the last chunk that's blank
//		if (StringUtils.isBlank(chunk3))
//		{
//			chunk3 = chunk2;
//			chunk2 = chunk1;
//			chunk1 = "";
//		}

		List<String> list = new ArrayList<String>();
		list.add(chunk1);
		list.add(chunk2);
		list.add(chunk3);

		return list;
	}

	private static String cleanup(StringBuilder sb)
	{
		String s = sb.toString();
		s = s.trim();
		s = s.replace("\t", " ");
		s = StringUtils.stripStart(s, "(");
		s = StringUtils.stripEnd(s, ")");
		s = s.trim();
		return s;
	}
}
