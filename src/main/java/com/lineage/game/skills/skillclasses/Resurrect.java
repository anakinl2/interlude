package com.lineage.game.skills.skillclasses;

import com.lineage.game.skills.Formulas;
import com.lineage.game.templates.StatsSet;
import javolution.util.FastList;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.cache.Msg;
import com.lineage.game.instancemanager.SiegeManager;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Playable;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.entity.siege.Siege;
import com.lineage.game.model.instances.L2PetInstance;

public class Resurrect extends L2Skill
{
	private final boolean _canPet;

	public Resurrect(StatsSet set)
	{
		super(set);
		_canPet = set.getBool("canPet", false);
	}

	@Override
	public boolean checkCondition(final L2Character activeChar, final L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		if(!activeChar.isPlayer())
			return false;

		L2Player player = (L2Player) activeChar;
		
		if(player.getTeam()>0)
		{
			player.sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
			return false;
		}

		Siege siege = SiegeManager.getSiege(activeChar, true);
		if((target == null || !target.isPet() && !target.isSummon()) && siege != null)
		{
			activeChar.sendPacket(Msg.IT_IS_IMPOSSIBLE_TO_BE_RESSURECTED_IN_BATTLEFIELDS_WHERE_SIEGE_WARS_ARE_IN_PROCESS);
			return false;
		}

		if(_targetType == SkillTargetType.TARGET_ONE)
		{
			if(target == null || !target.isDead())
				return false;

			if(!(target instanceof L2Playable))
			{
				player.sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
				return false;
			}

			if(target.isPet())
			{
				if(target.getPlayer() == null)
				{
					player.sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
					return false;
				}
				if(target.getPlayer().isReviveRequested())
				{
					if(target.getPlayer().isRevivingPet())
						activeChar.sendPacket(Msg.BETTER_RESURRECTION_HAS_BEEN_ALREADY_PROPOSED);
					else
						activeChar.sendPacket(Msg.SINCE_THE_MASTER_WAS_IN_THE_PROCESS_OF_BEING_RESURRECTED_THE_ATTEMPT_TO_RESURRECT_THE_PET_HAS_BEEN_CANCELLED);
					return false;
				}
				if(!(_canPet || _targetType == SkillTargetType.TARGET_PET))
				{
					player.sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
					return false;
				}
			}
			else if(target.isPlayer())
			{
				if(((L2Player) target).isReviveRequested())
				{
					if(((L2Player) target).isRevivingPet())
						activeChar.sendPacket(Msg.WHILE_A_PET_IS_ATTEMPTING_TO_RESURRECT_IT_CANNOT_HELP_IN_RESURRECTING_ITS_MASTER); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
					else
						activeChar.sendPacket(Msg.BETTER_RESURRECTION_HAS_BEEN_ALREADY_PROPOSED); // Resurrection is already been proposed.
					return false;
				}
				if(_targetType == SkillTargetType.TARGET_PET)
				{
					player.sendPacket(Msg.THAT_IS_THE_INCORRECT_TARGET);
					return false;
				}
				// Check to see if the player is in a festival.
				if(((L2Player) target).isFestivalParticipant())
				{
					player.sendMessage(new CustomMessage("l2d.game.skills.skillclasses.Resurrect", player));
					return false;
				}
			}
		}

		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		double percent = _power;

		if(percent < 100 && !isHandler())
		{
			double wit_bonus = _power * (Formulas.WITbonus[activeChar.getWIT()] - 1);
			percent += wit_bonus > 20 ? 20 : wit_bonus;
			if(percent > 100)
				percent = 100;
		}

		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();

			if(target.getPlayer() == null)
				continue;

			if(target.isPet() && _canPet)
			{
				if(target.getPlayer() == activeChar)
					((L2PetInstance) target).doRevive(percent);
				else
					target.getPlayer().reviveRequest((L2Player) activeChar, percent, true);
			}
			else if(target.isPlayer())
			{
				if(_targetType == SkillTargetType.TARGET_PET)
					continue;

				L2Player targetPlayer = (L2Player) target;

				if(targetPlayer.isReviveRequested())
					continue;

				if(targetPlayer.isFestivalParticipant())
					continue;

				targetPlayer.reviveRequest((L2Player) activeChar, percent, false);
			}
			else
				continue;

			getEffects(activeChar, target, getActivateRate() > 0, false);
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}
}