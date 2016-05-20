package l2d.game.clientpackets;

import java.util.logging.Logger;

import l2d.db.DatabaseUtils;
import l2d.db.FiltredPreparedStatement;
import l2d.db.L2DatabaseFactory;
import l2d.db.ThreadConnection;
import l2d.game.model.L2Player;
import l2d.game.model.L2Player.TransactionType;
import l2d.game.serverpackets.L2Friend;
import l2d.game.serverpackets.SystemMessage;

public class RequestFriendAddReply extends L2GameClientPacket
{
	// format: cd
	private static Logger _log = Logger.getLogger(RequestFriendAddReply.class.getName());

	private int _response;

	@Override
	public void readImpl()
	{
		_response = _buf.hasRemaining() ? readD() : 0;
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;

		L2Player requestor = activeChar.getTransactionRequester();

		activeChar.setTransactionRequester(null);

		if(requestor == null)
			return;

		requestor.setTransactionRequester(null);

		if(activeChar.getTransactionType() != TransactionType.FRIEND || activeChar.getTransactionType() != requestor.getTransactionType())
			return;

		if(_response == 1)
		{
			ThreadConnection con = null;
			FiltredPreparedStatement statement = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("insert into character_friends (char_id,friend_id,friend_name) values(?,?,?)");
				statement.setInt(1, requestor.getObjectId());
				statement.setInt(2, activeChar.getObjectId());
				statement.setString(3, activeChar.getName());
				statement.execute();
				DatabaseUtils.closeStatement(statement);

				statement = con.prepareStatement("insert into character_friends (char_id,friend_id,friend_name) values(?,?,?)");
				statement.setInt(1, activeChar.getObjectId());
				statement.setInt(2, requestor.getObjectId());
				statement.setString(3, requestor.getName());
				statement.execute();

				requestor.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_SUCCEEDED_IN_INVITING_A_FRIEND));
				// Player added to your friendlist
				requestor.sendPacket(new SystemMessage(SystemMessage.S1_HAS_BEEN_ADDED_TO_YOUR_FRIEND_LIST).addString(activeChar.getName()));
				// has joined as friend.
				activeChar.sendPacket(new SystemMessage(SystemMessage.S1_HAS_JOINED_AS_A_FRIEND).addString(requestor.getName()));

				//Обновисть список друзей. Вообще-то здесь должен слатся пакет 0xfb Friend, а не 0xfa FriendList, но от греха подальше :)
				requestor.sendPacket(new L2Friend(activeChar, true));
				activeChar.sendPacket(new L2Friend(requestor, true));
			}
			catch(Exception e)
			{
				_log.warning("could not add friend objectid: " + e);
			}
			finally
			{
				DatabaseUtils.closeDatabaseCS(con, statement);
			}
		}
		else
			requestor.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_INVITE_A_FRIEND));
	}
}