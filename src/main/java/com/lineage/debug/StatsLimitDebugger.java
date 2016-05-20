package com.lineage.debug;

import com.lineage.ext.listeners.StatsChangeListener;
import com.lineage.game.model.L2Player;
import com.lineage.game.skills.Env;
import com.lineage.game.skills.Stats;
import com.lineage.game.skills.funcs.Func;
import com.lineage.util.Log;

public class StatsLimitDebugger extends StatsChangeListener
{
	public StatsLimitDebugger(Stats stat)
	{
		super(stat);
	}

	@Override
	public void statChanged(Double oldValue, double newValue, double baseValue, Env env)
	{
		if(!_calculator._character.isPlayer())
			return;
		if(_calculator._stat._max == null || _calculator._stat._max > newValue)
			return;

		double saveEnvValue = env.value;
		env.value = baseValue;

		String log_str = _calculator._stat.getValue() + " LIMIT [" + _calculator._stat._max + "] is reached, prevValue: " + oldValue + "\r\n";
		Func[] funcs = _calculator.getFunctions();
		for(int i = 0; i < funcs.length; i++)
		{
			String order = Integer.toHexString(funcs[i]._order).toUpperCase();
			if(order.length() == 1)
				order = "0" + order;
			log_str += "\tFunc #" + i + "@ [0x" + order + "]" + funcs[i].getClass().getSimpleName() + "\t" + env.value;
			funcs[i].calc(env);
			log_str += " -> " + env.value + (funcs[i]._funcOwner != null ? "; owner: " + funcs[i]._funcOwner.toString() : "; no owner") + "\r\n";
		}
		env.value = saveEnvValue;

		Log.add(log_str, "debug_stats_limit", (L2Player) _calculator._character);
	}
}