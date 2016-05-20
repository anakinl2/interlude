package l2d.game.model.instances;

import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import javolution.text.TextBuilder;
import l2d.Config;
import l2d.ext.multilang.CustomMessage;
import l2d.game.instancemanager.CastleSiegeManager;
import l2d.game.instancemanager.SiegeManager;
import l2d.game.model.L2Alliance;
import l2d.game.model.L2Clan;
import l2d.game.model.L2Clan.SubPledge;
import l2d.game.model.L2ClanMember;
import l2d.game.model.L2Player;
import l2d.game.model.L2SkillLearn;
import l2d.game.model.L2SubClass;
import l2d.game.model.base.ClassId;
import l2d.game.model.base.ClassType;
import l2d.game.model.base.PlayerClass;
import l2d.game.model.base.Race;
import l2d.game.model.entity.olympiad.Olympiad;
import l2d.game.model.entity.siege.SiegeDatabase;
import l2d.game.model.quest.QuestState;
import l2d.game.serverpackets.AcquireSkillList;
import l2d.game.serverpackets.NpcHtmlMessage;
import l2d.game.serverpackets.PledgeReceiveSubPledgeCreated;
import l2d.game.serverpackets.PledgeShowInfoUpdate;
import l2d.game.serverpackets.PledgeShowMemberListAll;
import l2d.game.serverpackets.PledgeShowMemberListUpdate;
import l2d.game.serverpackets.PledgeStatusChanged;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.CharTemplateTable;
import l2d.game.tables.ClanTable;
import l2d.game.tables.SkillTable;
import l2d.game.tables.SkillTreeTable;
import l2d.game.templates.L2NpcTemplate;
import l2d.util.Util;

public final class L2VillageMasterInstance extends L2NpcInstance
{
	private static Logger _log = Logger.getLogger(L2VillageMasterInstance.class.getName());

	/**
	 * @param template
	 */
	public L2VillageMasterInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		// first do the common stuff
		// and handle the commands that all NPC classes know
		super.onBypassFeedback(player, command);

