package l2d.game.serverpackets;

import java.util.Vector;

import l2d.game.instancemanager.PartyRoomManager;
import l2d.game.model.L2Player;
import l2d.game.model.PartyRoom;

/**
 * Format:(ch) d d [dsdddd]
 */
public class ExPartyRoomMember extends L2GameServerPacket
{
	private Vector<L2Player> _members;
	private L2Player _activeChar;

	public ExPartyRoomMember(PartyRoom room, L2Player activeChar)
	{
		_members = room.getMembers();
		_activeChar = activeChar;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x0E);

		// 0x01 - we are leader
		// 0x00 - we are not leader
		L2Player leader = _members.get(0);
		boolean isLeader = _members.get(0).equals(_activeChar);
		writeD(isLeader ? 0x01 : 0x00);
		writeD(_members.size()); //size
		for(L2Player member : _members)
		{
			boolean isPartyMember = false;
			if(leader.getParty() != null && member.getParty() != null && leader.getParty() == member.getParty())
				isPartyMember = true;

			writeD(member.getObjectId()); //player object id
			writeS(member.getName()); //player name
			writeD(member.getClassId().ordinal()); //player class id
			writeD(member.getLevel()); //player level
			writeD(PartyRoomManager.getInstance().getLocation(member)); //location
			writeD(isLeader ? 0x01 : isPartyMember ? 0x02 : 0x00); //1-leader     2-party member    0-not party member
			isLeader = false;
		}
	}
}