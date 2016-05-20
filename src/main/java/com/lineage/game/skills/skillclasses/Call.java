package com.lineage.game.skills.skillclasses;

import static com.lineage.game.model.L2Zone.ZoneType.OlympiadStadia;
import static com.lineage.game.model.L2Zone.ZoneType.Siege;
import static com.lineage.game.model.L2Zone.ZoneType.no_restart;
import static com.lineage.game.model.L2Zone.ZoneType.no_summon;
import static com.lineage.game.model.L2Zone.ZoneType.offshore;
import javolution.util.FastList;
import com.lineage.game.cache.Msg;
import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.templates.StatsSet;
import com.lineage.util.GArray;

public class Call extends L2Skill
{
	final boolean _party;

	public Call(StatsSet set)
	{
		super(set);
		_party = set.getBool("party", false);
	}

	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		if(activeChar.isPlayer())
		{
			if(_party && ((L2Player) activeChar).getParty() == null)
				return false;

			SystemMessage msg = canSummonHere(activeChar);
			if(msg != null)
			{
				activeChar.sendPacket(msg);
				return false;
			}

			// Эта проверка только для одиночной цели
			if(!_party)
			{
				msg = canBeSummoned(target);
				if(msg != null)
				{
					activeChar.sendPacket(msg);
					return false;
				}
			}
		}

		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		SystemMessage msg = canSummonHere(activeChar);
		if(msg != null)
		{
			activeChar.sendPacket(msg);
			return;
		}

		if(_party)
		{
			if(((L2Player) activeChar).getParty() == null)
				return;
			GArray<L2Player> others = new GArray<L2Player>();
			others.addAll(((L2Player) activeChar).getParty().getPartyMembers());
			others.remove(activeChar);
			targets.reset();
			targets.addAll(others);
		}

		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(canBeSummoned(target) != null)
				continue;

			((L2Player) target).summonCharacterRequest(activeChar.getName(), GeoEngine.findPointToStay(activeChar.getX(), activeChar.getY(), activeChar.getZ(), 100, 150), getId() == 1403 || getId() == 1404 ? 1 : 0);

			getEffects(activeChar, target, getActivateRate() > 0, false);
		}

		if(isSSPossible())
			activeChar.unChargeShots(isMagic());
	}

	/**
	 * Может ли призывающий в данный момент использовать призыв
	 */
	public static SystemMessage canSummonHere(L2Character activeChar)
	{
		// "Нельзя вызывать персонажей в/из зоны свободного PvP"
		// "в зоны осад"
		// "на Олимпийский стадион"
		// "в зоны определенных рейд-боссов и эпик-боссов"
		if(activeChar.isInZoneBattle() || activeChar.isInZone(Siege) || activeChar.isInZoneIncludeZ(no_restart) || activeChar.isInZone(no_summon) || activeChar.isInZone(OlympiadStadia) || activeChar.isFlying() || activeChar.getReflection().getId() != 0)
			return Msg.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION;

		if(activeChar.isInZone(offshore) && activeChar.getReflection().getId() != 0)
			return Msg.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION;

		if(activeChar.isInCombat())
			return Msg.YOU_CANNOT_SUMMON_DURING_COMBAT;

		if(((L2Player) activeChar).getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE || ((L2Player) activeChar).isTransactionInProgress())
			return Msg.YOU_CANNOT_SUMMON_DURING_A_TRADE_OR_WHILE_USING_THE_PRIVATE_SHOPS;

		return null;
	}

	/**
	 * Может ли цель ответить на призыв
	 */
	public static SystemMessage canBeSummoned(L2Character target)
	{
		if(target == null || !target.isPlayer())
			return Msg.INVALID_TARGET;

		if(target.isInZone(OlympiadStadia))
			return Msg.YOU_CANNOT_SUMMON_PLAYERS_WHO_ARE_CURRENTLY_PARTICIPATING_IN_THE_GRAND_OLYMPIAD;

		if(target.isInZoneBattle() || target.isInZone(Siege) || target.isInZoneIncludeZ(no_restart) || target.isInZone(no_summon) || target.getReflection().getId() != 0 || target.isFlying())
			return Msg.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING;

		// Нельзя призывать мертвых персонажей
		if(target.isDead())
			return new SystemMessage(SystemMessage.S1_IS_DEAD_AT_THE_MOMENT_AND_CANNOT_BE_SUMMONED).addString(target.getName());

		// Нельзя призывать персонажей, которые находятся в режиме PvP
		if(target.getPvpFlag() != 0)
			return new SystemMessage(SystemMessage.S1_IS_ENGAGED_IN_COMBAT_AND_CANNOT_BE_SUMMONED).addString(target.getName());

		L2Player pTarget = (L2Player) target;

		// Нельзя призывать торгующих персонажей
		if(pTarget.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE || pTarget.isTransactionInProgress())
			return new SystemMessage(SystemMessage.S1_IS_CURRENTLY_TRADING_OR_OPERATING_A_PRIVATE_STORE_AND_CANNOT_BE_SUMMONED).addString(target.getName());

		return null;
	}
}