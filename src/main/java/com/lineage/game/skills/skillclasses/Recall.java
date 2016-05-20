package com.lineage.game.skills.skillclasses;

import com.lineage.game.templates.StatsSet;
import javolution.util.FastList;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.instancemanager.TownManager;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.L2Zone.ZoneType;
import com.lineage.game.serverpackets.StopMove;
import com.lineage.game.serverpackets.SystemMessage;

public class Recall extends L2Skill
{
	private final int _townId;
	private final boolean _clanhall;
	private final boolean _castle;

	public Recall(StatsSet set)
	{
		super(set);
		_townId = set.getInteger("townId", 0);
		_clanhall = set.getBool("clanhall", false);
		_castle = set.getBool("castle", false);
	}

	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		L2Player player = (L2Player) activeChar;

		if(player == null)
			return false;

		if(player.getTeam() > 0)
		{
			player.sendMessage("Please wait till event ends!");
			return false;
		}

		// BSOE в кланхолл/замок/форт работает только при наличии оного
		if(getHitTime() == 0)
		{
			if(_clanhall && (player.getClan() == null || player.getClan().getHasHideout() == 0))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addItemName(getItemConsumeId()[0]));
				return false;
			}
			if(_castle && (player.getClan() == null || player.getClan().getHasCastle() == 0))
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addItemName(getItemConsumeId()[0]));
				return false;
			}
		}

		if(activeChar.isPlayer())
		{
			L2Player p = (L2Player) activeChar;
			if(p.getDuel() != null)
			{
				activeChar.sendMessage(new CustomMessage("common.RecallInDuel", activeChar));
				return false;
			}
		}

		if(activeChar.isPlayer())
		{
			L2Player p = (L2Player) activeChar;
			if(p.isInOlympiadMode())
			{
				activeChar.sendMessage(new CustomMessage("com.lineage.game.skills.skillclasses.Recall.Here", activeChar));
				return false;
			}

			// SOE из тюрьмы? гг
			if(p.isInJail())
			{
				activeChar.sendMessage(new CustomMessage("com.lineage.game.skills.skillclasses.Recall.Here", activeChar));
				return false;
			}
		}

		if(activeChar.isInZone(ZoneType.no_escape))
		{
			activeChar.sendMessage(new CustomMessage("com.lineage.game.skills.skillclasses.Recall.Here", activeChar));
			return false;
		}

		if(activeChar.isInZone(ZoneType.offshore) && activeChar.getReflection().getId() != 0)
		{
			activeChar.sendMessage(new CustomMessage("com.lineage.game.skills.skillclasses.Recall.Here", activeChar));
			return false;
		}

		// Нельзя юзать рекал на олимпе.
		if(activeChar.isInZone(ZoneType.OlympiadStadia))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THIS_SKILL_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
			return false;
		}
		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		if(isSSPossible())
			activeChar.unChargeShots(true);

		for(L2Character target : targets)
		{
			L2Player pcTarget = target.getPlayer();
			if(pcTarget == null)
				continue;

			if(pcTarget.isFestivalParticipant())
			{
				activeChar.sendMessage(new CustomMessage("com.lineage.game.skills.skillclasses.Recall.Festival", activeChar));
				continue;
			}
			if(pcTarget.isInOlympiadMode())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.THIS_SKILL_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT));
				return;
			}
			if(pcTarget.getDuel() != null)
			{
				activeChar.sendMessage(new CustomMessage("common.RecallInDuel", activeChar));
				return;
			}

			target.abortAttack();
			target.abortCast();
			target.sendActionFailed();
			target.broadcastPacket(new StopMove(target));

			if(isHandler())
				switch(getItemConsumeId()[0])
				{
					case 1830: // SOE: castle
					case 5859: // BSOE: castle
						if(_castle)
							pcTarget.teleToCastle();
						else if(pcTarget.getKarma() > 0)
							pcTarget.teleToLocation(TownManager.getInstance().getTown(_townId).getPKSpawn());
						else
							pcTarget.teleToLocation(TownManager.getInstance().getTown(_townId).getSpawn());
						continue;
					case 1829: // SOE: CH
					case 5858: // BSOE: CH
						if(_clanhall)
							pcTarget.teleToClanhall();
						else if(pcTarget.getKarma() > 0)
							pcTarget.teleToLocation(TownManager.getInstance().getTown(_townId).getPKSpawn());
						else
							pcTarget.teleToLocation(TownManager.getInstance().getTown(_townId).getSpawn());
						continue;
					case 7117:
					case 7554:
					case 13395:
						pcTarget.teleToLocation(-84318, 244579, -3730); // Talking Island
						continue;
					case 7118:
					case 7555:
					case 13396:
						pcTarget.teleToLocation(46934, 51467, -2977); // Elven Village
						continue;
					case 7119:
					case 7556:
					case 13397:
						pcTarget.teleToLocation(9745, 15606, -4574); // Dark Elven Village
						continue;
					case 7120:
					case 7557:
					case 13398:
						pcTarget.teleToLocation(-44836, -112524, -235); // Orc Village
						continue;
					case 7121:
					case 7558:
					case 13399:
						pcTarget.teleToLocation(115113, -178212, -901); // Dwarven Village
						continue;
					case 7122:
					case 13400:
						if(pcTarget.getKarma() > 0)
							pcTarget.teleToLocation(TownManager.getInstance().getTown(7).getPKSpawn());
						else
							pcTarget.teleToLocation(TownManager.getInstance().getTown(7).getSpawn());
						// pcTarget.teleToLocation(-80826, 149775, -3043); // Gludin Village
						continue;
					case 7123:
					case 13401:
						if(pcTarget.getKarma() > 0)
							pcTarget.teleToLocation(TownManager.getInstance().getTown(6).getPKSpawn());
						else
							pcTarget.teleToLocation(TownManager.getInstance().getTown(6).getSpawn());
						// pcTarget.teleToLocation(-12678, 122776, -3116); // Gludio Castle Town
						continue;
					case 7124:
					case 13402:
						if(pcTarget.getKarma() > 0)
							pcTarget.teleToLocation(TownManager.getInstance().getTown(8).getPKSpawn());
						else
							pcTarget.teleToLocation(TownManager.getInstance().getTown(8).getSpawn());
						// pcTarget.teleToLocation(15670, 142983, -2705); // Dion Castle Town
						continue;
					case 7125:
					case 13403:
						if(pcTarget.getKarma() > 0)
							pcTarget.teleToLocation(TownManager.getInstance().getTown(19).getPKSpawn());
						else
							pcTarget.teleToLocation(TownManager.getInstance().getTown(19).getSpawn());
						// pcTarget.teleToLocation(17836, 170178, -3507); // Floran
						continue;
					case 7126:
					case 7559:
					case 13404:
						if(pcTarget.getKarma() > 0)
							pcTarget.teleToLocation(TownManager.getInstance().getTown(9).getPKSpawn());
						else
							pcTarget.teleToLocation(TownManager.getInstance().getTown(9).getSpawn());
						// pcTarget.teleToLocation(83400, 147943, -3404); // Giran Castle Town
						continue;
					case 7127:
					case 13405:
						pcTarget.teleToLocation(105918, 109759, -3207); // Hardin's Private Academy
						continue;
					case 7128:
					case 13406:
						if(pcTarget.getKarma() > 0)
							pcTarget.teleToLocation(TownManager.getInstance().getTown(13).getPKSpawn());
						else
							pcTarget.teleToLocation(TownManager.getInstance().getTown(13).getSpawn());
						// pcTarget.teleToLocation(111409, 219364, -3545); // Heine
						continue;
					case 7129:
					case 13407:
						if(pcTarget.getKarma() > 0)
							pcTarget.teleToLocation(TownManager.getInstance().getTown(10).getPKSpawn());
						else
							pcTarget.teleToLocation(TownManager.getInstance().getTown(10).getSpawn());
						// pcTarget.teleToLocation(82956, 53162, -1495); // Oren Castle Town
						continue;
					case 7130:
					case 13408:
						pcTarget.teleToLocation(85348, 16142, -3699); // Ivory Tower
						continue;
					case 7131:
					case 13409:
						if(pcTarget.getKarma() > 0)
							pcTarget.teleToLocation(TownManager.getInstance().getTown(12).getPKSpawn());
						else
							pcTarget.teleToLocation(TownManager.getInstance().getTown(12).getSpawn());
						// pcTarget.teleToLocation(116819, 76994, -2714); // Hunters Village
						continue;
					case 7132:
					case 13410:
						if(pcTarget.getKarma() > 0)
							pcTarget.teleToLocation(TownManager.getInstance().getTown(11).getPKSpawn());
						else
							pcTarget.teleToLocation(TownManager.getInstance().getTown(11).getSpawn());
						// pcTarget.teleToLocation(146331, 25762, -2018); // Aden Castle Town
						continue;
					case 7133:
					case 13411:
						if(pcTarget.getKarma() > 0)
							pcTarget.teleToLocation(TownManager.getInstance().getTown(15).getPKSpawn());
						else
							pcTarget.teleToLocation(TownManager.getInstance().getTown(15).getSpawn());
						// pcTarget.teleToLocation(147928, -55273, -2734); // Goddard Castle Town
						continue;
					case 7134:
					case 13412:
						if(pcTarget.getKarma() > 0)
							pcTarget.teleToLocation(TownManager.getInstance().getTown(14).getPKSpawn());
						else
							pcTarget.teleToLocation(TownManager.getInstance().getTown(14).getSpawn());
						// pcTarget.teleToLocation(43799, -47727, -798); // Rune Castle Town
						continue;
					case 7135:
					case 13413:
						if(pcTarget.getKarma() > 0)
							pcTarget.teleToLocation(TownManager.getInstance().getTown(16).getPKSpawn());
						else
							pcTarget.teleToLocation(TownManager.getInstance().getTown(16).getSpawn());
						// pcTarget.teleToLocation(87331, -142842, -1317); // Schuttgart Castle Town
						continue;
					case 7618:
						pcTarget.teleToLocation(149864, -81062, -5618); // Ketra Orc Village
						continue;
					case 7619:
						pcTarget.teleToLocation(108275, -53785, -2524); // Varka Silenos Village
						continue;
					case 9716:
					case 12753:
						continue;
				}

			if(target.isInZone(ZoneType.battle_zone) && target.getZone(ZoneType.battle_zone).getRestartPoints() != null)
			{
				target.teleToLocation(target.getZone(ZoneType.battle_zone).getSpawn());
				continue;
			}

			if(target.isInZone(ZoneType.peace_zone) && target.getZone(ZoneType.peace_zone).getRestartPoints() != null)
			{
				target.teleToLocation(target.getZone(ZoneType.peace_zone).getSpawn());
				continue;
			}

			target.teleToClosestTown();
		}
	}
}
