package com.lineage.ext.listeners.events.AbstractAI;

import com.lineage.ext.listeners.MethodCollection;
import com.lineage.ext.listeners.events.DefaultMethodInvokeEvent;
import com.lineage.game.ai.AbstractAI;

/**
 * @Author: Diamond
 * @Date: 08/11/2007
 * @Time: 7:17:24
 */
public class AbstractAINotifyEvent extends DefaultMethodInvokeEvent
{
	public AbstractAINotifyEvent(MethodCollection methodName, AbstractAI owner, Object[] args)
	{
		super(methodName, owner, args);
	}

	@Override
	public AbstractAI getOwner()
	{
		return (AbstractAI) super.getOwner();
	}

	@Override
	public Object[] getArgs()
	{
		return super.getArgs();
	}
}