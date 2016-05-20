package l2d.game.communitybbs.Manager;

import java.util.List;

import javolution.util.FastList;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.ShowBoard;

public abstract class BaseBBSManager
{
	public abstract void parsecmd(String command, L2Player activeChar);

	public abstract void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2Player activeChar);

	public static void separateAndSend(String html, L2Player activeChar)
	{
		ShowBoard.separateAndSend(html, activeChar);
	}

	protected void send1001(String html, L2Player activeChar)
	{
		if(html.length() < 8180)
			activeChar.sendPacket(new ShowBoard(html, "1001", activeChar));
	}

	protected void send1002(L2Player acha)
	{
		send1002(acha, " ", " ", "0");
	}

	protected void send1002(L2Player activeChar, String string, String string2, String string3)
	{
		List<String> _arg = new FastList<String>();
		_arg.add("0");
		_arg.add("0");
		_arg.add("0");
		_arg.add("0");
		_arg.add("0");
		_arg.add("0");
		_arg.add(activeChar.getName());
		_arg.add(Integer.toString(activeChar.getObjectId()));
		_arg.add(activeChar.getAccountName());
		_arg.add("9");
		_arg.add(string2);
		_arg.add(string2);
		_arg.add(string);
		_arg.add(string3);
		_arg.add(string3);
		_arg.add("0");
		_arg.add("0");
		activeChar.sendPacket(new ShowBoard(_arg));
	}
}