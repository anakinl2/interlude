package l2d.game.loginservercon.gspackets;

public class TestConnectionResponse extends GameServerBasePacket
{
	public TestConnectionResponse()
	{
		//System.out.println("GS: response sent");
		writeC(0x0d);
	}
}