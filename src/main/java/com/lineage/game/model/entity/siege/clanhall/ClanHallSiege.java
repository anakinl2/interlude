package com.lineage.game.model.entity.siege.clanhall;

import java.util.Calendar;

import com.lineage.game.model.entity.siege.SiegeClan;
import javolution.util.FastList;
import com.lineage.ext.scripts.Functions;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.geodata.GeoEngine;
import com.lineage.game.idfactory.IdFactory;
import com.lineage.game.instancemanager.ClanHallSiegeManager;
import com.lineage.game.instancemanager.SiegeGuardManager;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.entity.residence.ClanHall;
import com.lineage.game.model.entity.siege.Siege;
import com.lineage.game.model.entity.siege.SiegeClanType;
import com.lineage.game.model.entity.siege.SiegeEndTask;
import com.lineage.game.model.entity.siege.SiegeSpawn;
import com.lineage.game.model.entity.siege.SiegeStartTask;
import com.lineage.game.model.entity.siege.TeleportWhoType;
import com.lineage.game.model.instances.L2SiegeBossInstance;
import com.lineage.game.serverpackets.PledgeShowInfoUpdate;
import com.lineage.game.serverpackets.PledgeStatusChanged;
import com.lineage.game.serverpackets.SiegeInfo;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.ClanTable;
import com.lineage.game.tables.MapRegion;
import com.lineage.game.tables.NpcTable;

// TODO:
// SystemMessage.REGISTRATION_FOR_THE_CLAN_HALL_SIEGE_IS_CLOSED
public class ClanHallSiege extends Siege
{
	private FastList<L2SiegeBossInstance> _siegeBosses = new FastList<L2SiegeBossInstance>();

	public ClanHallSiege(final ClanHall siegeUnit)
	{
		super(siegeUnit);
		_database = new ClanHallSiegeDatabase(this);
		_siegeGuardManager = new SiegeGuardManager(getSiegeUnit());
		_database.loadSiegeClan();
	}

	@Override
	public void startSiege()
	{
		if(!_isInProgress)
		{
			_database.loadSiegeClan(); // Load siege clan from db

			if(getSiegeUnit().getOwnerId() > 0)
			{
				addAttacker(getSiegeUnit().getOwnerId());
				getSiegeUnit().changeOwner(null);
			}

			if(getAttackerClans().size() <= 0)
			{
				if(getSiegeUnit().getOwnerId() <= 0)
					announceToPlayer(new SystemMessage(SystemMessage.THE_SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST).addString(getSiegeUnit().getName()), false);
				else
					announceToPlayer(new SystemMessage(SystemMessage.S1S_SIEGE_WAS_CANCELED_BECAUSE_THERE_WERE_NO_CLANS_THAT_PARTICIPATED).addString(getSiegeUnit().getName()), false);
				return;
			}

			getZone().setActive(true);
			// TODO: Включить активацию после описания residence зон кланхоллов
			// getResidenseZone().setActive(true);

			_isInProgress = true; // Flag so that same siege instance cannot be started again

			defendersUpdate(false); // Add defenders to list
			attackersUpdate(false); // Add attackers to list
			updatePlayerSiegeStateFlags(false);

			teleportPlayer(TeleportWhoType.Attacker, MapRegion.TeleportWhereType.ClosestTown); // Teleport to the closest town
			teleportPlayer(TeleportWhoType.Spectator, MapRegion.TeleportWhereType.ClosestTown); // Teleport to the closest town

			spawnSiegeBosses();
			getSiegeUnit().spawnDoor(); // Spawn door
			getSiegeGuardManager().spawnSiegeGuard(); // Spawn siege guard

			// Schedule a task to prepare auto siege end
			_siegeEndDate = Calendar.getInstance();
			_siegeEndDate.add(Calendar.MINUTE, getSiegeLength());
			ThreadPoolManager.getInstance().scheduleGeneral(new SiegeEndTask(this), 1000); // Prepare auto end task
			announceToPlayer(new SystemMessage(SystemMessage.THE_SIEGE_OF_THE_CLAN_HALL_HAS_BEGUN), false);
		}
	}

	@Override
	public void midVictory()
	{
		// Если осада закончилась
		if(!isInProgress() || getSiegeUnit().getOwnerId() <= 0)
			return;

		final SiegeClan sc_newowner = getAttackerClan(getSiegeUnit().getOwnerId());
		removeAttacker(sc_newowner);
		addDefender(sc_newowner, SiegeClanType.OWNER);

		endSiege();
	}

