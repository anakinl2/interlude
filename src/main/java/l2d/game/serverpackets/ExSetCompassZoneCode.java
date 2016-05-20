package l2d.game.serverpackets;

import l2d.game.model.L2Player;

/**
 * http://forum.l2jserver.com/thread.php?threadid=22736
 */
public class ExSetCompassZoneCode extends L2GameServerPacket
{
	private static int ZONE_ALTERED = 8; // 9, 10 - Danger Area???
	private static int ZONE_SIEGE = 11;
	private static int ZONE_PEACE = 12;
	private static int ZONE_SS = 13;
	private static int ZONE_PVP = 14; // 1, 2, 3, 4, 5, 6, 7
	private static int ZONE_GENERAL_FIELD = 15; //0 и > 15

	int _zone = -1;

	public ExSetCompassZoneCode(L2Player player)
	{
		//Приоритеты ифам от фонаря:)

		if(player.isInDangerArea())
			_zone = ZONE_ALTERED;
		else if(player.isOnSiegeField())
			_zone = ZONE_SIEGE;
		else if(player.isInCombatZone())
			_zone = ZONE_PVP;
		else if(player.isInPeaceZone())
			_zone = ZONE_PEACE;
		else if(player.isInSSZone())
			_zone = ZONE_SS;
		else
			_zone = ZONE_GENERAL_FIELD;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x32);
		writeD(_zone);
	}
}