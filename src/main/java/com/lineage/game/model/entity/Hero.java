package com.lineage.game.model.entity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.lineage.game.model.entity.olympiad.Olympiad;
import javolution.util.FastMap;
import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.db.mysql;
import com.lineage.game.model.L2Alliance;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2ItemInstance.ItemClass;
import com.lineage.game.serverpackets.PledgeShowInfoUpdate;
import com.lineage.game.serverpackets.PledgeStatusChanged;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.ClanTable;
import com.lineage.game.tables.SkillTable;
import com.lineage.game.templates.L2Item;
import com.lineage.game.templates.StatsSet;

public class Hero
{
	private static Logger _log = Logger.getLogger(Hero.class.getName());

	private static Hero _instance;
	private static final String GET_HEROES = "SELECT * FROM heroes WHERE played = 1";
	private static final String GET_ALL_HEROES = "SELECT * FROM heroes";

	private static Map<Integer, StatsSet> _heroes;
	private static Map<Integer, StatsSet> _completeHeroes;

	public static final String COUNT = "count";
	public static final String PLAYED = "played";
	public static final String CLAN_NAME = "clan_name";
	public static final String CLAN_CREST = "clan_crest";
	public static final String ALLY_NAME = "ally_name";
	public static final String ALLY_CREST = "ally_crest";
	public static final String ACTIVE = "active";

	public static Hero getInstance()
	{
		if(_instance == null)
			_instance = new Hero();
		return _instance;
	}

	public Hero()
	{
		init();
	}

	private static void HeroSetClanAndAlly(final int charId, final StatsSet hero)
	{
		Entry<L2Clan, L2Alliance> e = ClanTable.getInstance().getClanAndAllianceByCharId(charId);
		hero.set(CLAN_CREST, e.getKey() == null ? 0 : e.getKey().getCrestId());
		hero.set(CLAN_NAME, e.getKey() == null ? "" : e.getKey().getName());
		hero.set(ALLY_CREST, e.getValue() == null ? 0 : e.getValue().getAllyCrestId());
		hero.set(ALLY_NAME, e.getValue() == null ? "" : e.getValue().getAllyName());
		e = null;
	}

	private void init()
	{
		_heroes = new FastMap<Integer, StatsSet>();
		_completeHeroes = new FastMap<Integer, StatsSet>();

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement(GET_HEROES);
			rset = statement.executeQuery();
			while(rset.next())
			{
				final StatsSet hero = new StatsSet();
				final int charId = rset.getInt(Olympiad.CHAR_ID);
				hero.set(Olympiad.CHAR_NAME, rset.getString(Olympiad.CHAR_NAME));
				hero.set(Olympiad.CLASS_ID, rset.getInt(Olympiad.CLASS_ID));
				hero.set(COUNT, rset.getInt(COUNT));
				hero.set(PLAYED, rset.getInt(PLAYED));
				hero.set(ACTIVE, rset.getInt(ACTIVE));
				HeroSetClanAndAlly(charId, hero);
				_heroes.put(charId, hero);
			}
			DatabaseUtils.closeDatabaseSR(statement, rset);

			statement = con.prepareStatement(GET_ALL_HEROES);
			rset = statement.executeQuery();
			while(rset.next())
			{
				final StatsSet hero = new StatsSet();
				final int charId = rset.getInt(Olympiad.CHAR_ID);
				hero.set(Olympiad.CHAR_NAME, rset.getString(Olympiad.CHAR_NAME));
				hero.set(Olympiad.CLASS_ID, rset.getInt(Olympiad.CLASS_ID));
				hero.set(COUNT, rset.getInt(COUNT));
				hero.set(PLAYED, rset.getInt(PLAYED));
				hero.set(ACTIVE, rset.getInt(ACTIVE));
				HeroSetClanAndAlly(charId, hero);
				_completeHeroes.put(charId, hero);
			}
		}
		catch(final SQLException e)
		{
			_log.warning("Hero System: Couldnt load Heroes");
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}

