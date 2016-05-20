package l2d.game.clientpackets;

public class RequestResetNickname extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{}

	@Override
	protected void runImpl()
	{
		System.out.println(getType());
	}
}