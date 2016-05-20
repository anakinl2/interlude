package com.lineage.game.communitybbs.Manager;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TreeSet;

import javolution.text.TextBuilder;
import com.lineage.Config;
import com.lineage.game.GameTimeController;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.tables.FakePlayersTable;

public class RegionBBSManager extends BaseBBSManager
{
	private static final Object _lock = new Object();
	private static long player_names_cache_expire = 0;
	private static String[] player_names_cache;

	@Override
	public void parsecmd(String command, L2Player activeChar)
	{
		if(command.equals("_bbsloc"))
			showRegion(activeChar, 0);
		else if(command.startsWith("_bbsloc;show;") && activeChar.isGM())
		{
			int index = 0;
			try
			{
				index = Integer.parseInt(activeChar.getVar("bbsloc_page"));
			}
			catch(Exception nfe)
			{}
			showRegion(activeChar, index);
			AdminCommandHandler.getInstance().useAdminCommandHandler(activeChar, "admin_character_list " + command.replaceFirst("_bbsloc;show;", ""));
		}
		else if(command.startsWith("_bbsloc;page;"))
			try
			{
				int index = Integer.parseInt(command.replaceFirst("_bbsloc;page;", ""));
				showRegion(activeChar, index);
			}
			catch(NumberFormatException nfe)
			{
				separateAndSend("<html><body><br><br><center>Error!</center><br><br></body></html>", activeChar);
			}
		else
			separateAndSend("<html><body><br><br><center>the command: " + command + " is not implemented yet</center><br><br></body></html>", activeChar);
	}

	private String[] getPlayersArray(L2Player activeChar)
	{
		TreeSet<String> temp = new TreeSet<String>();
		for(L2Player player : L2World.getAllPlayers())
		{
			if(player == null || player.isInvisible() && player != activeChar && !activeChar.isGM())
				continue;
			temp.add(player.getName());
		}
		temp.addAll(FakePlayersTable.getActiveFakePlayers());
		String[] player_names = temp.toArray(new String[temp.size()]);
		if(Config.COMMUNITYBOARD_SORTPLAYERSLIST)
			Arrays.sort(player_names);
		return player_names;
	}

	private void showRegion(L2Player activeChar, int startIndex)
	{
		TextBuilder htmlCode = new TextBuilder("<html><body><br>");

		SimpleDateFormat format = new SimpleDateFormat("HH:mm");
		Calendar cal = Calendar.getInstance();
		htmlCode.append("<table>");
		htmlCode.append("<tr><td width=100>Server Time: </td><td width=50>" + format.format(cal.getTime()) + "</td></tr>");
		int t = GameTimeController.getInstance().getGameTime();
		cal.set(Calendar.HOUR_OF_DAY, t / 60);
		cal.set(Calendar.MINUTE, t % 60);
		htmlCode.append("<tr><td width=100>Game Time: </td><td width=50>" + format.format(cal.getTime()) + "</td></tr>");
		htmlCode.append("</table>");
		htmlCode.append("<br><img src=\"L2UI.SquareWhite\" width=625 height=1><br>");

		if(Config.ALLOW_COMMUNITYBOARD_PLAYERSLIST.equalsIgnoreCase("all") || Config.ALLOW_COMMUNITYBOARD_PLAYERSLIST.equalsIgnoreCase("GM") && activeChar.isGM())
		{
			if(activeChar.isGM())
				activeChar.setVar("bbsloc_page", String.valueOf(startIndex));

			String[] player_names;
			if(Config.COMMUNITYBOARD_PLAYERSLIST_CACHE > 0 && !activeChar.isGM())
			{
				synchronized (_lock)
				{
					if(System.currentTimeMillis() > player_names_cache_expire)
					{
						player_names_cache = getPlayersArray(activeChar);
						player_names_cache_expire = System.currentTimeMillis() + Config.COMMUNITYBOARD_PLAYERSLIST_CACHE;
					}
				}
				player_names = player_names_cache;
			}
			else
				player_names = getPlayersArray(activeChar);

			htmlCode.append("<br> &nbsp; &nbsp; " + player_names.length + " Player(s) Online:<br><br>");

			htmlCode.append("<table border=0>");
			htmlCode.append("<tr><td><table border=0>");

			int cell = 0;
			int n = startIndex;
			for(int i = startIndex; i < startIndex + Config.NAME_PAGE_SIZE_COMMUNITYBOARD; i++)
			{
				if(i >= player_names.length)
					break;

				String player = player_names[i]; // Get the current record

				cell++;

				if(cell == 1)
					htmlCode.append("<tr>");
				if(activeChar.isGM())
					htmlCode.append("<td align=left valign=top fixwidth=120><a action=\"bypass _bbsloc;show;" + player + "\">" + player + "</a></td>");
				else
					htmlCode.append("<td align=left valign=top fixwidth=120>" + player + "</td>");

				if(cell == Config.NAME_PER_ROW_COMMUNITYBOARD)
				{
					cell = 0;
					htmlCode.append("</tr>");
				}
				n++;
			}

			if(cell > 0 && cell < Config.NAME_PER_ROW_COMMUNITYBOARD)
				htmlCode.append("</tr>");

			htmlCode.append("</table></td></tr>");

			if(player_names.length > Config.NAME_PAGE_SIZE_COMMUNITYBOARD)
			{
				htmlCode.append("<tr><td> </td></tr>"); // для отступа
				htmlCode.append("<tr><td align=center valign=top>Displaying " + (startIndex + 1) + " - " + n + " player(s)</td></tr>");
				htmlCode.append("<tr><td align=center valign=top>");
				htmlCode.append("<table border=0 width=610><tr>");

				if(startIndex == 0)
					htmlCode.append("<td><button value=\"Prev\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
				else
					htmlCode.append("<td><button value=\"Prev\" action=\"bypass _bbsloc;page;" + (startIndex - Config.NAME_PAGE_SIZE_COMMUNITYBOARD) + "\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");

				htmlCode.append("<td FIXWIDTH=10></td>");

				if(player_names.length <= startIndex + Config.NAME_PAGE_SIZE_COMMUNITYBOARD)
					htmlCode.append("<td><button value=\"Next\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
				else
					htmlCode.append("<td><button value=\"Next\" action=\"bypass _bbsloc;page;" + (startIndex + Config.NAME_PAGE_SIZE_COMMUNITYBOARD) + "\" width=50 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");

				htmlCode.append("</tr></table>");
				htmlCode.append("</td></tr>");
			}

			htmlCode.append("</table>");
		}

		htmlCode.append("</body></html>");
		separateAndSend(htmlCode.toString(), activeChar);
	}

	@Override
	public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2Player activeChar)
	{
		if(activeChar == null)
			return;
		separateAndSend("<html><body><br><br><center>the command: " + ar1 + " is not implemented yet</center><br><br></body></html>", activeChar);
	}

	private static RegionBBSManager _Instance = null;

	public static RegionBBSManager getInstance()
	{
		if(_Instance == null)
			_Instance = new RegionBBSManager();
		return _Instance;
	}
}