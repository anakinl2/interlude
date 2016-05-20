package com.lineage.game.instancemanager;

import java.sql.ResultSet;
import java.util.logging.Logger;

import com.lineage.game.ai.CtrlEvent;
import javolution.util.FastList;
import javolution.util.FastList.Node;
import javolution.util.FastMap;
import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.ext.listeners.L2ZoneEnterLeaveListener;
import com.lineage.ext.listeners.PropertyCollection;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2Spawn;
import com.lineage.game.model.L2Territory;
import com.lineage.game.model.L2Zone;
import com.lineage.game.model.L2Zone.ZoneType;
import com.lineage.game.model.entity.residence.Residence;
import com.lineage.game.model.entity.siege.Siege;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.NpcTable;
import com.lineage.game.tables.PetDataTable;
import com.lineage.game.tables.TerritoryTable;
import com.lineage.game.templates.L2NpcTemplate;
import com.lineage.util.Location;
import com.lineage.util.Rnd;

public class ZoneManager
{
	protected static Logger _log = Logger.getLogger(ZoneManager.class.getName());

	private static ZoneManager _instance;
	private static FastMap<ZoneType, FastList<L2Zone>> _zonesByType;

	private final NoLandingZoneListener _noLandingZoneListener = new NoLandingZoneListener();
	private final MonsterTrapZoneListener _monsterTrapZoneListener = new MonsterTrapZoneListener();

	private ZoneManager()
	{
		loadZone();
	}

	public static ZoneManager getInstance()
	{
		if(_instance == null)
			_instance = new ZoneManager();
		return _instance;
	}

	public boolean checkIfInZone(ZoneType zoneType, L2Object object)
	{
		return checkIfInZone(zoneType, object.getX(), object.getY(), object.getZ());
	}

	public boolean checkIfInZone(ZoneType zoneType, int x, int y)
	{
		FastList<L2Zone> list = _zonesByType.get(zoneType);
		if(list == null)
			return false;
		for(Node<L2Zone> n = list.head(), end = list.tail(); (n = n.getNext()) != end;)
		{
			if(n == null)
				break;
			L2Zone zone = n.getValue();
			if(zone.isActive() && zone.getLoc() != null && zone.getLoc().isInside(x, y))
				return true;
		}
		return false;
	}

	public boolean checkIfInZone(ZoneType zoneType, int x, int y, int z)
	{
		FastList<L2Zone> list = _zonesByType.get(zoneType);
		if(list == null)
			return false;
		for(Node<L2Zone> n = list.head(), end = list.tail(); (n = n.getNext()) != end;)
		{
			if(n == null)
				break;
			L2Zone zone = n.getValue();
			if(zone.isActive() && zone.getLoc() != null && zone.getLoc().isInside(x, y) && z >= zone.getLoc().getZmin() && z <= zone.getLoc().getZmax())
				return true;
		}
		return false;
	}

	public FastList<L2Zone> getZoneByType(ZoneType zoneType)
	{
		FastList<L2Zone> list = _zonesByType.get(zoneType);
		FastList<L2Zone> result = new FastList<L2Zone>();
		if(list == null)
			return result;
		for(Node<L2Zone> n = list.head(), end = list.tail(); (n = n.getNext()) != end;)
		{
			if(n == null)
				break;
			L2Zone zone = n.getValue();
			if(zone.isActive())
				result.add(zone);
		}
		return result;
	}

	public L2Zone getZoneByIndex(ZoneType zoneType, int index, boolean onlyActive)
	{
		FastList<L2Zone> list = _zonesByType.get(zoneType);
		if(list == null)
			return null;
		for(Node<L2Zone> n = list.head(), end = list.tail(); (n = n.getNext()) != end;)
		{
			if(n == null)
				break;
			L2Zone zone = n.getValue();
			if((!onlyActive || zone.isActive()) && zone.getIndex() == index)
				return zone;
		}
		return null;
	}

	public L2Zone getZoneById(ZoneType zoneType, int id, boolean onlyActive)
	{
		FastList<L2Zone> list = _zonesByType.get(zoneType);
		if(list == null)
			return null;
		for(Node<L2Zone> n = list.head(), end = list.tail(); (n = n.getNext()) != end;)
		{
			if(n == null)
				break;
			L2Zone zone = n.getValue();
			if((!onlyActive || zone.isActive()) && zone.getId() == id)
				return zone;
		}
		return null;
	}

	public L2Zone getZoneByTypeAndObject(ZoneType zoneType, L2Object object)
	{
		return getZoneByTypeAndCoords(zoneType, object.getX(), object.getY(), object.getZ());
	}

	public L2Zone getZoneByTypeAndCoords(ZoneType zoneType, int x, int y, int z)
	{
		FastList<L2Zone> list = _zonesByType.get(zoneType);
		if(list == null)
			return null;
		for(Node<L2Zone> n = list.head(), end = list.tail(); (n = n.getNext()) != end;)
		{
			if(n == null)
				break;
			L2Zone zone = n.getValue();
			if(zone.isActive() && zone.getLoc() != null && zone.getLoc().isInside(x, y, z))
				return zone;
		}
		return null;
	}

