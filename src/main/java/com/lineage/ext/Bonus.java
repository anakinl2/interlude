package com.lineage.ext;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import l2d.game.model.L2Player;

public class Bonus
{
	L2Player _owner = null;
	/** Rate control */
	public float RATE_XP = 1;
	public float RATE_SP = 1;
	public float RATE_QUESTS_REWARD = 1;
	public float RATE_QUESTS_DROP = 1;
	public float RATE_DROP_ADENA = 1;
	public float RATE_DROP_ITEMS = 1;
	public float RATE_DROP_SPOIL = 1;

	public Bonus(L2Player player)
	{
		if(player == null || player.getNetConnection() == null) // игрок отвалился при входе
			return;

		_owner = player;
		restore();

		if(player.getNetConnection() == null)
			return;

		float bonus = player.getNetConnection().getBonus();
		if(player.getNetConnection().getBonusExpire() > System.currentTimeMillis() / 1000)
			player.startBonusTask(player.getNetConnection().getBonusExpire());

		RATE_XP = bonus;
		RATE_SP = bonus;
		RATE_DROP_ADENA = bonus;
		RATE_DROP_ITEMS = bonus;
		RATE_DROP_SPOIL = bonus;
	}

	private void restore()
	{
		ThreadConnection con = null;
		FiltredStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			rset = statement.executeQuery("SELECT bonus_name,bonus_value from bonus where obj_id='" + _owner.getObjectId() + "' and (bonus_expire_time='-1' or bonus_expire_time > " + System.currentTimeMillis() / 1000 + ")");
			while(rset.next())
			{
				String bonus_name = rset.getString("bonus_name");
				float bonus_value = rset.getFloat("bonus_value");
				Class<?> cls = getClass();
				try
				{
					Field fld = cls.getField(bonus_name);
					try
					{
						fld.setFloat(this, bonus_value);
					}
					catch(IllegalArgumentException e)
					{
						e.printStackTrace();
					}
					catch(IllegalAccessException e)
					{
						e.printStackTrace();
					}
				}
				catch(SecurityException e)
				{
					e.printStackTrace();
				}
				catch(NoSuchFieldException e)
				{
					e.printStackTrace();
				}
			}
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}
}