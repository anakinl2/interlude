package l2d.game.serverpackets;

import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import l2d.db.DatabaseUtils;
import l2d.db.FiltredPreparedStatement;
import l2d.db.L2DatabaseFactory;
import l2d.db.ThreadConnection;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;

public class L2FriendList extends L2GameServerPacket
{
	private static Logger _log = Logger.getLogger(L2FriendList.class.getName());
	private FastList<Integer> id = new FastList<Integer>();
	private FastList<String> name = new FastList<String>();
	private FastList<Boolean> online = new FastList<Boolean>();
	private L2Player _cha;
	private boolean _message = false;
	private boolean _packet = false;

	public L2FriendList(L2Player cha)
	{
		_cha = cha;
		_message = true;
		_packet = false;
	}

	public L2FriendList(L2Player cha, boolean sendMessage)
	{
		_cha = cha;
		_message = sendMessage;
		_packet = true;
	}

	@Override
	final public void runImpl()
	{
		ThreadConnection con = null;
		FiltredPreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("select friend_id,friend_name from character_friends where char_id=?");
			statement.setInt(1, _cha.getObjectId());
			rset = statement.executeQuery();

			if(_message)
				_cha.sendPacket(new SystemMessage(SystemMessage._FRIENDS_LIST_));
			while(rset.next())
			{
				int objectId = rset.getInt("friend_id");
				String friendName = rset.getString("friend_name");
				L2Player friend = L2World.getPlayer(friendName);
				name.add(friendName);
				id.add(objectId);
				if(friend == null)
				{
					if(_message)
						_cha.sendPacket(new SystemMessage(SystemMessage.S1_CURRENTLY_OFFLINE).addString(friendName));
					online.add(false);
				}
				else
				{
					if(_message)
						_cha.sendPacket(new SystemMessage(SystemMessage.S1_CURRENTLY_ONLINE).addString(friendName));
					online.add(true);
				}
			}
			if(_message)
				_cha.sendPacket(new SystemMessage(SystemMessage.__EQUALS__));
		}
		catch(Exception e)
		{
			_log.log(Level.WARNING, "Error in friendlist ", e);
			_packet = false;
		}
		finally
		{
			DatabaseUtils.closeDatabaseCSR(con, statement, rset);
		}
	}

	@Override
	protected final void writeImpl()
	{
		if(!_packet)
			return;

		writeC(0xfa);
		if(id.size() == 0)
			writeD(0);
		else
			writeH(id.size());
		if(id.size() > 0)
			for(int i = 0; i < id.size(); i++)
			{
				writeH(0);
				writeD(id.get(i)); //object_id
				writeS(name.get(i)); //name
				writeD(online.get(i) ? 1 : 0); //online or offline
				writeH(0); // ??
			}
	}
}