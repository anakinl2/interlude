package l2d.game.serverpackets;

public class RestartResponse extends L2GameServerPacket
{
	private String _message;

	public RestartResponse()
	{
		_message = "bye";
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x5f);
		writeD(0x01); //01-ok
		writeS(_message);
	}
}