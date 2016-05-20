package com.lineage.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.serverpackets.MagicSkillUse;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.skills.Formulas;
import com.lineage.game.templates.StatsSet;

public class Charge extends L2Skill
{
	private int _charges;

	public Charge(final StatsSet set)
	{
		super(set);
		_charges = set.getInteger("charges", getLevel());
	}

	@Override
	public boolean checkCondition(final L2Character activeChar, final L2Character target, final boolean forceUse, final boolean dontMove, final boolean first)
	{
		if(!activeChar.isPlayer())
			return false;

		final L2Player player = (L2Player) activeChar;

		// Камушки можно юзать даже если заряд > 7, остальное только если заряд < уровень скила
		if(getPower() <= 0 && getId() != 2165 && player.getIncreasedForce() >= _charges)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOUR_FORCE_HAS_REACHED_MAXIMUM_CAPACITY_));
			return false;
		}
		else if(getId() == 2165)
			player.sendPacket(new MagicSkillUse(player, player, 2165, 1, 0, 0));

		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	@Override
	public void useSkill(final L2Character activeChar, final FastList<L2Character> targets)
	{
		if(!activeChar.isPlayer())
			return;

		final boolean ss = activeChar.getChargedSoulShot() && isSSPossible();
		if(ss && getTargetType() != SkillTargetType.TARGET_SELF)
			activeChar.unChargeShots(false);

		for(L2Character target : targets)
		{
			if(target.isDead() || target == activeChar)
				continue;

			if(target.checkReflectSkill(activeChar, this))
				target = activeChar;

			if(getPower() > 0) // Если == 0 значит скилл "отключен"
			{
				final double damage = Formulas.calcPhysDam(activeChar, target, this, false, false, ss).damage;

				target.reduceCurrentHp(damage, activeChar, this, true, true, false, true);
			}

			getEffects(activeChar, target, getActivateRate() > 0, false);
		}

		chargePlayer((L2Player) activeChar, getId());
	}

	public void chargePlayer(final L2Player player, final Integer skillId)
	{
		if(player.getIncreasedForce() >= _charges)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOUR_FORCE_HAS_REACHED_MAXIMUM_CAPACITY_));
			return;
		}
		player.setIncreasedForce(player.getIncreasedForce() + 1);
	}
}