package com.lineage.game.loginservercon;

import com.lineage.game.network.L2GameClient;

/**
 * @Author: Death
 * @Date: 13/11/2007
 * @Time: 20:14:14
 */
public class KickWaitingClientTask implements Runnable
{
	private final L2GameClient client;

	public KickWaitingClientTask(L2GameClient client)
	{
		this.client = client;
	}

	@Override
	public void run()
	{
		client.closeNow(false);
	}
}
