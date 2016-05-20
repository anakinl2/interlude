package l2d.util;

import java.sql.ResultSet;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import l2d.Config;
import l2d.db.DatabaseUtils;
import l2d.db.FiltredPreparedStatement;
import l2d.db.L2DatabaseFactory;
import l2d.db.ThreadConnection;
import l2d.db.mysql;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;

public class HWID
{
	private static final Logger _log = Logger.getLogger(HWID.class.getName());
	private static final FastList<String> banned_hwids = new FastList<String>();
	private static final FastMap<String, FastMap<String, Integer>> bonus_hwids = new FastMap<String, FastMap<String, Integer>>();

	public static void reloadBannedHWIDs()
	{
		banned_hwids.clear();

		ThreadConnection con = null;
		FiltredPreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			st = con.prepareStatement("SELECT HWID FROM hwid_bans");
			rs = st.executeQuery();
			while(rs.next())
				banned_hwids.add(rs.getString("HWID").toLowerCase());

			_log.info("[Protection] Loaded " + banned_hwids.size() + " banned HWIDs");
		}
		catch(Exception e)
		{
			_log.info("[Protection] Failed to load banned HWIDs");
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, st, rs);
		}
	}

	public static void reloadBonusHWIDs()
	{
		mysql.set("DELETE FROM `hwid_bonus` WHERE UNIX_TIMESTAMP(`time`) < UNIX_TIMESTAMP()-60*60*24*" + Config.SERVICES_WINDOW_DAYS);

		bonus_hwids.clear();

		ThreadConnection con = null;
		FiltredPreparedStatement st = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			st = con.prepareStatement("SELECT * FROM hwid_bonus");
			rs = st.executeQuery();
			while(rs.next())
			{
				FastMap<String, Integer> bonus = new FastMap<String, Integer>();
				bonus.put(rs.getString("type"), rs.getInt("value"));
				bonus_hwids.put(rs.getString("HWID").toLowerCase(), bonus);
			}

			_log.info("[Protection] Loaded " + bonus_hwids.size() + " bonus HWIDs");
		}
		catch(Exception e)
		{
			_log.info("[Protection] Failed to load bonus HWIDs");
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, st, rs);
		}
	}

	public static boolean checkHWIDBanned(String hwid)
	{
		return hwid == null || hwid.isEmpty() ? false : banned_hwids.contains(hwid.toLowerCase());
	}

	public static String handleBanHWID(String[] argv)
	{
		if(!Config.PROTECT_ENABLE || !Config.PROTECT_GS_ENABLE_HWID_BANS)
			return "HWID bans feature disabled";

		if(argv == null || argv.length < 2)
			return "USAGE: banhwid char_name|hwid [kick:true|false] [reason]";

		String hwid = argv[1]; // либо HWID, либо имя чара
		if(hwid.length() != 32)
		{
			L2Player plyr = L2World.getPlayer(hwid);
			if(plyr == null)
				return "Player " + hwid + " not found in world";
			if(plyr.getNetConnection() == null)
				return "Player " + hwid + " not connected (offline trade)";
			hwid = plyr.getNetConnection().HWID;
		}
		boolean kick = argv.length > 2 ? Boolean.parseBoolean(argv[2]) : true;
		String reason = argv.length > 3 ? argv[3] : "";
		BanHWID(hwid, reason, kick);
		return "HWID " + hwid + " banned";
	}

	public static boolean BanHWID(String hwid, String comment)
	{
		return BanHWID(hwid, comment, false);
	}

	public static boolean BanHWID(String hwid, String comment, boolean kick)
	{
		if(!Config.PROTECT_ENABLE || !Config.PROTECT_GS_ENABLE_HWID_BANS || hwid == null || hwid.isEmpty())
			return false;

		hwid = hwid.toLowerCase();

		if(checkHWIDBanned(hwid))
		{
			_log.info("[Protection] HWID: " + hwid + " already banned");
			return true;
		}

		ThreadConnection con = null;
		FiltredPreparedStatement st = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			st = con.prepareStatement("REPLACE INTO hwid_bans (HWID,comment) VALUES (?,?)");
			st.setString(1, hwid);
			st.setString(2, comment);
			st.execute();

			banned_hwids.add(hwid);
			Log.add("Banned HWID: " + hwid, "protect");

			if(kick)
				for(L2Player cha : getPlayersByHWID(hwid))
					cha.logout(false, false, true);
		}
		catch(Exception e)
		{
			_log.info("[Protection] Failed to ban HWID: " + hwid);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, st);
		}

		return checkHWIDBanned(hwid);
	}

	public static boolean UnbanHWID(String hwid)
	{
		if(!Config.PROTECT_ENABLE || !Config.PROTECT_GS_ENABLE_HWID_BANS || hwid == null || hwid.isEmpty())
			return false;

		hwid = hwid.toLowerCase();

		if(!checkHWIDBanned(hwid))
		{
			_log.info("[Protection] HWID: " + hwid + " already not banned");
			return true;
		}

		ThreadConnection con = null;
		FiltredPreparedStatement st = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			st = con.prepareStatement("DELETE FROM hwid_bans WHERE HWID=?");
			st.setString(1, hwid);
			st.execute();

			banned_hwids.remove(hwid);
			Log.add("Unbanned HWID: " + hwid, "protect");
		}
		catch(Exception e)
		{
			_log.info("[Protection] Failed to unban HWID: " + hwid);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, st);
		}
		return !checkHWIDBanned(hwid);
	}

	public static FastList<L2Player> getPlayersByHWID(final String hwid)
	{
		FastList<L2Player> result = new FastList<L2Player>();
		if(hwid != null)
			for(L2Player cha : L2World.getAllPlayers())
				if(cha != null && !cha.isInOfflineMode() && cha.getNetConnection() != null && cha.getNetConnection().protect_used && hwid.equalsIgnoreCase(cha.getNetConnection().HWID))
					result.add(cha);
		return result;
	}

	public static void setBonus(String hwid, String type, int value)
	{
		if(!Config.PROTECT_ENABLE || !Config.PROTECT_GS_ENABLE_HWID_BONUS || hwid == null || hwid.isEmpty())
			return;

		hwid = hwid.toLowerCase();

		ThreadConnection con = null;
		FiltredPreparedStatement st = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			st = con.prepareStatement("REPLACE INTO hwid_bonus (HWID,type,value) VALUES (?,?,?)");
			st.setString(1, hwid);
			st.setString(2, type);
			st.setInt(3, value);
			st.executeUpdate();

			FastMap<String, Integer> bonus = new FastMap<String, Integer>();
			bonus.put(type, value);
			bonus_hwids.put(hwid, bonus);
		}
		catch(Exception e)
		{
			_log.info("[Protection] Failed to add bonus HWID: " + hwid);
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, st);
		}
	}

	public static int getBonus(String hwid, String type)
	{
		FastMap<String, Integer> bonus = bonus_hwids.get(hwid);
		if(bonus == null)
			return 0;
		return bonus.get(type);
	}
}