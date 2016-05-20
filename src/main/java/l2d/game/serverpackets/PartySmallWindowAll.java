package l2d.game.serverpackets;

import javolution.util.FastList;
import l2d.game.model.L2Player;
import com.lineage.util.GArray;

/**
 *
 * sample
 * 4E
 * 01 00 00 00  count
 *
 * c1 b2 e0 4a  object id
 * 54 00 75 00 65 00 73 00 64 00 61 00 79 00 00 00  name
 * 5a 01 00 00  hp
 * 5a 01 00 00  hp max
 * 89 00 00 00  mp
 * 89 00 00 00  mp max
 * 0e 00 00 00  level
 * 12 00 00 00  class
 * 00 00 00 00
 * 01 00 00 00
 *
 * format   ddd (dSddddddddddd)
 */
public class PartySmallWindowAll extends L2GameServerPacket
{
	private int leader_id, loot;
	private FastList<MemberInfo> members = new FastList<MemberInfo>();
	private GArray<L2Player> _partyMembers;

	public PartySmallWindowAll(GArray<L2Player> _members, L2Player exclude)
	{
		leader_id = _members.get(0).getObjectId();
		loot = _members.get(0).getParty().getLootDistribution();
		_partyMembers = _members;

		String _name;
		int _id, curCp, maxCp, curHp, maxHp, curMp, maxMp, level, class_id;

		for(L2Player member : _partyMembers)
		{
			if(member.equals(exclude))
				continue;
			_name = member.getName();
			_id = member.getObjectId();
			curCp = (int) member.getCurrentCp();
			maxCp = member.getMaxCp();
			curHp = (int) member.getCurrentHp();
			maxHp = member.getMaxHp();
			curMp = (int) member.getCurrentMp();
			maxMp = member.getMaxMp();
			level = member.getLevel();
			class_id = member.getClassId().getId();
			members.add(new MemberInfo(_name, _id, curCp, maxCp, curHp, maxHp, curMp, maxMp, level, class_id));
		}
	}

	@Override
	final public void runImpl()
	{}

	@Override
	protected final void writeImpl()
	{
		writeC(0x4E);
		writeD(leader_id); // c3 party leader id
		writeD(loot); //c3 party loot type (0,1,2,....)
		writeD(members.size());
		for(MemberInfo member : members)
		{
			writeD(member._id);
			writeS(member._name);
			writeD(member.curCp);
			writeD(member.maxCp);
			writeD(member.curHp);
			writeD(member.maxHp);
			writeD(member.curMp);
			writeD(member.maxMp);
			writeD(member.level);
			writeD(member.class_id);
			writeD(0);//writeD(0x01); ??
			writeD(0);
			/*
			 * если последняя d > 0, тогда еще посылается структура ddSddddd, что это хз, может ExPartyPetWindowAdd?
			 */
		}
	}

	static class MemberInfo
	{
		public String _name;
		public int _id, curCp, maxCp, curHp, maxHp, curMp, maxMp, level, class_id;

		public MemberInfo(String __name, int __id, int _curCp, int _maxCp, int _curHp, int _maxHp, int _curMp, int _maxMp, int _level, int _class_id)
		{
			_name = __name;
			_id = __id;
			curCp = _curCp;
			maxCp = _maxCp;
			curHp = _curHp;
			maxHp = _maxHp;
			curMp = _curMp;
			maxMp = _maxMp;
			level = _level;
			class_id = _class_id;
		}
	}
}