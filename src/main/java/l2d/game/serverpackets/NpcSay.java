package l2d.game.serverpackets;

import l2d.game.model.instances.L2NpcInstance;

public class NpcSay extends L2GameServerPacket
{
	int _objId;
	int _type;
	String _name;
	String _text;

	// TODO не найден номер
	public NpcSay(L2NpcInstance npc, int chatType, String text)
	{
		_objId = npc.getObjectId();
		_type = chatType;
		_text = text;
		_name = npc.getName();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x4A);
		writeD(_objId);
		writeD(_type);
		writeS(_name);
		writeS(_text);
	}
}