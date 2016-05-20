package l2d.game.tables;

import java.sql.ResultSet;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javolution.util.FastMap;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import l2d.game.model.L2ArmorSet;
import l2d.game.model.L2Skill;

public class ArmorSetsTable
{
	private static Logger _log = Logger.getLogger(ArmorSetsTable.class.getName());
	private static ArmorSetsTable _instance;
	private boolean _initialized = true;

	private FastMap<Integer, L2ArmorSet> _armorSets;

	public static ArmorSetsTable getInstance()
	{
		if(_instance == null)
			_instance = new ArmorSetsTable();
		return _instance;
	}

	private ArmorSetsTable()
	{
		_armorSets = new FastMap<Integer, L2ArmorSet>();
		loadData();
	}

	private void loadData()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT chest, legs, head, gloves, feet, skill, shield, shield_skill, enchant6skill FROM armorsets");
			rset = statement.executeQuery();
			int count = 0;

			while(rset.next())
			{
				final int chest = rset.getInt("chest");
				final int legs = rset.getInt("legs");
				final int head = rset.getInt("head");
				final int gloves = rset.getInt("gloves");
				final int feet = rset.getInt("feet");

				L2Skill skill = null;
				StringTokenizer st = new StringTokenizer(rset.getString("skill"), ";");
				if(st.hasMoreTokens())
					skill = SkillTable.getInstance().getInfo(Integer.valueOf(st.nextToken()), Integer.valueOf(st.nextToken()));

				final int shield = rset.getInt("shield");

				L2Skill shield_skill = null;
				st = new StringTokenizer(rset.getString("shield_skill"), ";");
				if(st.hasMoreTokens())
					shield_skill = SkillTable.getInstance().getInfo(Integer.valueOf(st.nextToken()), Integer.valueOf(st.nextToken()));

				L2Skill enchant6skill = null;
				st = new StringTokenizer(rset.getString("enchant6skill"), ";");
				if(st.hasMoreTokens())
					enchant6skill = SkillTable.getInstance().getInfo(Integer.valueOf(st.nextToken()), Integer.valueOf(st.nextToken()));

				L2ArmorSet set;
				set = new L2ArmorSet(chest, legs, head, gloves, feet, skill, shield, shield_skill, enchant6skill);
				_armorSets.put(chest, set);
				count++;
			}
			_log.info("[ Armor Sets Table ]");
			_log.info(" ~ Loaded: " + count + " armor sets.");
			_log.info("[ Armor Sets Table ]\n");
		}
		catch(final Exception e)
		{
			_log.warning(" ~ Error: reading ArmorSets table: " + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	public boolean setExists(final int chestId)
	{
		return _armorSets.containsKey(chestId);
	}

	public L2ArmorSet getSet(final int chestId)
	{
		return _armorSets.get(chestId);
	}

	public boolean isInitialized()
	{
		return _initialized;
	}

	public static void unload()
	{
		if(_instance != null)
			_instance = null;
	}
}