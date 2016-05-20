package com.lineage.game.communitybbs.Manager;

import java.sql.ResultSet;

import javolution.util.FastMap;

import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.ExHeroList;
import com.lineage.util.Files;
import com.lineage.util.GArray;

/**
 * 130 players per page ^^
 * @author Midnex
 *
 */
public class TopBBSManager extends BaseBBSManager
{
	private FastMap<Integer,GArray<rankers>> _top = new FastMap<Integer,GArray<rankers>>();
	private GArray<rankers> _top_all = new GArray<rankers>();
	private GArray<rankers> _top_pk = new GArray<rankers>();
	private GArray<rankers> _top_clan = new GArray<rankers>();
	
	private long lastUpdate = 0;
	private long lastUpdate1 = 0;
	private long lastUpdate2 = 0;
	private long lastUpdate3 = 0;
	
	public void showTopPage(L2Player activeChar, String page, String subcontent)
	{
		if(page == null || page.isEmpty())
			page = "index";
		else
			page = page.replace("../", "").replace("..\\", "");

		page = Config.COMMUNITYBOARD_HTML_ROOT + page + ".htm";

		String content = Files.read(page, activeChar);
		if(content == null)
			if(subcontent == null)
				content = "<html><body><br><br><center>404 Not Found: " + page + "</center></body></html>";
			else
				content = "<html><body>%content%</body></html>";
		if(subcontent != null)
			content = content.replace("%content%", subcontent);
		separateAndSend(content, activeChar);
	}

	@Override
	public void parsecmd(String command, L2Player activeChar)
	{
		if(command.equals("_bbstop_PvP"))
		{
			String content = Files.read(Config.COMMUNITYBOARD_HTML_ROOT +"index.htm", activeChar);
			separateAndSend(content, activeChar);
			showTopAllByPvP(activeChar);
		}
		else if(command.equals("_bbstop_PK"))
		{
			String content = Files.read(Config.COMMUNITYBOARD_HTML_ROOT +"index.htm", activeChar);
			separateAndSend(content, activeChar);
			showTopAllPk(activeChar);
		}
		else if(command.equals("_bbstop_CLAN"))
		{
			String content = Files.read(Config.COMMUNITYBOARD_HTML_ROOT +"index.htm", activeChar);
			separateAndSend(content, activeChar);
			showTopClan(activeChar);
		}		
		else if(command.equals("_bbstop") || command.equals("_bbshome"))
			showTopPage(activeChar, "index", null);
		else if(command.startsWith("_bbstop;"))
			showTopPage(activeChar, command.replaceFirst("_bbstop;", ""), null);
		else
			separateAndSend("<html><body><br><br><center>the command: " + command + " is not implemented yet</center><br><br></body></html>", activeChar);
	}
	
	public void showTopbyClass(L2Player activeChar,int clas)
	{
		updateTopsByClass();
		activeChar.sendPacket(new ExHeroList(_top, clas == -1 ? activeChar.getActiveClassId() : clas));
	}
	
	public void showTopAllByPvP(L2Player activeChar)
	{
		updateAllTops();
		activeChar.sendPacket(new ExHeroList(_top_all));
	}
	
	public void showTopAllPk(L2Player activeChar)
	{
		updateTopsPK();
		activeChar.sendPacket(new ExHeroList(_top_pk));
	}
	
