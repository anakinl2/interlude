package l2d.game.serverpackets;

public class StartPledgeWar extends L2GameServerPacket
{
	private String _pledgeName;
	private String _char;

	public StartPledgeWar(String pledge, String charName)
	{
		_pledgeName = pledge;
		_char = charName;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x65);
		writeS(_char);
		writeS(_pledgeName);
	}
}