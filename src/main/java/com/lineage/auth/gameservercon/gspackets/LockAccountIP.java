package com.lineage.auth.gameservercon.gspackets;

import java.util.logging.Logger;

import com.lineage.auth.gameservercon.AttGS;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;

/**
 * @Author: SYS
 * @Date: 10/4/2007
 */
public class LockAccountIP extends ClientBasePacket
{
	private static final Logger _log = Logger.getLogger(LockAccountIP.class.getName());

	public LockAccountIP(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		String accname = readS();
		String IP = readS();

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE accounts SET AllowIPs = ? WHERE login = ?");
			statement.setString(1, IP);
			statement.setString(2, accname);
			statement.executeUpdate();
			DatabaseUtils.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.severe("Failed to lock/unlock account: " + e.getMessage());
		}
		finally
		{
			DatabaseUtils.closeConnection(con);
		}
	}
}