		if(command.startsWith("create_clan") && command.length() > 12)
		{
			String val = command.substring(12);
			createClan(player, val);
		}
		else if(command.startsWith("create_academy") && command.length() > 15)
		{
			String sub = command.substring(15, command.length());
			createSubPledge(player, sub, L2Clan.SUBUNIT_ACADEMY, 5, "");
		}
		else if(command.startsWith("create_royal") && command.length() > 15)
		{
			String[] sub = command.substring(13, command.length()).split(" ", 2);
			if(sub.length == 2)
				createSubPledge(player, sub[1], L2Clan.SUBUNIT_ROYAL1, 6, sub[0]);
		}
		else if(command.startsWith("create_knight") && command.length() > 16)
		{
			String[] sub = command.substring(14, command.length()).split(" ", 2);
			if(sub.length == 2)
				createSubPledge(player, sub[1], L2Clan.SUBUNIT_KNIGHT1, 7, sub[0]);
		}
		else if(command.startsWith("assign_subpl_leader") && command.length() > 22)
		{
			String[] sub = command.substring(20, command.length()).split(" ", 2);
			if(sub.length == 2)
				assignSubPledgeLeader(player, sub[1], sub[0]);
		}
		else if(command.startsWith("assign_new_clan_leader") && command.length() > 23)
		{
			String val = command.substring(23);
			setLeader(player, val);
			if(Config.DEBUG)
				_log.fine("Clan " + player.getClan() + " assign new clan leader: " + val + ".");
		}
		if(command.startsWith("create_ally") && command.length() > 12)
		{
			String val = command.substring(12);
			createAlly(player, val);
		}
		else if(command.startsWith("dissolve_ally"))
			dissolveAlly(player);
		else if(command.startsWith("dissolve_clan"))
			dissolveClan(player);
		else if(command.startsWith("increase_clan_level"))
			levelUpClan(player);
		else if(command.startsWith("learn_clan_skills"))
			showClanSkillWindow(player);
		else if(command.startsWith("Subclass"))
		{
			if(player.getPet() != null && !player.getPet().isPet())
			{
				player.sendPacket(new SystemMessage(SystemMessage.A_SUB_CLASS_MAY_NOT_BE_CREATED_OR_CHANGED_WHILE_A_SERVITOR_OR_PET_IS_SUMMONED));
				return;
			}

			// Саб класс нельзя получить или поменять, пока используется скилл или персонаж находится в режиме трансформации
			if(player.isActionsDisabled())
			{
				player.sendPacket(new SystemMessage(SystemMessage.SUB_CLASSES_MAY_NOT_BE_CREATED_OR_CHANGED_WHILE_A_SKILL_IS_IN_USE));
				return;
			}

			StringBuffer content = new StringBuffer("<html><body>");
			NpcHtmlMessage html = new NpcHtmlMessage(player, this);

			HashMap<Integer, L2SubClass> playerClassList = player.getSubClasses();
			Set<PlayerClass> subsAvailable;

			int classId = 0;
			int newClassId = 0;
			int intVal = 0;

			try
			{
				for(String id : command.substring(9, command.length()).split(" "))
				{
					if(intVal == 0)
					{
						intVal = Integer.parseInt(id);
						continue;
					}
					if(classId > 0)
					{
						newClassId = Short.parseShort(id);
						continue;
					}
					classId = Short.parseShort(id);
				}
			}
			catch(Exception NumberFormatException)
			{}

			switch(intVal)
			{
				case 1: // Возвращает список сабов, которые можно взять (см case 4)
					subsAvailable = getAvailableSubClasses(player);

					if(subsAvailable != null && !subsAvailable.isEmpty())
					{
						content.append("Add Subclass:<br>Which subclass do you wish to add?<br>");

						for(PlayerClass subClass : subsAvailable)
							content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 4 " + subClass.ordinal() + "\">" + formatClassForDisplay(subClass) + "</a><br>");
					}
					else
					{
						player.sendMessage(new CustomMessage("l2d.game.model.instances.L2VillageMasterInstance.NoSubAtThisTime", player));
						return;
					}
					break;
				case 2: // Установка уже взятого саба (см case 5)
					content.append("Change Subclass:<br>");

					final int baseClassId = player.getBaseClassId();

					if(playerClassList.size() < 2)
						content.append("You can't change subclasses when you don't have a subclass to begin with.<br><a action=\"bypass -h npc_" + getObjectId() + "_Subclass 1\">Add subclass.</a>");
					else
					{
						content.append("Which class would you like to switch to?<br>");

						if(baseClassId == player.getActiveClassId())
							content.append(CharTemplateTable.getClassNameById(baseClassId) + " <font color=\"LEVEL\">(Base Class)</font><br><br>");
						else
							content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 5 " + baseClassId + "\">" + CharTemplateTable.getClassNameById(baseClassId) + "</a> " + "<font color=\"LEVEL\">(Base Class)</font><br><br>");

						for(L2SubClass subClass : playerClassList.values())
						{
							if(subClass.isBase())
								continue;
							int subClassId = subClass.getClassId();

							if(subClassId == player.getActiveClassId())
								content.append(CharTemplateTable.getClassNameById(subClassId) + "<br>");
							else
								content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 5 " + subClassId + "\">" + CharTemplateTable.getClassNameById(subClassId) + "</a><br>");
						}
					}
					break;
				case 3: // Отмена сабкласса - список имеющихся (см case 6)
					content.append("Change Subclass:<br>Which of the following sub-classes would you like to change?<br>");

					for(L2SubClass sub : playerClassList.values())
					{
						content.append("<br>");
						if(!sub.isBase())
							content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 6 " + sub.getClassId() + "\">" + CharTemplateTable.getClassNameById(sub.getClassId()) + "</a><br>");
					}

					content.append("<br>If you change a sub-class, you'll start at level 40 after the 2nd class transfer.");
					break;
				case 4: // Добавление сабкласса - обработка выбора из case 1
					boolean allowAddition = true;

					// Проверка хватает ли уровня
					if(player.getLevel() < Config.ALT_GAME_LEVEL_TO_GET_SUBCLASS)
					{
						player.sendMessage(new CustomMessage("l2d.game.model.instances.L2VillageMasterInstance.NoSubBeforeLevel", player).addNumber(Config.ALT_GAME_LEVEL_TO_GET_SUBCLASS));
						allowAddition = false;
					}

					if(!playerClassList.isEmpty())
						for(L2SubClass subClass : playerClassList.values())
							if(subClass.getLevel() < Config.ALT_GAME_LEVEL_TO_GET_SUBCLASS)
							{
								player.sendMessage(new CustomMessage("l2d.game.model.instances.L2VillageMasterInstance.NoSubBeforeLevel", player).addNumber(Config.ALT_GAME_LEVEL_TO_GET_SUBCLASS));
								allowAddition = false;
								break;
							}

					if(Config.ENABLE_OLYMPIAD && (Olympiad.isRegisteredInComp(player) || player.getOlympiadGameId() > -1))
					{
						player.sendPacket(new SystemMessage(SystemMessage.SINCE_YOU_HAVE_CHANGED_YOUR_CLASS_INTO_A_SUB_JOB_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD));
						_log.warning("Unregister due tj subclass change.");
						Olympiad.unRegisterNoble(player, false);
					}
					/*
					 * Если требуется квест - проверка прохождения Mimir's Elixir (Path to Subclass)
					 * Для камаэлей квест 236_SeedsOfChaos
					 * Если саб первый, то проверить начилие предмета, если не первый, то даём сабкласс.
					 * Если сабов нету, то проверяем наличие предмета.
					 */
					if(!Config.ALT_GAME_SUBCLASS_WITHOUT_QUESTS && !playerClassList.isEmpty() && playerClassList.size() < 2 + Config.ALT_GAME_SUB_ADD)
					{
						QuestState qs = player.getQuestState("_234_FatesWhisper");
						allowAddition = qs != null && qs.isCompleted();
						if(allowAddition)
						{
							qs = player.getQuestState("_235_MimirsElixir");
							allowAddition = qs != null && qs.isCompleted();
							if(!allowAddition)
								player.sendMessage(new CustomMessage("l2d.game.model.instances.L2VillageMasterInstance.QuestMimirsElixir", player));
						}
						else
							player.sendMessage(new CustomMessage("l2d.game.model.instances.L2VillageMasterInstance.QuestFatesWhisper", player));
					}

					if(allowAddition)
					{
						String className = CharTemplateTable.getClassNameById(classId);

						// sm = new SystemMessage(SystemMessage.DO_YOU_WISH_TO_ADD_S1_CLASS_AS_YOUR_SUB_CLASS);
						// sm.addString(className);
						// player.sendPacket(sm); // Addition confirmation.

						if(!player.addSubClass(classId, true))
						{
							player.sendMessage(new CustomMessage("l2d.game.model.instances.L2VillageMasterInstance.SubclassCouldNotBeAdded", player));
							return;
						}

						content.append("Add Subclass:<br>The subclass of <font color=\"LEVEL\">" + className + "</font> has been added.");
						player.sendPacket(new SystemMessage(SystemMessage.CONGRATULATIONS_YOU_HAVE_TRANSFERRED_TO_A_NEW_CLASS)); // Transfer to new class.
					}
					else
						html.setFile("data/html/villagemaster/SubClass_Fail.htm");
					break;
				case 5: // Смена саба на другой из уже взятых - обработка выбора из case 2
					/*
					 * If the character is less than level 75 on any of their
					 * previously chosen classes then disallow them to change to
					 * their most recently added sub-class choice.
					 */
					/*
					 * for(L2SubClass sub : playerClassList.values())
					 * if(sub.isBase() && sub.getLevel() < Config.ALT_GAME_LEVEL_TO_GET_SUBCLASS)
					 * {
					 * player.sendMessage("You may not change to your subclass before you are level " + Config.ALT_GAME_LEVEL_TO_GET_SUBCLASS, "Вы не можете добавить еще сабкласс пока у вас уровень "
					 * + Config.ALT_GAME_LEVEL_TO_GET_SUBCLASS + " на Вашем предыдущем сабклассе.");
					 * return;
					 * }
					 */

					if(Config.ENABLE_OLYMPIAD && Olympiad.isRegisteredInComp(player) || player.getOlympiadGameId() > -1)
					{
						player.sendPacket(new SystemMessage(SystemMessage.SINCE_YOU_HAVE_CHANGED_YOUR_CLASS_INTO_A_SUB_JOB_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD));
						Olympiad.unRegisterNoble(player, true);
					}

					player.setActiveSubClass(classId, true);

					content.append("Change Subclass:<br>Your active subclass is now a <font color=\"LEVEL\">" + CharTemplateTable.getClassNameById(player.getActiveClassId()) + "</font>.");

					player.sendPacket(new SystemMessage(SystemMessage.THE_TRANSFER_OF_SUB_CLASS_HAS_BEEN_COMPLETED)); // Transfer
					// completed.
					break;
				case 6: // Отмена сабкласса - обработка выбора из case 3
					content.append("Please choose a subclass to change to. If the one you are looking for is not here, " + //
					"please seek out the appropriate master for that class.<br>" + //
					"<font color=\"LEVEL\">Warning!</font> All classes and skills for this class will be removed.<br><br>");

					subsAvailable = getAvailableSubClasses(player);

					if(!subsAvailable.isEmpty())
						for(PlayerClass subClass : subsAvailable)
							content.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 7 " + classId + " " + subClass.ordinal() + "\">" + formatClassForDisplay(subClass) + "</a><br>");
					else
					{
						player.sendMessage(new CustomMessage("l2d.game.model.instances.L2VillageMasterInstance.NoSubAtThisTime", player));
						return;
					}
					break;
				case 7: // Отмена сабкласса - обработка выбора из case 6
					// player.sendPacket(new SystemMessage(SystemMessage.YOUR_PREVIOUS_SUB_CLASS_WILL_BE_DELETED_AND_YOUR_NEW_SUB_CLASS_WILL_START_AT_LEVEL_40__DO_YOU_WISH_TO_PROCEED)); // Change
					// confirmation.

					if(player.modifySubClass(classId, newClassId))
					{
						/*
						 * if(player.getLevel() < Config.ALT_GAME_LEVEL_TO_GET_SUBCLASS && player.getActiveClass() == player.getBaseClass())
						 * {
						 * player.sendMessage("You may not switch to your subclass before you are level " + Config.ALT_GAME_LEVEL_TO_GET_SUBCLASS,
						 * "Вы не можете сменить свой сабкласс пока у Вас не будет " + Config.ALT_GAME_LEVEL_TO_GET_SUBCLASS + " уровня");
						 * return;
						 * }
						 */

						if(Config.ENABLE_OLYMPIAD && Olympiad.isRegisteredInComp(player) || player.getOlympiadGameId() > -1)
						{
							player.sendPacket(new SystemMessage(SystemMessage.SINCE_YOU_HAVE_CHANGED_YOUR_CLASS_INTO_A_SUB_JOB_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD));
							Olympiad.unRegisterNoble(player, false);
						}

						content.append("Change Subclass:<br>Your subclass has been changed to <font color=\"LEVEL\">" + CharTemplateTable.getClassNameById(newClassId) + "</font>.");
						player.sendPacket(new SystemMessage(SystemMessage.THE_NEW_SUB_CLASS_HAS_BEEN_ADDED)); // Subclass added.
					}
					else
					{
						player.sendMessage(new CustomMessage("l2d.game.model.instances.L2VillageMasterInstance.SubclassCouldNotBeAdded", player));
						return;
					}
					break;
			}
			content.append("</body></html>");

			// If the content is greater than for a basic blank page,
			// then assume no external HTML file was assigned.
			if(content.length() > 26)
				html.setHtml(content.toString());

			player.sendPacket(html);
		}
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String pom;
		if(val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		return "data/html/villagemaster/" + pom + ".htm";
	}

	// Private stuff
	public void createClan(L2Player player, String clanName)
	{
		if(Config.DEBUG)
			_log.fine(player.getObjectId() + "(" + player.getName() + ") requested clan creation from " + getObjectId() + "(" + getName() + ")");
		if(player.getLevel() < 10)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_QUALIFIED_TO_CREATE_A_CLAN));
			return;
		}

		if(player.getClanId() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_CREATE_A_CLAN));
			return;
		}

		if(!player.canCreateClan())
		{
			// you can't create a new clan within 10 days
			player.sendPacket(new SystemMessage(SystemMessage.YOU_MUST_WAIT_10_DAYS_BEFORE_CREATING_A_NEW_CLAN));
			return;
		}
		if(clanName.length() > 16)
		{
			player.sendPacket(new SystemMessage(SystemMessage.CLAN_NAMES_LENGTH_IS_INCORRECT));
			return;
		}
		if(!Util.isMatchingRegexp(clanName, Config.CLAN_NAME_TEMPLATE))
		{
			// clan name is not matching template
			player.sendPacket(new SystemMessage(SystemMessage.CLAN_NAME_IS_INCORRECT));
			return;
		}

		L2Clan clan = ClanTable.getInstance().createClan(player, clanName);
		if(clan == null)
		{
			// clan name is already taken
			player.sendPacket(new SystemMessage(SystemMessage.CLAN_NAME_IS_INCORRECT));
			return;
		}

		// should be update packet only
		player.sendPacket(new PledgeShowInfoUpdate(clan));
		player.sendPacket(new PledgeShowMemberListAll(clan, player));
		player.updatePledgeClass();
		player.broadcastUserInfo(true);
		player.sendPacket(new SystemMessage(SystemMessage.CLAN_HAS_BEEN_CREATED));
	}

	public void setLeader(L2Player player, String name)
	{
		if(!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_THE_CLAN_LEADER_IS_ENABLED));
			return;
		}

		L2Clan clan = player.getClan();

		if(clan == null)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_CREATE_A_CLAN));
			return;
		}

		L2ClanMember member = clan.getClanMember(name);
		if(member == null)
		{
			player.sendMessage(new CustomMessage("l2d.game.model.instances.L2VillageMasterInstance.S1IsNotMemberOfTheClan", player).addString(name));
			showChatWindow(player, "data/html/villagemaster/clan-20.htm");
			return;
		}
		// TODO: В данной редакции смена лидера производится сразу же.
		// Надо подумать над реализацией смены кланлидера в запланированный день недели.
		clan.setLeader(member);
		clan.broadcastClanStatus(true, true);
		clan.updateClanInDB();

		player.sendMessage(new CustomMessage("l2d.game.model.instances.L2VillageMasterInstance.ClanLeaderWillBeChangedFromS1ToS2", player).addString(player.getName()).addString(name));
	}

	public void createSubPledge(L2Player player, String clanName, int pledgeType, int minClanLvl, String leaderName)
	{
		int subLeaderId = 0;
		L2ClanMember subLeader = null;

		L2Clan clan = player.getClan();

		if(clan == null)
		{
			SystemMessage sm = new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_CREATE_A_CLAN);
			player.sendPacket(sm);
			return;
		}

		if(!player.isClanLeader())
		{
			SystemMessage sm = new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_CREATE_A_CLAN);
			player.sendPacket(sm);
			return;
		}

		if(!Util.isMatchingRegexp(clanName, Config.CLAN_NAME_TEMPLATE))
		{
			SystemMessage sm = new SystemMessage(SystemMessage.CLAN_NAME_IS_INCORRECT);
			player.sendPacket(sm);
			return;
		}

		SubPledge[] subPledge = clan.getAllSubPledges();
		for(SubPledge element : subPledge)
			if(element.getName().equals(clanName))
			{
				SystemMessage sm = new SystemMessage(SystemMessage.ANOTHER_MILITARY_UNIT_IS_ALREADY_USING_THAT_NAME_PLEASE_ENTER_A_DIFFERENT_NAME);
				player.sendPacket(sm);
				return;
			}

		if(ClanTable.getInstance().getClanByName(clanName) != null)
		{
			SystemMessage sm = new SystemMessage(SystemMessage.ANOTHER_MILITARY_UNIT_IS_ALREADY_USING_THAT_NAME_PLEASE_ENTER_A_DIFFERENT_NAME);
			player.sendPacket(sm);
			return;
		}

		if(clan.getLevel() < minClanLvl)
		{
			SystemMessage sm = new SystemMessage(SystemMessage.THE_CONDITIONS_NECESSARY_TO_CREATE_A_MILITARY_UNIT_HAVE_NOT_BEEN_MET);
			player.sendPacket(sm);
			return;
		}

		if(pledgeType != L2Clan.SUBUNIT_ACADEMY)
		{
			subLeader = clan.getClanMember(leaderName);
			if(subLeader == null || subLeader.getPledgeType() != L2Clan.SUBUNIT_NONE)
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2VillageMasterInstance.PlayerCantBeAssignedAsSubUnitLeader", player));
				return;
			}
			else if(subLeader.isClanLeader())
			{
				player.sendMessage(new CustomMessage("l2d.game.model.instances.L2VillageMasterInstance.YouCantBeASubUnitLeader", player));
				return;
			}
			else
				subLeaderId = subLeader.getObjectId();
		}

		pledgeType = clan.createSubPledge(player, pledgeType, subLeaderId, clanName);
		if(pledgeType == L2Clan.SUBUNIT_NONE)
			return;

		clan.broadcastToOnlineMembers(new PledgeReceiveSubPledgeCreated(clan.getSubPledge(pledgeType)));

		SystemMessage sm;
		if(pledgeType == L2Clan.SUBUNIT_ACADEMY)
		{
			sm = new SystemMessage(SystemMessage.CONGRATULATIONS_THE_S1S_CLAN_ACADEMY_HAS_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else if(pledgeType >= L2Clan.SUBUNIT_KNIGHT1)
		{
			sm = new SystemMessage(SystemMessage.THE_KNIGHTS_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else if(pledgeType >= L2Clan.SUBUNIT_ROYAL1)
		{
			sm = new SystemMessage(SystemMessage.THE_ROYAL_GUARD_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(player.getClan().getName());
		}
		else
			sm = new SystemMessage(SystemMessage.CLAN_HAS_BEEN_CREATED);

		player.sendPacket(sm);

		if(subLeader != null)
		{
			clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(subLeader));
			if(subLeader.isOnline())
			{
				subLeader.getPlayer().updatePledgeClass();
				subLeader.getPlayer().broadcastUserInfo(true);
			}
		}
	}

	public void assignSubPledgeLeader(L2Player player, String clanName, String leaderName)
	{
		L2Clan clan = player.getClan();

		if(clan == null)
		{
			player.sendMessage(new CustomMessage("l2d.game.model.instances.L2VillageMasterInstance.ClanDoesntExist", player));
			return;
		}

		if(!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_THE_CLAN_LEADER_IS_ENABLED));
			return;
		}

		SubPledge[] subPledge = clan.getAllSubPledges();
		int match = -1;
		for(int i = 0; i < subPledge.length; i++)
			if(subPledge[i].getName().equals(clanName))
			{
				match = i;
				break;
			}
		if(match < 0)
		{
			player.sendMessage(new CustomMessage("l2d.game.model.instances.L2VillageMasterInstance.SubUnitNotFound", player));
			return;
		}

		L2ClanMember subLeader = clan.getClanMember(leaderName);
		if(subLeader == null || subLeader.getPledgeType() != L2Clan.SUBUNIT_NONE)
		{
			player.sendMessage(new CustomMessage("l2d.game.model.instances.L2VillageMasterInstance.PlayerCantBeAssignedAsSubUnitLeader", player));
			return;
		}

		if(subLeader.isClanLeader())
		{
			player.sendMessage(new CustomMessage("l2d.game.model.instances.L2VillageMasterInstance.YouCantBeASubUnitLeader", player));
			return;
		}

		subPledge[match].setLeaderId(subLeader.getObjectId());
		clan.broadcastToOnlineMembers(new PledgeReceiveSubPledgeCreated(subPledge[match]));

		clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(subLeader));
		if(subLeader.isOnline())
		{
			subLeader.getPlayer().updatePledgeClass();
			subLeader.getPlayer().broadcastUserInfo(true);
		}

		player.sendMessage(new CustomMessage("l2d.game.model.instances.L2VillageMasterInstance.NewSubUnitLeaderHasBeenAssigned", player));
	}

	private void dissolveClan(L2Player player)
	{
		if(player == null || player.getClan() == null)
			return;
		L2Clan clan = player.getClan();

		if(!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_THE_CLAN_LEADER_IS_ENABLED));
			return;
		}
		if(clan.getAllyId() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_DISPERSE_THE_CLANS_IN_YOUR_ALLIANCE));
			return;
		}
		if(clan.isAtWar() > 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_DISSOLVE_A_CLAN_WHILE_ENGAGED_IN_A_WAR));
			return;
		}
		if(clan.getHasCastle() != 0 || clan.getHasHideout() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.UNABLE_TO_DISPERSE_YOUR_CLAN_OWNS_ONE_OR_MORE_CASTLES_OR_HIDEOUTS));
			return;
		}
		if(SiegeDatabase.checkIsRegistered(clan, 0))
		{
			player.sendPacket(new SystemMessage(SystemMessage.UNABLE_TO_DISPERSE_YOUR_CLAN_HAS_REQUESTED_TO_PARTICIPATE_IN_A_CASTLE_SIEGE));
			return;
		}

		ClanTable.getInstance().dissolveClan(player);
	}

	public void levelUpClan(L2Player player)
	{
		L2Clan clan = player.getClan();
		if(clan == null)
			return;
		if(!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_THE_CLAN_LEADER_IS_ENABLED));
			return;
		}

		boolean increaseClanLevel = false;

		switch(clan.getLevel())
		{
			case 0:
				// Upgrade to 1
				if(player.getSp() >= 20000 && player.getAdena() >= 650000 || player.isGM())
				{
					if(!player.isGM())
					{
						player.setSp(player.getSp() - 20000);
						player.reduceAdena(650000);
						increaseClanLevel = true;
					}
					increaseClanLevel = true;
				}
				break;
			case 1:
				// Upgrade to 2
				if(player.getSp() >= 100000 && player.getAdena() >= 2500000 || player.isGM())
				{
					if(!player.isGM())
					{
						player.setSp(player.getSp() - 100000);
						player.reduceAdena(2500000);
						increaseClanLevel = true;
					}
					increaseClanLevel = true;
				}
				break;
			case 2:
				// Upgrade to 3
				// itemid 1419 == Blood Mark
				if(player.getSp() >= 350000 && player.getInventory().getItemByItemId(1419) != null || player.isGM())
				{
					if(!player.isGM())
					{
						player.setSp(player.getSp() - 350000);
						player.getInventory().destroyItemByItemId(1419, 1, true);
						increaseClanLevel = true;
					}
					increaseClanLevel = true;
				}
				break;
			case 3:
				// Upgrade to 4
				// itemid 3874 == Alliance Manifesto
				if(player.getSp() >= 1000000 && player.getInventory().getItemByItemId(3874) != null || player.isGM())
				{
					if(!player.isGM())
					{
						player.setSp(player.getSp() - 1000000);
						player.getInventory().destroyItemByItemId(3874, 1, true);
						increaseClanLevel = true;
					}
					increaseClanLevel = true;
				}
				break;
			case 4:
				// Upgrade to 5
				// itemid 3870 == Seal of Aspiration
				if(player.getSp() >= 2500000 && player.getInventory().getItemByItemId(3870) != null || player.isGM())
				{
					if(!player.isGM())
					{
						player.setSp(player.getSp() - 2500000);
						player.getInventory().destroyItemByItemId(3870, 1, true);
						increaseClanLevel = true;
					}
					increaseClanLevel = true;
				}
				break;
			case 5:
				// Upgrade to 6
				if(clan.getReputationScore() >= 10000 && clan.getMembersCount() >= 15 || player.isGM())
				{
					if(!player.isGM())
					{
						clan.incReputation(-10000, false, "LvlUpClan");
						increaseClanLevel = true;
					}
					increaseClanLevel = true;
				}
				break;
			case 6:
				// Upgrade to 7
				if(clan.getReputationScore() >= 20000 && clan.getMembersCount() >= 40 || player.isGM())
				{
					if(!player.isGM())
					{
						clan.incReputation(-20000, false, "LvlUpClan");
						increaseClanLevel = true;
					}
					increaseClanLevel = true;
				}
				break;
			case 7:
				// Upgrade to 8
				if(clan.getReputationScore() >= 40000 && clan.getMembersCount() >= 60 || player.isGM())
				{
					if(!player.isGM())
					{
						clan.incReputation(-40000, false, "LvlUpClan");
						increaseClanLevel = true;
					}
					increaseClanLevel = true;
				}
				break;
		}

		if(increaseClanLevel)
		{
			player.sendChanges();

			clan.setLevel((byte) (clan.getLevel() + 1));
			clan.updateClanInDB();
			doCast(SkillTable.getInstance().getInfo(5103, 1), player, true);

			if(clan.getLevel() >= CastleSiegeManager.getSiegeClanMinLevel())
				SiegeManager.addSiegeSkills(player);

			if(clan.getLevel() == 5)
				player.sendPacket(new SystemMessage(SystemMessage.NOW_THAT_YOUR_CLAN_LEVEL_IS_ABOVE_LEVEL_5_IT_CAN_ACCUMULATE_CLAN_REPUTATION_POINTS));

			// notify all the members about it
			SystemMessage sm = new SystemMessage(SystemMessage.CLANS_SKILL_LEVEL_HAS_INCREASED);
			PledgeShowInfoUpdate pu = new PledgeShowInfoUpdate(clan);
			PledgeStatusChanged ps = new PledgeStatusChanged(clan);
			for(L2ClanMember mbr : clan.getMembers())
				if(mbr.isOnline())
				{
					mbr.getPlayer().updatePledgeClass();
					mbr.getPlayer().sendPacket(sm);
					mbr.getPlayer().sendPacket(pu);
					mbr.getPlayer().sendPacket(ps);
					mbr.getPlayer().broadcastUserInfo(true);
				}
		}
		else
			player.sendPacket(new SystemMessage(SystemMessage.CLAN_HAS_FAILED_TO_INCREASE_SKILL_LEVEL));
	}

	public void createAlly(L2Player player, String allyName)
	{
		// D5 You may not ally with clan you are battle with.
		// D6 Only the clan leader may apply for withdraw from alliance.
		// DD No response. Invitation to join an
		// D7 Alliance leaders cannot withdraw.
		// D9 Different Alliance
		// EB alliance information
		// Ec alliance name $s1
		// ee alliance leader: $s2 of $s1
		// ef affilated clans: total $s1 clan(s)
		// f6 you have already joined an alliance
		// f9 you cannot new alliance 10 days
		// fd cannot accept. clan ally is register as enemy during siege battle.
		// fe you have invited someone to your alliance.
		// 100 do you wish to withdraw from the alliance
		// 102 enter the name of the clan you wish to expel.
		// 202 do you realy wish to dissolve the alliance
		// 502 you have accepted alliance
		// 602 you have failed to invite a clan into the alliance
		// 702 you have withdraw

		if(Config.DEBUG)
			_log.fine(player.getObjectId() + "(" + player.getName() + ") requested ally creation from " + getObjectId() + "(" + getName() + ")");

		if(!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_CLAN_LEADERS_MAY_CREATE_ALLIANCES));
			return;
		}
		if(player.getClan().getAllyId() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ALREADY_BELONG_TO_ANOTHER_ALLIANCE));
			return;
		}
		if(allyName.length() > 16)
		{
			player.sendPacket(new SystemMessage(SystemMessage.INCORRECT_LENGTH_FOR_AN_ALLIANCE_NAME));
			return;
		}
		if(!Util.isMatchingRegexp(allyName, Config.ALLY_NAME_TEMPLATE))
		{
			player.sendPacket(new SystemMessage(SystemMessage.INCORRECT_ALLIANCE_NAME));
			return;
		}
		if(player.getClan().getLevel() < 5)
		{
			player.sendPacket(new SystemMessage(SystemMessage.TO_CREATE_AN_ALLIANCE_YOUR_CLAN_MUST_BE_LEVEL_5_OR_HIGHER));
			return;
		}
		if(ClanTable.getInstance().getAllyByName(allyName) != null)
		{
			player.sendPacket(new SystemMessage(SystemMessage.THIS_ALLIANCE_NAME_ALREADY_EXISTS));
			return;
		}
		if(!player.getClan().canCreateAlly())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_CREATE_A_NEW_ALLIANCE_WITHIN_10_DAYS_AFTER_DISSOLUTION));
			return;
		}

		L2Alliance alliance = ClanTable.getInstance().createAlliance(player, allyName);
		if(alliance == null)
			return;

		player.broadcastUserInfo(true);
		player.sendMessage("Alliance " + allyName + " has been created.");
	}

	private void dissolveAlly(L2Player player)
	{
		if(player == null || player.getAlliance() == null)
			return;

		if(!player.isAllyLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessage.FEATURE_AVAILABLE_TO_ALLIANCE_LEADERS_ONLY));
			return;
		}

		if(player.getAlliance().getMembersCount() > 1)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_DISSOLVE_THE_ALLIANCE));
			return;
		}

		ClanTable.getInstance().dissolveAlly(player);
	}

	private Set<PlayerClass> getAvailableSubClasses(L2Player player)
	{
		final int charClassId = player.getBaseClassId();
		final Race npcRace = getVillageMasterRace();
		final ClassType npcTeachType = getVillageMasterTeachType();

		PlayerClass currClass = PlayerClass.values()[charClassId];// .valueOf(charClassName);

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
		Set<PlayerClass> availSubs = currClass.getAvailableSubclasses();
		if(availSubs == null)
			return null;

		// Из списка сабов удаляем мейн класс игрока
		availSubs.remove(currClass);

		for(PlayerClass availSub : availSubs)
		{
			// Удаляем из списка возможных сабов, уже взятые сабы и их предков
			for(L2SubClass subClass : player.getSubClasses().values())
			{
				if(availSub.ordinal() == subClass.getClassId())
				{
					availSubs.remove(availSub);
					continue;
				}

				// Удаляем из возможных сабов их родителей, если таковые есть у чара
				ClassId parent = ClassId.values()[availSub.ordinal()].getParent(player.getSex());
				if(parent != null && parent.getId() == subClass.getClassId())
				{
					availSubs.remove(availSub);
					continue;
				}

				// Удаляем из возможных сабов родителей текущих сабклассов, иначе если взять саб berserker
				// и довести до 3ей профы - doombringer, игроку будет предложен berserker вновь (дежавю)
				ClassId subParent = ClassId.values()[subClass.getClassId()].getParent(player.getSex());
				if(subParent != null && subParent.getId() == availSub.ordinal())
					availSubs.remove(availSub);
			}

			if(!availSub.isOfRace(Race.human) && !availSub.isOfRace(Race.elf))
			{
				if(!availSub.isOfRace(npcRace))
					availSubs.remove(availSub);
			}
			else if(!availSub.isOfType(npcTeachType))
				availSubs.remove(availSub);
		}
		return availSubs;
	}

	private String formatClassForDisplay(PlayerClass className)
	{
		String classNameStr = className.toString();
		char[] charArray = classNameStr.toCharArray();

		for(int i = 1; i < charArray.length; i++)
			if(Character.isUpperCase(charArray[i]))
				classNameStr = classNameStr.substring(0, i) + " " + classNameStr.substring(i);

		return classNameStr;
	}

	private Race getVillageMasterRace()
	{
		switch(getTemplate().getRace())
		{
			case 14:
				return Race.human;
			case 15:
				return Race.elf;
			case 16:
				return Race.darkelf;
			case 17:
				return Race.orc;
			case 18:
				return Race.dwarf;
		}
		return null;
	}

	private ClassType getVillageMasterTeachType()
	{
		String npcClass = getTemplate().getJClass();

		if(npcClass.indexOf("sanctuary") > -1 || npcClass.indexOf("clergyman") > -1)
			return ClassType.Priest;

		if(npcClass.indexOf("mageguild") > -1 || npcClass.indexOf("patriarch") > -1)
			return ClassType.Mystic;

		return ClassType.Fighter;
	}

	/**
	 * this displays PledgeSkillList to the player.
	 * 
	 * @param player
	 */
	public void showClanSkillWindow(L2Player player)
	{
		if(player == null || player.getClan() == null)
			return;

		if(!Config.ALLOW_CLANSKILLS)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(player, this);
			TextBuilder sb = new TextBuilder();
			sb.append("<html><head><body>");
			sb.append("Not available now, try later.<br>");
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
			return;
		}

		L2SkillLearn[] skills = SkillTreeTable.getInstance().getAvailableClanSkills(player.getClan());
		AcquireSkillList asl = new AcquireSkillList(AcquireSkillList.CLAN);
		int counts = 0;

		for(L2SkillLearn s : skills)
		{
			counts++;
			asl.addSkill(s.getId(), s.getLevel(), s.getLevel(), s.getRepCost(), 0);
		}

		if(counts == 0)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(player, this);

			if(player.getClan().getLevel() < 10)
			{
				SystemMessage sm = new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ANY_FURTHER_SKILLS_TO_LEARN__COME_BACK_WHEN_YOU_HAVE_REACHED_LEVEL_S1);
				sm.addNumber(player.getClan().getLevel() + 1);
				player.sendPacket(sm);
			}
			else
			{
				TextBuilder sb = new TextBuilder();
				sb.append("<html><head><body>");
				sb.append("You've learned all skills available for your Clan.<br>");
				sb.append("</body></html>");
				html.setHtml(sb.toString());
				player.sendPacket(html);
			}
		}
		else
			player.sendPacket(asl);
		player.sendActionFailed();
	}
}
