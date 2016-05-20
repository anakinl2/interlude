package l2d.game.clientpackets;

import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import l2d.db.DatabaseUtils;
import l2d.db.FiltredPreparedStatement;
import l2d.db.L2DatabaseFactory;
import l2d.db.ThreadConnection;
import l2d.game.cache.Msg;
import l2d.game.model.L2Player;
import l2d.game.model.L2Player.TransactionType;
import l2d.game.model.L2World;
import l2d.game.serverpackets.FriendAddRequest;
import l2d.game.serverpackets.SystemMessage;

public class RequestFriendInvite extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestFriendInvite.class.getName());

	private String _name;

	@Override
	public void readImpl()
	{
		_name = readS();
	}

	public static final boolean TryFriendInvite(L2Player activeChar, String addFriend)
	{
		if(activeChar == null || addFriend == null || addFriend.isEmpty())
			return false;

		if(activeChar.isTransactionInProgress())
		{
			activeChar.sendPacket(Msg.WAITING_FOR_ANOTHER_REPLY);
			return false;
		}

		if(activeChar.getName().equalsIgnoreCase(addFriend))
		{
			activeChar.sendPacket(Msg.YOU_CANNOT_ADD_YOURSELF_TO_YOUR_OWN_FRIEND_LIST);
			return false;
		}

		L2Player friendChar = L2World.getPlayer(addFriend);
		if(friendChar == null)
		{
			activeChar.sendPacket(Msg.THE_USER_WHO_REQUESTED_TO_BECOME_FRIENDS_IS_NOT_FOUND_IN_THE_GAME);
			return false;
		}

		if(friendChar.isBlockAll())
		{
			activeChar.sendPacket(Msg.THE_PERSON_IS_IN_A_MESSAGE_REFUSAL_MODE);
			return false;
		}

		boolean result = true;
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT friend_id FROM character_friends WHERE char_id=? AND friend_id=?");
			statement.setInt(1, activeChar.getObjectId());
			statement.setInt(2, friendChar.getObjectId());
			rset = statement.executeQuery();
			if(rset.next())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.S1_IS_ALREADY_ON_YOUR_FRIEND_LIST).addString(friendChar.getName()));
				return false;
			}
			if(friendChar.isTransactionInProgress())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER));
				return false;
			}
			friendChar.setTransactionRequester(activeChar, System.currentTimeMillis() + 10000);
			friendChar.setTransactionType(TransactionType.FRIEND);
			activeChar.setTransactionRequester(friendChar, System.currentTimeMillis() + 10000);
			activeChar.setTransactionType(TransactionType.FRIEND);
			friendChar.sendPacket(new SystemMessage(SystemMessage.S1_HAS_REQUESTED_TO_BECOME_FRIENDS).addString(activeChar.getName()));
			friendChar.sendPacket(new FriendAddRequest(activeChar.getName()));
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "could not add friend objectid: ", e);
			result = false;
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}

		return result;
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		TryFriendInvite(activeChar, _name);
	}
}