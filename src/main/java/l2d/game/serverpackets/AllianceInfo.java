package l2d.game.serverpackets;

import l2d.game.model.L2Clan;
import l2d.game.model.L2Player;
import l2d.game.tables.ClanTable;

public class AllianceInfo extends L2GameServerPacket
{
	private final L2Player _cha;

	public AllianceInfo(L2Player cha)
	{
		_cha = cha;
	}

	@Override
	final public void runImpl()
	{
		SystemMessage sm;
		if(_cha.getAlliance() == null)
			return;

		_cha.sendPacket(new SystemMessage(SystemMessage._ALLIANCE_INFORMATION_));
		_cha.sendPacket(new SystemMessage(SystemMessage.ALLIANCE_NAME_S1).addString(_cha.getClan().getAlliance().getAllyName()));
		int clancount = 0;
		L2Clan leaderclan = _cha.getAlliance().getLeader();
		clancount = ClanTable.getInstance().getAlliance(leaderclan.getAllyId()).getMembers().length;
		int[] online = new int[clancount + 1];
		int[] count = new int[clancount + 1];
		L2Clan[] clans = _cha.getAlliance().getMembers();
		for(int i = 0; i < clancount; i++)
		{
			online[i + 1] = clans[i].getOnlineMembers(0).length;
			count[i + 1] = clans[i].getMembers().length;
			online[0] += online[i + 1];
			count[0] += count[i + 1];
		}
		//Connection
		sm = new SystemMessage(SystemMessage.CONNECTION_S1_TOTAL_S2);
		sm.addNumber(online[0]);
		sm.addNumber(count[0]);
		_cha.sendPacket(sm);
		sm = new SystemMessage(SystemMessage.ALLIANCE_LEADER_S2_OF_S1);
		sm.addString(leaderclan.getName());
		sm.addString(leaderclan.getLeaderName());
		_cha.sendPacket(sm);
		//clan count
		_cha.sendPacket(new SystemMessage(SystemMessage.AFFILIATED_CLANS_TOTAL_S1_CLAN_S).addNumber(clancount));
		_cha.sendPacket(new SystemMessage(SystemMessage._CLAN_INFORMATION_));
		for(int i = 0; i < clancount; i++)
		{
			_cha.sendPacket(new SystemMessage(SystemMessage.CLAN_NAME_S1).addString(clans[i].getName()));
			_cha.sendPacket(new SystemMessage(SystemMessage.CLAN_LEADER_S1).addString(clans[i].getLeaderName()));
			_cha.sendPacket(new SystemMessage(SystemMessage.CLAN_LEVEL_S1).addNumber(clans[i].getLevel()));
			sm = new SystemMessage(SystemMessage.CONNECTION_S1_TOTAL_S2);
			sm.addNumber(online[i + 1]);
			sm.addNumber(count[i + 1]);
			_cha.sendPacket(sm);
			_cha.sendPacket(new SystemMessage(SystemMessage.__DASHES__));
		}
		_cha.sendPacket(new SystemMessage(SystemMessage.__EQUALS__));
	}

	@Override
	protected final void writeImpl()
	{}
}