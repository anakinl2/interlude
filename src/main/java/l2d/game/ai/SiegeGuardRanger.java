package l2d.game.ai;

import l2d.game.model.L2Character;

public class SiegeGuardRanger extends SiegeGuard
{
	public SiegeGuardRanger(L2Character actor)
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
		return 25;
	}

	@Override
	public int getRateDOT()
	{
		return 50;
	}

	@Override
	public int getRateDEBUFF()
	{
		return 25;
	}

	@Override
	public int getRateDAM()
	{
		return 75;
	}

	@Override
	public int getRateSTUN()
	{
		return 75;
	}

	@Override
	public int getRateHEAL()
	{
		return 50;
	}
}