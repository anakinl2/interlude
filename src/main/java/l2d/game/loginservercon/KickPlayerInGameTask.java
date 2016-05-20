package l2d.game.loginservercon;

import l2d.game.cache.Msg;
import l2d.game.model.L2Player;
import l2d.game.network.L2GameClient;

/**
 * @Author: Death
 * @Date: 13/11/2007
 * @Time: 20:46:51
 */
public class KickPlayerInGameTask implements Runnable
{
	private final L2GameClient client;

	public KickPlayerInGameTask(L2GameClient client)
	{
		this.client = client;
	}

	@Override
	public void run()
	{
		L2Player activeChar = client.getActiveChar();

		if(activeChar != null)
			activeChar.logout(false, false, true);
		else
		{
			client.sendPacket(Msg.ServerClose);
			client.closeNow(false);
		}
	}
}
