package l2d.status;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Properties;
import java.util.logging.Logger;

import l2d.Config;
import l2d.status.gshandlers.HandlerBan;
import l2d.status.gshandlers.HandlerDebug;
import l2d.status.gshandlers.HandlerPerfomance;
import l2d.status.gshandlers.HandlerSay;
import l2d.status.gshandlers.HandlerStatus;
import l2d.status.gshandlers.HandlerWorld;

public class GameStatusThread extends Thread
{
	private static final Logger _log = Logger.getLogger(GameStatusThread.class.getName());

	public boolean LogChat = false;
	public boolean LogTell = false;

	public GameStatusThread next;

	private Socket _csocket;

	private PrintWriter _print;
	private BufferedReader _read;

	private void telnetOutput(int type, String text)
	{
		if(type == 1)
			_log.fine("GSTELNET | " + text);
		else if(type == 2)
			_log.fine("GSTELNET | " + text);
		else if(type == 3)
			_log.fine(text);
		else if(type == 4)
			_log.fine(text);
		else
			_log.fine("GSTELNET | " + text);
	}

	private boolean isValidIP(Socket client)
	{
		boolean result = false;
		InetAddress ClientIP = client.getInetAddress();

		// convert IP to String, and compare with list
		String clientStringIP = ClientIP.getHostAddress();

		telnetOutput(1, "Connection from: " + clientStringIP);

		// read and loop thru list of IPs, compare with newIP
		try
		{
			Properties telnetSettings = new Properties();
			InputStream telnetIS = new FileInputStream(new File(Config.TELNET_FILE));
			telnetSettings.load(telnetIS);
			telnetIS.close();

			String HostList = telnetSettings.getProperty("ListOfHosts", "127.0.0.1,localhost");

			// compare
			String ipToCompare;
			for(String ip : HostList.split(","))
				if(!result)
				{
					ipToCompare = InetAddress.getByName(ip).getHostAddress();
					if(clientStringIP.equals(ipToCompare))
						result = true;
				}
		}
		catch(IOException e)
		{
			telnetOutput(1, "Error: " + e);
		}

		return result;
	}

	public GameStatusThread(Socket client, String StatusPW) throws IOException
	{
		_csocket = client;

		_print = new PrintWriter(_csocket.getOutputStream());
		_read = new BufferedReader(new InputStreamReader(_csocket.getInputStream()));

		// проверяем IP
		if(!isValidIP(client))
		{
			telnetOutput(1, "Connection attempt from " + client.getInetAddress().getHostAddress() + " rejected.");
			_csocket.close();
			return;
		}

		telnetOutput(1, client.getInetAddress().getHostAddress() + " accepted.");
		_print.println("Welcome to the L2impulse GS Telnet Session.");

		if(StatusPW == null || StatusPW.isEmpty())
		{
			start();
			return;
		}

		// авторизируемся
		int authAttempts = 0;
		String authPass;
		_print.print("Enter your password, please! ");
		while(authAttempts < 5) //TODO maxAuthAttempts
		{
			authAttempts++;
			_print.print("Password: ");
			_print.flush();
			authPass = _read.readLine();

			if(StatusPW.equalsIgnoreCase(authPass))
			{
				_print.println("Password accepted!");
				_print.println();
				_print.flush();
				start();
				return;
			}

			try
			{
				Thread.sleep(authAttempts * 1000);
			}
			catch(Exception e)
			{
				_csocket.close();
				return;
			}

			_print.print("Password incorrect! ");
		}

		_print.println();
		_print.println("Maximum auth re-attempts reached! Disconnecting....");
		_print.flush();
		_csocket.close();
	}

