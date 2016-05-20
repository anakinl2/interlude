package l2d.game.loginservercon.lspackets;

import l2d.game.loginservercon.AttLS;
import l2d.game.loginservercon.gspackets.TestConnectionResponse;

public class TestConnection extends LoginServerBasePacket
{
	public TestConnection(byte[] decrypt, AttLS loginServer)
	{
		super(decrypt, loginServer);
	}

	@Override
	public void read()
	{
		//System.out.println("GS: request obtained");
		getLoginServer().sendPacket(new TestConnectionResponse());
	}
}