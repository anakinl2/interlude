package l2d.game.clientpackets;

import java.nio.ByteBuffer;

import l2d.game.model.L2Player;
import l2d.game.model.entity.Duel;
import l2d.game.network.L2GameClient;

public class RequestDuelSurrender extends L2GameClientPacket
{
	public RequestDuelSurrender(ByteBuffer buf, L2GameClient client)
	{
		_buf = buf;
		_client = client;
	}

	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		L2Player p = getClient().getActiveChar();

		if(p == null)
			return;

		Duel d = p.getDuel();

		if(d == null)
			return;

		d.doSurrender(p);
	}
}