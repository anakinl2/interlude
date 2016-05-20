package l2d.game.clientpackets;

import l2d.game.model.L2Player;

public class RequestCreatePledge extends L2GameClientPacket
{
	//Format: cS
	private String _pledgename;

	@Override
	public void readImpl()
	{
		_pledgename = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		System.out.println("Unfinished packet: " + getType() + " // S: " + _pledgename);
	}
}