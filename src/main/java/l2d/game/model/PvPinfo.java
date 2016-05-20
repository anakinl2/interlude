package l2d.game.model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PvPinfo
{
	private String _time;
	private String _opponent;
	private String _result;
	
	public PvPinfo(long time, String opponent, String result)
	{
		_time = convertDateToString(time);
		_opponent = opponent;
		_result = result;
	}
	
	public PvPinfo(String time, String oponent, String result)
	{
		_time = time;
		_opponent = oponent;
		_result = result;
	}

	public static String convertDateToString(long time)
	{
		Date dt = new Date(time);
		SimpleDateFormat s = new SimpleDateFormat("MM/dd HH:mm:ss");
		String stringDate = s.format(dt);
		return stringDate;
	}

	public String getTime()
	{
		return _time;
	}

	public String getOponent()
	{
		return _opponent;
	}
	
	public String getResult()
	{
		return _result;
	}

	public String getResultHtml()
	{
		if(_result.contains("WON"))
			return "<font color=\"6692af\">" + _result + "</font>";
		else if(_result.contains("LOST"))
			return "<font color=\"c64f4f\">" + _result + "</font>";
		else
			return "<font color=\"a9a9a2\">" + _result + "</font>";
	}
}
