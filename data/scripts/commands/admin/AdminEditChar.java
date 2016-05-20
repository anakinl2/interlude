package commands.admin;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;

import com.lineage.db.mysql;
import com.lineage.ext.multilang.CustomMessage;
import com.lineage.ext.scripts.ScriptFile;
import com.lineage.game.Announcements;
import com.lineage.game.cache.Msg;
import com.lineage.game.handler.AdminCommandHandler;
import com.lineage.game.handler.IAdminCommandHandler;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2SubClass;
import com.lineage.game.model.L2World;
import com.lineage.game.model.base.ClassId;
import com.lineage.game.model.base.PlayerClass;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.serverpackets.NpcHtmlMessage;
import com.lineage.game.serverpackets.SkillList;
import com.lineage.game.serverpackets.SocialAction;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.SkillTable;
import com.lineage.util.Log;

@SuppressWarnings("unused")
public class AdminEditChar implements IAdminCommandHandler, ScriptFile
{
	private static enum Commands
	{
		admin_edit_character, //
		admin_character_actions, //
		admin_current_player, //
		admin_nokarma, //
		admin_setkarma, //
		admin_addfame, //
		admin_setfame, //
		admin_character_list, //
		admin_show_characters, //
		admin_find_character, //
		admin_save_modifications, //
		admin_rec, //
		admin_settitle, //
		admin_setname, //
		admin_setsex, //
		admin_setcolor, //
		admin_add_exp_sp_to_character, //
		admin_add_exp_sp, //
		admin_sethero, //
		admin_setnoble, //
		admin_setsubclass
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean useAdminCommand(final Enum comm, final String[] wordList, final String fullString, final L2Player activeChar)
	{
		final Commands command = (Commands) comm;

		if(activeChar.getPlayerAccess().CanRename)
			if(fullString.startsWith("admin_settitle"))
				try
				{
					final String val = fullString.substring(15);
					final L2Object target = activeChar.getTarget();
					L2Player player = null;
					if(target != null && target.isPlayer())
						player = (L2Player) target;
					else
						return false;
					player.setTitle(val);
					player.sendMessage("Your title has been changed by a GM");
					player.sendChanges();

					Log.add("change title for player " + player.getName() + " to " + val, "gm_ext_actions", activeChar);
					return true;
				}
				catch(final StringIndexOutOfBoundsException e)
				{ // Case of empty character title
					activeChar.sendMessage("You need to specify the new title.");
					return false;
				}
			else if(fullString.startsWith("admin_setname"))
				try
				{
					final String val = fullString.substring(14);
					final L2Object target = activeChar.getTarget();
					L2Player player;
					if(target != null && target.isPlayer())
						player = (L2Player) target;
					else
						return false;
					if(mysql.simple_get_int("count(*)", "characters", "`char_name` like '" + val + "'") > 0)
					{
						activeChar.sendMessage("Name already exist.");
						return false;
					}
					Log.add("Character " + player.getName() + " renamed to " + val + " by GM " + activeChar.getName(), "renames");
					Log.add("set name for player " + player.getName() + " to " + val, "gm_ext_actions", activeChar);
					player.reName(val);
					player.sendMessage("Your name has been changed by a GM");
					return true;
				}
				catch(final StringIndexOutOfBoundsException e)
				{ // Case of empty character name
					activeChar.sendMessage("You need to specify the new name.");
					return false;
				}

		if( !activeChar.getPlayerAccess().CanEditChar && !activeChar.getPlayerAccess().CanViewChar)
			return false;

		if(fullString.equals("admin_current_player"))
			showCharacterList(activeChar, null);
		else if(fullString.startsWith("admin_character_list"))
			try
			{
				final String val = fullString.substring(21);
				final L2Player target = L2World.getPlayer(val);
				showCharacterList(activeChar, target);
			}
			catch(final StringIndexOutOfBoundsException e)
			{
				// Case of empty character name
			}
		else if(fullString.startsWith("admin_show_characters"))
			try
			{
				final String val = fullString.substring(22);
				final int page = Integer.parseInt(val);
				listCharacters(activeChar, page);
			}
			catch(final StringIndexOutOfBoundsException e)
			{
				// Case of empty page
			}
		else if(fullString.startsWith("admin_find_character"))
			try
			{
				final String val = fullString.substring(21);
				findCharacter(activeChar, val);
			}
			catch(final StringIndexOutOfBoundsException e)
			{ // Case of empty character name
				activeChar.sendMessage("You didnt enter a character name to find.");

				listCharacters(activeChar, 0);
			}
		else if( !activeChar.getPlayerAccess().CanEditChar)
			return false;
		else if(fullString.equals("admin_edit_character"))
			editCharacter(activeChar);
		else if(fullString.equals("admin_character_actions"))
			showCharacterActions(activeChar);
		else if(fullString.equals("admin_nokarma"))
			setTargetKarma(activeChar, 0);
		else if(fullString.startsWith("admin_setkarma"))
			try
			{
				final String val = fullString.substring(15);
				final int karma = Integer.parseInt(val);
				setTargetKarma(activeChar, karma);
			}
			catch(final StringIndexOutOfBoundsException e)
			{
				activeChar.sendMessage("Please specify new karma value.");
			}
		else if(fullString.startsWith("admin_save_modifications"))
			try
			{
				final String val = fullString.substring(24);
				adminModifyCharacter(activeChar, val);

				Log.add("save modifications for player " + val, "gm_ext_actions", activeChar);
			}
			catch(final StringIndexOutOfBoundsException e)
			{ // Case of empty character name
				activeChar.sendMessage("Error while modifying character.");
				listCharacters(activeChar, 0);
			}
		else if(fullString.equals("admin_rec"))
		{
			final L2Object target = activeChar.getTarget();
			L2Player player = null;
			if(target != null && target.isPlayer())
				player = (L2Player) target;
			else
				return false;
			player.setRecomHave(player.getRecomHave() + 1);
			player.sendMessage("You have been recommended by a GM");
			player.broadcastUserInfo(true);

			Log.add("recommend player " + player.getName() + " + 1", "gm_ext_actions", activeChar);
		}
		else if(fullString.startsWith("admin_rec"))
			try
			{
				final String val = fullString.substring(10);
				final int recVal = Integer.parseInt(val);
				final L2Object target = activeChar.getTarget();
				L2Player player = null;
				if(target != null && target.isPlayer())
					player = (L2Player) target;
				else
					return false;
				player.setRecomHave(player.getRecomHave() + recVal);
				player.sendMessage("You have been recommended by a GM");
				player.broadcastUserInfo(true);

				Log.add("recommend player " + player.getName() + " + " + recVal, "gm_ext_actions", activeChar);
			}
			catch(final NumberFormatException e)
			{
				activeChar.sendMessage("Command format is //rec <number>");
			}
		else if(fullString.startsWith("admin_sethero"))
		{
			// Статус меняется только на текущую логон сессию
			final L2Object target = activeChar.getTarget();
			L2Player player;
			if(wordList.length > 1 && wordList[1] != null)
			{
				player = L2World.getPlayer(wordList[1]);
				if(player == null)
				{
					activeChar.sendMessage("Character " + wordList[1] + " not found in game.");
					return false;
				}
			}
			else if(target != null && target.isPlayer())
				player = (L2Player) target;
			else
			{
				activeChar.sendMessage("You must specify the name or target character.");
				return false;
			}

			if(player.isHero())
			{
				player.setHero(false);
				player.removeSkill(SkillTable.getInstance().getInfo(395, 1));
				player.removeSkill(SkillTable.getInstance().getInfo(396, 1));
				player.removeSkill(SkillTable.getInstance().getInfo(1374, 1));
				player.removeSkill(SkillTable.getInstance().getInfo(1375, 1));
				player.removeSkill(SkillTable.getInstance().getInfo(1376, 1));

				for(L2ItemInstance item : player.getInventory().getItems())
				{
					if(item == null)
						continue;
					if(item.isHeroItem())
					{
						player.getInventory().unEquipItem(item);
						player.getInventory().destroyItem(item, 1, true);
					}
				}
			}
			else
			{
				player.setHero(true);
				player.addSkill(SkillTable.getInstance().getInfo(395, 1));
				player.addSkill(SkillTable.getInstance().getInfo(396, 1));
				player.addSkill(SkillTable.getInstance().getInfo(1374, 1));
				player.addSkill(SkillTable.getInstance().getInfo(1375, 1));
				player.addSkill(SkillTable.getInstance().getInfo(1376, 1));
			}

			player.sendPacket(new SkillList(player));

			if(player.isHero())
			{
				player.broadcastPacket(new SocialAction(player.getObjectId(), 16));
				Announcements.getInstance().announceToAll(player.getName() + " has become a hero.");
			}
			player.sendMessage("Admin changed your hero status.");
			player.broadcastUserInfo(true);

			Log.add("add hero status to player " + player.getName(), "gm_ext_actions", activeChar);
		}
		else if(fullString.startsWith("admin_setnoble"))
		{
			// Статус сохраняется в базе
			final L2Object target = activeChar.getTarget();
			L2Player player;
			if(wordList.length > 1 && wordList[1] != null)
			{
				player = L2World.getPlayer(wordList[1]);
				if(player == null)
				{
					activeChar.sendMessage("Character " + wordList[1] + " not found in game.");
					return false;
				}
			}
			else if(target != null && target.isPlayer())
				player = (L2Player) target;
			else
			{
				activeChar.sendMessage("You must specify the name or target character.");
				return false;
			}

			if(player.isNoble())
			{
				player.setNoble(false);
				player.removeSkill(SkillTable.getInstance().getInfo(1323, 1));
				player.removeSkill(SkillTable.getInstance().getInfo(325, 1));
				player.removeSkill(SkillTable.getInstance().getInfo(326, 1));
				player.removeSkill(SkillTable.getInstance().getInfo(327, 1));
				player.removeSkill(SkillTable.getInstance().getInfo(1324, 1));
				player.removeSkill(SkillTable.getInstance().getInfo(1325, 1));
				player.removeSkill(SkillTable.getInstance().getInfo(1326, 1));
				player.removeSkill(SkillTable.getInstance().getInfo(1327, 1));
			}
			else
			{
				player.setNoble(true);
				player.addSkill(SkillTable.getInstance().getInfo(1323, 1));
				player.addSkill(SkillTable.getInstance().getInfo(325, 1));
				player.addSkill(SkillTable.getInstance().getInfo(326, 1));
				player.addSkill(SkillTable.getInstance().getInfo(327, 1));
				player.addSkill(SkillTable.getInstance().getInfo(1324, 1));
				player.addSkill(SkillTable.getInstance().getInfo(1325, 1));
				player.addSkill(SkillTable.getInstance().getInfo(1326, 1));
				player.addSkill(SkillTable.getInstance().getInfo(1327, 1));
			}

			player.sendPacket(new SkillList(player));

			if(player.isNoble())
				player.broadcastPacket(new SocialAction(player.getObjectId(), 16));

			player.sendMessage("Admin changed your noble status.");
			player.broadcastUserInfo(true);

			Log.add("add noble status to player " + player.getName(), "gm_ext_actions", activeChar);
		}
		else if(fullString.startsWith("admin_setsex"))
		{
			final L2Object target = activeChar.getTarget();
			L2Player player = null;
			if(target != null && target.isPlayer())
				player = (L2Player) target;
			else
				return false;
			player.changeSex();
			player.sendMessage("Your gender has been changed by a GM");
			player.broadcastUserInfo(true);

			Log.add("change gender to player " + player.getName(), "gm_ext_actions", activeChar);
		}
		else if(fullString.startsWith("admin_setcolor"))
			try
			{
				final String val = fullString.substring(15);
				final L2Object target = activeChar.getTarget();
				L2Player player = null;
				if(target != null && target.isPlayer())
					player = (L2Player) target;
				else
					return false;
				player.setNameColor(Integer.decode("0x" + val));
				player.sendMessage("Your name color has been changed by a GM");
				player.broadcastUserInfo(true);

				Log.add("change name color for player " + player.getName(), "gm_ext_actions", activeChar);
			}
			catch(final StringIndexOutOfBoundsException e)
			{ // Case of empty color
				activeChar.sendMessage("You need to specify the new color.");
			}
		else if(fullString.startsWith("admin_add_exp_sp_to_character"))
			addExpSp(activeChar);
		else if(fullString.startsWith("admin_add_exp_sp"))
			try
			{
				final String val = fullString.substring(16);
				adminAddExpSp(activeChar, val);
			}
			catch(final StringIndexOutOfBoundsException e)
			{ // Case of empty character name
				activeChar.sendMessage("Error while adding Exp-Sp.");
			}
		else if(fullString.startsWith("admin_setsubclass"))
		{
			final L2Object target = activeChar.getTarget();
			if(target == null || !target.isPlayer())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.SELECT_TARGET));
				return false;
			}
			final L2Player player = (L2Player) target;

