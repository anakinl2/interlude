package com.lineage.game.clientpackets;

import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.serverpackets.AcquireSkillInfo;
import com.lineage.game.serverpackets.AcquireSkillList;
import com.lineage.game.tables.SkillTable;

public class RequestAquireSkillInfo extends L2GameClientPacket
{
	// format: cddd
	private int _id;
	private byte _level;
	private int _skillType;

	@Override
	public void readImpl()
	{
		_id = readD();
		_level = (byte) readD();
		_skillType = readD();// normal(0) learn or fisherman(1) clan(2) ? (3) transformation (4)
	}

	@Override
	public void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if(activeChar == null || SkillTable.getInstance().getInfo(_id, _level) == null)
			return;
		L2NpcInstance trainer = activeChar.getLastNpc();
		if((trainer == null || activeChar.getDistance(trainer.getX(), trainer.getY()) > L2Character.INTERACTION_DISTANCE) && !activeChar.isGM())
			return;
		if(_skillType == AcquireSkillList.CLAN)
			sendPacket(new AcquireSkillInfo(_id, _level, activeChar.getClassId(), activeChar.getClan()));
		else
			sendPacket(new AcquireSkillInfo(_id, _level, activeChar.getClassId(), null));
	}
}