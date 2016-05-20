package com.lineage.game.tables;

import java.sql.ResultSet;
import java.util.logging.Logger;

import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.game.handler.IScriptHandler;
import com.lineage.game.handler.ScriptHandler;
import com.lineage.game.instancemanager.CastleManager;
import com.lineage.game.instancemanager.ClanHallManager;
import com.lineage.game.instancemanager.SiegeManager;
import com.lineage.game.instancemanager.TownManager;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.model.L2Zone;
import com.lineage.game.model.L2Zone.ZoneType;
import com.lineage.game.model.Reflection;
import com.lineage.game.model.base.Race;
import com.lineage.game.model.entity.siege.Siege;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.util.Location;
import com.lineage.util.Rnd;

public class MapRegion
{
	private final static Logger _log = Logger.getLogger(MapRegion.class.getName());

	private static MapRegion _instance;

	private final int[][] _regions = new int[L2World.WORLD_SIZE_X][L2World.WORLD_SIZE_Y];

	public static enum TeleportWhereType
	{
		Castle,
		ClanHall,
		ClosestTown,
		SecondClosestTown,
		Headquarter
	}

	public static MapRegion getInstance()
	{
		if(_instance == null)
			_instance = new MapRegion();
		return _instance;
	}

	private MapRegion()
	{
		int count2 = 0;

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM mapregion");
			rset = statement.executeQuery();
			while(rset.next())
			{
				final int y = rset.getInt("y10_plus");

				for(int i = 0; i < L2World.WORLD_SIZE_X; i++)
				{
					final int region = rset.getInt("x" + (Math.abs(L2World.MAP_MIN_X >> 15) + 10 + i));
					_regions[i][y] = region;
					count2++;
				}
			}

			_log.fine("Loaded " + count2 + " mapregions.");
		}
		catch(final Exception e)
		{
			_log.warning("error while creating map region data: " + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	public final int getMapRegion(final int posX, final int posY)
	{
		final int tileX = posX - L2World.MAP_MIN_X >> 15;
		final int tileY = posY - L2World.MAP_MIN_Y >> 15;
		return _regions[tileX][tileY];
	}

	public static Location getTeleToCastle(final L2Character activeChar)
	{
		return getInstance().getTeleToLocation(activeChar, TeleportWhereType.Castle);
	}

	public static Location getTeleToClanHall(final L2Character activeChar)
	{
		return getInstance().getTeleToLocation(activeChar, TeleportWhereType.ClanHall);
	}

	public static Location getTeleToClosestTown(final L2Character activeChar)
	{
		return getInstance().getTeleToLocation(activeChar, TeleportWhereType.ClosestTown);
	}

	public static Location getTeleToSecondClosestTown(final L2Character activeChar)
	{
		return getInstance().getTeleToLocation(activeChar, TeleportWhereType.SecondClosestTown);
	}

	public static Location getTeleToHeadquarter(final L2Character activeChar)
	{
		return getInstance().getTeleToLocation(activeChar, TeleportWhereType.Headquarter);
	}

	public static Location getTeleTo(final L2Character activeChar, final TeleportWhereType teleportWhere)
	{
		return getInstance().getTeleToLocation(activeChar, teleportWhere);
	}

	public Location getTeleToLocation(final L2Character activeChar, final TeleportWhereType teleportWhere)
	{
		final L2Player player = activeChar.getPlayer();

		if(player != null)
		{
			final IScriptHandler handler = ScriptHandler.getInstance().getScriptHandler();
			if(handler != null)
			{
				final Location loc = handler.onEscape(player);
				if(loc != null)
					return loc;
			}

			final Reflection r = player.getReflection();
			if(r.getId() != 0)
			{
				if(r.getCoreLoc() != null)
					return r.getCoreLoc();
				player.setReflection(0);
				if(r.getReturnLoc() != null)
					return r.getReturnLoc();
			}

			final L2Clan clan = player.getClan();

			if(clan != null)
			{
				// If teleport to clan hall
				if(teleportWhere == TeleportWhereType.ClanHall && clan.getHasHideout() != 0)
					return ClanHallManager.getInstance().getClanHall(clan.getHasHideout()).getZone().getSpawn();

				// If teleport to castle
				if(teleportWhere == TeleportWhereType.Castle && clan.getHasCastle() != 0)
					return CastleManager.getInstance().getCastleByIndex(clan.getHasCastle()).getZone().getSpawn();

				// Checking if in siege
				final Siege siege = SiegeManager.getSiege(activeChar, true);
				if(siege != null && siege.isInProgress())
				{
					if(teleportWhere == TeleportWhereType.Headquarter)
					{
						// Check if player's clan is attacker
						final L2NpcInstance flag = siege.getHeadquarter(player.getClan());
						if(flag != null)
							// Спаун рядом с флагом
							return Rnd.coordsRandomize(flag.getLoc(), 49, 51);
						return TownManager.getInstance().getClosestTown(activeChar).getSpawn();
					}

					// Check if player's clan is defender
					if(siege.getDefenderClan(player.getClan()) != null && siege.getSiegeUnit() != null && siege.getSiegeUnit().getZone() != null)
						return player.getKarma() > 1 ? siege.getSiegeUnit().getZone().getPKSpawn() : siege.getSiegeUnit().getZone().getSpawn();
					return player.getKarma() > 1 ? TownManager.getInstance().getClosestTown(activeChar).getPKSpawn() : TownManager.getInstance().getClosestTown(activeChar).getSpawn();
				}
			}

			// Светлые эльфы не могут воскрешаться в городе темных
			if(player.getRace() == Race.elf && TownManager.getInstance().getClosestTown(activeChar).getTownId() == 3)
				return player.getKarma() > 1 ? TownManager.getInstance().getTown(2).getPKSpawn() : TownManager.getInstance().getTown(2).getSpawn();

			// Темные эльфы не могут воскрешаться в городе светлых
			if(player.getRace() == Race.darkelf && TownManager.getInstance().getClosestTown(activeChar).getTownId() == 2)
				return player.getKarma() > 1 ? TownManager.getInstance().getTown(3).getPKSpawn() : TownManager.getInstance().getTown(3).getSpawn();

			final L2Zone battle = activeChar.getZone(ZoneType.battle_zone);

			// If in battle zone
			if(battle != null && battle.getRestartPoints() != null)
				return player.getKarma() > 1 ? battle.getPKSpawn() : battle.getSpawn();

			// If in pease zone
			if(activeChar.isInZone(ZoneType.peace_zone) && activeChar.getZone(ZoneType.peace_zone).getRestartPoints() != null)
				return player.getKarma() > 1 ? activeChar.getZone(ZoneType.peace_zone).getPKSpawn() : activeChar.getZone(ZoneType.peace_zone).getSpawn();

			// If in offshore zone == pease zone options.
			if(activeChar.isInZone(ZoneType.offshore) && activeChar.getZone(ZoneType.offshore).getRestartPoints() != null)
				return player.getKarma() > 1 ? activeChar.getZone(ZoneType.offshore).getPKSpawn() : activeChar.getZone(ZoneType.offshore).getSpawn();

			return player.getKarma() > 1 ? TownManager.getInstance().getClosestTown(activeChar).getPKSpawn() : TownManager.getInstance().getClosestTown(activeChar).getSpawn();
		}

		return TownManager.getInstance().getClosestTown(activeChar).getSpawn();
	}
}