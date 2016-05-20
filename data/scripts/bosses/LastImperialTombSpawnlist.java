package bosses;

import java.sql.ResultSet;
import java.util.List;
import java.util.logging.Logger;

import javolution.util.FastList;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import l2d.game.model.L2Spawn;
import l2d.game.tables.NpcTable;
import l2d.game.templates.L2NpcTemplate;

public class LastImperialTombSpawnlist
{
	private static final Logger _log = Logger.getLogger(LastImperialTombSpawnlist.class.getName());

	private static List<L2Spawn> _Room1SpawnList1st = new FastList<L2Spawn>();
	private static List<L2Spawn> _Room1SpawnList2nd = new FastList<L2Spawn>();
	private static List<L2Spawn> _Room1SpawnList3rd = new FastList<L2Spawn>();
	private static List<L2Spawn> _Room1SpawnList4th = new FastList<L2Spawn>();
	private static List<L2Spawn> _Room2InsideSpawnList = new FastList<L2Spawn>();
	private static List<L2Spawn> _Room2OutsideSpawnList = new FastList<L2Spawn>();

	private static LastImperialTombSpawnlist _instance;

	public LastImperialTombSpawnlist()
	{}

	public static LastImperialTombSpawnlist getInstance()
	{
		if(_instance == null)
			_instance = new LastImperialTombSpawnlist();
		return _instance;
	}

	public void fill()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM lastimperialtomb_spawnlist ORDER BY id");
			rset = statement.executeQuery();

			int npcTemplateId;
			L2Spawn spawnDat;
			L2NpcTemplate npcTemplate;

			while(rset.next())
			{
				npcTemplateId = rset.getInt("npc_templateid");
				npcTemplate = NpcTable.getTemplate(npcTemplateId);

				if(npcTemplate != null)
				{
					spawnDat = new L2Spawn(npcTemplate);
					spawnDat.setId(rset.getInt("id"));
					spawnDat.setAmount(rset.getInt("count"));
					spawnDat.setLocx(rset.getInt("locx"));
					spawnDat.setLocy(rset.getInt("locy"));
					spawnDat.setLocz(rset.getInt("locz"));
					spawnDat.setHeading(rset.getInt("heading"));
					spawnDat.setRespawnDelay(rset.getInt("respawn_delay"));

					switch(npcTemplateId)
					{
						case 18328:
						case 18330:
						case 18332:
							_Room1SpawnList1st.add(spawnDat);
							break;

						case 18329:
							_Room1SpawnList2nd.add(spawnDat);
							break;

						case 18333:
							_Room1SpawnList3rd.add(spawnDat);
							break;

						case 18331:
							_Room1SpawnList4th.add(spawnDat);
							break;

						case 18339:
							_Room2InsideSpawnList.add(spawnDat);
							break;

						case 18334:
						case 18335:
						case 18336:
						case 18337:
						case 18338:
							_Room2OutsideSpawnList.add(spawnDat);
							break;
					}
				}
				else
				{
					_log.warning("LastImperialTombSpawnlist: Data missing in NPC table for ID: " + npcTemplateId + ".");
				}
			}
		}
		catch(Exception e)
		{
			_log.warning("LastImperialTombSpawnlist: Spawn could not be initialized: ");
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}

		_log.info("LastImperialTombSpawnlist: " + _Room1SpawnList1st.size() + " Room1 1st Npc Spawn Locations.");
		_log.info("LastImperialTombSpawnlist: " + _Room1SpawnList2nd.size() + " Room1 2nd Npc Spawn Locations.");
		_log.info("LastImperialTombSpawnlist: " + _Room1SpawnList3rd.size() + " Room1 3rd Npc Spawn Locations.");
		_log.info("LastImperialTombSpawnlist: " + _Room1SpawnList4th.size() + " Room1 4th Npc Spawn Locations.");
		_log.info("LastImperialTombSpawnlist: " + _Room2InsideSpawnList.size() + " Room2 Inside Npc Spawn Locations.");
		_log.info("LastImperialTombSpawnlist: " + _Room2OutsideSpawnList.size() + " Room2 Outside Npc Spawn Locations.");
	}

	public void clear()
	{
		_Room1SpawnList1st.clear();
		_Room1SpawnList2nd.clear();
		_Room1SpawnList3rd.clear();
		_Room1SpawnList4th.clear();
		_Room2InsideSpawnList.clear();
		_Room2OutsideSpawnList.clear();
	}

	public List<L2Spawn> getRoom1SpawnList1st()
	{
		return _Room1SpawnList1st;
	}

	public List<L2Spawn> getRoom1SpawnList2nd()
	{
		return _Room1SpawnList2nd;
	}

	public List<L2Spawn> getRoom1SpawnList3rd()
	{
		return _Room1SpawnList3rd;
	}

	public List<L2Spawn> getRoom1SpawnList4th()
	{
		return _Room1SpawnList4th;
	}

	public List<L2Spawn> getRoom2InsideSpawnList()
	{
		return _Room2InsideSpawnList;
	}

	public List<L2Spawn> getRoom2OutsideSpawnList()
	{
		return _Room2OutsideSpawnList;
	}
}
