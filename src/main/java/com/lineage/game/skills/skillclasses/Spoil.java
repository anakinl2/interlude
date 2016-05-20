package com.lineage.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.ai.CtrlEvent;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.instances.L2MonsterInstance;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.skills.Formulas;
import com.lineage.game.templates.StatsSet;
import com.lineage.util.Rnd;

public class Spoil extends L2Skill
{
	private final boolean _crush;

	@Override
	public boolean checkCondition(final L2Character activeChar, final L2Character target, final boolean forceUse, final boolean dontMove, final boolean first)
	{
		if(target == null)
			return false;
		if(!target.isMonster())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.IT_IS_A_CHARACTER_THAT_CANNOT_BE_SPOILED));
			return false;
		}
		if(((L2MonsterInstance) target).isSpoiled())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.ALREADY_SPOILED));
			return false;
		}
		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	public Spoil(final StatsSet set)
	{
		super(set);
		_crush = set.getBool("crush", false);
	}

	@Override
	public void useSkill(final L2Character activeChar, final FastList<L2Character> targets)
	{
		if(!activeChar.isPlayer())
			return;
		final boolean ss = activeChar.getChargedSoulShot();
		if(ss && _crush)
			activeChar.unChargeShots(false);

		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			final L2Character target = n.getValue();
			if(!target.isMonster() || target.isDead())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.IT_IS_A_CHARACTER_THAT_CANNOT_BE_SPOILED));
				continue;
			}

			if(_crush)
			{
				final double damage = Formulas.calcPhysDam(activeChar, target, this, false, false, ss).damage;
				target.reduceCurrentHp(damage, activeChar, this, true, true, false, true);
			}

			final L2MonsterInstance monster = (L2MonsterInstance) target;
			if(monster.isSpoiled())
				continue;

			boolean success;
			if(!Config.ALT_SPOIL_FORMULA)
			{
				final int monsterLevel = monster.getLevel();
				final int modifier = Math.abs(monsterLevel - activeChar.getLevel());
				double rateOfSpoil = Config.BASE_SPOIL_RATE;

				if(modifier > 8)
					rateOfSpoil = rateOfSpoil - rateOfSpoil * (modifier - 8) * 9 / 100;

				rateOfSpoil = rateOfSpoil * getMagicLevel() / monsterLevel;

				if(rateOfSpoil < Config.MINIMUM_SPOIL_RATE)
					rateOfSpoil = Config.MINIMUM_SPOIL_RATE;
				else if(rateOfSpoil > 99.)
					rateOfSpoil = 99.;

				activeChar.sendMessage(new CustomMessage("com.lineage.game.skills.skillclasses.Spoil.Chance", activeChar).addNumber((int) rateOfSpoil));
				success = Rnd.chance((int) rateOfSpoil);
			}
			else
				success = Formulas.calcSkillSuccess(activeChar, target, this);

			if(success)
			{
				monster.setSpoiled(true, (L2Player) activeChar);
				activeChar.sendPacket(new SystemMessage(SystemMessage.THE_SPOIL_CONDITION_HAS_BEEN_ACTIVATED));
			}
			else
				activeChar.sendPacket(new SystemMessage(SystemMessage.S1_HAS_FAILED).addSkillName(_id, getDisplayLevel()));

			target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, _effectPoint);

			if(success)
				getEffects(activeChar, target, false, false);
		}
	}
}