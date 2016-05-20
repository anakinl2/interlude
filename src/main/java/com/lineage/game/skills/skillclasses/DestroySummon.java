package com.lineage.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.L2Summon;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.skills.Formulas;
import com.lineage.game.templates.StatsSet;

public class DestroySummon extends L2Skill
{
	public DestroySummon(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();

			if(getActivateRate() > 0 && !Formulas.calcSkillSuccess(activeChar, target, this))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.C1_HAS_RESISTED_YOUR_S2).addString(target.getName()).addSkillName(getId(), getLevel()));
				continue;
			}

			if(target.isSummon())
			{
				((L2Summon) target).unSummon();
				getEffects(activeChar, target, getActivateRate() > 0, false);
			}
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}
}