package l2d.game.serverpackets;

import l2d.game.model.L2Clan;

public class PledgeShowInfoUpdate extends L2GameServerPacket
{
	private int clan_id, clan_level, clan_rep;
	private int HasCastle, HasHideout;

	public PledgeShowInfoUpdate(final L2Clan clan)
	{
		clan_id = clan.getClanId();
		clan_level = clan.getLevel();
		HasCastle = clan.getHasCastle();
		HasHideout = clan.getHasHideout();
		clan_rep = clan.getReputationScore();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x88);
		//sending empty data so client will ask all the info in response ;)
		writeD(clan_id);
		writeD(0);
		writeD(clan_level);
		writeD(HasCastle);
		writeD(HasHideout);
		writeD(0);// displayed in the "tree" view (with the clan skills)
		writeD(clan_rep);
		writeD(0);
		writeD(0);

		writeD(0); //c5
		writeS("bili"); //c5
		writeD(0); //c5
		writeD(0); //c5
	}
}