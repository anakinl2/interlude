package com.lineage.ext.listeners;

import com.lineage.ext.listeners.events.AbstractAI.AbstractAISetIntention;
import com.lineage.ext.listeners.events.MethodEvent;
import com.lineage.game.ai.AbstractAI;
import com.lineage.game.ai.CtrlIntention;

/**
 * @Author: Diamond
 * @Date: 08/11/2007
 * @Time: 7:17:24
 */
public abstract class AbstractAISetIntentionListener implements MethodInvokeListener
{
	@Override
	public final void methodInvoked(MethodEvent e)
	{
		AbstractAISetIntention event = (AbstractAISetIntention) e;
		AbstractAI ai = event.getOwner();
		CtrlIntention evt = (CtrlIntention) event.getArgs()[0];
		Object arg0 = event.getArgs()[1];
		Object arg1 = event.getArgs()[2];
		SetIntention(ai, evt, arg0, arg1);
	}

	@Override
	public final boolean accept(MethodEvent event)
	{
		MethodCollection method = event.getMethodName();
		return event instanceof AbstractAISetIntention && method.equals(MethodCollection.AbstractAIsetIntention);
	}

	public abstract void SetIntention(AbstractAI ai, CtrlIntention intention, Object arg0, Object arg1);
}
