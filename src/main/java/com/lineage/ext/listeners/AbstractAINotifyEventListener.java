package com.lineage.ext.listeners;

import com.lineage.ext.listeners.events.MethodEvent;
import com.lineage.ext.listeners.events.AbstractAI.AbstractAINotifyEvent;
import com.lineage.game.ai.AbstractAI;
import com.lineage.game.ai.CtrlEvent;

/**
 * @Author: Diamond
 * @Date: 08/11/2007
 * @Time: 7:17:24
 */
public abstract class AbstractAINotifyEventListener implements MethodInvokeListener
{
	@Override
	public final void methodInvoked(MethodEvent e)
	{
		AbstractAINotifyEvent event = (AbstractAINotifyEvent) e;
		AbstractAI ai = event.getOwner();
		CtrlEvent evt = (CtrlEvent) event.getArgs()[0];
		NotifyEvent(ai, evt, (Object[]) event.getArgs()[1]);
	}

	@Override
	public final boolean accept(MethodEvent event)
	{
		MethodType method = event.getMethodName();
		return event instanceof AbstractAINotifyEvent && method.equals(MethodType.ABSTRACT_AI_NOTIFY_EVENT);
	}

	public abstract void NotifyEvent(AbstractAI ai, CtrlEvent evt, Object[] args);
}
