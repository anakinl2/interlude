package l2d.game.clientpackets;

import l2d.game.model.L2Player;
import l2d.game.model.L2World;

/**
 * [C] AB SnoopQuit
 * 
 * @author Felixx
 */
public class SnoopQuit extends L2GameClientPacket
{
	private int _snoopID;

	/**
	 * format: cd
	 */
	@Override
	public void readImpl()
	{
		_snoopID = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = (L2Player) L2World.findObject(_snoopID);

		if(player == null)
			return;

		L2Player activeChar = getClient().getActiveChar();

		if(activeChar == null)
			return;

		player.removeSnooper(activeChar);
		activeChar.removeSnooped(player);
	}
}