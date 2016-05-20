package l2d.auth.gameservercon.gspackets;

import java.util.logging.Logger;

import l2d.auth.gameservercon.AttGS;
import l2d.db.DatabaseUtils;
import l2d.db.FiltredPreparedStatement;
import l2d.db.L2DatabaseFactory;
import l2d.db.ThreadConnection;

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