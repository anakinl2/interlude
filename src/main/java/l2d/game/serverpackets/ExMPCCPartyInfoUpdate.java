package l2d.game.serverpackets;

import l2d.game.model.L2Party;

/**
 * ch Sddd
 */
public class ExMPCCPartyInfoUpdate extends L2GameServerPacket
{
	private L2Party _party;
	private int _mode;

	/**
	 * @param party
	 * @param mode 0 = Remove, 1 = Add
	 */
	public ExMPCCPartyInfoUpdate(L2Party party, int mode)
	{
		_party = party;
		_mode = mode;
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x5a);

		writeS(_party.getPartyLeader().getName());
		writeD(_party.getPartyLeaderOID());
		writeD(_party.getMemberCount());
		writeD(_mode); //mode 0 = Remove Party, 1 = AddParty, maybe more...
	}
}