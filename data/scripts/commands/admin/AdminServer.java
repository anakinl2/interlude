package commands.admin;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import com.lineage.ext.mods.balancer.Balancer;
import com.lineage.ext.scripts.ScriptFile;
import l2d.game.ai.DefaultAI;
import l2d.game.handler.AdminCommandHandler;
import l2d.game.handler.IAdminCommandHandler;
import l2d.game.model.L2Player;
import l2d.game.serverpackets.NpcHtmlMessage;
import com.lineage.util.Files;

/**
 * This class handles following admin commands: - help path = shows
 * /data/html/admin/path file to char, should not be used by GM's directly
 */
public class AdminServer implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_server,
		admin_gc,
		admin_test,
		admin_pstat,
		admin_balancer
	}

	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, L2Player activeChar)
	{
		Commands command = (Commands) comm;

		if(!activeChar.getPlayerAccess().Menu)
			return false;

		switch(command)
		{
			case admin_server:
				try
				{
					String val = fullString.substring(13);
					showHelpPage(activeChar, val);
				}
				catch(StringIndexOutOfBoundsException e)
				{
					// case of empty filename
				}
				break;
			case admin_gc:
				try
				{
					System.gc();
					Thread.sleep(1000L);
					System.gc();
					Thread.sleep(1000L);
					System.gc();
				}
				catch(Exception e)
				{}
				activeChar.sendMessage("OK! - garbage collector called.");
				break;
			case admin_test:
				StringTokenizer st = new StringTokenizer(fullString);
				st.nextToken(); //skip command

				//String val1 = null;
				//if(st.hasMoreTokens())
				//	val1 = st.nextToken();
				//String val2 = null;
				//if(st.hasMoreTokens())
				//	val2 = st.nextToken();

				// Сюда пихать тестовый код
				try
				{
					//activeChar.sendMessage(activeChar.getTarget().getClass().getName());
					/*
					Location target = activeChar.getLoc();
					target.x += 100;
					activeChar.broadcastPacket(new ExJumpToLocation(activeChar.getObjectId(), activeChar.getLoc(), target));
					*/

					((DefaultAI) activeChar.getTarget().getAI()).DebugTasks();
				}
				catch(NumberFormatException e)
				{
					e.printStackTrace();
				}
				// тут тестовый код кончается

				activeChar.sendMessage("Test.");
				break;
			case admin_pstat:
				if(wordList.length == 2 && wordList[1].equals("on"))
					activeChar.packetsCount = true;
				else if(wordList.length == 2 && wordList[1].equals("off"))
				{
					activeChar.packetsCount = false;
					activeChar.packetsStat = null;
				}
				else if(activeChar.packetsCount)
				{
					activeChar.packetsCount = false;
					for(Entry<String, Integer> entry : activeChar.packetsStat.entrySet())
						activeChar.sendMessage(entry.getValue() + " : " + entry.getKey());
					activeChar.packetsCount = true;
				}
				break;
			case admin_balancer:
				try
				{
					Files.cacheClean();
					Balancer.openHTML(activeChar);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				break;
		}
		return true;
	}

	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	// PUBLIC & STATIC so other classes from package can include it directly
	public static void showHelpPage(L2Player targetChar, String filename)
	{
		File file = new File("./", "data/html/admin/" + filename);
		FileInputStream fis = null;

		try
		{
			fis = new FileInputStream(file);
			byte[] raw = new byte[fis.available()];
			fis.read(raw);

			String content = new String(raw, "UTF-8");

			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

			adminReply.setHtml(content);
			targetChar.sendPacket(adminReply);
		}
		catch(Exception e)
		{
			// problem with adminserver is ignored
		}
		finally
		{
			try
			{
				if(fis != null)
					fis.close();
			}
			catch(Exception e1)
			{
				// problems ignored
			}
		}
	}

	public void onLoad()
	{
		AdminCommandHandler.getInstance().registerAdminCommandHandler(this);
	}

	public void onReload()
	{}

	public void onShutdown()
	{}
}