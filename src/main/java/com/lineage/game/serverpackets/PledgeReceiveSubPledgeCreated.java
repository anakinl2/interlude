package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Clan.SubPledge;

public class PledgeReceiveSubPledgeCreated extends L2GameServerPacket
{
	private int type;
	private String _name, leader_name;

	public PledgeReceiveSubPledgeCreated(SubPledge subPledge)
	{
		type = subPledge.getType();
		_name = subPledge.getName();
		leader_name = subPledge.getLeaderName();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x3f);

		writeD(0x01);
		writeD(type);
		writeS(_name);
		writeS(leader_name);
	}
}