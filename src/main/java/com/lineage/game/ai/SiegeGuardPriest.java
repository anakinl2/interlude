package com.lineage.game.ai;

import com.lineage.game.model.L2Character;

public class SiegeGuardPriest extends SiegeGuard
{
	public SiegeGuardPriest(L2Character actor)
	{
		super(actor);
	}

	@Override
	public boolean getIsMobile()
	{
		return false;
	}

	@Override
	protected boolean createNewTask()
	{
		return defaultFightTask();
	}

	@Override
	public int getRatePHYS()
	{
		return _dam_skills.length == 0 ? 25 : 0;
	}

	@Override
	public int getRateDOT()
	{
		return 35;
	}

	@Override
	public int getRateDEBUFF()
	{
		return 50;
	}

	@Override
	public int getRateDAM()
	{
		return 60;
	}

	@Override
	public int getRateSTUN()
	{
		return 10;
	}

	@Override
	public int getRateHEAL()
	{
		return 90;
	}
}