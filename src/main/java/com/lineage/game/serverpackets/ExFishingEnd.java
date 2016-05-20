package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Player;

/**
 * Format: (ch) dc
 * d: character object id
 * c: 1 if won 0 if failed
 */
public class ExFishingEnd extends L2GameServerPacket
{
	private int char_obj_id;
	private boolean _win;

	public ExFishingEnd(boolean win, L2Player character)
	{
		_win = win;
		char_obj_id = character.getObjectId();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x14);
		writeD(char_obj_id);
		writeC(_win ? 1 : 0);
	}
}