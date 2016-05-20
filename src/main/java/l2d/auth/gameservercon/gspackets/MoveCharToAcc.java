package l2d.auth.gameservercon.gspackets;

import java.sql.ResultSet;
import java.util.logging.Logger;

import l2d.auth.L2LoginClient;
import l2d.auth.LoginController;
import l2d.auth.gameservercon.AttGS;
import l2d.auth.gameservercon.lspackets.MoveCharToAccResponse;
import l2d.db.DatabaseUtils;
import l2d.db.FiltredPreparedStatement;
import l2d.db.L2DatabaseFactory;
import l2d.db.ThreadConnection;
import l2d.util.Log;

/**
 * @Author: Abaddon
 */
public class MoveCharToAcc extends ClientBasePacket
{
	private static final Logger log = Logger.getLogger(MoveCharToAcc.class.getName());

	public MoveCharToAcc(byte[] decrypt, AttGS gameserver)
	{
		super(decrypt, gameserver);
	}

	@Override
	public void read()
	{
		String player = readS();
		String oldacc = readS();
		String newacc = readS();
		String pass = readS();

		String dbPassword = null;
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			try
			{
				statement = con.prepareStatement("SELECT * FROM accounts WHERE login = ?");
				statement.setString(1, oldacc);
				rs = statement.executeQuery();
				if(rs.next())
					dbPassword = rs.getString("password");
			}
			catch(Exception e)
			{
				log.warning("Can't recive password for account " + oldacc + ", exciption :" + e);
			}
			finally
			{
				DatabaseUtils.closeDatabaseSR(statement, rs);
			}

			try
			{
				statement = con.prepareStatement("SELECT * FROM accounts WHERE login = ?");
				statement.setString(1, newacc);
				rs = statement.executeQuery();
				if(!rs.next())
				{
					sendPacket(new MoveCharToAccResponse(player, 1));
					return;
				}
			}
			catch(Exception e)
			{
				log.warning("Can't recive password for account " + oldacc + ", exciption :" + e);
			}
			finally
			{
				DatabaseUtils.closeDatabaseSR(statement, rs);
			}

			//Encode old password and compare it to sended one, send packet to determine changed or not.
			try
			{
				if(!LoginController.DEFAULT_CRYPT.compare(pass, dbPassword))
					sendPacket(new MoveCharToAccResponse(player, 0));
				else
				{
					statement = con.prepareStatement("UPDATE accounts SET login = ? WHERE login = ?");
					statement.setString(1, newacc);
					statement.setString(2, oldacc);
					int result = statement.executeUpdate();
					L2LoginClient client = LoginController.getInstance().getAuthedClient(oldacc);
					if(result != 0)
						Log.add("<old=\"" + oldacc + "\" new=\"" + newacc + "\" ip=\"" + (client != null ? client.getIpAddress() : "0.0.0.0") + "\" />", "accounts");

					sendPacket(new MoveCharToAccResponse(player, 2));
				}
			}
			catch(Exception e1)
			{
				e1.printStackTrace();
			}
			finally
			{
				DatabaseUtils.closeStatement(statement);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DatabaseUtils.closeConnection(con);
		}
	}
}
