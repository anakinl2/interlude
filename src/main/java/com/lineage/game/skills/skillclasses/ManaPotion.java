package com.lineage.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Skill;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.templates.StatsSet;

/**
 * @author HellSystem
 */
public class ManaPotion extends L2Skill
{
	public ManaPotion(StatsSet set)
	{
		super(set);
	}

	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		if(!isHandler() && getTargetType() == SkillTargetType.TARGET_ONE && (target == null || target.getSkillLevel(_id) > 0))
		{
			activeChar.sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
			return false;
		}

		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
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

			double addToMp = _power + 0.1 * _power / 333;;
			if(addToMp > 0)
				target.setCurrentMp(addToMp + target.getCurrentMp());
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
