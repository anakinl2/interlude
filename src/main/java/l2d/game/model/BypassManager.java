package l2d.game.model;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import l2d.util.GArray;
import l2d.util.Strings;

public class BypassManager
{
	private static final Logger _log = Logger.getLogger(BypassManager.class.getName());

	private static final Pattern p = Pattern.compile("\"(bypass +-h +)(.+?)\"");

	public static enum BypassType
	{
		ENCODED,
		ENCODED_BBS,
		SIMPLE,
		SIMPLE_BBS,
		SIMPLE_DIRECT
	}

	public static BypassType getBypassType(String bypass)
	{
		switch(bypass.charAt(0))
		{
			case '0':
				return BypassType.ENCODED;
			case '1':
				return BypassType.ENCODED_BBS;
			default:
				if(Strings.matches(bypass, "^(_mrsl|_clbbs|_mm|_diary|friendlist|friendmail|manor_menu_select|_match).*", Pattern.DOTALL))
					return BypassType.SIMPLE;
				if(Strings.matches(bypass, "^(bbs_|_bbs|_mail|_friend|_block).*", Pattern.DOTALL))
					return BypassType.SIMPLE_BBS;
				return BypassType.SIMPLE_DIRECT;
		}
	}

	public static String encode(String html, GArray<EncodedBypass> bypassStorage, boolean bbs)
	{
		Matcher m = p.matcher(html);
		StringBuffer sb = new StringBuffer();

		while(m.find())
		{
			String bypass = m.group(2);
			String code = bypass;
			String params = "";
			int i = bypass.indexOf(" $");
			boolean use_params = i >= 0;
			if(use_params)
			{
				code = bypass.substring(0, i);
				params = bypass.substring(i).replace("$", "\\$");
			}

			if(bbs)
				m.appendReplacement(sb, "\"bypass -h 1" + Integer.toHexString(bypassStorage.size()) + params + "\"");
			else
				m.appendReplacement(sb, "\"bypass -h 0" + Integer.toHexString(bypassStorage.size()) + params + "\"");

			bypassStorage.add(new EncodedBypass(code, use_params));
		}

		m.appendTail(sb);
		return sb.toString();
	}

	public static DecodedBypass decode(String bypass, GArray<EncodedBypass> bypassStorage, boolean bbs, L2Player player)
	{
		synchronized (bypassStorage)
		{
			String[] bypass_parsed = bypass.split(" ");
			int idx = Integer.parseInt(bypass_parsed[0].substring(1), 16);
			EncodedBypass bp;
			DecodedBypass result = null;

			try
			{
				bp = bypassStorage.get(idx);
			}
			catch(Exception e)
			{
				bp = null;
			}

			if(bp == null)
				_log.warning("Can't decode bypass (bypass not exists): " + (bbs ? "[bbs] " : "") + bypass + " / Player: " + player.getName());
			else if(bypass_parsed.length > 1 && !bp.useParams)
				_log.warning("bypass with wrong params" + (bbs ? " [bbs]: " : ": ") + bp.code + " / Player: " + player.getName());
			else
			{
				result = new DecodedBypass(bp.code, bbs);
				for(int i = 1; i < bypass_parsed.length; i++)
					result.bypass += " " + bypass_parsed[i];
				result.trim();
			}

			return result;
		}
	}

	public static class EncodedBypass
	{
		public final String code;
		public final boolean useParams;

		public EncodedBypass(String _code, boolean _useParams)
		{
			code = _code;
			useParams = _useParams;
		}
	}

	public static class DecodedBypass
	{
		public String bypass;
		public boolean bbs;

		public DecodedBypass(String _bypass, boolean _bbs)
		{
			bypass = _bypass;
			bbs = _bbs;
		}

		public DecodedBypass trim()
		{
			bypass = bypass.trim();
			return this;
		}
	}
}