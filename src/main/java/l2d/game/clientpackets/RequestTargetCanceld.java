package l2d.game.clientpackets;

import l2d.game.model.L2Player;
import l2d.game.model.instances.L2CubicInstance;
import l2d.game.model.instances.L2CubicInstance.CubicType;

public class RequestTargetCanceld extends L2GameClientPacket
{
	private int _unselect;

	/**
	 * packet type id 0x48
	 * format:		ch
	 */
	@Override
	public void readImpl()
	{
		_unselect = readH();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(activeChar.getUnstuck() == 1)
		{
			activeChar.setUnstuck(0);
			activeChar.unblock();
		}

		for(L2CubicInstance cubic : activeChar.getCubics())
			if(cubic.getType() != CubicType.LIFE_CUBIC)
				cubic.stopAction();

		if(_unselect == 0)
		{
			if(activeChar.isCastingNow())
				activeChar.abortCast();
			else if(activeChar.getTarget() != null)
				activeChar.setTarget(null);
		}
		else if(activeChar.getTarget() != null)
			activeChar.setTarget(null);
	}
}