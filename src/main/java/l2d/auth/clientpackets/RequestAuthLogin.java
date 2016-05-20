package l2d.auth.clientpackets;

import java.security.GeneralSecurityException;

import javax.crypto.Cipher;

import l2d.Config;
import l2d.auth.L2LoginClient;
import l2d.auth.L2LoginClient.LoginClientState;
import l2d.auth.LoginController;
import l2d.auth.LoginController.State;
import l2d.auth.LoginController.Status;
import l2d.auth.gameservercon.GameServerInfo;
import l2d.auth.serverpackets.AccountKicked;
import l2d.auth.serverpackets.AccountKicked.AccountKickedReason;
import l2d.auth.serverpackets.LoginFail.LoginFailReason;
import l2d.auth.serverpackets.LoginOk;
import l2d.auth.serverpackets.ServerList;

/**
 * Format: b[128]ddddddhc
 * b[128]: the rsa encrypted block with the login an password
 */
public class RequestAuthLogin extends L2LoginClientPacket
{
	private byte[] _raw = new byte[128];

	private String _user;
	private String _password;
	@SuppressWarnings("unused")
	private int _ncotp, clientOrder;

	public String getPassword()
	{
		return _password;
	}

	public String getUser()
	{
		return _user;
	}

	public int getOneTimePassword()
	{
		return _ncotp;
	}

	@Override
	public boolean readImpl()
	{
		if(getAvaliableBytes() >= 128)
		{
			readB(_raw);
			try
			{
				readD();
				readD();
				readD();
				readD();
				readD();
				//это как-то связано с GG
				readD(); //const = 8
				readH();
				clientOrder = readC();
				// System.out.println("RequestAuthLogin: d1:"+d1+"|d2:"+d2+"|d3:"+d3+"|d4:"+d4+"|d5:"+d5+"|d6:"+d6+"|h:"+h+"|ClientOrder:"+clientOrder);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			return true;
		}
		return false;
	}

	@Override
	public void runImpl()
	{
		L2LoginClient client = getClient();

		byte[] decrypted;
		try
		{
			Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
			rsaCipher.init(Cipher.DECRYPT_MODE, client.getRSAPrivateKey());
			decrypted = rsaCipher.doFinal(_raw, 0x00, 0x80);
		}
		catch(GeneralSecurityException e)
		{
			e.printStackTrace();
			return;
		}

		_user = new String(decrypted, 0x5E, 14).trim();
		_user = _user.toLowerCase();
		_password = new String(decrypted, 0x6C, 16).trim();
		_ncotp = decrypted[0x7c];
		_ncotp |= decrypted[0x7d] << 8;
		_ncotp |= decrypted[0x7e] << 16;
		_ncotp |= decrypted[0x7f] << 24;

		LoginController lc = LoginController.getInstance();

		Status status = lc.tryAuthLogin(_user, _password, client);
		if(status.state == State.VALID)
		{
			client.setAccount(_user);
			client.setState(LoginClientState.AUTHED_LOGIN);
			client.setSessionKey(lc.assignSessionKeyToClient());
			lc.addAuthedLoginClient(_user, client);
			client.setBonus(status.bonus, status.bonus_expire);
			//client.setProxy(status.proxy);
			if(Config.SHOW_LICENCE)
				client.sendPacket(new LoginOk(client.getSessionKey()));
			else
				client.sendPacket(new ServerList(client));
		}
		else if(status.state == State.WRONG)
			client.close(LoginFailReason.REASON_USER_OR_PASS_WRONG);
		else if(status.state == State.BANNED)
			client.close(new AccountKicked(AccountKickedReason.REASON_PERMANENTLY_BANNED));
		else if(status.state == State.IP_ACCESS_DENIED)
			client.close(LoginFailReason.REASON_ATTEMPTED_RESTRICTED_IP);
		else if(status.state == State.IN_USE)
		{
			L2LoginClient oldClient = lc.getAuthedClient(_user);
			GameServerInfo gsi = lc.getAccountOnGameServer(_user);

			// кикаем другого клиента, подключенного к логину
			if(oldClient != null)
				oldClient.close(LoginFailReason.REASON_ACCOUNT_IN_USE);

			// кикаем другого клиента из игры
			if(gsi != null && gsi.isAuthed())
				gsi.getGameServer().kickPlayer(_user);

			if(lc.isAccountInLoginServer(_user))
				lc.removeAuthedLoginClient(_user).close(LoginFailReason.REASON_ACCOUNT_IN_USE);

			client.close(LoginFailReason.REASON_ACCOUNT_IN_USE);
		}
	}
}