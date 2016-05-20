package com.lineage.game.cache;

import java.sql.ResultSet;

import javolution.util.FastMap;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.ext.scripts.Functions;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;

/**
 * 
 * @author Midnex
 *
 */
public abstract class PlayerShiftCache
{
	private static final FastMap<String, String> _shift_stat = new FastMap<String, String>();
	private static final FastMap<String, String> _lastkill_stat = new FastMap<String, String>();

	public static void update(String name, String shift, String kills)
	{
		_shift_stat.put(name, shift);
		_lastkill_stat.put(name, kills);
	}

	public static void sendShiftStat(L2Player request, String player)
	{
		L2Player target = L2World.getPlayer(player);
		if(target == null)
		{
			if(_shift_stat.containsKey(player))
				Functions.show(_shift_stat.get(player), request);
		}
		else
			Functions.show(target._shift_page, request);
	}

	public static void sendShiftKillStat(L2Player request, String player)
	{
		L2Player target = L2World.getPlayer(player);
		if(target == null)
		{
			if(_lastkill_stat.containsKey(player))
				Functions.show(_shift_stat.get(player), request);
		}
		else
			Functions.show(target._shift_page_last_kills, request);
	}

	public static void restore()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM character_shift");
			rset = statement.executeQuery();
			while(rset.next())
			{
				_shift_stat.put(rset.getString("player"), rset.getString("shift_html"));
				_lastkill_stat.put(rset.getString("player"), rset.getString("kills_html"));
			}
		}
		catch(final Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}
}