	private void loadZone()
	{
		_zonesByType = new FastMap<ZoneType, FastList<L2Zone>>();

		int count = 0;

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM `zone`");
			rset = statement.executeQuery();
			while(rset.next())
			{
				Integer zone_id = rset.getInt("id");
				if(zone_id == 0)
				{
					_log.warning("Null zone!");
					continue;
				}

				L2Zone z = new L2Zone(zone_id);

				z.setIndex(rset.getInt("index"));
				z.setTaxById(rset.getInt("taxById"));
				z.setName(rset.getString("name"));
				z.setType(ZoneType.valueOf(rset.getString("type")));
				z.setEnteringMessageId(rset.getInt("entering_message_no"));
				z.setLeavingMessageId(rset.getInt("leaving_message_no"));
				z.setTarget(rset.getString("target"));
				z.setSkill(rset.getString("skill_name"));
				z.setSkillProb(rset.getString("skill_prob"));
				z.setUnitTick(rset.getString("unit_tick"));
				z.setInitialDelay(rset.getString("initial_delay"));
				z.setRestartTime(rset.getLong("restart_time"));
				z.setBlockedActions(rset.getString("blocked_actions"));
				z.setDamageOnHP(rset.getString("damage_on_hp"));
				z.setDamageOnМP(rset.getString("damage_on_mp"));
				z.setMessageNumber(rset.getString("message_no"));
				z.setMoveBonus(rset.getString("move_bonus"));
				z.setRegenBonusHP(rset.getString("hp_regen_bonus"));
				z.setRegenBonusMP(rset.getString("mp_regen_bonus"));
				z.setAffectRace(rset.getString("affect_race"));
				z.setEvent(rset.getString("event"));

				/**
				 StatsSet zoneDat = new StatsSet();
				 zoneDat.set("on_time", rset.getString("on_time"));
				 zoneDat.set("off_time", rset.getString("off_time"));
				 zoneDat.set("random_time", rset.getString("random_time"));
				 zoneDat.set("skill_action_type", rset.getString("skill_action_type"));
				 zoneDat.set("restart_allowed_time", rset.getString("restart_allowed_time"));
				 zoneDat.set("exp_penalty_per", rset.getString("exp_penalty_per"));
				 zoneDat.set("item_drop", rset.getString("item_drop"));
				 zoneDat.set("event_id", rset.getString("event_id"));
				 */

				if(z.getType() == ZoneType.no_landing || z.getType() == ZoneType.Siege || z.getType() == ZoneType.Castle || z.getType() == ZoneType.Fortress || z.getType() == ZoneType.OlympiadStadia)
					z.getListenerEngine().addMethodInvokedListener(_noLandingZoneListener);

				if(z.getType() == ZoneType.monster_trap)
					z.getListenerEngine().addMethodInvokedListener(_monsterTrapZoneListener);

				if(_zonesByType.get(z.getType()) == null)
					_zonesByType.put(z.getType(), new FastList<L2Zone>());

				_zonesByType.get(z.getType()).add(z);

				L2Territory terr = TerritoryTable.getInstance().getLocations().get(rset.getInt("loc_id"));
				if(terr != null)
				{
					if(terr.getZone() != null)
						_log.warning("Zone for territory " + terr.getId() + " already defined");
					z.setLoc(terr);
					terr.setZone(z);
				}
				else
					_log.warning("Not defined territory " + rset.getString("loc_id") + " of " + " " + rset.getString("type") + " " + rset.getString("name"));

				String restart_point = rset.getString("restart_point");
				if(restart_point != null)
				{
					terr = TerritoryTable.getInstance().getLocations().get(Integer.parseInt(restart_point));
					if(terr != null)
					{
						if(terr.getZone() != null)
							_log.warning("Zone for territory " + terr.getId() + " already defined");
						z.setRestartPoints(terr);
					}
					else
						_log.warning("Unknown restart point " + restart_point);
				}

				String PKrestart_point = rset.getString("PKrestart_point");
				if(PKrestart_point != null)
				{
					terr = TerritoryTable.getInstance().getLocations().get(Integer.parseInt(PKrestart_point));
					if(terr != null)
					{
						if(terr.getZone() != null)
							_log.warning("Zone for territory " + terr.getId() + " already defined");
						z.setPKRestartPoints(terr);
					}
					else
						_log.warning("Unknown PK restart point " + PKrestart_point);
				}

				String s = rset.getString("default_status");
				if(s == null)
					z.setActive(true);
				else if(s.equals("on") || z.getType() == ZoneType.water)
					z.setActive(true);

				count++;
			}
		}
		catch(Exception e1)
		{
			_log.warning("zones couldnt be initialized: " + e1);
			e1.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}

		_log.config("ZoneManager: Loaded " + count + " zones");

