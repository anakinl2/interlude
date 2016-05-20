package com.lineage.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.instances.L2FeedableBeastInstance;
import com.lineage.game.templates.StatsSet;

public class BeastFeed extends L2Skill
{
	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		if(!(target instanceof L2FeedableBeastInstance))
		{
			activeChar.sendPacket(Msg.INVALID_TARGET);
			return false;
		}
		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	public BeastFeed(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		_log.fine("Beast Feed casting succeded.");

		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();

			((L2FeedableBeastInstance) target).onSkillUse((L2Player) activeChar, _id);
		}
	}
}
