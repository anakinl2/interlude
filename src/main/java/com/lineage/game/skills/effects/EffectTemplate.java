package com.lineage.game.skills.effects;

import java.util.logging.Logger;

import com.lineage.game.model.L2Effect;
import com.lineage.game.model.L2Effect.EffectType;
import com.lineage.game.skills.Env;
import com.lineage.game.skills.conditions.Condition;
import com.lineage.game.skills.funcs.FuncTemplate;
import com.lineage.game.templates.StatsSet;

public final class EffectTemplate
{
	static Logger _log = Logger.getLogger(EffectTemplate.class.getName());

	public static final String NO_STACK = "none".intern();

	public Condition _attachCond;
	public final double _value;
	public int _counter;
	public long _period; // in milliseconds
	public final int _abnormalEffect;

	public FuncTemplate[] _funcTemplates;
	public final EffectType _effectType;

	public final String _stackType;
	public final String _stackType2;
	public final int _stackOrder;
	public final int _displayId;
	public final int _displayLevel;

	public final boolean _hidden;
	public final boolean _noforcaster;
	public final String _options;
	public final boolean _applyOnCaster;
	
	//for balance panel.
	public final int _counterOriginal;
	public final long _periodOriginal;


	public EffectTemplate(StatsSet set)
	{
		_value = set.getDouble("value");
		_counter = set.getInteger("count", 1) < 0 ? Integer.MAX_VALUE : set.getInteger("count", 1);
		_counterOriginal = _counter;
		_period = Math.min(Integer.MAX_VALUE, 1000 * (set.getInteger("time", 1) < 0 ? Integer.MAX_VALUE : set.getInteger("time", 1)));
		_periodOriginal = _period;
		_abnormalEffect = set.getInteger("abnormal", 0);
		_stackType = set.getString("stackType", NO_STACK);
		_stackType2 = set.getString("stackType2", NO_STACK);
		_stackOrder = set.getInteger("stackOrder", _stackType == NO_STACK && _stackType2 == NO_STACK ? 1 : 0);
		_applyOnCaster = set.getBool("applyOnCaster", false);
		_hidden = set.getBool("hidden", false);
		_noforcaster = set.getBool("noForCaster", false);
		_options = set.getString("options", "");
		_displayId = set.getInteger("displayId", 0);
		_displayLevel = set.getInteger("displayLevel", 0);
		_effectType = set.getEnum("name", EffectType.class);
	}

	public L2Effect getEffect(Env env)
	{
		if(_attachCond != null && !_attachCond.test(env))
			return null;
		return _effectType.makeEffect(env, this);
	}

	public void attachCond(Condition c)
	{
		_attachCond = c;
	}

	public void attachFunc(FuncTemplate f)
	{
		if(_funcTemplates == null)
			_funcTemplates = new FuncTemplate[] { f };
		else
		{
			int len = _funcTemplates.length;
			FuncTemplate[] tmp = new FuncTemplate[len + 1];
			System.arraycopy(_funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			_funcTemplates = tmp;
		}
	}

	public long getPeriod()
	{
		return _period;
	}

	public EffectType getEffectType()
	{
		return _effectType;
	}

	public long getPeriodOriginal()
	{
		return _periodOriginal;
	}

}