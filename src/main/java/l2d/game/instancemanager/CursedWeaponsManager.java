package l2d.game.instancemanager;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import javolution.util.FastMap;
import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import l2d.game.ThreadPoolManager;
import l2d.game.model.CursedWeapon;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.ItemTable;
import l2d.game.tables.SkillTable;
import l2d.game.templates.L2Item;
import com.lineage.util.Location;
import com.lineage.util.Rnd;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class CursedWeaponsManager
{
	private static final Logger _log = Logger.getLogger(CursedWeaponsManager.class.getName());

	private static CursedWeaponsManager _instance;

	public static CursedWeaponsManager getInstance()
	{
		if(_instance == null)
			_instance = new CursedWeaponsManager();
		return _instance;
	}

	Map<Integer, CursedWeapon> _cursedWeapons;
	private ScheduledFuture<?> _removeTask;

	private static final int CURSEDWEAPONS_MAINTENANCE_INTERVAL = 5 * 60 * 1000; // 5 min in millisec

	public CursedWeaponsManager()
	{
		_cursedWeapons = new FastMap<Integer, CursedWeapon>();

		if(!Config.ALLOW_CURSED_WEAPONS)
			return;

		_log.info("CursedWeaponsManager: Initializing");

		load();
		restore();
		checkConditions();

		cancelTask();
		_removeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RemoveTask(), CURSEDWEAPONS_MAINTENANCE_INTERVAL, CURSEDWEAPONS_MAINTENANCE_INTERVAL);

		_log.info("CursedWeaponsManager: Loaded " + _cursedWeapons.size() + " cursed weapon(s).");
	}

	public final void reload()
	{
		_instance = new CursedWeaponsManager();
	}

	private void load()
	{
		if(Config.DEBUG)
			System.out.print("CursedWeaponsManager: Parsing ... ");
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			final File file = new File(Config.DATAPACK_ROOT + "/data/cursed_weapons.xml");
			if(!file.exists())
			{
				if(Config.DEBUG)
					System.out.println("CursedWeaponsManager: NO FILE");
				return;
			}

			final Document doc = factory.newDocumentBuilder().parse(file);

			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				if("list".equalsIgnoreCase(n.getNodeName()))
					for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						if("item".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							final int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							final Integer skillId = Integer.parseInt(attrs.getNamedItem("skillId").getNodeValue());
							String name = "Unknown cursed weapon";
							if(attrs.getNamedItem("name") != null)
								name = attrs.getNamedItem("name").getNodeValue();
							else if(ItemTable.getInstance().getTemplate(id) != null)
								name = ItemTable.getInstance().getTemplate(id).getName();

							if(id == 0)
								continue;

							final CursedWeapon cw = new CursedWeapon(id, skillId, name);
							for(Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
								if("dropRate".equalsIgnoreCase(cd.getNodeName()))
									cw.setDropRate(Integer.parseInt(cd.getAttributes().getNamedItem("val").getNodeValue()));
								else if("duration".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									cw.setDurationMin(Integer.parseInt(attrs.getNamedItem("min").getNodeValue()));
									cw.setDurationMax(Integer.parseInt(attrs.getNamedItem("max").getNodeValue()));
								}
								else if("durationLost".equalsIgnoreCase(cd.getNodeName()))
									cw.setDurationLost(Integer.parseInt(cd.getAttributes().getNamedItem("val").getNodeValue()));
								else if("disapearChance".equalsIgnoreCase(cd.getNodeName()))
									cw.setDisapearChance(Integer.parseInt(cd.getAttributes().getNamedItem("val").getNodeValue()));
								else if("stageKills".equalsIgnoreCase(cd.getNodeName()))
									cw.setStageKills(Integer.parseInt(cd.getAttributes().getNamedItem("val").getNodeValue()));

							// Store cursed weapon
							_cursedWeapons.put(id, cw);
						}

			if(Config.DEBUG)
				System.out.println("CursedWeaponsManager: OK");
		}
		catch(final Exception e)
		{
			_log.severe("CursedWeaponsManager: Error parsing cursed_weapons file. " + e);

			if(Config.DEBUG)
				System.out.println("CursedWeaponsManager: ERROR");
		}
	}

	private void restore()
	{
		if(Config.DEBUG)
			System.out.print("CursedWeaponsManager: restoring ... ");

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("SELECT * FROM cursed_weapons");
			rset = statement.executeQuery();

			while(rset.next())
			{
				final int itemId = rset.getInt("item_id");
				final CursedWeapon cw = _cursedWeapons.get(itemId);
				if(cw != null)
				{
					cw.setPlayerId(rset.getInt("player_id"));
					cw.setPlayerKarma(rset.getInt("player_karma"));
					cw.setPlayerPkKills(rset.getInt("player_pkkills"));
					cw.setNbKills(rset.getInt("nb_kills"));
					cw.setLoc(new Location(rset.getInt("x"), rset.getInt("y"), rset.getInt("z")));
					cw.setEndTime(rset.getLong("end_time") * 1000);

					if(!cw.reActivate())
						endOfLife(cw);
				}
				else
				{
					removeFromDb(itemId);
					_log.warning("CursedWeaponsManager: Unknown cursed weapon " + itemId + ", deleted");
				}
			}
			if(Config.DEBUG)
				System.out.println("CursedWeaponsManager: OK");
		}
		catch(final Exception e)
		{
			_log.warning("CursedWeaponsManager: Could not restore cursed_weapons data: " + e);
			e.printStackTrace();

			if(Config.DEBUG)
				System.out.println("CursedWeaponsManager: ERROR");
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	private void checkConditions()
	{
		if(Config.DEBUG)
			System.out.print("CursedWeaponsManager: Checking conditions ... ");

		ThreadConnection con = null;
		FiltredPreparedStatement statement1 = null, statement2 = null;
		ResultSet rset = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement1 = con.prepareStatement("DELETE FROM character_skills WHERE skill_id=?");
			statement2 = con.prepareStatement("SELECT owner_id FROM items WHERE item_id=?");

			for(final CursedWeapon cw : _cursedWeapons.values())
			{
				// Do an item check to be sure that the cursed weapon and/or skill isn't hold by someone
				final int itemId = cw.getItemId();
				final int skillId = cw.getSkillId();
				boolean foundedInItems = false;

				// Delete all cursed weapons skills (we don`t care about same skill on multiply weapons, when player back, skill will appears again)
				statement1.setInt(1, skillId);
				statement1.executeUpdate();

				statement2.setInt(1, itemId);
				rset = statement2.executeQuery();

				while(rset.next())
				{
					// A player has the cursed weapon in his inventory ...
					final int playerId = rset.getInt("owner_id");

					if(!foundedInItems)
					{
						if(playerId != cw.getPlayerId() || cw.getPlayerId() == 0)
						{
							emptyPlayerCursedWeapon(playerId, itemId, cw);
							_log.info("CursedWeaponsManager[254]: Player " + playerId + " owns the cursed weapon " + itemId + " but he shouldn't.");
						}
						else
							foundedInItems = true;
					}
					else
					{
						emptyPlayerCursedWeapon(playerId, itemId, cw);
						_log.info("CursedWeaponsManager[262]: Player " + playerId + " owns the cursed weapon " + itemId + " but he shouldn't.");
					}
				}

				if(!foundedInItems && cw.getPlayerId() != 0)
				{
					removeFromDb(cw.getItemId());

					_log.info("CursedWeaponsManager: Unownered weapon, removing from table...");
				}
			}
		}
		catch(final Exception e)
		{
			_log.warning("CursedWeaponsManager: Could not check cursed_weapons data: " + e);

			if(Config.DEBUG)
				System.out.println("CursedWeaponsManager: ERROR");
			return;
		}
		finally
		{
			DatabaseUtils.closeStatement(statement1);
			DatabaseUtils.closeDatabaseCSR(con, statement2, rset);
		}

		if(Config.DEBUG)
			System.out.println("CursedWeaponsManager: DONE");
	}

	private void emptyPlayerCursedWeapon(final int playerId, final int itemId, final CursedWeapon cw)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			// Delete the item
			statement = con.prepareStatement("DELETE FROM items WHERE owner_id=? AND item_id=?");
			statement.setInt(1, playerId);
			statement.setInt(2, itemId);
			statement.executeUpdate();
			DatabaseUtils.closeStatement(statement);

			statement = con.prepareStatement("UPDATE characters SET karma=?, pkkills=? WHERE obj_id=?");
			statement.setInt(1, cw.getPlayerKarma());
			statement.setInt(2, cw.getPlayerPkKills());
			statement.setInt(3, playerId);
			if(statement.executeUpdate() != 1)
				_log.warning("Error while updating karma & pkkills for userId " + cw.getPlayerId());
			// clean up the cursedweapons table.
			removeFromDb(itemId);
		}
		catch(final SQLException e)
		{}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public void removeFromDb(final int itemId)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("DELETE FROM cursed_weapons WHERE item_id = ?");
			statement.setInt(1, itemId);
			statement.executeUpdate();

			if(getCursedWeapon(itemId) != null)
				getCursedWeapon(itemId).initWeapon();
		}
		catch(final SQLException e)
		{
			_log.severe("CursedWeaponsManager: Failed to remove data: " + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	private void cancelTask()
	{
		if(_removeTask != null)
		{
			_removeTask.cancel(true);
			_removeTask = null;
		}
	}

	private class RemoveTask implements Runnable
	{
		@Override
		public void run()
		{
			for(final CursedWeapon cw : _cursedWeapons.values())
				if(cw.isActive() && cw.getTimeLeft() <= 0)
					endOfLife(cw);
		}
	}

	public void endOfLife(final CursedWeapon cw)
	{
		if(cw.isActivated())
		{
			if(cw.getPlayer() != null && cw.getPlayer().isOnline())
			{
				final L2Player player = cw.getPlayer();

				// Remove from player
				_log.info("CursedWeaponsManager: " + cw.getName() + " being removed online from " + player + ".");

				player.abortAttack();

				player.setKarma(cw.getPlayerKarma());
				player.setPkKills(cw.getPlayerPkKills());
				player.setCursedWeaponEquippedId(0);
				player.removeSkill(SkillTable.getInstance().getInfo(cw.getSkillId(), player.getSkillLevel(cw.getSkillId())), false);

				// Remove
				player.getInventory().unEquipItemInBodySlot(L2Item.SLOT_LR_HAND, null);
				player.store(false);

				// Destroy
				if(player.getInventory().destroyItemByItemId(cw.getItemId(), 1, false) == null)
					_log.info("CursedWeaponsManager[395]: Error! Cursed weapon not found!!!");

				player.broadcastUserInfo(true);
			}
			else
			{
				// Remove from Db
				_log.info("CursedWeaponsManager: " + cw.getName() + " being removed offline.");

				ThreadConnection con = null;
				FiltredPreparedStatement statement = null;
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();

					// Delete the item
					statement = con.prepareStatement("DELETE FROM items WHERE owner_id=? AND item_id=?");
					statement.setInt(1, cw.getPlayerId());
					statement.setInt(2, cw.getItemId());
					statement.execute();
					DatabaseUtils.closeStatement(statement);

					// Delete the skill
					statement = con.prepareStatement("DELETE FROM character_skills WHERE char_obj_id=? AND skill_id=?");
					statement.setInt(1, cw.getPlayerId());
					statement.setInt(2, cw.getSkillId());
					statement.execute();
					DatabaseUtils.closeStatement(statement);

					// Restore the karma
					statement = con.prepareStatement("UPDATE characters SET karma=?, pkkills=? WHERE obj_Id=?");
					statement.setInt(1, cw.getPlayerKarma());
					statement.setInt(2, cw.getPlayerPkKills());
					statement.setInt(3, cw.getPlayerId());
					statement.execute();
				}
				catch(final SQLException e)
				{
					_log.warning("CursedWeaponsManager: Could not delete : " + e);
				}
				finally
				{
					DatabaseUtils.closeDatabaseCS(con, statement);
				}
			}
		}
		else // either this cursed weapon is in the inventory of someone who has another cursed weapon equipped,
		// OR this cursed weapon is on the ground.
		if(cw.getPlayer() != null && cw.getPlayer().getInventory().getItemByItemId(cw.getItemId()) != null)
		{
			final L2Player player = cw.getPlayer();
			if(cw.getPlayer().getInventory().destroyItemByItemId(cw.getItemId(), 1, false) == null)
				_log.info("CursedWeaponsManager[453]: Error! Cursed weapon not found!!!");

			player.sendChanges();
			player.broadcastUserInfo(true);
		}
		// is dropped on the ground
		else if(cw.getItem() != null)
		{
			cw.getItem().deleteMe();
			_log.info("CursedWeaponsManager: " + cw.getName() + " item has been removed from World.");
		}

		cw.initWeapon();
		removeFromDb(cw.getItemId());

		announce(new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED_CW).addString(cw.getName()));
	}

	public void saveData(final CursedWeapon cw)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			// Delete previous datas
			statement = con.prepareStatement("DELETE FROM cursed_weapons WHERE item_id = ?");
			statement.setInt(1, cw.getItemId());
			statement.executeUpdate();
			DatabaseUtils.closeStatement(statement);
			statement = null;
			if(cw.isActive())
			{
				statement = con.prepareStatement("REPLACE INTO cursed_weapons (item_id, player_id, player_karma, player_pkkills, nb_kills, x, y, z, end_time) VALUES (?,?,?,?,?,?,?,?,?)");
				statement.setInt(1, cw.getItemId());
				statement.setInt(2, cw.getPlayerId());
				statement.setInt(3, cw.getPlayerKarma());
				statement.setInt(4, cw.getPlayerPkKills());
				statement.setInt(5, cw.getNbKills());
				statement.setInt(6, cw.getLoc().x);
				statement.setInt(7, cw.getLoc().y);
				statement.setInt(8, cw.getLoc().z);
				statement.setLong(9, cw.getEndTime() / 1000);
				statement.executeUpdate();
			}
		}
		catch(final SQLException e)
		{
			_log.severe("CursedWeapon: Failed to save data: " + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}

	public void saveData()
	{
		if(Config.DEBUG)
			System.out.println("CursedWeaponsManager: saving data to disk.");
		for(final CursedWeapon cw : _cursedWeapons.values())
			saveData(cw);
	}

	/**
	 * вызывается при логине игрока
	 */
	public void checkPlayer(final L2Player player)
	{
		for(final CursedWeapon cw : _cursedWeapons.values())
			if(player.getInventory().getItemByItemId(cw.getItemId()) != null)
				checkPlayer(player, player.getInventory().getItemByItemId(cw.getItemId()));
	}

	/**
	 * вызывается, когда проклятое оружие оказывается в инвентаре игрока
	 */
	public void checkPlayer(final L2Player player, final L2ItemInstance item)
	{
		if(player == null || item == null)
			return;

		final CursedWeapon cw = _cursedWeapons.get(item.getItemId());
		if(cw == null)
			return;

		if(player.getObjectId() == cw.getPlayerId() || cw.getPlayerId() == 0 || cw.isDropped())
		{
			activate(player, item);
			showUsageTime(player, cw);
		}
		else
		{
			// wtf? how you get it?
			_log.warning("CursedWeaponsManager: " + player + " tried to obtain " + item + " in wrong way");
			player.getInventory().destroyItem(item, item.getCount(), true);
		}
	}

	public void activate(final L2Player player, final L2ItemInstance item)
	{
		final CursedWeapon cw = _cursedWeapons.get(item.getItemId());
		if(cw != null)
			if(player.isCursedWeaponEquipped()) // cannot own 2 cursed swords
			{
				if(player.getCursedWeaponEquippedId() != item.getItemId())
				{
					final CursedWeapon cw2 = _cursedWeapons.get(player.getCursedWeaponEquippedId());
					cw2.setNbKills(cw2.getStageKills() - 1);
					cw2.increaseKills();
				}

				// erase the newly obtained cursed weapon
				endOfLife(cw);
			}
			else if(cw.getTimeLeft() > 0)
			{
				cw.activate(player, item);
				saveData(cw);

				final SystemMessage sm = new SystemMessage(SystemMessage.THE_OWNER_OF_S2_HAS_APPEARED_IN_THE_S1_REGION);
				sm.addZoneName(cw.getPlayer().getX(), cw.getPlayer().getY(), cw.getPlayer().getZ()); // Region Name
				sm.addString(cw.getName());
				announce(sm);
			}
			else
				endOfLife(cw);
	}

	public void doLogout(final L2Player player)
	{
		for(final CursedWeapon cw : _cursedWeapons.values())
			if(player.getInventory().getItemByItemId(cw.getItemId()) != null)
			{
				cw.setPlayer(null);
				cw.setItem(null);
			}
	}

	/**
	 * drop from L2NpcInstance killed by L2Player
	 */
	public void dropAttackable(final L2NpcInstance attackable, final L2Player killer)
	{
		if(killer.isCursedWeaponEquipped() || _cursedWeapons.size() == 0)
			return;

		synchronized (_cursedWeapons)
		{
			int num = 0;
			short count = 0;
			byte breakFlag = 0;

			while(breakFlag == 0)
			{
				num = _cursedWeapons.keySet().toArray(new Integer[_cursedWeapons.size()])[Rnd.get(_cursedWeapons.size())];
				count++;

				if(_cursedWeapons.get(num) != null && !_cursedWeapons.get(num).isActive())
					breakFlag = 1;
				else if(count >= getCursedWeapons().size())
					breakFlag = 2;
			}

			if(breakFlag == 1)
				_cursedWeapons.get(num).create(attackable, killer);
		}
	}

	/**
	 * Выпадение оружия из владельца, или исчезновение с определенной вероятностью.
	 * Вызывается при смерти игрока.
	 */
	public void dropPlayer(final L2Player player)
	{
		final CursedWeapon cw = _cursedWeapons.get(player.getCursedWeaponEquippedId());
		if(cw == null)
			return;

		if(cw.dropIt(null, null, player))
		{
			saveData(cw);

			final SystemMessage sm = new SystemMessage(SystemMessage.S2_WAS_DROPPED_IN_THE_S1_REGION);
			sm.addZoneName(cw.getPlayer().getX(), cw.getPlayer().getY(), cw.getPlayer().getZ()); // Region Name
			sm.addString(cw.getName());
			announce(sm);
		}
		else
			endOfLife(cw);
	}

	public void increaseKills(final int itemId)
	{
		final CursedWeapon cw = _cursedWeapons.get(itemId);
		if(cw != null)
		{
			cw.increaseKills();
			saveData(cw);
		}
	}

	public int getLevel(final int itemId)
	{
		final CursedWeapon cw = _cursedWeapons.get(itemId);
		return cw != null ? cw.getLevel() : 0;
	}

	public void announce(final SystemMessage sm)
	{
		for(final L2Player player : L2World.getAllPlayers())
			player.sendPacket(sm);
	}

	public void showUsageTime(final L2Player player, final short itemId)
	{
		final CursedWeapon cw = _cursedWeapons.get(itemId);
		if(cw != null)
			showUsageTime(player, cw);
	}

	public void showUsageTime(final L2Player player, final CursedWeapon cw)
	{
		final SystemMessage sm = new SystemMessage(SystemMessage.S2_MINUTE_OF_USAGE_TIME_ARE_LEFT_FOR_S1);
		sm.addString(cw.getName());
		sm.addNumber(new Long(cw.getTimeLeft() / 60000).intValue());
		player.sendPacket(sm);
	}

	public boolean isCursed(final int itemId)
	{
		return _cursedWeapons.containsKey(itemId);
	}

	public Collection<CursedWeapon> getCursedWeapons()
	{
		return _cursedWeapons.values();
	}

	public Set<Integer> getCursedWeaponsIds()
	{
		return _cursedWeapons.keySet();
	}

	public CursedWeapon getCursedWeapon(final int itemId)
	{
		return _cursedWeapons.get(itemId);
	}
}