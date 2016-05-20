package l2d.game.serverpackets;

import java.util.List;
import java.util.logging.Logger;

import l2d.game.model.L2Player;

public class ShowBoard extends L2GameServerPacket
{
	private static Logger _log = Logger.getLogger(ShowBoard.class.getName());
	private String _htmlCode;
	private String _id;
	private List<String> _arg;

	public static void separateAndSend(String html, L2Player activeChar)
	{
		if(html.length() < 8180)
		{
			activeChar.sendPacket(new ShowBoard(html, "101", activeChar));
			activeChar.sendPacket(new ShowBoard(null, "102", activeChar));
			activeChar.sendPacket(new ShowBoard(null, "103", activeChar));
		}
		else if(html.length() < 8180 * 2)
		{
			activeChar.sendPacket(new ShowBoard(html.substring(0, 8180), "101", activeChar));
			activeChar.sendPacket(new ShowBoard(html.substring(8180, html.length()), "102", activeChar));
			activeChar.sendPacket(new ShowBoard(null, "103", activeChar));
		}
		else if(html.length() < 8180 * 3)
		{
			activeChar.sendPacket(new ShowBoard(html.substring(0, 8180), "101", activeChar));
			activeChar.sendPacket(new ShowBoard(html.substring(8180, 8180 * 2), "102", activeChar));
			activeChar.sendPacket(new ShowBoard(html.substring(8180 * 2, html.length()), "103", activeChar));
		}
	}

	public ShowBoard(String htmlCode, String id, L2Player activeChar)
	{
		if(htmlCode != null && htmlCode.length() > 8192) // html code must not exceed 8192 bytes
		{
			_log.warning("Html '" + htmlCode + "' is too long! this will crash the client!");
			_htmlCode = "<html><body>Html was too long</body></html>";
			return;
		}
		_id = id;

		if(htmlCode != null)
		{
			if(id.equalsIgnoreCase("101"))
				activeChar.cleanBypasses(true);
			_htmlCode = activeChar.encodeBypasses(htmlCode, true);
		}
		else
			_htmlCode = null;
	}

	public ShowBoard(List<String> arg)
	{
		_id = "1002";
		_htmlCode = null;
		_arg = arg;
	}

	public ShowBoard(String html)
	{
		_htmlCode = html;
		_id = "";
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x6e);
		writeC(0x01); //c4 1 to show community 00 to hide
		writeS("bypass _bbshome"); // top
		writeS("bypass _bbsgetfav"); // favorite
		writeS("bypass _bbsloc"); // region
		writeS("bypass _bbsclan"); // clan
		writeS("bypass _bbsmemo"); // memo
		writeS("bypass _bbsmail"); // mail
		writeS("bypass _bbsfriends"); // friends
		writeS("bypass bbs_add_fav"); // add fav.	
		/*String str = _id + "\u0008";
		if(_id.equals("1002"))
			for(String arg : _arg)
				str += arg + " \u0008";
		else if(_htmlCode != null)
			str += _htmlCode;*/
		writeS(_htmlCode);
	}
}