package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.game.ai.CtrlEvent;
import l2d.game.ai.CtrlIntention;
import l2d.game.model.L2Character;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.L2Summon;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.templates.StatsSet;
import l2d.util.Rnd;

public class Aggression extends L2Skill
{
	private final boolean _unaggring;
	private final boolean _silent;

	public Aggression(StatsSet set)
	{
		super(set);
		_unaggring = set.getBool("unaggroing", false);
		_silent = set.getBool("silent", false);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{

		int effect = _effectPoint;

		if(isSSPossible() && (activeChar.getChargedSoulShot() || activeChar.getChargedSpiritShot() > 0))
			effect *= 2;

		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();

			//Allseron Myzreal: Pvp implementation of Aggression and Hate Aura.
			if(target instanceof L2Player || target instanceof L2Summon)
			{
				boolean _hasAggDebuff = false;
				// Allseron: Myzreal - code for new type of Aggression and Aura of Hate skills.
				// They now inflict a debuff on a player target changing it's target to the caster
				// once every second. Lasts for 3 seconds. See: EffectAggroDebuff.java.
				// The instant aggro effect has 100% chance.
				target.setTarget(activeChar);
				target.abortAttack();
				target.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, activeChar);
				// 35% lowest Hate Aura, 65% highest. 37% bd&sws agro, 72% max lvl agro (tanks)
				if(Rnd.get(10000) <= getPower())
				{
					// If a character already has an aggro debuff the next one is resisted.
					if(target.getEffectList().getEffectsBySkillId(18) != null || target.getEffectList().getEffectsBySkillId(28) != null)
					{
						SystemMessage sm = new SystemMessage(SystemMessage.C1_HAS_RESISTED_YOUR_S2);
						sm.addName(target);
						sm.addSkillName(getId(), getLevel());
						activeChar.sendPacket(sm);
						_hasAggDebuff = true;
					}
					if(_hasAggDebuff == false)
						getEffects(activeChar, target, true, false);
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessage.C1_HAS_RESISTED_YOUR_S2);
					sm.addName(target);
					sm.addSkillName(getId(), getLevel());
					activeChar.sendPacket(sm);
				}
			}
			else
			{

				if(target == null || !target.isNpc() && !target.isPlayable())
					continue;

				if(_unaggring)
					if(target.isNpc() && activeChar instanceof L2Playable)
						((L2Playable) activeChar).addDamageHate((L2NpcInstance) target, 0, -effect);
				target.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, activeChar, effect);
				if(!_silent && target.isNpc())
					((L2NpcInstance) target).callFriends(activeChar);
			}

			getEffects(activeChar, target, getActivateRate() > 0, false);
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}
}
