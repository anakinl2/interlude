package com.lineage.ext.listeners.events;

import com.lineage.ext.listeners.MethodType;

/**
 * @author Death
 */
public class DefaultMethodInvokeEvent implements MethodEvent
{
	private final Object owner;
	private final Object[] args;
	private final MethodType methodName;

	public DefaultMethodInvokeEvent(MethodType methodName, Object owner, Object[] args)
	{
		this.methodName = methodName;
		this.owner = owner;
		this.args = args;
	}

	@Override
	public Object getOwner()
	{
		return owner;
	}

	@Override
	public Object[] getArgs()
	{
		return args;
	}

	@Override
	public MethodType getMethodName()
	{
		return methodName;
	}
}