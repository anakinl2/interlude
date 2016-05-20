package com.lineage.game.clientpackets;

import java.util.logging.Logger;

import com.lineage.game.cache.CrestCache;
import com.lineage.game.model.L2Clan;
import com.lineage.game.model.L2Player;
import com.lineage.game.serverpackets.SystemMessage;

public class RequestSetPledgeCrestLarge extends L2GameClientPacket
{
	static Logger _log = Logger.getLogger(RequestSetPledgeCrestLarge.class.getName());
	private int _size;
	private byte[] _data;

	/**
	 * format: chd(b)
	 */
	@Override
	public void readImpl()
	{
		_size = readD();
		if(_size > _buf.remaining() || _size > Short.MAX_VALUE || _size <= 0)
			return;
		_data = new byte[_size];
		readB(_data);
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null)
			return;
		if(activeChar.isGM())
			activeChar.sendMessage("RequestSetPledgeCrestLarge");

		L2Clan clan = activeChar.getClan();
		if(clan == null)
			return;

		if((activeChar.getClanPrivileges() & L2Clan.CP_CL_REGISTER_CREST) == L2Clan.CP_CL_REGISTER_CREST)
		{
			if(clan.getHasCastle() == 0 && clan.getHasHideout() == 0)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.THE_CLANS_EMBLEM_WAS_SUCCESSFULLY_REGISTERED__ONLY_A_CLAN_THAT_OWNS_A_CLAN_HALL_OR_A_CASTLE_CAN_GET_THEIR_EMBLEM_DISPLAYED_ON_CLAN_RELATED_ITEMS));
				return;
			}

			if(clan.hasCrestLarge())
				CrestCache.removePledgeCrestLarge(clan);

			System.out.println("PCL size: " + _size);
			if(_data != null && _data.length <= 2176)
			{
				CrestCache.savePledgeCrestLarge(clan, _data);
				activeChar.sendPacket(new SystemMessage(SystemMessage.THE_CLANS_EMBLEM_WAS_SUCCESSFULLY_REGISTERED__ONLY_A_CLAN_THAT_OWNS_A_CLAN_HALL_OR_A_CASTLE_CAN_GET_THEIR_EMBLEM_DISPLAYED_ON_CLAN_RELATED_ITEMS));
			}

			clan.broadcastClanStatus(false, true);
		}
	}
}