		_log.info("Hero System: Loaded " + _heroes.size() + " Heroes.");
		_log.info("Hero System: Loaded " + _completeHeroes.size() + " all time Heroes.");
	}

	public Map<Integer, StatsSet> getHeroes()
	{
		return _heroes;
	}

	public synchronized void clearHeroes()
	{
		mysql.set("UPDATE heroes SET played = 0, active = 0");

		if(_heroes.size() != 0)
			for(final StatsSet hero : _heroes.values())
			{
				if(hero.getInteger(ACTIVE) == 0)
					continue;

				final String name = hero.getString(Olympiad.CHAR_NAME);

				final L2Player player = L2World.getPlayer(name);

				if(player != null)
				{
					player.getInventory().unEquipItemInBodySlot(L2Item.SLOT_L_HAND, null);
					player.getInventory().unEquipItemInBodySlot(L2Item.SLOT_R_HAND, null);
					player.getInventory().unEquipItemInBodySlot(L2Item.SLOT_LR_HAND, null);
					player.getInventory().unEquipItemInBodySlot(L2Item.SLOT_HAIR, null);
					player.getInventory().unEquipItemInBodySlot(L2Item.SLOT_HAIRALL, null);
					player.getInventory().unEquipItemInBodySlot(L2Item.SLOT_DHAIR, null);

					for(final L2ItemInstance item : player.getInventory().getItems())
					{
						if(item == null)
							continue;
						if(item.isHeroItem())
							player.getInventory().destroyItem(item, 1, true);
					}

					for(final L2ItemInstance item : player.getWarehouse().listItems(ItemClass.EQUIPMENT))
					{
						if(item == null)
							continue;
						if(item.isHeroItem())
							player.getWarehouse().destroyItem(item.getItemId(), 1);
					}

					player.setHero(false);
					player.broadcastUserInfo(true);
				}
			}

		_heroes.clear();
	}

	public synchronized boolean computeNewHeroes(final List<StatsSet> newHeroes)
	{
		if(newHeroes.size() == 0)
			return true;

		final Map<Integer, StatsSet> heroes = new FastMap<Integer, StatsSet>();
		final boolean error = false;

		for(final StatsSet hero : newHeroes)
		{
			final int charId = hero.getInteger(Olympiad.CHAR_ID);

			if(_completeHeroes != null && _completeHeroes.containsKey(charId))
			{
				final StatsSet oldHero = _completeHeroes.get(charId);
				final int count = oldHero.getInteger(COUNT);
				oldHero.set(COUNT, count + 1);
				oldHero.set(PLAYED, 1);
				oldHero.set(ACTIVE, 0);

				heroes.put(charId, oldHero);
			}
			else
			{
				final StatsSet newHero = new StatsSet();
				newHero.set(Olympiad.CHAR_NAME, hero.getString(Olympiad.CHAR_NAME));
				newHero.set(Olympiad.CLASS_ID, hero.getInteger(Olympiad.CLASS_ID));
				newHero.set(COUNT, 1);
				newHero.set(PLAYED, 1);
				newHero.set(ACTIVE, 0);

				heroes.put(charId, newHero);
			}
		}

		_heroes.putAll(heroes);
		heroes.clear();

		updateHeroes(0);

		for(final Integer heroId : _heroes.keySet())
		{
			final StatsSet hero = _heroes.get(heroId);
			if(hero.getInteger(ACTIVE) == 0)
				continue;
			final String name = hero.getString(Olympiad.CHAR_NAME);

			final L2Player player = L2World.getPlayer(name);

			if(player != null)
			{
				player.addSkill(SkillTable.getInstance().getInfo(395, 1));
				player.addSkill(SkillTable.getInstance().getInfo(396, 1));
				player.addSkill(SkillTable.getInstance().getInfo(1374, 1));
				player.addSkill(SkillTable.getInstance().getInfo(1375, 1));
				player.addSkill(SkillTable.getInstance().getInfo(1376, 1));
				player.setHero(true);
			}

			final L2Clan playerClan = player == null ? ClanTable.getInstance().getClanByCharId(heroId) : player.getClan();
			if(playerClan != null && playerClan.getLevel() >= 5)
			{
				playerClan.incReputation(1000, true, "Hero:computeNewHeroes:" + name);
				playerClan.broadcastToOtherOnlineMembers(new SystemMessage(SystemMessage.CLAN_MEMBER_S1_WAS_NAMED_A_HERO_2S_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_REPUTATION_SCORE).addString(name).addNumber(Math.round(1000 * Config.RATE_CLAN_REP_SCORE)), player);
			}
			else if(player != null)
				player.broadcastUserInfo(true);
		}

		return error;
	}

	public void updateHeroes(final int id)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE INTO heroes VALUES (?,?,?,?,?,?)");

			for(final Integer heroId : _heroes.keySet())
			{
				if(id > 0 && heroId != id)
					continue;
				final StatsSet hero = _heroes.get(heroId);
				try
				{
					statement.setInt(1, heroId);
					statement.setString(2, hero.getString(Olympiad.CHAR_NAME));
					statement.setInt(3, hero.getInteger(Olympiad.CLASS_ID));
					statement.setInt(4, hero.getInteger(COUNT));
					statement.setInt(5, hero.getInteger(PLAYED));
					statement.setInt(6, hero.getInteger(ACTIVE));
					statement.execute();
					if(_completeHeroes != null && !_completeHeroes.containsKey(heroId))
					{
						HeroSetClanAndAlly(heroId, hero);
						_heroes.remove(heroId);
						_heroes.put(heroId, hero);
						_completeHeroes.put(heroId, hero);
					}
				}
				catch(final SQLException e)
				{
					_log.warning("Hero System: Couldnt update Hero: " + heroId);
					e.printStackTrace();
				}
			}

		}
		catch(final SQLException e)
		{
			_log.warning("Hero System: Couldnt update Heroes");
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public boolean isHero(final int id)
	{
		if(_heroes == null || _heroes.size() == 0)
			return false;
		if(_heroes.containsKey(id) && _heroes.get(id).getInteger(ACTIVE) == 1)
			return true;
		return false;
	}

	public boolean isInactiveHero(final int id)
	{
		if(_heroes == null || _heroes.size() == 0)
			return false;
		if(_heroes.containsKey(id) && _heroes.get(id).getInteger(ACTIVE) == 0)
			return true;
		return false;
	}

	public void activateHero(final L2Player player)
	{
		final StatsSet hero = _heroes.get(player.getObjectId());
		hero.set(ACTIVE, 1);
		_heroes.remove(player.getObjectId());
		_heroes.put(player.getObjectId(), hero);

		if(player.getBaseClassId() == player.getActiveClassId())
		{
			player.addSkill(SkillTable.getInstance().getInfo(395, 1));
			player.addSkill(SkillTable.getInstance().getInfo(396, 1));
			player.addSkill(SkillTable.getInstance().getInfo(1374, 1));
			player.addSkill(SkillTable.getInstance().getInfo(1375, 1));
			player.addSkill(SkillTable.getInstance().getInfo(1376, 1));
		}
		player.setHero(true);
		if(player.getClan() != null && player.getClan().getLevel() >= 5)
		{
			player.getClan().incReputation(1000, true, "Hero:activateHero:" + player);
			player.getClan().broadcastToOtherOnlineMembers(new SystemMessage(SystemMessage.CLAN_MEMBER_S1_WAS_NAMED_A_HERO_2S_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_REPUTATION_SCORE).addString(player.getName()).addNumber(Math.round(1000 * Config.RATE_CLAN_REP_SCORE)), player);
			player.getClan().broadcastToOnlineMembers(new PledgeShowInfoUpdate(player.getClan()));
			player.getClan().broadcastToOnlineMembers(new PledgeStatusChanged(player.getClan()));

		}
		else
			player.broadcastUserInfo(true);
		updateHeroes(player.getObjectId());
	}
}