		TerritoryTable.getInstance().registerZones();
	}

	public void reload()
	{
		loadZone();
	}

	public boolean checkIfInZoneFishing(int x, int y, int z)
	{
		return checkIfInZone(ZoneType.water, x, y, z);
	}

	private class NoLandingZoneListener extends L2ZoneEnterLeaveListener
	{
		@Override
		public void objectEntered(L2Zone zone, L2Object object)
		{
			L2Player player = object.getPlayer();
			if(player != null)
				if(player.isFlying())
				{
					Siege siege = SiegeManager.getSiege(player, false);
					if(siege != null)
					{
						Residence unit = siege.getSiegeUnit();
						if(unit != null && player.getClan() != null && player.isClanLeader() && player.getClan().getHasCastle() == unit.getId())
							return;
					}

					player.stopMove();
					player.sendPacket(new SystemMessage(SystemMessage.THIS_AREA_CANNOT_BE_ENTERED_WHILE_MOUNTED_ATOP_OF_A_WYVERN_YOU_WILL_BE_DISMOUNTED_FROM_YOUR_WYVERN_IF_YOU_DO_NOT_LEAVE));

					Integer enterCount = (Integer) player.getProperty(PropertyCollection.ZoneEnteredNoLandingFlying);
					if(enterCount == null)
						enterCount = 0;

					Location loc = player.getLastServerPosition();
					if(loc == null || enterCount >= 5)
					{
						player.setMount(0, 0, 0);
						player.addProperty(PropertyCollection.ZoneEnteredNoLandingFlying, 0);
						return;
					}

					player.teleToLocation(loc);
					player.addProperty(PropertyCollection.ZoneEnteredNoLandingFlying, enterCount + 1);
				}
				else if(Config.ALT_DONT_ALLOW_PETS_ON_SIEGE && player.getPet() != null)
				{
					int id = player.getPet().getNpcId();
					if(PetDataTable.isBabyPet(id) && SiegeManager.getSiege(player, true) != null)
					{
						player.getPet().unSummon();
						player.sendMessage("These pets are prohibited from using in areas of sieges.");
					}
				}
		}

		@Override
		public void objectLeaved(L2Zone zone, L2Object object)
		{}
	}

	private class MonsterTrapZoneListener extends L2ZoneEnterLeaveListener
	{
		@Override
		public void objectEntered(L2Zone zone, L2Object object)
		{
			try
			{
				L2Player player = object.getPlayer();
				if(player != null && zone.getEvent() != null)
				{
					// Структура: reuse;chance1,id11,id12,id1N;chance2,id221,id22,id2N;chanceM,idM1,idM2,idMN; .....
					String[] params = zone.getEvent().split(";");
					int reuse = Integer.parseInt(params[0]); // В секундах
					if(zone.getLastEventTime() != 0 && zone.getLastEventTime() + reuse * 1000 > System.currentTimeMillis())
						return;
					zone.setLastEventTime(System.currentTimeMillis());
					int[] chances = new int[params.length - 1];
					int[][] groups = new int[params.length - 1][];
					for(int i = 1; i < params.length; i++)
					{
						// Структура: chance,id1,id2,idN
						String[] group = params[i].split(",");
						chances[i - 1] = Integer.parseInt(group[0]);
						int[] mobs = new int[group.length - 1];
						for(int j = 1; j < group.length; j++)
							mobs[j - 1] = Integer.parseInt(group[j]);
						groups[i - 1] = mobs;
					}
					int[] monsters = groups[choose_group(chances)];
					for(int monster : monsters)
					{
						L2NpcTemplate template = NpcTable.getTemplate(monster);
						if(template != null)
						{
							L2Spawn spawn = new L2Spawn(template);
							spawn.setLocation(zone.getLoc().getId());
							spawn.setHeading(-1);
							spawn.setAmount(1);
							spawn.stopRespawn();
							L2NpcInstance mob = spawn.doSpawn(true);
							if(mob != null)
							{
								ThreadPoolManager.getInstance().scheduleAi(new UnSpawnTask(mob), 300000, false);
								if(mob.getAI().isSilentMoveNotVisible(player) && mob.isAggressive())
									mob.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, player, 1);
							}
						}
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		@Override
		public void objectLeaved(L2Zone zone, L2Object object)
		{}
	}

	private int choose_group(int[] chances)
	{
		int sum = 0;

		for(int i = 0; i < chances.length; i++)
			sum += chances[i];

		int[] table = new int[sum];
		int k = 0;

		for(int i = 0; i < chances.length; i++)
			for(int j = 0; j < chances[i]; j++)
			{
				table[k] = i;
				k++;
			}

		return table[Rnd.get(table.length)];
	}

	public class UnSpawnTask implements Runnable
	{
		L2NpcInstance _monster;

		public UnSpawnTask(L2NpcInstance monster)
		{
			_monster = monster;
		}

		@Override
		public void run()
		{
			if(_monster != null)
				_monster.deleteMe();
			_monster = null;
		}
	}
}