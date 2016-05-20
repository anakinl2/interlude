package com.lineage.game.serverpackets;

import javolution.util.FastList;
import com.lineage.game.model.L2Party;
import com.lineage.game.model.L2Player;

/**
 * Format: ch d[Sdd]
 * @author SYS
 */
public class ExMPCCShowPartyMemberInfo extends L2GameServerPacket
{
	private FastList<PartyMemberInfo> members;

	public ExMPCCShowPartyMemberInfo(L2Player partyLeader)
	{
		if(!partyLeader.isInParty())
			return;

		L2Party _party = partyLeader.getParty();
		if(_party == null)
			return;

		if(!_party.isInCommandChannel())
			return;

		members = new FastList<PartyMemberInfo>();
		for(L2Player _member : _party.getPartyMembers())
			members.add(new PartyMemberInfo(_member.getName(), _member.getObjectId(), _member.getClassId().getId()));
	}

	@Override
	protected final void writeImpl()
	{
		if(members == null)
			return;

		writeC(EXTENDED_PACKET);
		writeH(0x4a);
		writeD(members.size()); // Количество членов в пати

		for(PartyMemberInfo member : members)
		{
			writeS(member.name); // Имя члена пати
			writeD(member.object_id); // object Id члена пати
			writeD(member.class_id); // id класса члена пати
		}

		members.clear();
	}

	static class PartyMemberInfo
	{
		public String name;
		public int object_id, class_id;

		public PartyMemberInfo(String _name, int _object_id, int _class_id)
		{
			name = _name;
			object_id = _object_id;
			class_id = _class_id;
		}
	}
}