package com.lineage.game.clientpackets;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lineage.ext.scripts.Scripts;
import com.lineage.game.communitybbs.CommunityBoard;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.handler.IVoicedCommandHandler;
import com.lineage.game.handler.VoicedCommandHandler;
import com.lineage.game.cache.PlayerShiftCache;
import com.lineage.game.model.BypassManager.DecodedBypass;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Multisell;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.network.L2GameClient;
import com.lineage.game.serverpackets.NpcHtmlMessage;

public class RequestBypassToServer extends L2GameClientPacket
{
	//Format: cS
	private static Logger _log = Logger.getLogger(RequestBypassToServer.class.getName());
	private DecodedBypass bp = null;

	@Override
	public void readImpl()
	{
		String bypass = readS();
		if(!bypass.isEmpty())
			bp = getClient().getActiveChar().decodeBypass(bypass);
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null || bp == null)
			return;
		try
		{
			if(bp.bbs)
				CommunityBoard.getInstance().handleCommands(getClient(), bp.bypass);
			else if(bp.bypass.startsWith("lastkills"))
				PlayerShiftCache.sendShiftKillStat(activeChar, bp.bypass.split(" ")[1]);
			else if(bp.bypass.startsWith("showstat"))
				PlayerShiftCache.sendShiftStat(activeChar, bp.bypass.split(" ")[1]);
			else if(bp.bypass.startsWith("admin_"))
				AdminCommandHandler.getInstance().useAdminCommandHandler(activeChar, bp.bypass);
			else if(bp.bypass.equals("come_here") && activeChar.getPlayerAccess().IsGM)
				comeHere(getClient());
			else if(bp.bypass.startsWith("player_help "))
				playerHelp(activeChar, bp.bypass.substring(12));
			else if(bp.bypass.startsWith("scripts_"))
			{
				String command = bp.bypass.substring(8).trim();
				String[] word = command.split("\\s+");
				String[] args = command.substring(word[0].length()).trim().split("\\s+");
				String[] path = word[0].split(":");
				if(path.length != 2)
				{
					_log.warning("Bad Script bypass!");
					return;
				}

				HashMap<String, Object> variables = new HashMap<String, Object>();

				if(activeChar.getTarget() instanceof L2NpcInstance)
					variables.put("npc", activeChar.getTarget());
				else
					variables.put("npc", null);

				if(word.length == 1)
					Scripts.callScripts(path[0], path[1],activeChar, new Object[] {}, variables);
				else
					Scripts.callScripts(path[0], path[1],activeChar, new Object[] { args }, variables);
			}
			else if(bp.bypass.startsWith("user_"))
			{
				String command = bp.bypass.substring(5).trim();
				String word = command.split("\\s+")[0];
				String args = command.substring(word.length()).trim();
				IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getVoicedCommandHandler(word);

				if(vch != null)
					vch.useVoicedCommand(word, activeChar, args);
				else
					_log.warning("Unknow voiced command '" + word + "'");
			}
			else if(bp.bypass.startsWith("npc_"))
			{
				int endOfId = bp.bypass.indexOf('_', 5);
				String id;
				if(endOfId > 0)
					id = bp.bypass.substring(4, endOfId);
				else
					id = bp.bypass.substring(4);
				L2Object object = activeChar.getVisibleObject(Integer.parseInt(id));
				if(object != null && object.isNpc() && endOfId > 0 && activeChar.isInRange(object.getLoc(), L2Character.INTERACTION_DISTANCE))
				{
					activeChar.setLastNpc((L2NpcInstance) object);
					((L2NpcInstance) object).onBypassFeedback(activeChar, bp.bypass.substring(endOfId + 1));
				}
			}
			else if(bp.bypass.startsWith("manor_menu_select?")) // Navigate throught Manor windows
			{
				L2Object object = activeChar.getTarget();
				if(object != null && object.isNpc())
					((L2NpcInstance) object).onBypassFeedback(activeChar, bp.bypass);
			}
			else if(bp.bypass.startsWith("multisell "))
				L2Multisell.getInstance().SeparateAndSend(Integer.parseInt(bp.bypass.substring(10)), activeChar, 0);
			else if(bp.bypass.startsWith("Quest "))
			{
				String p = bp.bypass.substring(6).trim();
				int idx = p.indexOf(' ');
				if(idx < 0)
					activeChar.processQuestEvent(p, "");
				else
					activeChar.processQuestEvent(p.substring(0, idx), p.substring(idx).trim());
			}
		}
		catch(Exception e)
		{
			String st = "Bad RequestBypassToServer: " + bp.bypass;
			if(activeChar.getTarget() instanceof L2NpcInstance)
				st = st + " via NPC #" + ((L2NpcInstance) activeChar.getTarget()).getNpcId();
			_log.log(Level.WARNING, st, e);
		}
	}

	private void comeHere(L2GameClient client)
	{
		L2Object obj = client.getActiveChar().getTarget();
		if(obj != null && obj.isNpc())
		{
			L2NpcInstance temp = (L2NpcInstance) obj;
			L2Player activeChar = client.getActiveChar();
			temp.setTarget(activeChar);
			temp.moveToLocation(activeChar.getLoc(), 0, true);
		}
	}

	private void playerHelp(L2Player activeChar, String path)
	{
		String filename = "data/html/" + path;
		NpcHtmlMessage html = new NpcHtmlMessage(5);
		html.setFile(filename);
		activeChar.sendPacket(html);
	}
}