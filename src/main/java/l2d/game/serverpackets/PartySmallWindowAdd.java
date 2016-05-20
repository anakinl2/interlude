package l2d.game.serverpackets;

import l2d.game.model.L2Player;

public class PartySmallWindowAdd extends L2GameServerPacket
{
	//dddSdddddddddd
	private int char_obj_id = 0;
	private int member_obj_id, member_level, member_class_id;
	private int member_curHp, member_maxHp, member_curCp, member_maxCp, member_curMp, member_maxMp;
	private String member_name;

	public PartySmallWindowAdd(L2Player member)
	{
		member_obj_id = member.getObjectId();
		member_name = member.getName();
		member_curCp = (int) member.getCurrentCp();
		member_maxCp = member.getMaxCp();
		member_curHp = (int) member.getCurrentHp();
		member_maxHp = member.getMaxHp();
		member_curMp = (int) member.getCurrentMp();
		member_maxMp = member.getMaxMp();
		member_level = member.getLevel();
		member_class_id = member.getClassId().getId();
	}

	@Override
	final public void runImpl()
	{
		L2Player player = getClient().getActiveChar();
		if(player == null)
			return;
		char_obj_id = player.getObjectId();
	}

	@Override
	protected final void writeImpl()
	{
		if(char_obj_id == 0)
			return;

		writeC(0x4F);
		writeD(char_obj_id); // c3
		writeD(0);//writeD(0x04); ?? //c3
		writeD(member_obj_id);
		writeS(member_name);
		writeD(member_curCp);
		writeD(member_maxCp);
		writeD(member_curHp);
		writeD(member_maxHp);
		writeD(member_curMp);
		writeD(member_maxMp);
		writeD(member_level);
		writeD(member_class_id);
		writeD(0);//writeD(0x01); ??
		writeD(0);
	}
}