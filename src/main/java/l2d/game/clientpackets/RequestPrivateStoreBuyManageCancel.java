package l2d.game.clientpackets;

public class RequestPrivateStoreBuyManageCancel extends L2GameClientPacket
{
	@Override
	public void runImpl()
	{
		System.out.println(getType());
	}

	@Override
	public void readImpl()
	{}
}