	@Override
	public void run()
	{
		next = Status.telnetlist;
		Status.telnetlist = this;

		String cmd;
		String[] argv;
		try
		{
			for(;;)
			{
				_print.print("l2pgs> ");
				_print.flush();
				cmd = _read.readLine();

				if(cmd == null)
				{
					_csocket.close();
					break;
				}

				argv = cmd.split(" ");

				try
				{
					if(argv == null || argv.length == 0 || argv[0].isEmpty())
					{ /* do nothing */}
					else if(argv[0].equalsIgnoreCase("?") || argv[0].equalsIgnoreCase("h") || argv[0].equalsIgnoreCase("help"))
						Help(_print);
					else if(argv[0].equalsIgnoreCase("status") || argv[0].equalsIgnoreCase("s"))
						HandlerStatus.Status(cmd, argv, _print);
					else if(argv[0].equalsIgnoreCase("database") || argv[0].equalsIgnoreCase("db"))
						HandlerStatus.Database(cmd, argv, _print);
					else if(argv[0].equalsIgnoreCase("gmlist"))
						HandlerStatus.GmList(cmd, argv, _print);
					else if(argv[0].equalsIgnoreCase("ver") || argv[0].equalsIgnoreCase("version"))
						HandlerStatus.Version(cmd, argv, _print);
					else if(argv[0].equalsIgnoreCase("cfg") || argv[0].equalsIgnoreCase("config"))
						HandlerStatus.Config(cmd, argv, _print);

					else if(argv[0].equalsIgnoreCase("lazyitems"))
						HandlerPerfomance.LazyItems(cmd, argv, _print);
					else if(argv[0].equalsIgnoreCase("performance") || argv[0].equalsIgnoreCase("p"))
						HandlerPerfomance.ThreadPool(cmd, argv, _print);
					else if(argv[0].equalsIgnoreCase("garbage") || argv[0].equalsIgnoreCase("gc"))
						HandlerPerfomance.GC(cmd, argv, _print);

					else if(argv[0].equalsIgnoreCase("announce") || argv[0].equalsIgnoreCase("!"))
						HandlerSay.Announce(cmd, argv, _print);
					else if(argv[0].equalsIgnoreCase("message") || argv[0].equalsIgnoreCase("msg"))
						HandlerSay.Message(cmd, argv, _print);
					else if(argv[0].equalsIgnoreCase("gmchat"))
						HandlerSay.GmChat(cmd, argv, _print, _csocket);
					else if(argv[0].equalsIgnoreCase("tell"))
						HandlerSay.TelnetTell(cmd, argv, _print, _csocket);

					else if(argv[0].equalsIgnoreCase("baniplist") || argv[0].equalsIgnoreCase("banip"))
						HandlerBan.BanIP(cmd, argv, _print, _csocket);
					else if(argv[0].equalsIgnoreCase("unbanip"))
						HandlerBan.UnBanIP(cmd, argv, _print);
					else if(argv[0].equalsIgnoreCase("kick"))
						HandlerBan.Kick(cmd, argv, _print);
					else if(argv[0].equalsIgnoreCase("banhwid") || argv[0].equalsIgnoreCase("ban_hwid"))
						HandlerBan.BanHWID(cmd, argv, _print);
					else if(argv[0].equalsIgnoreCase("unbanhwid") || argv[0].equalsIgnoreCase("unban_hwid"))
						HandlerBan.UnBanHWID(cmd, argv, _print);

					else if(argv[0].equalsIgnoreCase("whois"))
						HandlerWorld.Whois(cmd, argv, _print);
					else if(argv[0].equalsIgnoreCase("enemy"))
						HandlerWorld.ListEnemy(cmd, argv, _print);
					else if(argv[0].equalsIgnoreCase("reload"))
						HandlerWorld.Reload(cmd, argv, _print);
					else if(argv[0].equalsIgnoreCase("shutdown"))
						HandlerWorld.Shutdown(cmd, argv, _print, _csocket);
					else if(argv[0].equalsIgnoreCase("restart"))
						HandlerWorld.Restart(cmd, argv, _print, _csocket);
					else if(argv[0].equalsIgnoreCase("abort") || argv[0].equalsIgnoreCase("a"))
						HandlerWorld.AbortShutdown(cmd, argv, _print, _csocket);
					else if(argv[0].equalsIgnoreCase("stopLogin"))
						HandlerWorld.StopLogin(cmd, argv, _print, _csocket);

					else if(argv[0].equalsIgnoreCase("debug"))
						HandlerDebug.Debug(cmd, argv, _print, _csocket);
					else if(argv[0].equalsIgnoreCase("dumpmem") || argv[0].equalsIgnoreCase("memdump"))
						HandlerDebug.HprofMemDump(cmd, argv, _print);

					else if(argv[0].equalsIgnoreCase("log_chat"))
					{
						LogChat = !LogChat;
						_print.println("Log chat is turned " + (LogChat ? "on" : "off"));
					}
					else if(argv[0].equalsIgnoreCase("log_tell"))
					{
						LogTell = !LogTell;
						_print.println("Log tell is turned " + (LogTell ? "on" : "off"));
					}
					else if(argv[0].equalsIgnoreCase("quit") || argv[0].equalsIgnoreCase("q") || argv[0].equalsIgnoreCase("exit"))
					{
						_print.println("Bye Bye!");
						_print.flush();
						_csocket.close();
						break;
					}
					else
						_print.println("Unknown command: " + argv[0] + " Please use 'help' or '?' for more options...");
				}
				catch(Exception e)
				{
					e.printStackTrace(_print);
				}

				_print.println();
				_print.flush();
				try
				{
					Thread.sleep(100);
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
			} /* end for */

			telnetOutput(1, "Connection from " + _csocket.getInetAddress().getHostAddress() + " was closed by client.");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		//      begin clean telnetlist
		if(this == Status.telnetlist)
			Status.telnetlist = next;
		else
		{
			GameStatusThread temp = Status.telnetlist;
			while(temp.next != this)
				temp = temp.next;
			temp.next = next;
		}
		// finished clean telnetlist
	}

	private static void Help(PrintWriter _print)
	{
		_print.println("Command line HELP: ");
		_print.println("+ ... Main options: ");
		_print.println("-->  shutdown (time) - Shutdown this server if 'NOW' immediately (time value in seconds)");
		_print.println("-->  restart (time) - Restart this server if 0 immediately (time value in seconds)");
		_print.println("-->  abort - Abort operation shutdown or restart (or command 'a')");
		_print.println("-->  gmaccess_reload - Reload GMAccess.xml");
		_print.println("-->  scripts_reload - Reload all Scrits (or command 'sreload')");
		_print.println("-->  multisell_reload - Reload all multisell");
		_print.println("-->  npc_reload - Reload NPC Data");
		_print.println("-->  dumpmem - Memory DUMP");
		_print.println("-->  skill_reload - Reload skill data");
		_print.println("-->  gc - Start Garbage Collector");
		_print.println("-->  performance - Performance server (or command 'p')");
		_print.println("-->  p packets - Performance packets");
		_print.println("-->  p general - Performance general");
		_print.println("-->  p npc - Performance NPC");
		_print.println("-->  lazyitems - Lazy items show status");
		_print.println("-->  database - Show database threads");
		_print.println("-->  status - Show server status");
		_print.println("-->  stopLogin - Stop login server");
		_print.println("-->  ver - Show version (or command 'version')");
		_print.println("-->  quit - Quit from command line (or command 'exit','q')");
		_print.println("+ ... Charaters and chat: ");
		_print.println("-->  log_chat - Show chat log in console mode (to switch off retry command)");
		_print.println("-->  log_tell - Show tell log in console mode char-2-char (to switch off retry command)");
		_print.println("-->  kick (charname) - Kick charname from server");
		_print.println("-->  whois (charname) - Whois information charname (name,account,ip)");
		_print.println("-->  baniplist - Show ip ban list");
		_print.println("-->  banip (ip) - Ban ip address");
		_print.println("-->  banhwid - Ban HWID");
		_print.println("-->  unbanip (ip) - Remove Ban ip address");
		_print.println("-->  gmlist - Show GM list");
		_print.println("-->  gmchat (text) - Send message to all gm's");
		_print.println("-->  unblock - Command not configured!!!");
		_print.println("-->  msg (text) - Send message to server");
		_print.println("-->  announce (text) - Send announce to server (or command '! (text)')");
		_print.println("+ ... L2impulse Terminal HELP ... ");
	}

	public void write(String _text)
	{
		_print.println(_text);
		_print.flush();
	}
}