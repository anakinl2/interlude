package l2d.game.clientpackets;

import l2d.game.ai.CtrlEvent;
import l2d.game.model.L2Player;
import l2d.util.Location;

/**
 * [C] 36 CannotMoveAnymore
 * @author Felixx
 */
public class CannotMoveAnymore extends L2GameClientPacket
{
	private Location _loc = new Location(0, 0, 0);

	/**
	 * packet type id 0x47
	 *
	 * sample
	 *
	 * 36
	 * a8 4f 02 00 // x
	 * 17 85 01 00 // y
	 * a7 00 00 00 // z
	 * 98 90 00 00 // heading
	 *
	 * format:		cdddd
	 * @param decrypt
	 */
	@Override
	public void readImpl()
	{
		_loc.x = readD();
		_loc.y = readD();
		_loc.z = readD();
		_loc.h = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();

		if(activeChar == null)
			return;

		activeChar.getAI().notifyEvent(CtrlEvent.EVT_ARRIVED_BLOCKED, _loc, null);
	}
}