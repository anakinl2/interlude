package l2d.game.clientpackets;

import l2d.game.model.L2Player;

public class RequestObserverEnd extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		if(activeChar.inObserverMode())
			if(activeChar.getOlympiadGameId() > 0)
				activeChar.leaveOlympiadObserverMode();
			else
				activeChar.leaveObserverMode();
	}
}