package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Player;

public class RelationChanged extends L2GameServerPacket
{
	private boolean _isAutoAttackable;
	private int _relation, _karma, _pvpFlag;
	private int _charObjId = 0;

	public RelationChanged(L2Player cha, boolean isAutoAttackable, int relation)
	{
		_isAutoAttackable = isAutoAttackable;
		_relation = relation;
		_charObjId = cha.getObjectId();
		_karma = cha.getKarma();
		_pvpFlag = cha.getPvpFlag();
	}

	@Override
	protected final void writeImpl()
	{
		if(_charObjId == 0)
			return;

		writeC(0xCE);
		writeD(_charObjId);
		writeD(_relation);
		writeD(_isAutoAttackable ? 1 : 0);
		writeD(_karma);
		writeD(_pvpFlag);
	}
}