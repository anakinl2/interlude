package com.lineage.game.skills.funcs;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.lineage.game.skills.Stats;
import com.lineage.game.skills.conditions.Condition;

public final class FuncTemplate
{
	public Condition _applyCond;
	@SuppressWarnings("unchecked")
	public Class _func;
	@SuppressWarnings("unchecked")
	public Constructor _constructor;
	public Stats _stat;
	public int _order;
	public double _value;

	@SuppressWarnings("unchecked")
	public FuncTemplate(final Condition applyCond, final String func, final Stats stat, final int order, final double value)
	{
		_applyCond = applyCond;
		_stat = stat;
		_order = order;
		_value = value;
		try
		{
			_func = Class.forName("Func" + func);
		}
		catch(final ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
		try
		{
			_constructor = _func.getConstructor(new Class[] { Stats.class, // stats to update
					Integer.TYPE, // order of execution
					Object.class, // owner
					Double.TYPE // value for function
			});
		}
		catch(final NoSuchMethodException e)
		{
			throw new RuntimeException(e);
		}
	}

	public Func getFunc(final Object owner)
	{
		try
		{
			final Func f = (Func) _constructor.newInstance(_stat, _order, owner, _value);
			if(_applyCond != null)
				f.setCondition(_applyCond);
			return f;
		}
		catch(final IllegalAccessException e)
		{
			e.printStackTrace();
			return null;
		}
		catch(final InstantiationException e)
		{
			e.printStackTrace();
			return null;
		}
		catch(final InvocationTargetException e)
		{
			e.printStackTrace();
			return null;
		}
	}
}