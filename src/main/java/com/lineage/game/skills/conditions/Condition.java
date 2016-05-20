package com.lineage.game.skills.conditions;

import java.util.logging.Logger;

import com.lineage.game.skills.Env;

public abstract class Condition implements ConditionListener
{
	static final Logger _log = Logger.getLogger(Condition.class.getName());

	private ConditionListener _listener;

	private String _msg;
	private int _msgId;
	private boolean _addName = false;

	private boolean _result;

	public final void setMessage(String msg)
	{
		_msg = msg;
	}

	public final String getMessage()
	{
		return _msg;
	}

	public final void setMessageId(int msgId)
	{
		_msgId = msgId;
	}

	public final int getMessageId()
	{
		return _msgId;
	}

	public final void addName()
	{
		_addName = true;
	}

	public final boolean isAddName()
	{
		return _addName;
	}

	public void setListener(ConditionListener listener)
	{
		_listener = listener;
		notifyChanged();
	}

	public final ConditionListener getListener()
	{
		return _listener;
	}

	public final boolean test(Env env)
	{
		boolean res = testImpl(env);
		if(_listener != null && res != _result)
		{
			_result = res;
			notifyChanged();
		}
		return res;
	}

	public abstract boolean testImpl(Env env);

	@Override
	public void notifyChanged()
	{
		if(_listener != null)
			_listener.notifyChanged();
	}
}
