package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Clan.RankPrivs;
import com.lineage.game.model.L2Player;

public class ManagePledgePower extends L2GameServerPacket
{
	private int _action, _clanId, privs;

	public ManagePledgePower(L2Player player, int action, int rank)
	{
		_clanId = player.getClanId();
		_action = action;
		RankPrivs temp = player.getClan().getRankPrivs(rank);
		privs = temp == null ? 0 : temp.getPrivs();
		player.sendPacket(new PledgeReceiveUpdatePower(privs));
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x30);
		writeD(_clanId);
		writeD(_action);
		writeD(privs);
	}
}