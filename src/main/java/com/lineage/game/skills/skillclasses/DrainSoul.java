package com.lineage.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Skill;
import com.lineage.game.templates.StatsSet;

public class DrainSoul extends L2Skill
{
	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		if(!target.isMonster())
		{
			activeChar.sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
			return false;
		}
		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	public DrainSoul(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		if(!activeChar.isPlayer())
			return;

		// This is just a dummy skill for the soul crystal skill condition,
		// since the Soul Crystal item handler already does everything.
	}
}
