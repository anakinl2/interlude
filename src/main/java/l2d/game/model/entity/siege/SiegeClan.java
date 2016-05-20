package l2d.game.model.entity.siege;

import l2d.game.model.L2Clan;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.tables.ClanTable;

public class SiegeClan
{
	private int _clanId = 0;
	private L2NpcInstance _headquarter;

	private SiegeClanType _type;

	public SiegeClan(int clanId, SiegeClanType type)
	{
		_clanId = clanId;
		_type = type;
	}

	public void setHeadquarter(L2NpcInstance headquarter)
	{
		_headquarter = headquarter;
	}

	public boolean removeHeadquarter()
	{
		if(_headquarter == null)
			return false;
		_headquarter.deleteMe();
		_headquarter = null;
		return true;
	}

	public int getClanId()
	{
		return _clanId;
	}

	public L2Clan getClan()
	{
		return ClanTable.getInstance().getClan(_clanId);
	}

	public L2NpcInstance getHeadquarter()
	{
		return _headquarter;
	}

	public SiegeClanType getType()
	{
		return _type;
	}

	public void setTypeId(SiegeClanType type)
	{
		_type = type;
	}
}