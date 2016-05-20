package com.lineage.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Skill;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.templates.StatsSet;

public class ManaHealPercent extends L2Skill
{
	public ManaHealPercent(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(target.isDead() || target.isHealBlocked())
				continue;

			getEffects(activeChar, target, getActivateRate() > 0, false);

			double addToMp = Math.min(Math.max(0, target.getMaxMp() - target.getCurrentMp()), target.getMaxMp() * _power / 100);

			if(addToMp > 0)
				target.setCurrentMp(target.getCurrentMp() + addToMp);
			if(target.isPlayer())
				if(activeChar != target)
					target.sendPacket(new SystemMessage(SystemMessage.XS2S_MP_HAS_BEEN_RESTORED_BY_S1).addString(activeChar.getName()).addNumber(Math.round(addToMp)));
				else
					activeChar.sendPacket(new SystemMessage(SystemMessage.S1_MPS_HAVE_BEEN_RESTORED).addNumber(Math.round(addToMp)));
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}
}