package l2d.game.tables;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

import javolution.util.FastMap;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;

/**
 * 
 * @author Midnex
 *
 */
public class EpicRespawnTimesHolder
{
	FastMap<Integer, String> holder = new FastMap<Integer, String>();
	private static EpicRespawnTimesHolder _instance;
	
	public void update()
	{
		holder.clear();
		holder = new FastMap<Integer, String>();
		getRespawn(false,29001);//Queen Ant
		getRespawn(false,29006);//Core
		getRespawn(false,29014);//Orfen
		getRespawn(false,29022);//Zaken
		
		getRespawn(true,29020);//baium
		getRespawn(true,29019);//antharas
		getRespawn(true,29045);//frintezza
		getRespawn(true,29028);//valakas
	}
	
	public void getRespawn(boolean fromepicbossSpawn , int boss)
	{
		String queryRow="",query="";

		if(fromepicbossSpawn)
		{
			queryRow="respawnDate";
		  query="SELECT * FROM epic_boss_spawn WHERE bossId = ? LIMIT 1";
		}
		else
		{
			queryRow="respawn_delay";
		  query="SELECT * FROM raidboss_status WHERE id = ? LIMIT 1";
		}
		
		
		long respawn = 0;
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(query);
			statement.setInt(1, boss);
			rset = statement.executeQuery();
			if(rset.next())
				respawn = rset.getLong(queryRow);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
		
		Date dt = new Date(respawn*1000L);
		SimpleDateFormat s = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		String stringDate = s.format(dt);
		holder.put(boss, (respawn*1000L) < System.currentTimeMillis() ? "<font color=\"39ba41\">Epic is Alive</font>" : "<font color=\"bd4539\">Respawn at "+stringDate+"</font>");
	}
	
	public String getStatus(int request_npc)
	{
		int boss = 0;
		switch(request_npc)
		{
			case 11:
				boss = 29001;
				break;
			case 12:
				boss = 29006;
				break;
			case 13:
				boss = 29014;
				break;
			case 14:
				boss = 29022;
				break;
			case 15:
				boss = 29020;
				break;
			case 16:
				boss = 29019;
				break;
			case 17:
				boss = 29028;
				break;
			case 18:
				boss = 29045;
				break;
		}		
		
		if(holder.containsKey(boss))
			return holder.get(boss);
		return "ERR0R";
	}
	
	public static EpicRespawnTimesHolder getInstance()
	{
		if(_instance == null)
			_instance = new EpicRespawnTimesHolder();
		return _instance;
	}
}
