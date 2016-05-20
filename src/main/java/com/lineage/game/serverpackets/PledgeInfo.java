package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Clan;

public class PledgeInfo extends L2GameServerPacket
{
	private int clan_id;
	private String clan_name, ally_name;

	public PledgeInfo(L2Clan clan)
	{
		clan_id = clan.getClanId();
		clan_name = clan.getName();
		ally_name = clan.getAlliance() == null ? "" : clan.getAlliance().getAllyName();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x83);
		writeD(clan_id);
		writeS(clan_name);
		writeS(ally_name);
	}
}