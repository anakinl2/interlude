package com.lineage.game.clientpackets;

import java.util.logging.Logger;

import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.SystemMessage;
import com.lineage.game.tables.ClanTable;

/**
 * [C] 51 RequestSurrenderPledgeWar
 * opcode(id) = 0x08
 * 
 * @author Felixx
 */
public final class RequestSurrenderPledgeWar extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(RequestSurrenderPledgeWar.class.getName());

	private String _pledgeName;
	private L2Clan _clan;
	private L2Player _activeChar;

	@Override
	protected void readImpl()
	{
		_pledgeName = readS();
	}

	@Override
	protected void runImpl()
	{
		_activeChar = getClient().getActiveChar();
		if(_activeChar == null)
			return;
		_clan = _activeChar.getClan();
		if(_clan == null)
			return;
		final L2Clan clan = ClanTable.getInstance().getClanByName(_pledgeName);

		if(clan == null)
		{
			_activeChar.sendPacket(new SystemMessage(SystemMessage.THE_FOLLOWING_CLAN_DOES_NOT_EXIST));
			_activeChar.sendActionFailed();
			return;
		}

		_log.info("RequestSurrenderPledgeWar by " + getClient().getActiveChar().getClan().getName() + " with " + _pledgeName);

		if(!_clan.isAtWarWith(clan.getClanId()))
		{
			_activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_CURRENTLY_ALLIED_WITH_ANY_CLANS));
			_activeChar.sendActionFailed();
			return;
		}

		SystemMessage msg = new SystemMessage(SystemMessage.YOU_HAVE_SURRENDERED_TO_THE_S1_CLAN);
		msg.addString(_pledgeName);
		_activeChar.sendPacket(msg);
		msg = null;
	}
}