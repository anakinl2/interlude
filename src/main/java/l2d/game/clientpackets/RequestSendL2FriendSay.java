package l2d.game.clientpackets;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import l2d.Config;
import l2d.game.cache.Msg;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.serverpackets.L2FriendSay;
import l2d.game.serverpackets.SystemMessage;

/**
 * Recieve Private (Friend) Message
 * Format: c SS
 * S: Message
 * S: Receiving Player
 */
public class RequestSendL2FriendSay extends L2GameClientPacket
{
	private static Logger _logChat = Logger.getLogger("chat");

	private String _message;
	private String _reciever;

	@Override
	public void readImpl()
	{
		_message = readS();
		_reciever = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		if(activeChar.getNoChannel() != 0)
		{
			if(activeChar.getNoChannelRemained() > 0 || activeChar.getNoChannel() < 0)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.CHATTING_IS_CURRENTLY_PROHIBITED_IF_YOU_TRY_TO_CHAT_BEFORE_THE_PROHIBITION_IS_REMOVED_THE_PROHIBITION_TIME_WILL_BECOME_EVEN_LONGER));
				return;
			}
			activeChar.updateNoChannel(0);
		}

		L2Player targetPlayer = L2World.getPlayer(_reciever);
		if(targetPlayer == null)
		{
			activeChar.sendPacket(Msg.THAT_PLAYER_IS_NOT_ONLINE);
			return;
		}
		if(targetPlayer.isBlockAll())
		{
			activeChar.sendPacket(Msg.THE_PERSON_IS_IN_A_MESSAGE_REFUSAL_MODE);
			return;
		}

		if(Config.LOG_CHAT)
		{
			LogRecord record = new LogRecord(Level.INFO, _message);
			record.setLoggerName("chat");
			record.setParameters(new Object[] { "PRIV_MSG", "[" + activeChar.getName() + " to " + _reciever + "]" });

			_logChat.log(record);
		}

		targetPlayer.sendPacket(new L2FriendSay(activeChar.getName(), _reciever, _message));
	}
}