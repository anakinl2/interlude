package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Player;
import com.lineage.util.Location;

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