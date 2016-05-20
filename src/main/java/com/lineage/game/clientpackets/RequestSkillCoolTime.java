package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Player;
import com.lineage.game.network.L2GameClient;
import com.lineage.game.serverpackets.SkillCoolTime;

public class RequestSkillCoolTime extends L2GameClientPacket
{
	L2GameClient _client;

	@Override
	public void readImpl()
	{
		_client = getClient();
	}

	@Override
	public void runImpl()
	{
		L2Player pl = _client.getActiveChar();
		if(pl != null)
			pl.sendPacket(new SkillCoolTime(pl));
	}
}