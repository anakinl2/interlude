package l2d.game.clientpackets;

/**
 * Format: d d|dd
 */
public class RequestExMoveToLocationAirShip extends L2GameClientPacket
{
	@Override
	protected void runImpl()
	{}

	@Override
	protected void readImpl()
	{
		int MoveType = readD();
		if(MoveType == 4)
		{
			int SpotID = readD();
			System.out.println(getType() + " | MoveType: 4 | SpotID: " + SpotID);
		}
		else
		{
			int x = readD();
			int y = readD();
			System.out.println(getType() + " | MoveType: " + MoveType + " | x: " + x + " | y: " + y);
		}
	}
}