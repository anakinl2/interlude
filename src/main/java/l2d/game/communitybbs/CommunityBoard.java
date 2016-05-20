package l2d.game.communitybbs;

import java.util.HashMap;
import java.util.StringTokenizer;

import com.lineage.Config;
import com.lineage.ext.mods.balancer.Balancer;
import com.lineage.ext.scripts.Scripts;
import l2d.game.communitybbs.Manager.ClanBBSManager;
import l2d.game.communitybbs.Manager.FriendsBBSManager;
import l2d.game.communitybbs.Manager.MailBBSManager;
import l2d.game.communitybbs.Manager.PostBBSManager;
import l2d.game.communitybbs.Manager.RegionBBSManager;
import l2d.game.communitybbs.Manager.TopBBSManager;
import l2d.game.communitybbs.Manager.TopicBBSManager;
import l2d.game.model.L2Multisell;
import l2d.game.model.L2Player;
import l2d.game.network.L2GameClient;
import l2d.game.serverpackets.ShowBoard;
import l2d.game.serverpackets.SystemMessage;

public class CommunityBoard
{
	private static CommunityBoard _instance;

	public static CommunityBoard getInstance()
	{
		if(_instance == null)
			_instance = new CommunityBoard();

		return _instance;
	}

	public void handleCommands(L2GameClient client, String command)
	{
		L2Player activeChar = client.getActiveChar();
		if(activeChar == null)
			return;
		if(!Config.ALLOW_COMMUNITYBOARD)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THE_COMMUNITY_SERVER_IS_CURRENTLY_OFFLINE));
			return;
		}

		if(command.startsWith("_bbs_b"))
			Balancer.usebypass(activeChar, command);
		else if(command.startsWith("_bbsclan"))
			ClanBBSManager.getInstance().parsecmd(command, activeChar);
		else if(command.startsWith("_bbsmemo"))
			TopicBBSManager.getInstance().parsecmd(command, activeChar);
		else if(command.startsWith("_bbstopics"))
			TopicBBSManager.getInstance().parsecmd(command, activeChar);
		else if(command.startsWith("_bbsposts"))
			PostBBSManager.getInstance().parsecmd(command, activeChar);
		else if(command.startsWith("_bbstop"))
			TopBBSManager.getInstance().parsecmd(command, activeChar);
		else if(command.startsWith("_bbshome"))
			TopBBSManager.getInstance().parsecmd(command, activeChar);
		else if(command.startsWith("_bbsloc"))
			RegionBBSManager.getInstance().parsecmd(command, activeChar);
		else if(command.startsWith("_friend") || command.startsWith("_block"))
			FriendsBBSManager.getInstance().parsecmd(command, activeChar);
		else if(command.startsWith("_bbsgetfav"))
			ShowBoard.separateAndSend("<html><body><br><br><center>Comming soon...</center><br><br></body></html>", activeChar);
		else if(command.startsWith("_maillist"))
			MailBBSManager.getInstance().parsecmd(command, activeChar);
		else if(command.startsWith("_bbsmultisell;"))
		{
			StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			TopBBSManager.getInstance().parsecmd("_bbstop;" + st.nextToken(), activeChar);
			L2Multisell.getInstance().SeparateAndSend(Integer.parseInt(st.nextToken()), activeChar, 0);
		}
		else if(command.startsWith("_bbsscripts;"))
		{
			StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			TopBBSManager.getInstance().parsecmd("_bbstop;" + st.nextToken(), activeChar);

			String com = st.nextToken();
			String[] word = com.split("\\s+");
			String[] args = com.substring(word[0].length()).trim().split("\\s+");
			String[] path = word[0].split(":");
			if(path.length != 2)
			{
				System.out.println("Bad Script bypass!");
				return;
			}

			HashMap<String, Object> variables = new HashMap<String, Object>();
			variables.put("npc", null);
			Scripts.callScripts(path[0], path[1],activeChar, word.length == 1 ? new Object[] {} : new Object[] { args }, variables);
		}
		else if(command.startsWith("_bbsscripts_ret;"))
		{
			StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			String page = st.nextToken();

			String com = st.nextToken();
			String[] word = com.split("\\s+");
			String[] args = com.substring(word[0].length()).trim().split("\\s+");
			String[] path = word[0].split(":");
			if(path.length != 2)
			{
				System.out.println("Bad Script bypass!");
				return;
			}
			HashMap<String, Object> variables = new HashMap<String, Object>();
			variables.put("npc", null);
			Object subcontent = Scripts.callScripts(path[0], path[1],activeChar, word.length == 1 ? new Object[] {} : new Object[] { args }, variables);

			TopBBSManager.getInstance().showTopPage(activeChar, page, String.valueOf(subcontent));
		}
		else
			ShowBoard.separateAndSend("<html><body><br><br><center>Function: " + command + " currently not handled.</center><br><br></body></html>", activeChar);
	}

	public void handleWriteCommands(L2GameClient client, String url, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		L2Player activeChar = client.getActiveChar();
		if(activeChar == null)
			return;
		if(!Config.ALLOW_COMMUNITYBOARD)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.THE_COMMUNITY_SERVER_IS_CURRENTLY_OFFLINE));
			return;
		}

		if(url.equals("Topic"))
			TopicBBSManager.getInstance().parsewrite(arg1, arg2, arg3, arg4, arg5, activeChar);
		else if(url.equals("Post"))
			PostBBSManager.getInstance().parsewrite(arg1, arg2, arg3, arg4, arg5, activeChar);
		else if(url.equals("Region"))
			RegionBBSManager.getInstance().parsewrite(arg1, arg2, arg3, arg4, arg5, activeChar);
		else if(url.equals("Notice"))
			ClanBBSManager.getInstance().parsewrite(arg1, arg2, arg3, arg4, arg5, activeChar);
		else if(url.equals("Mail"))
			MailBBSManager.getInstance().parsewrite(arg1, arg2, arg3, arg4, arg5, activeChar);
		else
			ShowBoard.separateAndSend("<html><body><br><br><center>Function: " + url + " currently not handled</center><br><br></body></html>", activeChar);
	}
}