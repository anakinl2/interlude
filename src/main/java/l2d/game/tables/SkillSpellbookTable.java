package l2d.game.tables;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;

@SuppressWarnings({ "nls", "unqualified-field-access", "boxing" })
public class SkillSpellbookTable
{
	private static Logger _log = Logger.getLogger(SkillTreeTable.class.getName());
	private static final SkillSpellbookTable _instance = new SkillSpellbookTable();

	public static HashMap<Integer, Integer> _skillSpellbooks;
	public static HashMap<Integer, ArrayList<Integer>> _spellbookHandlers;

	public static HashMap<Integer, Integer> getSkillSpellbooks()
	{
		return _skillSpellbooks;
	}

	public static HashMap<Integer, ArrayList<Integer>> getSpellbookHandlers()
	{
		return _spellbookHandlers;
	}

	public static SkillSpellbookTable getInstance()
	{
		return _instance;
	}

	private SkillSpellbookTable()
	{
		_skillSpellbooks = new HashMap<Integer, Integer>();
		_spellbookHandlers = new HashMap<Integer, ArrayList<Integer>>();
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet spbooks = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT skill_id, level, item_id, item_count FROM skill_spellbooks" + (Config.ALT_DISABLE_SPELLBOOKS ? " WHERE item_count = -1" : ""));
			spbooks = statement.executeQuery();

			while(spbooks.next())
			{
				int skill_id = spbooks.getInt("skill_id");
				int level = spbooks.getInt("level");
				int item_id = spbooks.getInt("item_id");
				_skillSpellbooks.put(hashCode(new int[] { skill_id, level }), item_id);
				if(spbooks.getInt("item_count") == -1)
				{
					ArrayList<Integer> list = _spellbookHandlers.get(item_id);
					if(list == null)
						list = new ArrayList<Integer>();
					list.add(skill_id);
					_spellbookHandlers.put(item_id, list);
				}
			}

			_log.config("[ Skill Spellbook Table ]");
			_log.config(" ~ Loaded: " + _skillSpellbooks.size() + " Spellbooks.");
			_log.config("[ Skill Spellbook Table ]\n");
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "error while loading spellbooks	", e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, spbooks);
		}
	}

	public static int hashCode(int a[])
	{
		if(a == null)
			return 0;

		return a[1] + (a[0] << 16);
	}

	public static void unload()
	{
		_skillSpellbooks.clear();
		_spellbookHandlers.clear();
	}
}