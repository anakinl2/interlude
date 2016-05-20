package l2d.game.clientpackets;

import java.util.logging.Logger;

import l2d.game.model.L2Player;
import l2d.game.serverpackets.ExListPartyMatchingWaitingRoom;

/** 
 * Format: dddd
 */
public class RequestListPartyMatchingWaitingRoom extends L2GameClientPacket
{
	protected static final Logger _log = Logger.getLogger(RequestListPartyMatchingWaitingRoom.class.getName());

	@SuppressWarnings("unused")
	private int _minLevel, _maxLevel, _page, _unk;

	@Override
	protected void readImpl()
	{
		_page = readD();
		_minLevel = readD();
		_maxLevel = readD();
		_unk = readD(); // всегда 1?
	}

	@Override
	protected void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		activeChar.sendPacket(new ExListPartyMatchingWaitingRoom(activeChar, _minLevel, _maxLevel, _page));
	}
}