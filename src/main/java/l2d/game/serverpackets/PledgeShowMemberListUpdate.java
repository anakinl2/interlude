package l2d.game.serverpackets;

import l2d.game.model.L2ClanMember;
import l2d.game.model.L2Player;

public class PledgeShowMemberListUpdate extends L2GameServerPacket
{
	private String _name;
	private int _lvl;
	private int _classId;
	private int _sex;
	private int _isOnline;
	private int _objectId;
	private int _pledgeType;
	private int _isApprentice = 0;

	public PledgeShowMemberListUpdate(final L2Player player)
	{
		_name = player.getName();
		_lvl = player.getLevel();
		_classId = player.getClassId().getId();
		_sex = player.getSex();
		_objectId = player.getObjectId();
		_isOnline = player.isOnline() ? 1 : 0;
		_pledgeType = player.getPledgeType();
		if(player.getClan() != null && player.getClan().getClanMember(_objectId) != null)
			_isApprentice = player.getClan().getClanMember(_objectId).hasSponsor() ? 1 : 0;
	}

	public PledgeShowMemberListUpdate(final L2ClanMember cm)
	{
		_name = cm.getName();
		_lvl = cm.getLevel();
		_classId = cm.getClassId();
		_sex = cm.getSex();
		_objectId = cm.getObjectId();
		_isOnline = cm.isOnline() ? 1 : 0;
		_pledgeType = cm.getPledgeType();
		_isApprentice = cm.hasSponsor() ? 1 : 0;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x54);
		writeS(_name);
		writeD(_lvl);
		writeD(_classId);
		writeD(_sex);
		writeD(_objectId);
		writeD(_isOnline); // 1=online 0=offline
		writeD(_pledgeType);
		writeD(_isApprentice); // does a clan member have a sponsor
	}
}