package com.lineage.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.instances.L2DoorInstance;
import com.lineage.game.model.instances.L2SiegeHeadquarterquarterInstance;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.skills.Stats;
import com.lineage.game.templates.StatsSet;

public class Heal extends L2Skill
{
	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		if(target instanceof L2DoorInstance || target instanceof L2SiegeHeadquarterquarterInstance)
			return false;

		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	public Heal(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		// Надо уточнить формулу.
		double hp = _power + 0.1 * _power * Math.sqrt(activeChar.getMAtk(null, null) / 333);

		int sps = isSSPossible() ? activeChar.getChargedSpiritShot() : 0;

		if(sps == 2)
			hp *= 1.5;
		else if(sps == 1)
			hp *= 1.3;

		if(activeChar.getSkillMastery(getId()) == 3)
		{
			activeChar.removeSkillMastery(getId());
			hp *= 3;
		}

		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(target == null || target.isDead() || target.isHealBlocked())
				continue;

			// Player holding a cursed weapon can't be healed and can't heal
			if(target != activeChar)
				if(target.isPlayer() && target.isCursedWeaponEquipped())
					continue;
				else if(activeChar.isPlayer() && activeChar.isCursedWeaponEquipped())
					continue;

			double maxNewHp = hp * target.calcStat(Stats.HEAL_EFFECTIVNESS, 100, null, null) / 100;
			maxNewHp *= activeChar.calcStat(Stats.HEAL_POWER, 100, null, null) / 100;
			double addToHp = Math.min(Math.max(0, target.getMaxHp() - target.getCurrentHp()), maxNewHp);
			if(addToHp > 0)
				target.setCurrentHp(addToHp + target.getCurrentHp(), false);
			if(getId() == 4051)
				target.sendPacket(new SystemMessage(SystemMessage.REJUVENATING_HP));
			else if(target.isPlayer())
				if(activeChar != target)
					target.sendPacket(new SystemMessage(SystemMessage.XS2S_HP_HAS_BEEN_RESTORED_BY_S1).addString(activeChar.getName()).addNumber(Math.round(addToHp)));
				else
					activeChar.sendPacket(new SystemMessage(SystemMessage.S1_HPS_HAVE_BEEN_RESTORED).addNumber(Math.round(addToHp)));
			getEffects(activeChar, target, getActivateRate() > 0, false);
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}
}
