package com.lineage.game.skills.skillclasses;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.lineage.game.skills.effects.EffectTemplate;
import javolution.util.FastList;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.cache.Msg;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Effect;
import com.lineage.game.model.L2Skill;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.skills.Env;
import com.lineage.game.skills.Formulas;
import com.lineage.game.templates.StatsSet;
import com.lineage.util.Rnd;

public class StealBuff extends L2Skill
{
	private final int _stealCount;

	public StealBuff(StatsSet set)
	{
		super(set);
		_stealCount = set.getInteger("stealCount", 1);
	}

	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		if(target == null || !target.isPlayer())
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

			if(getActivateRate() > 0 && !Formulas.calcSkillSuccess(activeChar, target, this))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.C1_HAS_RESISTED_YOUR_S2).addString(target.getName()).addSkillName(getId(), getLevel()));
				continue;
			}

			if(!target.isPlayer())
				continue;

			if(target.checkReflectSkill(activeChar, this))
				target = activeChar;

			int counter = 0;
			byte antiloop = 24;
			boolean update = false;
			while(counter < _stealCount && antiloop > 0)
			{
				ConcurrentLinkedQueue<L2Effect> eff = target.getEffectList().getAllEffects();
				if(eff.size() == 0)
				{
					counter = _stealCount;
					continue;
				}
				L2Effect e = eff.toArray(new L2Effect[eff.size()])[Rnd.get(eff.size())];
				if(e.getSkill().isCancelable() && e.getSkill().isActive() && !e.getSkill().isOffensive() && !e._template._applyOnCaster)
				{
					L2Effect stealedEffect = cloneEffect(activeChar, e);
					e.exit();
					if(stealedEffect != null)
					{
						activeChar.getEffectList().addEffect(stealedEffect);
						update = true;
					}
					counter++;
				}
				antiloop--;
			}

			if(update)
			{
				activeChar.sendMessage(new CustomMessage("l2d.game.skills.skillclasses.StealBuff.Success", activeChar).addNumber(counter));
				activeChar.sendChanges();
				activeChar.updateEffectIcons();
			}

			getEffects(activeChar, target, getActivateRate() > 0, false);
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}

	private L2Effect cloneEffect(L2Character cha, L2Effect eff)
	{
		L2Skill skill = eff.getSkill();

		for(EffectTemplate et : skill.getEffectTemplates())
		{
			L2Effect effect = et.getEffect(new Env(cha, cha, skill));
			if(effect != null)
			{
				effect.setCount(eff.getCount());
				if(eff.getCount() == 1)
					effect.setPeriod(eff.getPeriod() - eff.getTime());
				else
					effect.setPeriod(eff.getPeriod());
				return effect;
			}
		}
		return null;
	}
}