	public void showTopClan(L2Player activeChar)
	{
		updateTopsClan();
		activeChar.sendPacket(new ExHeroList(_top_clan));
	}

	
	public void updateTopsByClass()
	{
		if(lastUpdate + 60000 > System.currentTimeMillis())
			return;
		_top = new FastMap<Integer,GArray<rankers>>();

		for(int i = 88; i != 137; i++)
		{
			ThreadConnection con = null;
			FiltredStatement statement = null;
			ResultSet rset = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.createStatement();
				rset = statement.executeQuery("SELECT char_name,class_id,clan_name,clan_id,ally_name,clan_data.ally_id,pvp FROM characters LEFT JOIN `clan_data` ON (`clanid` = `clan_id`) LEFT JOIN `character_subclasses` ON (`Obj_id` = `char_obj_id`) LEFT JOIN `ally_data` ON (clan_data.ally_id = ally_data.ally_id) WHERE class_id = "+i+" ORDER BY pvp DESC limit 100");
				while(rset.next())
				{
					rankers r = new rankers();
					r.name = rset.getString("char_name");
					r.class_id = rset.getInt("class_id");
					r.clan_name = rset.getString("clan_name");
					r.clan_id = rset.getInt("clan_id");
					r.ally_name = rset.getString("ally_name");
					r.ally_id = rset.getInt("ally_id");
					r.kills = rset.getInt("pvp");

					if(!_top.containsKey(r.class_id))
						_top.put(r.class_id, new GArray<rankers>());
					_top.get(r.class_id).add(r);
				}
			}
			catch(Exception e)
			{}
			finally
			{
				DatabaseUtils.closeDatabaseCS(con, statement);
			}
		}
		lastUpdate = System.currentTimeMillis();
	}
	
	public void updateAllTops()
	{
		if(lastUpdate3 + 60000 > System.currentTimeMillis())
			return;
		_top_all = new GArray<rankers>();
		ThreadConnection con = null;
		FiltredStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			rset = statement.executeQuery("SELECT char_name,class_id,clan_name,clan_id,ally_name,clan_data.ally_id,pvpkills FROM characters LEFT JOIN `clan_data` ON (`clanid` = `clan_id`) LEFT JOIN `character_subclasses` ON (`Obj_id` = `char_obj_id`) LEFT JOIN `ally_data` ON (clan_data.ally_id = ally_data.ally_id) WHERE isBase=1 ORDER BY pvpkills DESC limit 100");
			while(rset.next())
			{
				rankers r = new rankers();
				r.name = rset.getString("char_name");
				r.class_id = rset.getInt("class_id");
				r.clan_name = rset.getString("clan_name");
				r.clan_id = rset.getInt("clan_id");
				r.ally_name = rset.getString("ally_name");
				r.ally_id = rset.getInt("ally_id");
				r.kills = rset.getInt("pvpkills");
				_top_all.add(r);
			}
		}
		catch(Exception e)
		{}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
		lastUpdate3 = System.currentTimeMillis();
	}
	
	public void updateTopsPK()
	{
		if(lastUpdate1 + 60000 > System.currentTimeMillis())
			return;
		
		_top_pk = new GArray<rankers>();
		
		ThreadConnection con = null;
		FiltredStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			rset = statement.executeQuery("SELECT char_name,class_id,clan_name,clan_id,ally_name,clan_data.ally_id,pkkills FROM characters LEFT JOIN `clan_data` ON (`clanid` = `clan_id`) LEFT JOIN `character_subclasses` ON (`Obj_id` = `char_obj_id`) LEFT JOIN `ally_data` ON (clan_data.ally_id = ally_data.ally_id) WHERE isBase=1 ORDER BY pkkills DESC limit 100");
			while(rset.next())
			{
				rankers r = new rankers();
				r.name = rset.getString("char_name");
				r.class_id = rset.getInt("class_id");
				r.clan_name = rset.getString("clan_name");
				r.clan_id = rset.getInt("clan_id");
				r.ally_name = rset.getString("ally_name");
				r.ally_id = rset.getInt("ally_id");
				r.kills = rset.getInt("pkkills");
				_top_pk.add(r);
			}
		}
		catch(Exception e)
		{}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
		lastUpdate1 = System.currentTimeMillis();
	}

	public void updateTopsClan()
	{
		if(lastUpdate2 + 60000 > System.currentTimeMillis())
			return;
		
		_top_clan = new GArray<rankers>();		
		ThreadConnection con = null;
		FiltredStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			rset = statement.executeQuery("SELECT char_name,class_id,clan_name,clan_id,ally_name,clan_data.ally_id,reputation_score FROM clan_data LEFT JOIN `characters` ON (`obj_Id` = `leader_id`) LEFT JOIN `character_subclasses` ON (`leader_id` = `char_obj_id`) LEFT JOIN `ally_data` ON (clan_data.ally_id = ally_data.ally_id) WHERE isBase=1 ORDER BY reputation_score DESC limit 100");

			while(rset.next())
			{
				rankers r = new rankers();
				r.name = rset.getString("char_name");
				r.class_id = rset.getInt("class_id");
				r.clan_name = rset.getString("clan_name");
				r.clan_id = rset.getInt("clan_id");
				r.ally_name = rset.getString("ally_name");
				r.ally_id = rset.getInt("ally_id");
				r.kills = rset.getInt("reputation_score");
				_top_clan.add(r);
			}
		}
		catch(Exception e)
		{}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
		lastUpdate2 = System.currentTimeMillis();
	}



	
	public static class rankers
	{
		public String name;
		public int class_id;
		public String clan_name;
		public int clan_id;
		public String ally_name;
		public int ally_id;
		public int kills;
	}

	@Override
	public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2Player activeChar)
	{}

	private static TopBBSManager _Instance = new TopBBSManager();

	public static TopBBSManager getInstance()
	{
		return _Instance;
	}
}