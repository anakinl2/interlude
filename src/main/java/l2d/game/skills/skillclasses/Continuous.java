package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.Config;
import l2d.game.cache.Msg;
import l2d.game.model.L2Character;
import l2d.game.model.L2Skill;
import l2d.game.skills.Stats;
import l2d.game.templates.StatsSet;
import l2d.util.Rnd;

public class Continuous extends L2Skill
{
	private final int _lethal1;
	private final int _lethal2;

	public Continuous(StatsSet set)
	{
		super(set);
		_lethal1 = set.getInteger("lethal1", 0);
		_lethal2 = set.getInteger("lethal2", 0);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(target == null)
				continue;
			// Player holding a cursed weapon can't be buffed and can't buff
			if(getSkillType() == L2Skill.SkillType.BUFF && target != activeChar)
				if(target.isCursedWeaponEquipped() || activeChar.isCursedWeaponEquipped())
					continue;

			if(isOffensive() && target.checkReflectSkill(activeChar, this))
				target = activeChar;

			double mult = 0.01 * target.calcStat(Stats.DEATH_RECEPTIVE, 100, target, this);
			double lethal1 = _lethal1 * mult;
			double lethal2 = _lethal2 * mult;

			if(lethal1 > 0 && Rnd.chance(lethal1))
			{
				if(target.isPlayer())
				{
					target.reduceCurrentHp(target.getCurrentCp(), activeChar, this, true, true, false, true);
					target.sendPacket(Msg.LETHAL_STRIKE);
					activeChar.sendPacket(Msg.YOUR_LETHAL_STRIKE_WAS_SUCCESSFUL);
				}
				else if(target.isNpc() && !target.isLethalImmune())
				{
					target.reduceCurrentHp(target.getCurrentHp() / 2, activeChar, this, true, true, false, true);
					activeChar.sendPacket(Msg.YOUR_LETHAL_STRIKE_WAS_SUCCESSFUL);
				}
			}
			else if(lethal2 > 0 && Rnd.chance(lethal2))
				if(target.isPlayer())
				{
					target.reduceCurrentHp(target.getCurrentHp() + target.getCurrentCp() - 1, activeChar, this, true, true, false, true);
					target.sendPacket(Msg.LETHAL_STRIKE);
					activeChar.sendPacket(Msg.YOUR_LETHAL_STRIKE_WAS_SUCCESSFUL);
				}
				else if(target.isNpc() && !target.isLethalImmune())
				{
					target.reduceCurrentHp(target.getCurrentHp(), activeChar, this, true, true, false, true);
					activeChar.sendPacket(Msg.YOUR_LETHAL_STRIKE_WAS_SUCCESSFUL);
				}

			getEffects(activeChar, target, getActivateRate() > 0, false);
		}

		if(isSSPossible())
			if(!(Config.SAVING_SPS && _skillType == SkillType.BUFF) && getTargetType() != SkillTargetType.TARGET_SELF) // Селф бафы не тратят шоты.
				activeChar.unChargeShots(isMagic());
	}
}