package com.lineage.game.loginservercon.lspackets;

import com.lineage.game.loginservercon.AttLS;
import com.lineage.game.loginservercon.gspackets.TestConnectionResponse;

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