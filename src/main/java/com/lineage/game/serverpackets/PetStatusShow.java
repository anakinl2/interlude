package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Summon;

public class PetStatusShow extends L2GameServerPacket
{
	private int _summonType;

	public PetStatusShow(L2Summon summon)
	{
		_summonType = summon.getSummonType();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xb0);
		writeD(_summonType);
	}
}