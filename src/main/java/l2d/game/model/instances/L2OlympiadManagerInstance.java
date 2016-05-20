package l2d.game.model.instances;

import java.util.List;
import java.util.logging.Logger;

import com.lineage.Config;
import l2d.game.model.L2Multisell;
import l2d.game.model.L2Player;
import l2d.game.model.entity.Hero;
import l2d.game.model.entity.olympiad.Olympiad;
import l2d.game.model.entity.olympiad.OlympiadDatabase;
import l2d.game.serverpackets.ExHeroList;
import l2d.game.serverpackets.NpcHtmlMessage;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.templates.L2NpcTemplate;

/**
 * Olympiad Npc's Instance
 */
public class L2OlympiadManagerInstance extends L2NpcInstance
{
	private static Logger _log = Logger.getLogger(L2OlympiadManagerInstance.class.getName());
	private static final short _gatePass = 6651;

	public L2OlympiadManagerInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
		if(Config.ENABLE_OLYMPIAD && template.npcId == 31688)
			Olympiad.addOlympiadNpc(this);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(!Config.ENABLE_OLYMPIAD)
			return;

		if(command.startsWith("OlympiadDesc"))
		{
			int val = Integer.parseInt(command.substring(13, 14));
			String suffix = command.substring(14);
			showChatWindow(player, val, suffix);
		}
		else if(command.startsWith("OlympiadNoble"))
		{
			int classId = player.getClassId().getId();
			if(!Config.ENABLE_OLYMPIAD || !player.isNoble() || classId < 88 || classId > 118 && classId < 131 || classId > 134)
				return;

			int val = Integer.parseInt(command.substring(14));
			NpcHtmlMessage reply;
			StringBuffer replyMSG;

			switch(val)
			{
				case 1:
					Olympiad.unRegisterNoble(player, false);
					break;
				case 2:
					int classed = 0;
					int nonClassed = 0;
					int[] array = Olympiad.getWaitingList();

					if(array != null)
					{
						classed = array[0];
						nonClassed = array[1];
					}

					reply = new NpcHtmlMessage(player, this);
					replyMSG = new StringBuffer("<html><body>");
					replyMSG.append("The number of people on the waiting list for Grand Olympiad" + //
					"<img src=\"L2UI.SquareWhite\" width=270 height=1><img src=\"L2UI.SquareBlank\" width=1 height=3>" + //
					"<center>" + "<table width=270 border=0 bgcolor=\"000000\">" + "<tr>" + "<td align=\"left\">General</td>" + //
					"<td align=\"right\">" + classed + "</td></tr><tr><td align=\"left\">Not class-defined</td><td align=\"right\">" + //
					nonClassed + "</td></tr></table><br><img src=\"L2UI.SquareWhite\" width=270 height=1> <img src=\"L2UI.SquareBlank\" width=1 height=3>" + //
					"<button value=\"Back\" action=\"bypass -h npc_%objectId%_OlympiadDesc 2a\" " + //
					"width=70 height=21 back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\"></center>");

					replyMSG.append("</body></html>");

					reply.setHtml(replyMSG.toString());
					player.sendPacket(reply);
					break;
				case 3:
					reply = new NpcHtmlMessage(player, this);
					replyMSG = new StringBuffer("<html><body>");
					replyMSG.append("There are " + Olympiad.getNoblePoints(player.getObjectId()) + //
					" Grand Olympiad points granted for this event.<br><br>" + //
					"<a action=\"bypass -h npc_%objectId%_OlympiadDesc 2a\">Return</a>");
					replyMSG.append("</body></html>");
					reply.setHtml(replyMSG.toString());
					player.sendPacket(reply);
					break;
				case 4:
					Olympiad.registerNoble(player, false);
					break;
				case 5:
					Olympiad.registerNoble(player, true);
					break;
				case 6:
					int passes = Olympiad.getNoblessePasses(player.getObjectId());
					if(passes > 0)
					{
						L2ItemInstance item = player.getInventory().addItem(_gatePass, passes, 0, "Olympiad");

						SystemMessage sm = new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1S);
						sm.addNumber(passes);
						sm.addItemName(item.getItemId());
						player.sendPacket(sm);
					}
					else
					{
						reply = new NpcHtmlMessage(player, this);
						replyMSG = new StringBuffer("<html><body>");
						replyMSG.append("Grand Olympiad Manager:<br>");
						replyMSG.append("Sorry, you don't have enough points for a Noblesse Gate Pass. Better luck next time.<br>");
						replyMSG.append("<a action=\"bypass -h npc_%objectId%_OlympiadDesc 4a\">Return</a>");
						replyMSG.append("</body></html>");
						reply.setHtml(replyMSG.toString());
						player.sendPacket(reply);
					}
					break;
				case 7:
					L2Multisell.getInstance().SeparateAndSend(1000, player, 0);
					break;
				case 8:
					reply = new NpcHtmlMessage(player, this);
					replyMSG = new StringBuffer("<html><body>");
					replyMSG.append("Your Grand Olympiad Score from previous period is " + Olympiad.getNoblePointsPast(player.getObjectId()) + " point(s).<br>");
					replyMSG.append("<a action=\"bypass -h npc_%objectId%_OlympiadDesc 4a\">Return</a>");
					replyMSG.append("</body></html>");
					reply.setHtml(replyMSG.toString());
					player.sendPacket(reply);
					break;
				default:
					_log.warning("Olympiad System: Couldnt send packet for request " + val);
					break;
			}
		}
		else if(command.startsWith("Olympiad"))
		{
			if(!Config.ENABLE_OLYMPIAD)
				return;
			int val = Integer.parseInt(command.substring(9, 10));

			NpcHtmlMessage reply;
			StringBuffer replyMSG;

			switch(val)
			{
				case 1:
					String[] matches = Olympiad.getAllTitles();

					reply = new NpcHtmlMessage(player, this);
					replyMSG = new StringBuffer("<html><body>");
					replyMSG.append("Grand Olympiad Games Overview<br><br>" + "* Caution: Please note, if you watch an Olympiad " + "game, the summoning of your Servitors or Pets will be " + "cancelled. Be careful.<br>");

					for(int i = 0; i < matches.length; i++)
						replyMSG.append("<br1><a action=\"bypass -h npc_%objectId%_Olympiad 3_" + i + "\">" + matches[i] + "</a>");

					replyMSG.append("</body></html>");

					reply.setHtml(replyMSG.toString());
					player.sendPacket(reply);
					break;
				case 2:
					// for example >> Olympiad 2_88
					int classId = Integer.parseInt(command.substring(11));
					if(classId >= 88)
					{
						reply = new NpcHtmlMessage(player, this);
						replyMSG = new StringBuffer("<html><body>");
						replyMSG.append("<center>Grand Olympiad Ranking");
						replyMSG.append("<img src=\"L2UI.SquareWhite\" width=270 height=1><img src=\"L2UI.SquareBlank\" width=1 height=3>");

						List<String> names = OlympiadDatabase.getClassLeaderBoard(classId);
						if(names.size() != 0)
						{
							replyMSG.append("<table width=300 border=0 bgcolor=\"000000\">");

							int index = 1;

							for(String name : names)
							{
								replyMSG.append("<tr>");
								replyMSG.append("<td><center>" + index + "<center></td>");
								replyMSG.append("<td><center>" + name + "<center></td>");
								replyMSG.append("</tr>");
								index++;
							}

							// 3 пустых строки
							replyMSG.append("<tr><td><br><br><br></td><td></td></tr>");

							replyMSG.append("</table>");
						}

						replyMSG.append("<img src=\"L2UI.SquareWhite\" width=270 height=1> <img src=\"L2UI.SquareBlank\" width=1 height=3>");
						replyMSG.append("<button value=\"Back\" action=\"bypass -h npc_%objectId%_OlympiadDesc 3a\" back=\"L2UI.DefaultButton_click\" fore=\"L2UI.DefaultButton\" width=67 height=19>");
						replyMSG.append("</center>");
						replyMSG.append("</body></html>");

						reply.setHtml(replyMSG.toString());
						player.sendPacket(reply);
					}
					// TODO Send player each class rank
					break;
				case 3:
					if(!Config.ENABLE_OLYMPIAD_SPECTATING)
						break;
					int id = Integer.parseInt(command.substring(11));
					Olympiad.addSpectator(id, player);
					break;
				case 4:
					player.sendPacket(new ExHeroList());
					break;
				case 5:
					reply = new NpcHtmlMessage(player, this);
					replyMSG = new StringBuffer("<html><body>");
					if(Hero.getInstance().isInactiveHero(player.getObjectId()))
					{
						Hero.getInstance().activateHero(player);
						replyMSG.append("Congratulations! You are a Hero now.");
					}
					else
						replyMSG.append("You cannot be a Hero.");
					replyMSG.append("</body></html>");
					reply.setHtml(replyMSG.toString());
					player.sendPacket(reply);
					break;
				default:
					_log.warning("Olympiad System: Couldnt send packet for request " + val);
					break;
			}
		}
		else
			super.onBypassFeedback(player, command);
	}

	private void showChatWindow(L2Player player, int val, String suffix)
	{
		String filename = Olympiad.OLYMPIAD_HTML_FILE;
		filename += "noble_desc" + val;
		filename += suffix != null ? suffix + ".htm" : ".htm";
		if(filename.equals(Olympiad.OLYMPIAD_HTML_FILE + "noble_desc0.htm"))
			filename = Olympiad.OLYMPIAD_HTML_FILE + "noble_main.htm";
		player.sendPacket(new NpcHtmlMessage(player, this, filename, val));
	}
}
