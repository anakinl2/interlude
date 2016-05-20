package com.lineage.game.model.instances;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javolution.util.FastMap;
import com.lineage.Config;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.clientpackets.Say2C;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.serverpackets.PlaySound;
import com.lineage.game.serverpackets.Say2;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.serverpackets.VehicleCheckLocation;
import com.lineage.game.serverpackets.VehicleInfo;
import com.lineage.game.templates.L2CharTemplate;
import com.lineage.game.templates.L2Weapon;
import com.lineage.util.GArray;
import com.lineage.util.Location;

public class L2BoatInstance extends L2Character
{
	protected static final Logger _logBoat = Logger.getLogger(L2BoatInstance.class.getName());

	public class L2BoatTrajet
	{
		@SuppressWarnings("serial")
		public class L2BoatPoint extends Location implements Serializable
		{
			public L2BoatPoint()
			{
				super(0, 0, 0);
			}

			public int speed1;
			public int speed2;
			public int time;
		}

		public L2BoatTrajet(int idWaypoint1, int idWTicket1, int ntx1, int nty1, int ntz1, String npc1, String sysmess10_1, String sysmess5_1, String sysmess1_1, String sysmess0_1, String sysmessb_1, String boatname)
		{
			_IdWaypoint1 = idWaypoint1;
			_IdWTicket1 = idWTicket1;
			_ntx1 = ntx1;
			_nty1 = nty1;
			_ntz1 = ntz1;
			_npc1 = npc1;
			_sysmess10_1 = sysmess10_1;
			_sysmess5_1 = sysmess5_1;
			_sysmess1_1 = sysmess1_1;
			_sysmessb_1 = sysmessb_1;
			_sysmess0_1 = sysmess0_1;
			_boatname = boatname;
			loadBoatPath();
		}

		public void parseLine(String line)
		{
			// L2BoatPath bp = new L2BoatPath();
			_path = new FastMap<Integer, L2BoatPoint>();
			StringTokenizer st = new StringTokenizer(line, ";");
			Integer.parseInt(st.nextToken());
			_max = Integer.parseInt(st.nextToken());
			for(int i = 0; i < _max; i++)
			{
				L2BoatPoint bp = new L2BoatPoint();
				bp.speed1 = Integer.parseInt(st.nextToken());
				bp.speed2 = Integer.parseInt(st.nextToken());
				bp.x = Integer.parseInt(st.nextToken());
				bp.y = Integer.parseInt(st.nextToken());
				bp.z = Integer.parseInt(st.nextToken());
				bp.time = Integer.parseInt(st.nextToken());
				_path.put(i, bp);
			}
		}

		private void loadBoatPath()
		{
			LineNumberReader lnr = null;
			try
			{
				File doorData = new File(Config.DATAPACK_ROOT, "data/boatpath.csv");
				lnr = new LineNumberReader(new BufferedReader(new FileReader(doorData)));
				String line = null;
				while((line = lnr.readLine()) != null)
				{
					if(line.trim().length() == 0 || !line.startsWith(_IdWaypoint1 + ";"))
						continue;
					parseLine(line);
					return;
				}
				_logBoat.warning("No path for boat " + _boatname + " !!!");
			}
			catch(FileNotFoundException e)
			{
				_logBoat.warning("boatpath.csv is missing in data folder");
			}
			catch(Exception e)
			{
				_logBoat.warning("error while creating boat table " + e);
			}
			finally
			{
				try
				{
					if(lnr != null)
						lnr.close();
				}
				catch(Exception e1)
				{ /* ignore problems */}
			}
		}

		private Map<Integer, L2BoatPoint> _path;
		public int _IdWaypoint1;
		public int _IdWTicket1;
		public int _ntx1;
		public int _nty1;
		public int _ntz1;
		public int _max;
		public String _boatname;
		public String _npc1;
		public String _sysmess10_1;
		public String _sysmess5_1;
		public String _sysmess1_1;
		public String _sysmessb_1;
		public String _sysmess0_1;

