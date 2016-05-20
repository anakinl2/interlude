package com.lineage.game.instancemanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.lineage.game.idfactory.IdFactory;
import javolution.util.FastMap;
import com.lineage.Config;
import com.lineage.game.model.instances.L2BoatInstance;
import com.lineage.game.templates.L2CharTemplate;
import com.lineage.game.templates.StatsSet;

public class BoatManager
{
	private static final Logger _log = Logger.getLogger(BoatManager.class.getName());

	private static BoatManager _instance;

	public static BoatManager getInstance()
	{
		if(_instance == null)
		{
			_log.info("Initializing BoatManager");
			_instance = new BoatManager();
			_instance.load();
		}
		return _instance;
	}

	private Map<Integer, L2BoatInstance> _staticItems = new FastMap<Integer, L2BoatInstance>();

	private void load()
	{
		if(!Config.ALLOW_BOAT)
			return;

		if(_staticItems == null)
			_staticItems = new FastMap<Integer, L2BoatInstance>();
		LineNumberReader lnr = null;

		String filename;
		filename = "data/boat.csv";
		try
		{
			final File doorData = new File(Config.DATAPACK_ROOT, filename);
			lnr = new LineNumberReader(new BufferedReader(new FileReader(doorData)));

			String line = null;
			while((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
					continue;
				final L2BoatInstance boat = parseLine(line);
				boat.spawn();
				_staticItems.put(boat.getObjectId(), boat);
				if(Config.DEBUG)
					_log.info("Boat ID : " + boat.getObjectId());
			}
		}
		catch(final FileNotFoundException e)
		{
			_log.warning("boat.csv is missing in data folder");
		}
		catch(final Exception e)
		{
			_log.warning("error while creating boat table " + e);
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(lnr != null)
					lnr.close();
			}
			catch(final Exception e1)
			{}
		}
	}

	private L2BoatInstance parseLine(final String line)
	{
		L2BoatInstance boat;
		final StringTokenizer st = new StringTokenizer(line, ";");

		final String name = st.nextToken();
		final int id = Integer.parseInt(st.nextToken());
		final int xspawn = Integer.parseInt(st.nextToken());
		final int yspawn = Integer.parseInt(st.nextToken());
		final int zspawn = Integer.parseInt(st.nextToken());
		final int heading = Integer.parseInt(st.nextToken());

		final StatsSet npcDat = new StatsSet();
		npcDat.set("npcId", id);
		npcDat.set("level", 0);
		npcDat.set("jClass", "boat");

		npcDat.set("baseSTR", 0);
		npcDat.set("baseCON", 0);
		npcDat.set("baseDEX", 0);
		npcDat.set("baseINT", 0);
		npcDat.set("baseWIT", 0);
		npcDat.set("baseMEN", 0);

		npcDat.set("baseShldDef", 0);
		npcDat.set("baseShldRate", 0);
		npcDat.set("baseAccCombat", 38);
		npcDat.set("baseEvasRate", 38);
		npcDat.set("baseCritRate", 38);

		npcDat.set("collision_radius", 0);
		npcDat.set("collision_height", 0);
		npcDat.set("sex", "male");
		npcDat.set("type", "");
		npcDat.set("baseAtkRange", 0);
		npcDat.set("baseMpMax", 0);
		npcDat.set("baseCpMax", 0);
		npcDat.set("revardExp", 0);
		npcDat.set("revardSp", 0);
		npcDat.set("basePAtk", 0);
		npcDat.set("baseMAtk", 0);
		npcDat.set("basePAtkSpd", 0);
		npcDat.set("aggroRange", 0);
		npcDat.set("baseMAtkSpd", 0);
		npcDat.set("rhand", 0);
		npcDat.set("lhand", 0);
		npcDat.set("armor", 0);
		npcDat.set("baseWalkSpd", 0);
		npcDat.set("baseRunSpd", 0);
		npcDat.set("name", name);
		npcDat.set("baseHpMax", 50000);
		npcDat.set("baseHpReg", 3.e-3f);
		npcDat.set("baseCpReg", 0);
		npcDat.set("baseMpReg", 3.e-3f);
		npcDat.set("basePDef", 100);
		npcDat.set("baseMDef", 100);
		final L2CharTemplate template = new L2CharTemplate(npcDat);
		boat = new L2BoatInstance(IdFactory.getInstance().getNextId(), template, name);
		boat.setHeading(heading);
		boat.setXYZ(xspawn, yspawn, zspawn);

		int IdWaypoint1 = Integer.parseInt(st.nextToken());
		int IdWTicket1 = Integer.parseInt(st.nextToken());
		int ntx1 = Integer.parseInt(st.nextToken());
		int nty1 = Integer.parseInt(st.nextToken());
		int ntz1 = Integer.parseInt(st.nextToken());
		String npc1 = st.nextToken();
		String mess10_1 = st.nextToken();
		String mess5_1 = st.nextToken();
		String mess1_1 = st.nextToken();
		String mess0_1 = st.nextToken();
		String messb_1 = st.nextToken();
		boat.SetTrajet1(IdWaypoint1, IdWTicket1, ntx1, nty1, ntz1, npc1, mess10_1, mess5_1, mess1_1, mess0_1, messb_1);
		IdWaypoint1 = Integer.parseInt(st.nextToken());
		IdWTicket1 = Integer.parseInt(st.nextToken());
		ntx1 = Integer.parseInt(st.nextToken());
		nty1 = Integer.parseInt(st.nextToken());
		ntz1 = Integer.parseInt(st.nextToken());
		npc1 = st.nextToken();
		mess10_1 = st.nextToken();
		mess5_1 = st.nextToken();
		mess1_1 = st.nextToken();
		mess0_1 = st.nextToken();
		messb_1 = st.nextToken();
		boat.SetTrajet2(IdWaypoint1, IdWTicket1, ntx1, nty1, ntz1, npc1, mess10_1, mess5_1, mess1_1, mess0_1, messb_1);
		return boat;
	}

	public L2BoatInstance GetBoat(final int boatId)
	{
		if(_staticItems == null)
			_staticItems = new FastMap<Integer, L2BoatInstance>();
		return _staticItems.get(boatId);
	}

	public void addStaticItem(final L2BoatInstance boat)
	{
		_staticItems.put(boat.getObjectId(), boat);
	}
}