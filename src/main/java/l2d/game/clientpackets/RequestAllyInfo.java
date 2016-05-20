package l2d.game.clientpackets;

import l2d.game.model.L2Player;
import l2d.game.serverpackets.AllianceInfo;

public class RequestAllyInfo extends L2GameClientPacket
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
		sendPacket(new AllianceInfo(activeChar));
	}
}