		public int state(int state)
		{
			if(state < _max)
			{
				L2BoatPoint bp = _path.get(state);
				double dx = bp.x - getX();
				double dy = bp.y - getY();
				double distance = Math.sqrt(dx * dx + dy * dy);
				double cos = dx / distance;
				double sin = dy / distance;
				int heading = (int) (Math.atan2(-sin, -cos) * 10430.378350470452724949566316381);
				heading += 32768;
				setHeading(heading);
				_speed1 = bp.speed1;
				_speed2 = bp.speed2;
				moveToLocation(bp, 0, false);
				if(bp.time == 0)
					bp.time = 1;
				return bp.time;
			}
			return 0;
		}
	}

	private String _name;
	protected L2BoatTrajet _t1;
	protected L2BoatTrajet _t2;
	protected int _cycle = 0;
	private Map<Integer, L2Player> _inboat;
	private int _speed1;
	private int _speed2;

	public L2BoatInstance(int objectId, L2CharTemplate template, String name)
	{
		super(objectId, template);
		_name = name;
	}

	@Override
	public float getMoveSpeed()
	{
		return _speed1;
	}

	public int getRotationSpeed()
	{
		return _speed2;
	}

	@Override
	public void setXYZ(int x, int y, int z)
	{
		super.setXYZ(x, y, z);
		updatePeopleInTheBoat(new Location(x, y, z));
	}

	class BoatCaptain implements Runnable
	{
		private int _state;

		public BoatCaptain(int i)
		{
			_state = i;
		}

		@Override
		public void run()
		{
			BoatCaptain bc;
			switch(_state)
			{
				case 1:
					say(5);
					bc = new BoatCaptain(2);
					ThreadPoolManager.getInstance().scheduleGeneral(bc, 240000);
					break;
				case 2:
					say(1);
					bc = new BoatCaptain(3);
					ThreadPoolManager.getInstance().scheduleGeneral(bc, 40000);
					break;
				case 3:
					say(0);
					bc = new BoatCaptain(4);
					ThreadPoolManager.getInstance().scheduleGeneral(bc, 20000);
					break;
				case 4:
					say(-1);
					begin();
					break;
			}
		}
	}

	class Boatrun implements Runnable
	{
		private int _state;

		public Boatrun(int i)
		{
			_state = i;
		}

		@Override
		public void run()
		{
			needOnVehicleCheckLocation = false;
			if(_cycle == 1)
			{
				int time = _t1.state(_state);
				// _logBoat.warn("Boat " + _name + " cycle: " + _boat._cycle + " time: " + time);
				if(time > 0)
				{
					_state++;
					ThreadPoolManager.getInstance().scheduleGeneral(new Boatrun(_state), time);
				}
				else if(time == 0)
				{
					_cycle = 2;
					say(10);
					BoatCaptain bc = new BoatCaptain(1);
					ThreadPoolManager.getInstance().scheduleGeneral(bc, 300000);
				}
				else
				{
					needOnVehicleCheckLocation = true;
					_state++;
					_runstate = _state;
				}
			}
			else if(_cycle == 2)
			{
				int time = _t2.state(_state);
				// _logBoat.warn("Boat " + _name + " cycle: " + _boat._cycle + " time: " + time);
				if(time > 0)
				{
					_state++;
					ThreadPoolManager.getInstance().scheduleGeneral(new Boatrun(_state), time);
				}
				else if(time == 0)
				{
					_cycle = 1;
					say(10);
					BoatCaptain bc = new BoatCaptain(1);
					ThreadPoolManager.getInstance().scheduleGeneral(bc, 300000);
				}
				else
				{
					needOnVehicleCheckLocation = true;
					_state++;
					_runstate = _state;
				}
			}
		}
	}

	public int _runstate = 0;

	public void BoatArrived()
	{
		// _logBoat.warn("Boat " + _name + " EvtArrived: runstate = " + _runstate);

		updatePeopleInTheBoat(getLoc());

		if(_runstate == 0)
		{
			// DO nothing :P
		}
		else
		{
			// _runstate++;
			ThreadPoolManager.getInstance().scheduleGeneral(new Boatrun(_runstate), 10);
			_runstate = 0;
		}
	}

