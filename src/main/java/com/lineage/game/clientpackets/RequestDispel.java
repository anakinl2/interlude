package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Skill;
import com.lineage.game.model.base.L2EnchantSkillLearn;
import com.lineage.game.tables.SkillTable;
import com.lineage.game.tables.SkillTreeTable;

public class RequestDispel extends L2GameClientPacket
{
	private int _id, _level;

	@Override
	protected void readImpl() throws Exception
	{
		_id = readD();
		_level = readD();
	}

	@Override
	protected void runImpl() throws Exception
	{
		if(getClient() == null || getClient().getActiveChar() == null)
			return;

		if(_level > 100)
		{
			L2EnchantSkillLearn sl = SkillTreeTable.getSkillEnchant(_id, (short) _level);
			if(sl == null)
			{
				System.out.println("ERROR: RequestDispel: skill " + _id + " level " + _level + "not found!");
				return;
			}
			_level = SkillTreeTable.convertEnchantLevel(sl.getBaseLevel(), _level);
		}
		L2Skill s = SkillTable.getInstance().getInfo(_id, _level);
		if(!s.isOffensive() && !s.isSongDance())
			getClient().getActiveChar().getEffectList().stopEffectByDisplayId(_id);
	}
}