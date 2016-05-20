package l2d.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class StrTable
{

	private final HashMap<Integer, HashMap<String, String>> rows;
	private final LinkedHashMap<String, Integer> columns;
	private final ArrayList<String> titles;

	public StrTable(String title)
	{
		rows = new HashMap<Integer, HashMap<String, String>>();
		columns = new LinkedHashMap<String, Integer>();
		titles = new ArrayList<String>();
		if(title != null)
			titles.add(title);
	}

	public StrTable()
	{
		this(null);
	}

	public StrTable set(int rowIndex, String colName, Object value)
	{
		String val;
		val = value.toString();
		HashMap<String, String> row;
		synchronized (rows)
		{
			if(rows.containsKey(Integer.valueOf(rowIndex)))
				row = rows.get(Integer.valueOf(rowIndex));
			else
			{
				row = new HashMap<String, String>();
				rows.put(Integer.valueOf(rowIndex), row);
			}
		}
		synchronized (row)
		{
			row.put(colName, val);
		}

		try
		{
			int columnSize;
			if(!columns.containsKey(colName))
				columnSize = Math.max(colName.length(), val.length());
			else if(columns.get(colName).intValue() >= (columnSize = val.length()))
				return this;
			columns.put(colName, Integer.valueOf(columnSize));
		}
		catch(Exception e)
		{}

		return this;
	}

	public StrTable addTitle(String s)
	{
		synchronized (rows)
		{
			titles.add(s);
		}
		return this;
	}

	public static String pad_right(String s, int sz)
	{
		String result = s;
		if((sz -= s.length()) > 0)
			result = new StringBuilder().append(result).append(repeat(" ", sz)).toString();
		return result;
	}

	public static String pad_left(String s, int sz)
	{
		String result = s;
		if((sz -= s.length()) > 0)
			result = new StringBuilder().append(repeat(" ", sz)).append(result).toString();
		return result;
	}

	public static String pad_center(String s, int sz)
	{
		String result = s;
		int i;
		while((i = sz - result.length()) > 0)
			if(i == 1)
				result = new StringBuilder().append(result).append(" ").toString();
			else
				result = new StringBuilder().append(" ").append(result).append(" ").toString();
		return result;
	}

	public static String repeat(String s, int sz)
	{
		String result = "";
		for(int i = 0; i < sz; i++)
			result = new StringBuilder().append(result).append(s).toString();

		return result;
	}

	@Override
	public String toString()
	{
		String result[];

		//try {
		if(columns.isEmpty())
			return "";

		String header = "|";
		String line = "|";
		for(Iterator<String> i$ = columns.keySet().iterator(); i$.hasNext();)
		{
			String c = i$.next();
			header = new StringBuilder().append(header).append(pad_center(c, columns.get(c).intValue() + 2)).append("|").toString();
			line = new StringBuilder().append(line).append(repeat("-", columns.get(c).intValue() + 2)).append("|").toString();
		}

		result = new String[rows.size() + 4 + (titles.isEmpty() ? 0 : titles.size() + 1)];
		int i = 0;
		if(!titles.isEmpty())
		{
			result[i++] = new StringBuilder().append(" ").append(repeat("-", header.length() - 2)).append(" ").toString();
			for(Iterator<String> i$ = titles.iterator(); i$.hasNext();)
			{
				String title = i$.next();
				result[i++] = new StringBuilder().append("| ").append(pad_right(title, header.length() - 3)).append("|").toString();
			}

		}
		result[i++] = result[result.length - 1] = new StringBuilder().append(" ").append(repeat("-", header.length() - 2)).append(" ").toString();
		result[i++] = header;
		result[i++] = line;
		for(Iterator<HashMap<String, String>> i$ = rows.values().iterator(); i$.hasNext();)
		{
			HashMap<?, ?> row = i$.next();
			line = "|";
			for(Iterator<String> it = columns.keySet().iterator(); it.hasNext();)
			{
				String c = it.next();
				line = new StringBuilder().append(line).append(pad_center(row.containsKey(c) ? (String) row.get(c) : "-", columns.get(c).intValue() + 2)).append("|").toString();
			}

			result[i++] = line;
		}
		//        }
		//        catch(Exception e) {}

		return Strings.joinStrings("\r\n", result);
	}

	public String toL2Html()
	{
		return toString().replaceAll("\r\n", "<br1>");
	}
}
