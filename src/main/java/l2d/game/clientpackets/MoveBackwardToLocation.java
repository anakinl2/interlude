package l2d.game.clientpackets;

import java.util.logging.Logger;

import l2d.Config;
import l2d.game.model.L2Player;
import l2d.game.network.L2GameClient;
import l2d.game.serverpackets.CharMoveToLocation;
import l2d.util.Location;

/**
 * [C] 01 MoveBackwardToLoc
 * @author Felixx
 */
public class MoveBackwardToLocation extends L2GameClientPacket
{
	private static Logger _log = Logger.getLogger(MoveBackwardToLocation.class.getName());
	private Location _targetLoc = new Location(0, 0, 0);
	private Location _originLoc = new Location(0, 0, 0);
	private int _moveMovement;

	/**
	 * packet type id 0x0f
	 */
	@Override
	public void readImpl()
	{
		_targetLoc.x = readD();
		_targetLoc.y = readD();
		_targetLoc.z = readD();
		_originLoc.x = readD();
		_originLoc.y = readD();
		_originLoc.z = readD();
		L2GameClient client = getClient();
		L2Player activeChar = client.getActiveChar();
		if(activeChar == null)
			return;

		if(_buf.hasRemaining())
			_moveMovement = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();

		if(activeChar == null)
			return;

		if(_moveMovement == 0 && !Config.ALLOW_KEYBOARD_MOVE)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(System.currentTimeMillis() - activeChar.getLastMovePacket() < Config.MOVE_PACKET_DELAY)
		{
			activeChar.sendActionFailed();
			return;
		}

		activeChar.setLastMovePacket();

		if(activeChar.isTeleporting())
		{
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.inObserverMode())
		{
			if(activeChar.getOlympiadGameId() == -1)
				activeChar.sendActionFailed();
			else
				activeChar.sendPacket(new CharMoveToLocation(activeChar.getObjectId(), _originLoc, _targetLoc));
			return;
		}

		if(activeChar.isOutOfControl() && activeChar.getOlympiadGameId() == -1)
		{
			activeChar.sendActionFailed();
			return;
		}

		if(activeChar.isInBoat())
			activeChar.setBoat(null);

		if(activeChar.getTeleMode() > 0)
		{
			if(activeChar.getTeleMode() == 1)
				activeChar.setTeleMode(0);
			activeChar.sendActionFailed();
			activeChar.teleToLocation(_targetLoc);
			return;
		}

		int water_z = activeChar.getWaterZ();
		if(water_z != -1 && _targetLoc.z > water_z)
			_targetLoc.z = water_z;

		activeChar.moveToLocation(_targetLoc, 0, _moveMovement != 0);
	}
}