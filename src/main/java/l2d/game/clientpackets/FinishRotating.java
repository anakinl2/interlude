package l2d.game.clientpackets;

import l2d.game.serverpackets.StopRotation;

/**
 * [C] 4B FinishRotating
 * @author Felixx
 */
public class FinishRotating extends L2GameClientPacket
{
	private int _degree;
	private int _unknown;

	@Override
	protected void readImpl()
	{
		_degree = readD();
		_unknown = readD();
	}

	@Override
	protected void runImpl()
	{
		if(getClient().getActiveChar() == null)
			return;
		StopRotation sr = new StopRotation(getClient().getActiveChar().getObjectId(), _degree, 0);
		getClient().getActiveChar().broadcastPacket(sr);
	}
}