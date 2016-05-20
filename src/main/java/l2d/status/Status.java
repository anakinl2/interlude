package l2d.status;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;

import l2d.Config;
import l2d.Server;
import l2d.util.Rnd;

public class Status extends Thread
{
	protected static Logger _log = Logger.getLogger(Status.class.getName());
	private ServerSocket statusServerSocket;

	private int _mode;
	private String _StatusPW;
	public static GameStatusThread telnetlist;
	private ArrayList<LoginStatusThread> _loginStatus = new ArrayList<LoginStatusThread>();

	@Override
	public void run()
	{
		while(true)
		{
			try
			{
				Socket connection = statusServerSocket.accept();

				if(_mode == Server.MODE_GAMESERVER)
					new GameStatusThread(connection, _StatusPW);
				else if(_mode == Server.MODE_LOGINSERVER)
				{
					LoginStatusThread lst = new LoginStatusThread(connection, _StatusPW);
					if(lst.isAlive())
						_loginStatus.add(lst);
				}
				if(isInterrupted())
				{
					try
					{
						statusServerSocket.close();
					}
					catch(IOException io)
					{
						io.printStackTrace();
					}
					break;
				}
			}
			catch(IOException e)
			{
				if(isInterrupted())
				{
					try
					{
						statusServerSocket.close();
					}
					catch(IOException io)
					{
						io.printStackTrace();
					}
					break;
				}
			}
			try
			{
				Thread.sleep(3000); /* Защита от глюков */
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public Status(int mode) throws IOException
	{
		super("Status");
		_mode = mode;
		Properties telnetSettings = new Properties();
		InputStream is;
		if(_mode == Server.MODE_GAMESERVER)
		{
			is = new FileInputStream(l2d.Server.L2DreamHomeDir + Config.TELNET_FILE);
			telnetSettings.load(is);
			is.close();
		}
		if(_mode == Server.MODE_LOGINSERVER)
		{
			is = new FileInputStream(l2d.Server.L2DreamHomeDir + Config.LOGIN_TELNET_FILE);
			telnetSettings.load(is);
			is.close();
		}

		int _StatusPort = Integer.parseInt(telnetSettings.getProperty("StatusPort", "12345"));
		_StatusPW = telnetSettings.getProperty("StatusPW");

		if(_StatusPW == null)
			_log.warning("Warning: server's Telnet Function Has No Password Defined!");
		//_log.warning("A Password Has Been Automaticly Created!");
		//_StatusPW = RndPW(10);
		//System.out.println("Password Has Been Set To: " + _StatusPW);
		else
			_log.fine("Password Has Been Set");
		statusServerSocket = new ServerSocket(_StatusPort);
		if(_mode == Server.MODE_LOGINSERVER)
			_log.fine("StatusServer for LoginServer Started! - Listening on Port: " + _StatusPort);
		else
			_log.fine("StatusServer for GameServer Started! - Listening on Port: " + _StatusPort);
	}

	@SuppressWarnings("unused")
	private String RndPW(int length)
	{
		StringBuffer password = new StringBuffer();
		String lowerChar = "qwertyuiopasdfghjklzxcvbnm";
		String upperChar = "QWERTYUIOPASDFGHJKLZXCVBNM";
		String digits = "1234567890";
		for(int i = 0; i < length; i++)
		{
			int charSet = Rnd.get(3);
			switch(charSet)
			{
				case 0:
					password.append(lowerChar.charAt(Rnd.get(lowerChar.length() - 1)));
					break;
				case 1:
					password.append(upperChar.charAt(Rnd.get(upperChar.length() - 1)));
					break;
				case 2:
					password.append(digits.charAt(Rnd.get(digits.length() - 1)));
					break;
			}
		}
		return password.toString();
	}

	public void SendMessageToTelnets(String msg)
	{
		for(LoginStatusThread ls : _loginStatus)
			if(!ls.isInterrupted())
				ls.printToTelnet(msg);
	}
}