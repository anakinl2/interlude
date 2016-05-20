package l2d.game.clientpackets;

import l2d.game.serverpackets.ShowMiniMap;

public class RequestShowMiniMap extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		sendPacket(new ShowMiniMap(1665));
	}
}