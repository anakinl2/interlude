package l2d.game.skills;

import l2d.Config;
import l2d.debug.StatsLimitDebugger;
import l2d.ext.listeners.StatsChangeListener;
import l2d.game.model.L2Character;
import l2d.game.model.L2Playable;
import l2d.game.skills.funcs.Func;

/**
 * A calculator is created to manage and dynamically calculate the effect of a character property (ex : MAX_HP, REGENERATE_HP_RATE...).
 * In fact, each calculator is a table of Func object in which each Func represents a mathematic function : <BR><BR>
 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<BR><BR>
 * When the calc method of a calculator is launched, each mathematic function is called according to its priority <B>_order</B>.
 * Indeed, Func with lowest priority order is executed firsta and Funcs with the same order are executed in unspecified order.
 * The result of the calculation is stored in the value property of an Env class instance.<BR><BR>
 * Method addFunc and removeFunc permit to add and remove a Func object from a Calculator.<BR><BR>
 */
public final class Calculator
{
	/** Empty Func table definition */
	private static final Func[] emptyFuncs = new Func[0];

	/** Table of Func object */
	private Func[] _functions;

	private static final StatsChangeListener[] emptyListeners = new StatsChangeListener[0];
	private StatsChangeListener[] _listeners = emptyListeners;

	private Double _base = null;
	private Double _last = null;

	public final Stats _stat;
	public final L2Character _character;

	/**
	 * Constructor of Calculator (Init value : emptyFuncs).<BR><BR>
	 */
	public Calculator(final Stats stat, final L2Character character)
	{
		_stat = stat;
		_character = character;
		_functions = emptyFuncs;
		if(character instanceof L2Playable || !stat.isLimitOnlyPlayable())
			if(Config.DEBUG_STAT_LIMITS && character.isPlayer())
				addListener(new StatsLimitDebugger(stat));
	}

	/**
	 * Check if 2 calculators are equals.<BR><BR>
	 */
	public static boolean equalsCals(final Calculator c1, final Calculator c2)
	{
		if(c1 == c2)
			return true;

		if(c1 == null || c2 == null)
			return false;

		final Func[] funcs1 = c1.getFunctions();
		final Func[] funcs2 = c2.getFunctions();

		if(funcs1.length != funcs2.length)
			return false;

		if(funcs1 == funcs2)
			return true;

		if(funcs1.length == 0)
			return true;

		for(int i = 0; i < funcs1.length; i++)
			if(funcs1[i] != funcs2[i])
				return false;
		return true;

	}

	/**
	 * Return the number of Funcs in the Calculator.<BR><BR>
	 */
	public int size()
	{
		return _functions.length;
	}

	public synchronized void addListener(final StatsChangeListener l)
	{
		final StatsChangeListener[] tmp_listeners = new StatsChangeListener[_listeners.length + 1];
		if(_listeners.length > 0)
			System.arraycopy(_listeners, 0, tmp_listeners, 0, _listeners.length);
		tmp_listeners[_listeners.length] = l;
		l.setCalculator(this);
		_listeners = tmp_listeners;
	}

	/**
	 * Add a Func to the Calculator.<BR><BR>
	 */
	public synchronized void addFunc(final Func f)
	{
		final Func[] funcs = _functions;
		final Func[] tmp = new Func[funcs.length + 1];

		final int order = f._order;
		int i;

		for(i = 0; i < funcs.length && order >= funcs[i]._order; i++)
			tmp[i] = funcs[i];

		tmp[i] = f;

		for(; i < funcs.length; i++)
			tmp[i + 1] = funcs[i];

		_functions = tmp;
	}

	/**
	 * Remove a Func from the Calculator.<BR><BR>
	 */
	public synchronized void removeFunc(final Func f)
	{
		final Func[] funcs = _functions;
		final Func[] tmp = new Func[funcs.length - 1];

		int i;

		for(i = 0; i < funcs.length && f != funcs[i]; i++)
			tmp[i] = funcs[i];

		if(i == funcs.length)
			return;

		for(i++; i < funcs.length; i++)
			tmp[i - 1] = funcs[i];

		if(tmp.length == 0)
			_functions = emptyFuncs;
		else
			_functions = tmp;
	}

	/**
	 * Remove each Func with the specified owner of the Calculator.<BR><BR>
	 */
	public synchronized void removeOwner(final Object owner)
	{
		final Func[] funcs = _functions;
		for(final Func element : funcs)
			if(element._funcOwner == owner)
				removeFunc(element);
	}

	/**
	 * Run each Func of the Calculator.<BR><BR>
	 */
	public void calc(final Env env)
	{
		final Func[] funcs = _functions;
		_base = env.value;
		for(final Func element : funcs)
			element.calc(env);

		if(_stat._min != null && env.value < _stat._min)
			env.value = _stat._min;
		if(_stat._max != null && env.value > _stat._max && (_character.isPlayer() || !_stat.isLimitOnlyPlayable()))
			env.value = _stat._max;
		if(_last == null || _last != env.value)
		{
			for(final StatsChangeListener _listener : _listeners)
				_listener.statChanged(_last, env.value, _base, env);
			_last = env.value;
		}
	}

	/**
	 * Для отладки
	 */
	public Func[] getFunctions()
	{
		return _functions;
	}

	public Double getBase()
	{
		return _base;
	}

	public Double getLast()
	{
		return _last;
	}
}