package com.lineage.game.serverpackets;

public class PledgeSkillListAdd extends L2GameServerPacket
{
	private static int _skillId;
	private static int _skillLevel;

	public PledgeSkillListAdd(int skillId, int skillLevel)
	{
		_skillId = skillId;
		_skillLevel = skillLevel;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x3a);
		writeD(_skillId);
		writeD(_skillLevel);
	}
}