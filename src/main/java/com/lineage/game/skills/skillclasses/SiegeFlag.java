package com.lineage.game.skills.skillclasses;

import java.util.logging.Logger;

import com.lineage.game.skills.funcs.FuncMul;
import javolution.util.FastList;
import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.idfactory.IdFactory;
import com.lineage.game.instancemanager.SiegeManager;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Skill;
import com.lineage.game.model.L2Zone.ZoneType;
import com.lineage.game.model.entity.siege.Siege;
import com.lineage.game.model.entity.siege.SiegeClan;
import com.lineage.game.model.instances.L2SiegeHeadquarterquarterInstance;
import com.lineage.game.skills.Stats;
import com.lineage.game.tables.NpcTable;
import com.lineage.game.templates.StatsSet;
import com.lineage.util.Location;

public class SiegeFlag extends L2Skill
{
	protected static Logger _log = Logger.getLogger(SiegeFlag.class.getName());
	private final boolean _advanced;
	private final double _advancedMult;

	public SiegeFlag(StatsSet set)
	{
		super(set);
		_advanced = set.getBool("advancedFlag", false);
		_advancedMult = set.getDouble("advancedMultiplier", 1.);
	}

	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		if(!activeChar.isPlayer())
			return false;
		if(!super.checkCondition(activeChar, target, forceUse, dontMove, first))
			return false;

		L2Player player = (L2Player) activeChar;
		if(player.getClan() == null || !player.isClanLeader())
		{
			_log.warning(player.toFullString() + " has " + toString() + ", but he isn't in a clan leader.");
			return false;
		}

		Siege siege = SiegeManager.getSiege(activeChar, true);
		if(siege == null || !checkIfOkToPlaceFlag(player, siege))
			return false;

		return true;
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		L2Player player = (L2Player) activeChar;

		Siege siege = SiegeManager.getSiege(activeChar, true);

		if(siege == null || !checkIfOkToPlaceFlag(player, siege))
			return;

		L2SiegeHeadquarterquarterInstance flag = new L2SiegeHeadquarterquarterInstance(player, IdFactory.getInstance().getNextId(), NpcTable.getTemplate(35062));

		if(_advanced)
			flag.addStatFunc(new FuncMul(Stats.MAX_HP, 0x50, flag, _advancedMult));

		flag.setCurrentHpMp(flag.getMaxHp(), flag.getMaxMp(), true);
		flag.setHeading(player.getHeading());

		// Ставим флаг перед чаром
		int x = (int) (player.getX() + 100 * Math.cos(player.headingToRadians(player.getHeading() - 32768)));
		int y = (int) (player.getY() + 100 * Math.sin(player.headingToRadians(player.getHeading() - 32768)));
		flag.spawnMe(new Location(x, y, GeoEngine.getHeight(x, y, player.getZ())));

		SiegeClan clan = siege.getAttackerClan(player.getClan());
		if(clan != null)
			clan.setHeadquarter(flag);
	}

	public boolean checkIfOkToPlaceFlag(L2Player activeChar, Siege siege)
	{
		if(siege == null || !activeChar.isInZone(ZoneType.Siege))
			activeChar.sendMessage("You must be on siege field to place a flag.");
		else if(activeChar.isInZone(ZoneType.siege_residense))
			activeChar.sendMessage("Flag can't be placed at castle.");
		else if(!siege.isInProgress())
			activeChar.sendMessage("You can only place a flag during a siege.");
		else if(siege.getAttackerClan(activeChar.getClan()) == null)
			activeChar.sendMessage("You must be an attacker to place a flag.");
		else if(activeChar.getClan() == null || !activeChar.isClanLeader())
			activeChar.sendMessage("You must be a clan leader to place a flag.");
		else
			return true;
		return false;
	}
}