	@Override
	public void endSiege()
	{
		getZone().setActive(false);

		// TODO: Включить деактивацию после описания residence зон кланхоллов
		// getResidenseZone().setActive(false);

		if(isInProgress())
		{
			announceToPlayer(new SystemMessage(SystemMessage.THE_SIEGE_OF_THE_CLAN_HALL_IS_FINISHED), false);

			if(getSiegeUnit().getOwnerId() <= 0)
				announceToPlayer(new SystemMessage(SystemMessage.THE_SIEGE_OF_S1_HAS_ENDED_IN_A_DRAW).addString(getSiegeUnit().getName()), false);
			else
			{
				final L2Clan newOwner = ClanTable.getInstance().getClan(getSiegeUnit().getOwnerId());
				// clanhall was taken over from scratch
				if(newOwner.getLevel() >= 5)
					newOwner.broadcastToOnlineMembers(new SystemMessage(SystemMessage.YOUR_CLAN_NEWLY_ACQUIRED_CONTESTED_CLAN_HALL_HAS_ADDED_S1_POINTS_TO_YOUR_CLAN_REPUTATION_SCORE).addNumber(newOwner.incReputation(500, true, "ClanHallSiege")));

				newOwner.broadcastToOnlineMembers(new PledgeShowInfoUpdate(newOwner));
				newOwner.broadcastToOnlineMembers(new PledgeStatusChanged(newOwner));
			}

			// TODO
			// _player.getClan().broadcastToOnlineMembers(new
			// SystemMessage(SystemMessage.YOUR_CLAN_HAS_CAPTURED_YOUR_OPPONENT_CONTESTED_CLAN_HALL_S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_YOUR_OPPONENT_CLAN_REPUTATION_SCORE).addNumber(500));
			// ClanHallManager.getInstance().getClanHall(_id).getOwner().broadcastToOnlineMembers(new
			// SystemMessage(SystemMessage.AN_OPPOSING_CLAN_HAS_CAPTURED_YOUR_CLAN_CONTESTED_CLAN_HALL_S1_POINTS_HAVE_BEEN_DEDUCTED_FROM_YOUR_CLAN_REPUTATION_SCORE).addNumber(300));
			// ClanHallManager.getInstance().getClanHall(_id).getOwner().incReputation(-300, false, "CHSiege");
			// attacker.broadcastToOnlineMembers(new SystemMessage(SystemMessage.AFTER_LOSING_THE_CONTESTED_CLAN_HALL_300_POINTS_HAVE_BEEN_DEDUCTED_FROM_YOUR_CLAN_REPUTATION_SCORE));
			// attacker.incReputation(-300, false, "CHSiege");

			unspawnSiegeBosses();
			removeHeadquarters();
			teleportPlayer(TeleportWhoType.Attacker, MapRegion.TeleportWhereType.ClosestTown); // Teleport to the closest town
			teleportPlayer(TeleportWhoType.Spectator, MapRegion.TeleportWhereType.ClosestTown); // Teleport to the closest town
			removeSiegeSummons();
			_isInProgress = false; // Flag so that siege instance can be started
			updatePlayerSiegeStateFlags(true);
			saveSiege(); // Save clanhall specific data
			_database.clearSiegeClan(); // Clear siege clan from db
			getSiegeGuardManager().unspawnSiegeGuard(); // Remove all spawned siege guard
			getSiegeUnit().spawnDoor(); // Respawn door
			defendersUpdate(true);
			attackersUpdate(true);

			if(_siegeStartTask != null)
			{
				_siegeStartTask.cancel(false);
				_siegeStartTask = null;
			}
			setRegistrationOver(false);
		}
	}

	@Override
	public void Engrave(final L2Clan clan, final int objId)
	{
		if(clan != null)
		{
			getSiegeUnit().changeOwner(clan);
			midVictory();
		}
		else
			endSiege();
	}

	@Override
	protected void addDefender(final SiegeClan sc, final SiegeClanType type)
	{}

	@Override
	public void registerDefender(final L2Player player, final boolean force)
	{}

	/**
	 * Start the auto tasks<BR><BR>
	 */
	@Override
	public void startAutoTask()
	{
		if(_siegeStartTask != null)
			return;
		correctSiegeDateTime();
		System.out.println("Siege of " + getSiegeUnit().getName() + ": " + _siegeDate.getTime());
		// Schedule registration end
		_siegeRegistrationEndDate = Calendar.getInstance();
		_siegeRegistrationEndDate.setTimeInMillis(_siegeDate.getTimeInMillis());
		_siegeRegistrationEndDate.add(Calendar.DAY_OF_MONTH, -1);
		// Schedule siege auto start
		_siegeStartTask = ThreadPoolManager.getInstance().scheduleGeneral(new SiegeStartTask(this), 1000);
	}

