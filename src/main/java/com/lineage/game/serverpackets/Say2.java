package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Player;

public class Say2 extends L2GameServerPacket
{
	private int _objectId, _textType;
	private String _charName, _text;

	public Say2(final int objectId, final int messageType, final String charName, final String text)
	{
		_objectId = objectId;
		_textType = messageType;
		_charName = charName;
		_text = text;
	}

	@Override
	final public void runImpl()
	{
		final L2Player _pci = getClient().getActiveChar();
		if(_pci != null)
			_pci.broadcastSnoop(_textType, _charName, _text);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x4A);
		writeD(_objectId);
		writeD(_textType);
		if(_textType == 11)
		{
			writeD(0x00); // npcId?
			writeD(0x00); // SysMsgId?
		}
		else
		{
			writeS(_charName);
			writeS(_text);
		}
	}
}