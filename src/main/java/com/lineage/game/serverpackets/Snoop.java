package com.lineage.game.serverpackets;

/**
 * [S] db Snoop
 * 
 * @author Felixx
 */
public class Snoop extends L2GameServerPacket
{
	private int _convoID;
	private String _name;
	private int _type;
	private String _speaker;
	private String _msg;

	public Snoop(final int id, final String name, final int type, final String speaker, final String msg)
	{
		_convoID = id;
		_name = name;
		_type = type;
		_speaker = speaker;
		_msg = msg;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xd5);

		writeD(_convoID);
		writeS(_name);
		writeD(0x00); // ??
		writeD(_type);
		writeS(_speaker);
		writeS(_msg);
	}
}