package l2d.game.loginservercon.lspackets;

import java.util.logging.Logger;

import l2d.game.ThreadPoolManager;
import l2d.game.loginservercon.AttLS;
import l2d.game.loginservercon.KickWaitingClientTask;
import l2d.game.loginservercon.LSConnection;
import l2d.game.loginservercon.SessionKey;
import l2d.game.loginservercon.gspackets.PlayerInGame;
import l2d.game.loginservercon.gspackets.PlayerLogout;
import l2d.game.model.L2World;
import l2d.game.network.L2GameClient;
import l2d.game.serverpackets.CharacterSelectionInfo;
import l2d.game.serverpackets.LoginFail;
import l2d.game.tables.FakePlayersTable;
import l2d.game.templates.StatsSet;

public class PlayerAuthResponse extends LoginServerBasePacket
{
	private static final Logger log = Logger.getLogger(PlayerAuthResponse.class.getName());

	public PlayerAuthResponse(byte[] decrypt, AttLS loginserver)
	{
		super(decrypt, loginserver);
	}

	@Override
	public void read()
	{
		String account = readS();
		boolean authed = readC() == 1;
		int playOkId1 = readD();
		int playOkId2 = readD();
		int loginOkId1 = readD();
		int loginOkId2 = readD();
		String s_bonus = readS();
		String account_fields = readS();
		//int bonusExpire = readD();

		float bonus = s_bonus == null || s_bonus.equals("") ? 1 : Float.parseFloat(s_bonus);

		L2GameClient client = getLoginServer().getCon().removeWaitingClient(account);

		if(client != null)
		{
			if(client.getState() != L2GameClient.GameClientState.CONNECTED)
			{
				log.severe("Trying to authd allready authed client.");
				client.closeNow(true);
				return;
			}

			if(client.getLoginName() == null || client.getLoginName().isEmpty())
			{
				client.closeNow(true);
				log.warning("PlayerAuthResponse: empty accname for " + client);
				return;
			}

			SessionKey key = client.getSessionId();

			if(authed)
				if(getLoginServer().isLicenseShown())
					authed = key.playOkID1 == playOkId1 && key.playOkID2 == playOkId2 && key.loginOkID1 == loginOkId1 && key.loginOkID2 == loginOkId2;
				else
					authed = key.playOkID1 == playOkId1 && key.playOkID2 == playOkId2;

			if(authed)
			{
				client.account_fields = StatsSet.unserialize(account_fields);
				client.setState(L2GameClient.GameClientState.AUTHED);
				client.setBonus(bonus);
				//client.setBonusExpire(bonusExpire);
				getLoginServer().getCon().addAccountInGame(client);
				CharacterSelectionInfo csi = new CharacterSelectionInfo(client.getLoginName(), client.getSessionId().playOkID1);
				client.sendPacket(csi);
				client.setCharSelection(csi.getCharInfo());
				sendPacket(new PlayerInGame(client.getLoginName(), L2World.getAllPlayersCount() + FakePlayersTable.getFakePlayersCount()));
			}
			else
			{
				log.severe("Cheater? SessionKey invalid! Login: " + client.getLoginName() + ", IP: " + client.getIpAddr());
				client.sendPacket(new LoginFail(LoginFail.INCORRECT_ACCOUNT_INFO_CONTACT_CUSTOMER_SUPPORT));
				ThreadPoolManager.getInstance().scheduleGeneral(new KickWaitingClientTask(client), 1000);
				LSConnection.getInstance().sendPacket(new PlayerLogout(client.getLoginName()));
				LSConnection.getInstance().removeAccount(client);
			}
		}
	}
}