	public void beginCycle()
	{
		say(10);
		BoatCaptain bc = new BoatCaptain(1);
		ThreadPoolManager.getInstance().scheduleGeneral(bc, 300000);
	}

	private int lastx = -1;
	private int lasty = -1;
	protected boolean needOnVehicleCheckLocation = false;

	public void updatePeopleInTheBoat(Location loc)
	{
		if(_inboat != null)
		{
			boolean check = false;
			if(lastx == -1 || lasty == -1)
			{
				check = true;
				lastx = loc.x;
				lasty = loc.y;
			}
			else if((loc.x - lastx) * (loc.x - lastx) + (loc.y - lasty) * (loc.y - lasty) > 2250000) // 1500 * 1500 = 2250000
			{
				check = true;
				lastx = loc.x;
				lasty = loc.y;
			}
			for(int i = 0; i < _inboat.size(); i++)
			{
				L2Player player = _inboat.get(i);
				if(player == null)
					continue;
				if(player.isInBoat() && player.getBoat() == this)
					player.setXYZ(loc.x, loc.y, loc.z);
				if(check && needOnVehicleCheckLocation)
					player.sendPacket(new VehicleCheckLocation(this));
			}
		}
	}

	public void begin()
	{
		if(_cycle == 1)
		{
			Collection<L2Player> knownPlayers = L2World.getAroundPlayers(this);
			if(knownPlayers != null && !knownPlayers.isEmpty())
			{
				_inboat = new FastMap<Integer, L2Player>();
				int i = 0;
				for(L2Player player : knownPlayers)
					if(player != null && player.isInBoat())
					{
						L2ItemInstance it;
						it = player.getInventory().getItemByItemId(_t1._IdWTicket1);
						if(it != null && it.getCount() >= 1)
						{
							player.getInventory().destroyItem(it.getObjectId(), 1, false);
							_inboat.put(i, player);
							i++;
						}
						else if(it == null && _t1._IdWTicket1 == 0)
						{
							_inboat.put(i, player);
							i++;
						}
						else
						{
							player.sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_GET_ON_BOARD_WITHOUT_A_PASS));
							player.teleToLocation(_t1._ntx1, _t1._nty1, _t1._ntz1);
						}
					}
			}
			ThreadPoolManager.getInstance().scheduleGeneral(new Boatrun(0), 0);
		}
		else if(_cycle == 2)
		{
			Collection<L2Player> knownPlayers = L2World.getAroundPlayers(this);
			if(knownPlayers != null && !knownPlayers.isEmpty())
			{
				_inboat = new FastMap<Integer, L2Player>();
				int i = 0;
				for(L2Player player : knownPlayers)
					if(player != null && player.isInBoat())
					{
						L2ItemInstance it;
						it = player.getInventory().getItemByItemId(_t2._IdWTicket1);
						if(it != null && it.getCount() >= 1)
						{
							player.getInventory().destroyItem(it.getObjectId(), 1, false);
							_inboat.put(i, player);
							i++;
						}
						else if(it == null && _t2._IdWTicket1 == 0)
						{
							_inboat.put(i, player);
							i++;
						}
						else
						{
							player.sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_GET_ON_BOARD_WITHOUT_A_PASS));
							player.teleToLocation(_t2._ntx1, _t2._nty1, _t2._ntz1);
						}
					}
			}
			ThreadPoolManager.getInstance().scheduleGeneral(new Boatrun(0), 0);
		}
	}

	public void say(int i)
	{
		Collection<L2Player> knownPlayers = L2World.getAroundPlayers(this);
		Say2 sm;
		PlaySound ps;
		switch(i)
		{
			case 10:
				if(_cycle == 1)
					sm = new Say2(0, Say2C.SHOUT, _t1._npc1, _t1._sysmess10_1);
				else
					sm = new Say2(0, Say2C.SHOUT, _t2._npc1, _t2._sysmess10_1);
				ps = new PlaySound(0, "itemsound.ship_arrival_departure", 1, getObjectId(), getLoc());
				if(knownPlayers == null || knownPlayers.isEmpty())
					return;
				for(L2Player player : knownPlayers)
				{
					player.sendPacket(sm);
					player.sendPacket(ps);
				}
				break;
			case 5:
				if(_cycle == 1)
					sm = new Say2(0, Say2C.SHOUT, _t1._npc1, _t1._sysmess5_1);
				else
					sm = new Say2(0, Say2C.SHOUT, _t2._npc1, _t2._sysmess5_1);
				ps = new PlaySound(0, "itemsound.ship_5min", 1, getObjectId(), getLoc());
				if(knownPlayers == null || knownPlayers.isEmpty())
					return;
				for(L2Player player : knownPlayers)
				{
					player.sendPacket(sm);
					player.sendPacket(ps);
				}
				break;
			case 1:
				if(_cycle == 1)
					sm = new Say2(0, Say2C.SHOUT, _t1._npc1, _t1._sysmess1_1);
				else
					sm = new Say2(0, Say2C.SHOUT, _t2._npc1, _t2._sysmess1_1);
				ps = new PlaySound(0, "itemsound.ship_1min", 1, getObjectId(), getLoc());
				if(knownPlayers == null || knownPlayers.isEmpty())
					return;
				for(L2Player player : knownPlayers)
				{
					player.sendPacket(sm);
					player.sendPacket(ps);
				}
				break;
			case 0:
				if(_cycle == 1)
					sm = new Say2(0, Say2C.SHOUT, _t1._npc1, _t1._sysmess0_1);
				else
					sm = new Say2(0, Say2C.SHOUT, _t2._npc1, _t2._sysmess0_1);
				if(knownPlayers == null || knownPlayers.isEmpty())
					return;
				for(L2Player player : knownPlayers)
					player.sendPacket(sm);
				break;
			case -1:
				if(_cycle == 1)
					sm = new Say2(0, Say2C.SHOUT, _t1._npc1, _t1._sysmessb_1);
				else
					sm = new Say2(0, Say2C.SHOUT, _t2._npc1, _t2._sysmessb_1);
				ps = new PlaySound(0, "itemsound.ship_arrival_departure", 1, getObjectId(), getLoc());
				for(L2Player player : knownPlayers)
				{
					player.sendPacket(sm);
					player.sendPacket(ps);
				}
				break;
		}
	}

	public void spawn()
	{
		GArray<L2Player> knownPlayers = L2World.getAroundPlayers(this);
		_cycle = 1;
		beginCycle();
		if(knownPlayers.isEmpty())
			return;
		VehicleInfo vi = new VehicleInfo(this);
		for(L2Player player : knownPlayers)
			player.sendPacket(vi);
	}

	public void SetTrajet1(int idWaypoint1, int idWTicket1, int ntx1, int nty1, int ntz1, String idnpc1, String sysmess10_1, String sysmess5_1, String sysmess1_1, String sysmess0_1, String sysmessb_1)
	{
		_t1 = new L2BoatTrajet(idWaypoint1, idWTicket1, ntx1, nty1, ntz1, idnpc1, sysmess10_1, sysmess5_1, sysmess1_1, sysmess0_1, sysmessb_1, _name);
	}

	public void SetTrajet2(int idWaypoint1, int idWTicket1, int ntx1, int nty1, int ntz1, String idnpc1, String sysmess10_1, String sysmess5_1, String sysmess1_1, String sysmess0_1, String sysmessb_1)
	{
		_t2 = new L2BoatTrajet(idWaypoint1, idWTicket1, ntx1, nty1, ntz1, idnpc1, sysmess10_1, sysmess5_1, sysmess1_1, sysmess0_1, sysmessb_1, _name);
	}

	@Override
	public void updateAbnormalEffect()
	{}

	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}

	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}

	@Override
	public byte getLevel()
	{
		return 0;
	}

	@Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}

	@Override
	public boolean isAttackable()
	{
		return false;
	}
}