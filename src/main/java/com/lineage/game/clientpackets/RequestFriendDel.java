package com.lineage.game.clientpackets;

import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lineage.db.DatabaseUtils;
import com.lineage.db.FiltredPreparedStatement;
import com.lineage.db.L2DatabaseFactory;
import com.lineage.db.ThreadConnection;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.L2World;
import com.lineage.game.serverpackets.L2Friend;
import com.lineage.game.serverpackets.SystemMessage;

public class RequestFriendDel extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestFriendDel.class.getName());

	private String _name;

	@Override
	public void readImpl()
	{
		_name = readS();
	}

	public static final boolean TryFriendDelete(L2Player activeChar, String delFriend)
	{
		if(activeChar == null || delFriend == null || delFriend.isEmpty())
			return false;

		boolean result = true;
		L2Player friendChar = L2World.getPlayer(delFriend);
		if(friendChar != null)
			delFriend = friendChar.getName();

		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT friend_id FROM character_friends WHERE char_id=? AND friend_name LIKE ?");
			statement.setInt(1, activeChar.getObjectId());
			statement.setString(2, delFriend);
			rset = statement.executeQuery();
			if(!rset.next())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.S1_IS_NOT_ON_YOUR_FRIEND_LIST).addString(delFriend));
				return false;
			}

			int targetId = rset.getInt("friend_id");
			DatabaseUtils.closeDatabaseSR(statement, rset);
			rset = null;

			statement = con.prepareStatement("DELETE FROM character_friends WHERE (char_id=? AND friend_id=?) OR (char_id=? AND friend_id=?)");
			statement.setInt(1, activeChar.getObjectId());
			statement.setInt(2, targetId);
			statement.setInt(3, targetId);
			statement.setInt(4, activeChar.getObjectId());
			statement.execute();

			//Player deleted from your friendlist
			activeChar.sendPacket(new SystemMessage(SystemMessage.S1_HAS_BEEN_REMOVED_FROM_YOUR_FRIEND_LIST).addString(delFriend));
			activeChar.sendPacket(new L2Friend(delFriend, false, friendChar != null, targetId)); //Офф посылает 0xFB Friend, хотя тут нету разници что именно посылать
			if(friendChar != null)
			{
				friendChar.sendPacket(new SystemMessage(SystemMessage.S1__HAS_BEEN_DELETED_FROM_YOUR_FRIENDS_LIST).addString(activeChar.getName()));
				friendChar.sendPacket(new L2Friend(activeChar, false)); //Офф посылает 0xFB Friend, хотя тут нету разници что именно посылать
			}
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "could not del friend objectid: ", e);
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
		TryFriendDelete(activeChar, _name);
	}
}