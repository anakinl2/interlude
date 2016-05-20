package l2d.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public class ThreadConnection
{
	static Logger _log = Logger.getLogger(ThreadConnection.class.getName());

	private final Connection myConnection;
	private int counter;

	public ThreadConnection(Connection con)
	{
		myConnection = con;
		counter = 1;
	}

	public void updateCounter()
	{
		counter++;
	}

	public FiltredPreparedStatement prepareStatement(String sql) throws SQLException
	{
		return new FiltredPreparedStatement(myConnection.prepareStatement(sql));
	}

	public void close()
	{
		counter--;
		if(counter == 0)
			try
			{
				L2DatabaseFactory f = L2DatabaseFactory.getInstance();
				synchronized (f.getConnections())
				{
					myConnection.close();
					String key = f.generateKey();
					f.getConnections().remove(key);
				}
			}
			catch(Exception e)
			{
				_log.warning("Couldn't close connection. Cause: " + e.getMessage());
			}
	}

	public FiltredStatement createStatement() throws SQLException
	{
		return new FiltredStatement(myConnection.createStatement());
	}
}
