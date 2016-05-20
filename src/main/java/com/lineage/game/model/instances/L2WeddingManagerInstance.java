package com.lineage.game.model.instances;

import com.lineage.Config;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.game.Announcements;
import com.lineage.game.cache.Msg;
import com.lineage.game.instancemanager.CoupleManager;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.model.entity.Couple;
import com.lineage.game.serverpackets.MagicSkillUse;
import com.lineage.game.serverpackets.NpcHtmlMessage;
import com.lineage.game.templates.L2NpcTemplate;

public class L2WeddingManagerInstance extends L2NpcInstance
{
	public L2WeddingManagerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename = "data/html/wedding/start.htm";
		String replace = "";
		NpcHtmlMessage html = new NpcHtmlMessage(player, this);
		html.setFile(filename);
		html.replace("%replace%", replace);
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		// standard msg
		String filename = "data/html/wedding/start.htm";
		String replace = "";

		// if player has no partner
		if(player.getPartnerId() == 0)
		{
			filename = "data/html/wedding/nopartner.htm";
			sendHtmlMessage(player, filename, replace);
			return;
		}

		L2Player ptarget = (L2Player) L2World.findObject(player.getPartnerId());

		// partner online ?
		if(ptarget == null || !ptarget.isOnline())
		{
			filename = "data/html/wedding/notfound.htm";
			sendHtmlMessage(player, filename, replace);
			return;
		}
		else if(player.isMaried()) // already married ?
		{
			filename = "data/html/wedding/already.htm";
			sendHtmlMessage(player, filename, replace);
			return;
		}
		else if(command.startsWith("AcceptWedding"))
		{
			// accept the wedding request
			player.setMaryAccepted(true);
			Couple couple = CoupleManager.getInstance().getCouple(player.getCoupleId());
			couple.marry();

			// messages to the couple
			player.sendMessage(new CustomMessage("com.lineage.game.model.instances.L2WeddingManagerMessage", player));
			player.setMaried(true);
			player.setMaryRequest(false);
			ptarget.sendMessage(new CustomMessage("com.lineage.game.model.instances.L2WeddingManagerMessage", ptarget));
			ptarget.setMaried(true);
			ptarget.setMaryRequest(false);

			// Давать ли лук молодоженам ?
			if(Config.WEDDING_GIVE_ITEM > 0)
			{
				// give cupid's bows to couple's
				player.getInventory().addItem(Config.WEDDING_GIVE_ITEM, Config.WEDDING_GIVE_COUNT, 0, "Coupe");
				player.getInventory().updateDatabase(true);
				ptarget.getInventory().addItem(Config.WEDDING_GIVE_ITEM, Config.WEDDING_GIVE_COUNT, 0, "Coupe");
				ptarget.getInventory().updateDatabase(true);
			}

			// wedding march
			player.broadcastPacket(new MagicSkillUse(player, player, 2230, 1, 1, 0));
			ptarget.broadcastPacket(new MagicSkillUse(ptarget, ptarget, 2230, 1, 1, 0));

			// fireworks
			player.broadcastPacket(new MagicSkillUse(player, player, 2025, 1, 1, 0));
			ptarget.broadcastPacket(new MagicSkillUse(ptarget, ptarget, 2025, 1, 1, 0));

			Announcements.getInstance().announceToAll("Gratulations, " + player.getName() + " and " + ptarget.getName() + " has married.");

			filename = "data/html/wedding/accepted.htm";
			replace = ptarget.getName();
			sendHtmlMessage(ptarget, filename, replace);
			return;
		}
		else if(player.isMaryRequest())
		{
			// check for formalwear
			if(Config.WEDDING_FORMALWEAR && !player.isWearingFormalWear())
			{
				filename = "data/html/wedding/noformal.htm";
				sendHtmlMessage(player, filename, replace);
				return;
			}
			filename = "data/html/wedding/ask.htm";
			player.setMaryRequest(false);
			ptarget.setMaryRequest(false);
			replace = ptarget.getName();
			sendHtmlMessage(player, filename, replace);
			return;
		}
		else if(command.startsWith("AskWedding"))
		{
			// check for formalwear
			if(Config.WEDDING_FORMALWEAR && !player.isWearingFormalWear())
			{
				filename = "data/html/wedding/noformal.htm";
				sendHtmlMessage(player, filename, replace);
				return;
			}
			else if(player.getAdena() < Config.WEDDING_PRICE)
			{
				player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
				return;
			}
			else
			{
				player.setMaryAccepted(true);
				ptarget.setMaryRequest(true);
				replace = ptarget.getName();
				filename = "data/html/wedding/requested.htm";
				player.reduceAdena(Config.WEDDING_PRICE);
				sendHtmlMessage(player, filename, replace);
				return;
			}
		}
		else if(command.startsWith("DeclineWedding"))
		{
			player.setMaryRequest(false);
			ptarget.setMaryRequest(false);
			player.setMaryAccepted(false);
			ptarget.setMaryAccepted(false);
			player.sendMessage("You declined");
			ptarget.sendMessage("Your partner declined");
			replace = ptarget.getName();
			filename = "data/html/wedding/declined.htm";
			sendHtmlMessage(ptarget, filename, replace);
			return;
		}
		else if(player.isMaryAccepted())
		{
			filename = "data/html/wedding/waitforpartner.htm";
			sendHtmlMessage(player, filename, replace);
			return;
		}
		sendHtmlMessage(player, filename, replace);
	}

	private void sendHtmlMessage(L2Player player, String filename, String replace)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(player, this);
		html.setFile(filename);
		html.replace("%replace%", replace);
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
}