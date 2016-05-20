package l2d.game.serverpackets;

import l2d.game.model.L2Player;
import l2d.util.Location;

public class ObserverEnd extends L2GameServerPacket
{
	// ddSS
	private Location _loc;

	public ObserverEnd(L2Player observer)
	{
		_loc = observer.getLoc();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xe0);
		writeD(_loc.x);
		writeD(_loc.y);
		writeD(_loc.z);
	}
}