package l2d.game.loginservercon.lspackets;

import l2d.game.loginservercon.AttLS;

public class KickPlayer extends LoginServerBasePacket
{
	public KickPlayer(byte[] decrypt, AttLS loginserver)
	{
		super(decrypt, loginserver);
	}

	@Override
	public void read()
	{
		getLoginServer().getCon().kickAccountInGame(readS());
	}
}