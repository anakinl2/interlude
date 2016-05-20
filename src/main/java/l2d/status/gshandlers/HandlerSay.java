package l2d.status.gshandlers;

import java.io.PrintWriter;
import java.net.Socket;

import l2d.game.Announcements;
import l2d.game.clientpackets.Say2C;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.serverpackets.Say2;
import l2d.game.tables.GmListTable;
import l2d.status.GameStatusThread;
import l2d.status.Status;
import l2d.util.Util;

public class HandlerSay
{
	public static void Announce(String fullCmd, String[] argv, PrintWriter _print)
	{
		if(argv.length < 2 || argv[1] == null || argv[1].isEmpty() || argv[1].equalsIgnoreCase("?"))
			_print.println("USAGE: announce Text");
		else
		{
			Announcements.getInstance().announceToAll(Util.joinStrings(" ", argv, 1));
			_print.println("Announcement Sent!");
		}
	}

	public static void Message(String fullCmd, String[] argv, PrintWriter _print)
	{
		if(argv.length < 3 || argv[1] == null || argv[2] == null || argv[1].isEmpty() || argv[2].isEmpty() || argv[1].equalsIgnoreCase("?"))
			_print.println("USAGE: message Player Text");
		else
		{
			L2Player player = L2World.getPlayer(argv[1]);
			if(player == null)
				_print.println("Unable to find Player: " + argv[1]);
			else
			{
				String msg = Util.joinStrings(" ", argv, 2);
				Say2 cs = new Say2(0, Say2C.TELL, "Server Admin", msg);
				player.sendPacket(cs);
				_print.println("Private message ->" + argv[1] + ": " + msg);
			}
		}
	}

	public static void GmChat(String fullCmd, String[] argv, PrintWriter _print, Socket _csocket)
	{
		if(argv.length < 2 || argv[1] == null || argv[1].isEmpty() || argv[1].equalsIgnoreCase("?"))
			_print.println("USAGE: gmchat Text");
		else
		{
			Say2 cs = new Say2(0, 9, "Telnet GM Broadcast from " + _csocket.getInetAddress().getHostAddress(), Util.joinStrings(" ", argv, 1));
			GmListTable.broadcastToGMs(cs);
			_print.println("Your Message Has Been Sent To " + GmListTable.getAllGMs().size() + " GM(s).");
		}
	}

	public static void TelnetTell(String fullCmd, String[] argv, PrintWriter _print, Socket _csocket)
	{
		if(argv.length < 2 || argv[1] == null || argv[1].isEmpty() || argv[1].equalsIgnoreCase("?"))
			_print.println("USAGE: tell Text");
		else
		{
			GameStatusThread temp = Status.telnetlist;
			if(temp == null)
				_print.println("telnetlist is null!");
			else
				do
					temp.write("Telnet[" + _csocket.getInetAddress().getHostAddress() + "]: " + argv[1]);
				while((temp = temp.next) != null);
		}
	}
}