			final StringTokenizer st = new StringTokenizer(fullString);
			if(st.countTokens() > 1)
			{
				st.nextToken();
				final short classId = Short.parseShort(st.nextToken());
				if( !player.addSubClass(classId, true))
				{
					activeChar.sendMessage(new CustomMessage("l2d.game.model.instances.L2VillageMasterInstance.SubclassCouldNotBeAdded", activeChar));
					return false;
				}
				player.sendPacket(new SystemMessage(SystemMessage.CONGRATULATIONS_YOU_HAVE_TRANSFERRED_TO_A_NEW_CLASS)); // Transfer to new class.
			}
			else
				setSubclass(activeChar, player);
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}

	private void listCharacters(final L2Player activeChar, int page)
	{
		final L2Player[] players = L2World.getAllPlayers();

		final int MaxCharactersPerPage = 20;
		int MaxPages = players.length / MaxCharactersPerPage;

		if(players.length > MaxCharactersPerPage * MaxPages)
			MaxPages++;

		// Check if number of users changed
		if(page > MaxPages)
			page = MaxPages;

		final int CharactersStart = MaxCharactersPerPage * page;
		int CharactersEnd = players.length;
		if(CharactersEnd - CharactersStart > MaxCharactersPerPage)
			CharactersEnd = CharactersStart + MaxCharactersPerPage;

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

		final StringBuffer replyMSG = new StringBuffer("<html><body>");
		replyMSG.append("<table width=260><tr>");
		replyMSG.append("<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=180><center>Character Selection Menu</center></td>");
		replyMSG.append("<td width=40><button value=\"Back\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<br><br>");
		replyMSG.append("<table width=270>");
		replyMSG.append("<tr><td width=270>You can find a character by writing his name and</td></tr>");
		replyMSG.append("<tr><td width=270>clicking Find bellow.<br></td></tr>");
		replyMSG.append("<tr><td width=270>Note: Names should be written case sensitive.</td></tr>");
		replyMSG.append("</table><br>");
		replyMSG.append("<center><table><tr><td>");
		replyMSG.append("<edit var=\"character_name\" width=80></td><td><button value=\"Find\" action=\"bypass -h admin_find_character $character_name\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
		replyMSG.append("</td></tr></table></center><br><br>");

		for(int x = 0; x < MaxPages; x++)
		{
			final int pagenr = x + 1;
			replyMSG.append("<center><a action=\"bypass -h admin_show_characters " + x + "\">Page " + pagenr + "</a></center>");
		}
		replyMSG.append("<br>");

		// List Players in a Table
		replyMSG.append("<table width=270>");
		replyMSG.append("<tr><td width=80>Name:</td><td width=110>Class:</td><td width=40>Level:</td></tr>");
		for(int i = CharactersStart; i < CharactersEnd; i++)
			replyMSG.append("<tr><td width=80>" + "<a action=\"bypass -h admin_character_list " + players[i].getName() + "\">" + players[i].getName() + "</a></td><td width=110>" + players[i].getTemplate().className + "</td><td width=40>" + players[i].getLevel() + "</td></tr>");
		replyMSG.append("</table>");
		replyMSG.append("</body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	public static void showCharacterList(final L2Player activeChar, L2Player player)
	{
		if(player == null)
		{
			final L2Object target = activeChar.getTarget();
			if(target != null && target.isPlayer())
				player = (L2Player) target;
			else
				return;
		}
		else
			activeChar.setTarget(player);

		String clanName = "No Clan";
		if(player.getClan() != null)
			clanName = player.getClan().getName() + "/" + player.getClan().getLevel();

		final NumberFormat df = NumberFormat.getNumberInstance(Locale.ENGLISH);
		df.setMaximumFractionDigits(4);
		df.setMinimumFractionDigits(1);

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

		final StringBuffer replyMSG = new StringBuffer("<html><body>");
		replyMSG.append("<table width=260><tr>");
		replyMSG.append("<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=180><center>Character Selection Menu</center></td>");
		replyMSG.append("<td width=40><button value=\"Back\" action=\"bypass -h admin_show_characters 0\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br>");

		replyMSG.append("<table width=270>");
		replyMSG.append("<tr><td width=100>Account/IP:</td><td>" + player.getNetConnection().getLoginName() + "/" + player.getNetConnection().getIpAddr() + "</td></tr>");
		replyMSG.append("<tr><td width=100>Name/Level:</td><td>" + player.getName() + "/" + player.getLevel() + "</td></tr>");
		replyMSG.append("<tr><td width=100>Class/Id:</td><td>" + player.getTemplate().className + "/" + player.getClassId().getId() + "</td></tr>");
		replyMSG.append("<tr><td width=100>Clan/Level:</td><td>" + clanName + "</td></tr>");
		replyMSG.append("<tr><td width=100>Exp/Sp:</td><td>" + player.getExp() + "/" + player.getSp() + "</td></tr>");
		replyMSG.append("<tr><td width=100>Cur/Max Hp:</td><td>" + (int) player.getCurrentHp() + "/" + player.getMaxHp() + "</td></tr>");
		replyMSG.append("<tr><td width=100>Cur/Max Mp:</td><td>" + (int) player.getCurrentMp() + "/" + player.getMaxMp() + "</td></tr>");
		replyMSG.append("<tr><td width=100>Cur/Max Load:</td><td>" + player.getCurrentLoad() + "/" + player.getMaxLoad() + "</td></tr>");
		replyMSG.append("<tr><td width=100>Patk/Matk:</td><td>" + player.getPAtk(null) + "/" + player.getMAtk(null, null) + "</td></tr>");
		replyMSG.append("<tr><td width=100>Pdef/Mdef:</td><td>" + player.getPDef(null) + "/" + player.getMDef(null, null) + "</td></tr>");
		replyMSG.append("<tr><td width=100>PAtkSpd/MAtkSpd:</td><td>" + player.getPAtkSpd() + "/" + player.getMAtkSpd() + "</td></tr>");
		replyMSG.append("<tr><td width=100>Acc/Evas:</td><td>" + player.getAccuracy() + "/" + player.getEvasionRate(null) + "</td></tr>");
		replyMSG.append("<tr><td width=100>Crit/MCrit:</td><td>" + player.getCriticalHit(null, null) + "/" + df.format(player.getMagicCriticalRate(null, null)) + "%</td></tr>");
		replyMSG.append("<tr><td width=100>Walk/Run:</td><td>" + player.getWalkSpeed() + "/" + player.getRunSpeed() + "</td></tr>");
		replyMSG.append("<tr><td width=100>Karma:</td><td>" + player.getKarma() + "</td></tr>");
		replyMSG.append("<tr><td width=100>PvP/PK:</td><td>" + player.getPvpKills() + "/" + player.getPkKills() + "</td></tr>");
		replyMSG.append("<tr><td width=100>Coordinates:</td><td>" + player.getX() + "," + player.getY() + "," + player.getZ() + "</td></tr>");
		replyMSG.append("</table><br>");

		replyMSG.append("<table<tr>");
		replyMSG.append("<td><button value=\"Skills\" action=\"bypass -h admin_show_skills\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td><button value=\"Effects\" action=\"bypass -h admin_show_effects\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td><button value=\"Actions\" action=\"bypass -h admin_character_actions\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr><tr>");
		replyMSG.append("<td><button value=\"Stats\" action=\"bypass -h admin_edit_character\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td><button value=\"Exp & Sp\" action=\"bypass -h admin_add_exp_sp_to_character\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td></td>");
		replyMSG.append("</tr></table></body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void setTargetKarma(final L2Player activeChar, final int newKarma)
	{
		final L2Object target = activeChar.getTarget();
		if(target == null)
		{
			activeChar.sendPacket(Msg.INVALID_TARGET);
			return;
		}

		L2Player player;
		if(target.isPlayer())
			player = (L2Player) target;
		else
			return;

		if(newKarma >= 0)
		{
			final int oldKarma = player.getKarma();
			player.setKarma(newKarma);

			player.sendMessage("Admin has changed your karma from " + oldKarma + " to " + newKarma + ".");
			player.sendUserInfo(false);
			activeChar.sendMessage("Successfully Changed karma for " + player.getName() + " from (" + oldKarma + ") to (" + newKarma + ").");

			Log.add("Changed karma for " + player.getName() + " from (" + oldKarma + ") to (" + newKarma + ")", "gm_ext_actions", activeChar);
		}
		else
			activeChar.sendMessage("You must enter a value for karma greater than or equal to 0.");
	}

	private void adminModifyCharacter(final L2Player activeChar, final String modifications)
	{
		final L2Object target = activeChar.getTarget();
		if(target == null || !target.isPlayer())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.SELECT_TARGET));
			return;
		}

		final L2Player player = (L2Player) target;
		final String[] strvals = modifications.split("&");
		final Integer[] vals = new Integer[strvals.length];
		for(int i = 0; i < strvals.length; i++)
		{
			strvals[i] = strvals[i].trim();
			vals[i] = strvals[i].isEmpty() ? null : Integer.valueOf(strvals[i]);
		}

		if(vals[0] != null)
			player.setCurrentHp(vals[0], false);

		if(vals[1] != null)
			player.setCurrentMp(vals[1]);

		if(vals[2] != null)
			player.setKarma(vals[2]);

		if(vals[3] != null)
			player.setPvpFlag(vals[3]);

		if(vals[4] != null)
			player.setPvpKills(vals[4]);

		if(vals[5] != null)
			player.setClassId(vals[5], true);

		player.sendChanges();
		editCharacter(activeChar); // Back to start
		player.broadcastUserInfo(true);
		player.decayMe();
		player.spawnMe(activeChar.getLoc());
	}

	private void editCharacter(final L2Player activeChar)
	{
		final L2Object target = activeChar.getTarget();
		if(target == null || !target.isPlayer())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.SELECT_TARGET));
			return;
		}

		final L2Player player = (L2Player) target;
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

		final StringBuffer replyMSG = new StringBuffer("<html><body>");
		replyMSG.append("<table width=260><tr>");
		replyMSG.append("<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=180><center>Character Selection Menu</center></td>");
		replyMSG.append("<td width=40><button value=\"Back\" action=\"bypass -h admin_current_player\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<br><br>");
		replyMSG.append("<center>Editing character: " + player.getName() + "</center><br>");
		replyMSG.append("<table width=250>");
		replyMSG.append("<tr><td width=40></td><td width=70>Curent:</td><td width=70>Max:</td><td width=70></td></tr>");
		replyMSG.append("<tr><td width=40>HP:</td><td width=70>" + player.getCurrentHp() + "</td><td width=70>" + player.getMaxHp() + "</td><td width=70>Karma: " + player.getKarma() + "</td></tr>");
		replyMSG.append("<tr><td width=40>MP:</td><td width=70>" + player.getCurrentMp() + "</td><td width=70>" + player.getMaxMp() + "</td><td width=70>Pvp Kills: " + player.getPvpKills() + "</td></tr>");
		replyMSG.append("<tr><td width=40>Load:</td><td width=70>" + player.getCurrentLoad() + "</td><td width=70>" + player.getMaxLoad() + "</td><td width=70>Pvp Flag: " + player.getPvpFlag() + "</td></tr>");
		replyMSG.append("</table>");
		replyMSG.append("<table width=270><tr><td>Class Template Id: " + player.getClassId() + "/" + player.getClassId().getId() + "</td></tr></table><br>");
		replyMSG.append("<table width=270>");
		replyMSG.append("<tr><td>Note: Fill all values before saving the modifications.</td></tr>");
		replyMSG.append("</table><br>");
		replyMSG.append("<table width=270>");
		replyMSG.append("<tr><td width=50>Hp:</td><td><edit var=\"hp\" width=50></td><td width=50>Mp:</td><td><edit var=\"mp\" width=50></td></tr>");
		replyMSG.append("<tr><td width=50>Pvp Flag:</td><td><edit var=\"pvpflag\" width=50></td><td width=50>Karma:</td><td><edit var=\"karma\" width=50></td></tr>");
		replyMSG.append("<tr><td width=50>Class Id:</td><td><edit var=\"classid\" width=50></td><td width=50>Pvp Kills:</td><td><edit var=\"pvpkills\" width=50></td></tr>");
		replyMSG.append("</table><br>");
		replyMSG.append("<center><button value=\"Save Changes\" action=\"bypass -h admin_save_modifications $hp & $mp & $karma & $pvpflag & $pvpkills & $classid &\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></center><br>");
		replyMSG.append("</body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void showCharacterActions(final L2Player activeChar)
	{
		final L2Object target = activeChar.getTarget();
		L2Player player;
		if(target != null && target.isPlayer())
			player = (L2Player) target;
		else
			return;

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

		final StringBuffer replyMSG = new StringBuffer("<html><body>");
		replyMSG.append("<table width=260><tr>");
		replyMSG.append("<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=180><center>Character Selection Menu</center></td>");
		replyMSG.append("<td width=40><button value=\"Back\" action=\"bypass -h admin_current_player\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table><br><br>");
		replyMSG.append("<center>Admin Actions for: " + player.getName() + "</center><br>");
		replyMSG.append("<center><table width=200><tr>");
		replyMSG.append("<td width=100>Argument(*):</td><td width=100><edit var=\"arg\" width=100></td>");
		replyMSG.append("</tr></table><br></center>");
		replyMSG.append("<table width=270>");

		replyMSG.append("<tr><td width=90><button value=\"Teleport\" action=\"bypass -h admin_teleportto " + player.getName() + "\" width=85 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=90><button value=\"Recall\" action=\"bypass -h admin_recall " + player.getName() + "\" width=85 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=90><button value=\"Quests\" action=\"bypass -h admin_quests " + player.getName() + "\" width=85 height=20 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td></tr>");

		replyMSG.append("</body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void findCharacter(final L2Player activeChar, final String CharacterToFind)
	{
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
		int CharactersFound = 0;

		final StringBuffer replyMSG = new StringBuffer("<html><body>");
		replyMSG.append("<table width=260><tr>");
		replyMSG.append("<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=180><center>Character Selection Menu</center></td>");
		replyMSG.append("<td width=40><button value=\"Back\" action=\"bypass -h admin_show_characters 0\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<br><br>");

		for(final L2Player element : L2World.getAllPlayers())
			if(element.getName().startsWith(CharacterToFind))
			{
				CharactersFound = CharactersFound + 1;
				replyMSG.append("<table width=270>");
				replyMSG.append("<tr><td width=80>Name</td><td width=110>Class</td><td width=40>Level</td></tr>");
				replyMSG.append("<tr><td width=80><a action=\"bypass -h admin_character_list " + element.getName() + "\">" + element.getName() + "</a></td><td width=110>" + element.getTemplate().className + "</td><td width=40>" + element.getLevel() + "</td></tr>");
				replyMSG.append("</table>");
			}

		if(CharactersFound == 0)
		{
			replyMSG.append("<table width=270>");
			replyMSG.append("<tr><td width=270>Your search did not find any characters.</td></tr>");
			replyMSG.append("<tr><td width=270>Please try again.<br></td></tr>");
			replyMSG.append("</table><br>");
			replyMSG.append("<center><table><tr><td>");
			replyMSG.append("<edit var=\"character_name\" width=80></td><td><button value=\"Find\" action=\"bypass -h admin_find_character $character_name\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\">");
			replyMSG.append("</td></tr></table></center>");
		}
		else
		{
			replyMSG.append("<center><br>Found " + CharactersFound + " character");

			if(CharactersFound == 1)
				replyMSG.append(".");
			else if(CharactersFound > 1)
				replyMSG.append("s.");
		}

		replyMSG.append("</center></body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void addExpSp(final L2Player activeChar)
	{
		final L2Object target = activeChar.getTarget();
		L2Player player;
		if(target != null && target.isPlayer() && (activeChar == target || activeChar.getPlayerAccess().CanEditCharAll))
			player = (L2Player) target;
		else
		{
			activeChar.sendPacket(Msg.INVALID_TARGET);
			return;
		}

		final NpcHtmlMessage adminReply = new NpcHtmlMessage(5);

		final StringBuffer replyMSG = new StringBuffer("<html><body>");
		replyMSG.append("<table width=260><tr>");
		replyMSG.append("<td width=40><button value=\"Main\" action=\"bypass -h admin_admin\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("<td width=180><center>Character Selection Menu</center></td>");
		replyMSG.append("<td width=40><button value=\"Back\" action=\"bypass -h admin_current_player\" width=40 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table>");
		replyMSG.append("<br><br>");
		replyMSG.append("<table width=270><tr><td>Name: " + player.getName() + "</td></tr>");
		replyMSG.append("<tr><td>Lv: " + player.getLevel() + " " + player.getTemplate().className + "</td></tr>");
		replyMSG.append("<tr><td>Exp: " + player.getExp() + "</td></tr>");
		replyMSG.append("<tr><td>Sp: " + player.getSp() + "</td></tr></table>");
		replyMSG.append("<br><table width=270><tr><td>Note: Dont forget that modifying players skills can</td></tr>");
		replyMSG.append("<tr><td>ruin the game...</td></tr></table><br>");
		replyMSG.append("<table width=270><tr><td>Note: Fill all values before saving the modifications.,</td></tr>");
		replyMSG.append("<tr><td>Note: Use 0 if no changes are needed.</td></tr></table><br>");
		replyMSG.append("<center><table><tr>");
		replyMSG.append("<td>Exp: <edit var=\"exp_to_add\" width=50></td>");
		replyMSG.append("<td>Sp:  <edit var=\"sp_to_add\" width=50></td>");
		replyMSG.append("<td>&nbsp;<button value=\"Save Changes\" action=\"bypass -h admin_add_exp_sp $exp_to_add & $sp_to_add &\" width=80 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
		replyMSG.append("</tr></table></center>");
		replyMSG.append("</body></html>");

		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}

	private void adminAddExpSp(final L2Player activeChar, final String ExpSp)
	{
		if( !activeChar.getPlayerAccess().CanEditCharAll)
		{
			activeChar.sendMessage("You have not enough privileges, for use this function.");
			return;
		}

		final L2Object target = activeChar.getTarget();
		if(target == null || !target.isPlayer())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.SELECT_TARGET));
			return;
		}

		final L2Player player = (L2Player) target;
		final String[] strvals = ExpSp.split("&");
		final long[] vals = new long[strvals.length];
		for(int i = 0; i < strvals.length; i++)
		{
			strvals[i] = strvals[i].trim();
			vals[i] = strvals[i].isEmpty() ? 0 : Long.parseLong(strvals[i]);
		}

		player.addExpAndSp(vals[0], vals[1], false, false);
		player.sendMessage("Admin is adding you " + vals[0] + " exp and " + vals[1] + " SP.");
		activeChar.sendMessage("Added " + vals[0] + " exp and " + vals[1] + " SP to " + player.getName() + ".");
		Log.add("Added " + vals[0] + " exp and " + vals[1] + " SP to " + player.getName(), "gm_ext_actions", activeChar);
	}

	private void setSubclass(final L2Player activeChar, final L2Player player)
	{
		final StringBuffer content = new StringBuffer("<html><body>");
		final NpcHtmlMessage html = new NpcHtmlMessage(5);
		Set<PlayerClass> subsAvailable;
		subsAvailable = getAvailableSubClasses(player);

		if(subsAvailable != null && !subsAvailable.isEmpty())
		{
			content.append("Add Subclass:<br>Which subclass do you wish to add?<br>");

			for(final PlayerClass subClass : subsAvailable)
				content.append("<a action=\"bypass -h admin_setsubclass " + subClass.ordinal() + "\">" + formatClassForDisplay(subClass) + "</a><br>");
		}
		else
		{
			activeChar.sendMessage(new CustomMessage("l2d.game.model.instances.L2VillageMasterInstance.NoSubAtThisTime", activeChar));
			return;
		}
		content.append("</body></html>");
		html.setHtml(content.toString());
		activeChar.sendPacket(html);
	}

	private Set<PlayerClass> getAvailableSubClasses(final L2Player player)
	{
		final int charClassId = player.getBaseClassId();

		final PlayerClass currClass = PlayerClass.values()[charClassId];// .valueOf(charClassName);

		/**
		 * If the race of your main class is Elf or Dark Elf, you may not select
		 * each class as a subclass to the other class, and you may not select
		 * Overlord and Warsmith class as a subclass.
		 * You may not select a similar class as the subclass. The occupations
		 * classified as similar classes are as follows:
		 * Treasure Hunter, Plainswalker and Abyss Walker Hawkeye, Silver Ranger
		 * and Phantom Ranger Paladin, Dark Avenger, Temple Knight and Shillien
		 * Knight Warlocks, Elemental Summoner and Phantom Summoner Elder and
		 * Shillien Elder Swordsinger and Bladedancer Sorcerer, Spellsinger and
		 * Spellhowler
		 * Kamael могут брать только сабы Kamael
		 * Другие классы не могут брать сабы Kamael
		 */
		final Set<PlayerClass> availSubs = currClass.getAvailableSubclasses();
		if(availSubs == null)
			return null;

		// Из списка сабов удаляем мейн класс игрока
		availSubs.remove(currClass);

		for(final PlayerClass availSub : availSubs)
		{
			// Удаляем из списка возможных сабов, уже взятые сабы и их предков
			for(final L2SubClass subClass : player.getSubClasses().values())
			{
				if(availSub.ordinal() == subClass.getClassId())
				{
					availSubs.remove(availSub);
					continue;
				}

				// Удаляем из возможных сабов их родителей, если таковые есть у чара
				final ClassId parent = ClassId.values()[availSub.ordinal()].getParent(player.getSex());
				if(parent != null && parent.getId() == subClass.getClassId())
				{
					availSubs.remove(availSub);
					continue;
				}

				// Удаляем из возможных сабов родителей текущих сабклассов, иначе если взять саб berserker
				// и довести до 3ей профы - doombringer, игроку будет предложен berserker вновь (дежавю)
				final ClassId subParent = ClassId.values()[subClass.getClassId()].getParent(player.getSex());
				if(subParent != null && subParent.getId() == availSub.ordinal())
					availSubs.remove(availSub);
			}
		}
		return availSubs;
	}

	private String formatClassForDisplay(final PlayerClass className)
	{
		String classNameStr = className.toString();
		final char[] charArray = classNameStr.toCharArray();

		for(int i = 1; i < charArray.length; i++)
			if(Character.isUpperCase(charArray[i]))
				classNameStr = classNameStr.substring(0, i) + " " + classNameStr.substring(i);

		return classNameStr;
	}

	@Override
	public void onLoad()
	{
		AdminCommandHandler.getInstance().registerAdminCommandHandler(this);
	}

	@Override
	public void onReload()
	{}

	@Override
	public void onShutdown()
	{}
}