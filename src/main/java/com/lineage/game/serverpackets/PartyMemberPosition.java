package com.lineage.game.serverpackets;

import java.util.Map;

import javolution.util.FastMap;

import com.lineage.game.model.L2Party;
import com.lineage.game.model.L2Player;
import com.lineage.game.network.L2GameClient;
import com.lineage.util.Location;

public class PartyMemberPosition extends L2GameServerPacket
{
	Map<Integer, Location> locations = new FastMap<Integer, Location>();

	public PartyMemberPosition(L2Party party)
	{
		reuse(party);
	}

	public void reuse(L2Party party)
	{
		locations.clear();
		for(L2Player member : party.getPartyMembers())
		{
			if(member == null)
				continue;
			locations.put(member.getObjectId(), new Location(member));
		}
	}

	@Override
	protected final void writeImpl()
	{
		L2GameClient client = getClient();
		if(client == null)
			return;

		writeC(0xa7);
		writeD(locations.size());
		for(Map.Entry<Integer, Location> entry : locations.entrySet())
		{
			Location loc = entry.getValue();
			writeD(entry.getKey());
			writeD(loc.x);
			writeD(loc.y);
			writeD(loc.z);
		}

	}
}