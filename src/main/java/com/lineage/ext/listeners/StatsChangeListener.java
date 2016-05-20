package com.lineage.ext.listeners;

import com.lineage.game.skills.Calculator;
import com.lineage.game.skills.Env;
import com.lineage.game.skills.Stats;

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