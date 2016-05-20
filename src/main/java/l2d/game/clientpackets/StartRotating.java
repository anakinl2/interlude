package l2d.game.clientpackets;

import l2d.game.serverpackets.StartRotation;

public class StartRotating extends L2GameClientPacket
{
	private int _degree;
	private int _side;

	@Override
	protected void readImpl()
	{
		_degree = readD();
		_side = readD();
	}

	@Override
	protected void runImpl()
	{
		if(getClient().getActiveChar() == null)
			return;
		StartRotation br = new StartRotation(getClient().getActiveChar().getObjectId(), _degree, _side, 0);
		getClient().getActiveChar().broadcastPacket(br);
	}
}