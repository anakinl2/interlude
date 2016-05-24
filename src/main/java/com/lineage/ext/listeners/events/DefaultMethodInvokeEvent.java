package com.lineage.ext.listeners.events;

import com.lineage.ext.listeners.MethodCollection;

/**
 * @author Death
 */
public class DefaultMethodInvokeEvent implements MethodEvent
{
	private final Object owner;
	private final Object[] args;
	private final MethodCollection methodName;

	public DefaultMethodInvokeEvent(MethodCollection methodName, Object owner, Object[] args)
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
	public MethodCollection getMethodName()
	{
		return methodName;
	}
}