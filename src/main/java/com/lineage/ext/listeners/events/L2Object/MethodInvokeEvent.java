package com.lineage.ext.listeners.events.L2Object;

import com.lineage.ext.listeners.MethodType;
import com.lineage.ext.listeners.events.DefaultMethodInvokeEvent;
import com.lineage.game.model.L2Object;

/**
 * @author Death
 */
public class MethodInvokeEvent extends DefaultMethodInvokeEvent
{
	public MethodInvokeEvent(MethodType methodName, L2Object owner, Object[] args)
	{
		super(methodName, owner, args);
	}

	public L2Object getObject()
	{
		return (L2Object) getOwner();
	}
}
