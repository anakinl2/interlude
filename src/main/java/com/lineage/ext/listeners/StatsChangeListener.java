package com.lineage.ext.listeners;

import l2d.game.skills.Calculator;
import l2d.game.skills.Env;
import l2d.game.skills.Stats;

public abstract class StatsChangeListener
{
	public final Stats _stat;
	protected Calculator _calculator;

	public StatsChangeListener(Stats stat)
	{
		_stat = stat;
	}

	public void setCalculator(Calculator calculator)
	{
		_calculator = calculator;
	}

	public abstract void statChanged(Double oldValue, double newValue, double baseValue, Env env);
}