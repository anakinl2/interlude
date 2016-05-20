package com.lineage.game.serverpackets;

import com.lineage.game.model.L2Player;

public class ExOlympiadUserInfoSpectator extends L2GameServerPacket
{
	// cdSddddd Отсутствует в Kamael
	private int _side, class_id, curHp, maxHp, curCp, maxCp;
	private int obj_id = 0;
	private String _name;

	/**
	 * @param _player
	 * @param _side (1 = right, 2 = left)
	 */
	public ExOlympiadUserInfoSpectator(L2Player player, int side)
	{
		if(player == null)
			return;

		_side = side;
		obj_id = player.getObjectId();
		class_id = player.getClassId().getId();
		_name = player.getName();
		curHp = (int) player.getCurrentHp();
		maxHp = player.getMaxHp();
		curCp = (int) player.getCurrentCp();
		maxCp = player.getMaxCp();
	}

	@Override
	protected final void writeImpl()
	{
		if(obj_id == 0)
			return;

		writeC(EXTENDED_PACKET);
		writeH(0x29);
		writeC(_side);
		writeD(obj_id);
		writeS(_name);
		writeD(class_id);
		writeD(curHp);
		writeD(maxHp);
		writeD(curCp);
		writeD(maxCp);
	}
}