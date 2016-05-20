package l2d.game.clientpackets;

import java.util.logging.Level;
import java.util.logging.Logger;

import l2d.Config;
import l2d.db.mysql;
import l2d.game.network.L2GameClient;
import l2d.game.serverpackets.CharacterDeleteFail;
import l2d.game.serverpackets.CharacterDeleteSuccess;
import l2d.game.serverpackets.CharacterSelectionInfo;

/**
 * [C] 0C CharacterDelete
 * <b>Format:</b> cd
 * @author Felixx
 *
 */
public class CharacterDelete extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(CharacterDelete.class.getName());
	private int _charSlot;

	@Override
	public void readImpl()
	{
		_charSlot = readD();
	}

	@Override
	public void runImpl()
	{
		if(Config.DEBUG)
			_log.fine("deleting slot:" + _charSlot);

		int clan = clanStatus();
		if(clan > 0)
		{
			if(clan == 2)
				sendPacket(new CharacterDeleteFail(CharacterDeleteFail.REASON_CLAN_LEADERS_MAY_NOT_BE_DELETED));
			else if(clan == 1)
				sendPacket(new CharacterDeleteFail(CharacterDeleteFail.REASON_YOU_MAY_NOT_DELETE_CLAN_MEMBER));
			return;
		}
		if(clan < 0)
			return;

		L2GameClient client = getClient();
		try
		{
			if(Config.DELETE_DAYS == 0)
				client.deleteChar(_charSlot);
			else
				client.markToDeleteChar(_charSlot);
		}
		catch(Exception e)
		{
			_log.log(Level.SEVERE, "Error:", e);
		}

		sendPacket(new CharacterDeleteSuccess());

		CharacterSelectionInfo cl = new CharacterSelectionInfo(client.getLoginName(), client.getSessionId().playOkID1);
		sendPacket(cl);
		client.setCharSelection(cl.getCharInfo());
	}

	private int clanStatus()
	{
		int obj = getClient().getObjectIdForSlot(_charSlot);
		if(obj == -1)
			return 0;
		if(mysql.simple_get_int("clanid", "characters", "obj_Id=" + obj) > 0)
		{
			if(mysql.simple_get_int("leader_id", "clan_data", "leader_id=" + obj) > 0)
				return 2;
			return 1;
		}
		return 0;
	}
}