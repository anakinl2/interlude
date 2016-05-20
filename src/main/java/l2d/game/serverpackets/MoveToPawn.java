package l2d.game.serverpackets;

import l2d.game.model.L2Character;
import l2d.game.model.L2Object;

/**
 * format dddddd (player id, target id, distance, startx, starty, startz)<p>
 */
public class MoveToPawn extends L2GameServerPacket
{
	private int _chaId;
	private int _targetId;
	private int _distance;
	private int _x, _y, _z;

	private L2Character cha;
	private L2Object target;

	public MoveToPawn(L2Character cha, L2Object l2Object, int distance)
	{
		if(cha == l2Object)
		{
			cha.sendActionFailed();
			return;
		}

		//cha = cha;
		target = l2Object;
		_chaId = cha.getObjectId();
		_targetId = l2Object.getObjectId();
		_distance = distance;
		_x = cha.getX();
		_y = cha.getY();
		_z = cha.getZ();
	}

	@Override
	protected final void writeImpl()
	{
		if(_chaId == 0)
			return;

		writeC(0x60);

		writeD(_chaId);
		writeD(_targetId);
		writeD(_distance);

		writeD(_x);
		writeD(_y);
		writeD(_z);
	}

	@Override
	public String getType()
	{
		return super.getType() + ": " + cha + ", target=" + target + "; dist=" + _distance;
	}
}