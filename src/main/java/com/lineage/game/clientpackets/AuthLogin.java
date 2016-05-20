package com.lineage.game.clientpackets;

import com.lineage.Config;
import com.lineage.game.cache.Msg;
import com.lineage.game.ThreadPoolManager;
import com.lineage.game.loginservercon.LSConnection;
import com.lineage.game.loginservercon.SessionKey;
import com.lineage.game.network.L2GameClient;

/**
 * [C] 08 AuthLogin
 * <b>Format:</b> 
 * cSddddd cSdddddQ <p>
 * loginName + keys must match what the loginserver used.
 * @author Felixx
 */
public class AuthLogin extends L2GameClientPacket
{
	private String _loginName;
	private int _playKey1;
	private int _playKey2;
	private int _loginKey1;
	private int _loginKey2;

	@Override
	public void readImpl()
	{
		_loginName = readS().toLowerCase();
		_playKey2 = readD();
		_playKey1 = readD();
		_loginKey1 = readD();
		_loginKey2 = readD();
		//readD(); LanguageType
		//readQ(); unk
		// ignore the rest
		_buf.clear();
	}

	@Override
	public void runImpl()
	{
		SessionKey key = new SessionKey(_loginKey1, _loginKey2, _playKey1, _playKey2);

		final L2GameClient client = getClient();
		client.setSessionId(key);
		client.setLoginName(_loginName);
		if(Config.GG_CHECK)
		{
			client.sendPacket(Msg.GameGuardQuery);
			ThreadPoolManager.getInstance().scheduleAi(new GGTest(client), 500, true);
		}
		LSConnection.getInstance().addWaitingClient(client);
	}

	private class GGTest implements Runnable
	{
		private final L2GameClient targetClient;

		private int attempts = 0;

		public GGTest(L2GameClient targetClient)
		{
			this.targetClient = targetClient;
		}

		@Override
		public void run()
		{
			if(!targetClient.isGameGuardOk())
				if(attempts < 3)
				{
					targetClient.sendPacket(Msg.GameGuardQuery);
					attempts++;
					ThreadPoolManager.getInstance().scheduleGeneral(this, 500 * attempts);
				}
				else
					targetClient.closeNow(false);
		}
	}
}