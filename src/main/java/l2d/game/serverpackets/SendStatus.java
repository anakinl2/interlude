package l2d.game.serverpackets;

import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.tables.FakePlayersTable;

public final class SendStatus extends L2GameServerPacket
{
	public SendStatus()
	{}

	private static final long MIN_UPDATE_PERIOD = 30000;
	private static int online_players = 0;
	private static int max_online_players = 0;
	private static int online_priv_store = 0;
	private static long last_update = 0;

	@Override
	public void runImpl()
	{
		if(System.currentTimeMillis() - last_update < MIN_UPDATE_PERIOD)
			return;
		last_update = System.currentTimeMillis();
		int i = 0;
		int j = 0;
		for(L2Player player : L2World.getAllPlayers())
			if(player != null)
			{
				i++;
				if(player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
					j++;
			}
		online_players = i + FakePlayersTable.getFakePlayersCount();
		online_priv_store = j;
		max_online_players = Math.max(max_online_players, online_players);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x2E); // Packet ID
		writeD(0x01); // World ID
		writeD(0x9A);// Max Online
		writeD(online_players + 2); // Current Online
		writeD(online_players); // Current Online
		writeD((int) (online_players * (float) 12 / 100F)); // Priv.Sotre Chars

		// SEND TRASH
		writeH(0x30);
		writeH(0x2C);
		writeH(0x35);
		writeH(0x34);
		writeH(0x38);
		writeH(0x30);
		writeH(0x34);
		writeH(0x32);
		writeH(0x2C);
		writeH(0x38);
		writeH(0x32);
		writeH(0x32);
		writeH(0x37);
		writeH(0x2C);
		writeH(0x33);
		writeH(0x32);
		writeH(0x31);
		writeH(0x30);
		writeD(0x00);
		writeD(41117);
		writeH(0x00);
		writeD(41117);
		writeH(0x00);
		writeC(0x66);
		writeC(0xA2);
		writeQ(0x00);
		writeD(0x00);
		writeH(0x00);
		writeD(0x02);
	}
}