	/** Set the date for the next siege. */
	@Override
	protected void setNextSiegeDate()
	{
		if(_siegeDate.getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
		{
			// Set next siege date if siege has passed
			_siegeDate.add(Calendar.DAY_OF_MONTH, 14); // Schedule to happen in 14 days
			if(_siegeDate.getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
				setNextSiegeDate(); // Re-run again if still in the pass
		}
	}

	@Override
	protected void correctSiegeDateTime()
	{
		boolean corrected = false;
		if(_siegeDate.getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
		{
			// Since siege has past reschedule it to the next one (14 days)
			// This is usually caused by server being down
			corrected = true;
			setNextSiegeDate();
		}
		if(_siegeDate.get(Calendar.DAY_OF_WEEK) != getSiegeUnit().getSiegeDayOfWeek())
		{
			corrected = true;
			_siegeDate.set(Calendar.DAY_OF_WEEK, getSiegeUnit().getSiegeDayOfWeek());
		}
		if(_siegeDate.get(Calendar.HOUR_OF_DAY) != getSiegeUnit().getSiegeHourOfDay())
		{
			corrected = true;
			_siegeDate.set(Calendar.HOUR_OF_DAY, getSiegeUnit().getSiegeHourOfDay());
		}
		_siegeDate.set(Calendar.MINUTE, 0);
		if(corrected)
			_database.saveSiegeDate();
	}

	@Override
	protected void saveSiege()
	{
		// Выставляем дату следующей осады
		setNextSiegeDate();
		// Сохраняем дату следующей осады
		_database.saveSiegeDate();
		// Запускаем таск для следующей осады
		startAutoTask();
	}

	/** Display list of registered clans */
	@Override
	public void listRegisterClan(final L2Player player)
	{
		player.sendPacket(new SiegeInfo(getSiegeUnit()));
	}

	/** Один из боссов убит */
	public void killedSiegeBoss(final L2SiegeBossInstance boss)
	{
		if(boss.getNpcId() == 35408)
			Functions.npcShout(boss, "Has once more $$ln the defeat the shame.. But the tragedy had not ended...");
		else if(boss.getNpcId() == 35409)
			Functions.npcShout(boss, "Is this my boundary.. But does not have Gustave's permission, I can die in no way!");
		else if(boss.getNpcId() == 35410)
		{
			Functions.npcShout(boss, "Day.. Unexpectedly is defeated? But I certainly can again come back! Comes back takes your head!");
			Engrave(boss.getWinner(), boss.getObjectId());
		}
		else if(boss.getNpcId() == 35368)
			Engrave(boss.getWinner(), boss.getObjectId());
		else if(boss.getNpcId() == 35629)
			Engrave(boss.getWinner(), boss.getObjectId());
		_siegeBosses.remove(boss);
	}

	private void unspawnSiegeBosses()
	{
		for(final L2SiegeBossInstance siegeBoss : _siegeBosses)
			if(siegeBoss != null)
				siegeBoss.deleteMe();
		_siegeBosses.clear();
	}

	private void spawnSiegeBosses()
	{
		for(final SiegeSpawn sp : ClanHallSiegeManager.getSiegeBossSpawnList(getSiegeUnit().getId()))
		{
			final L2SiegeBossInstance siegeBoss = new L2SiegeBossInstance(IdFactory.getInstance().getNextId(), NpcTable.getTemplate(sp.getNpcId()));
			siegeBoss.setCurrentHpMp(siegeBoss.getMaxHp(), siegeBoss.getMaxMp(), true);
			siegeBoss.setXYZInvisible(sp.getLoc().x, sp.getLoc().y, GeoEngine.getHeight(sp.getLoc()));
			siegeBoss.setSpawnedLoc(siegeBoss.getLoc());
			siegeBoss.setHeading(sp.getLoc().h);
			siegeBoss.spawnMe();
			_siegeBosses.add(siegeBoss);
			if(sp.getNpcId() == 35408)
				Functions.npcShout(siegeBoss, "Gustave's soldiers, fight! Delivers the invader to die!");
			if(sp.getNpcId() == 35409)
				Functions.npcShout(siegeBoss, "Qrants kingdom of Aden lion, honorable! Grants does not die $$ln Gustave to be honorable!");
			if(sp.getNpcId() == 35410)
				Functions.npcShout(siegeBoss, "Comes to understand! Your these foreign lands invaders! This fort forever ruler, my Gustave lifts the sword!");
		}
	}
}