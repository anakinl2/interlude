package com.lineage.ext.listeners;

import com.lineage.ext.listeners.events.MethodEvent;
import com.lineage.game.model.L2Character;

/**
 * User: Death
 */
public abstract class StartAttackListener implements MethodInvokeListener
{
	@Override
	public final void methodInvoked(MethodEvent e)
	{
		onAttackStart((L2Character) e.getArgs()[0]);
	}

	@Override
	public final boolean accept(MethodEvent event)
	{
		return event.getMethodName().equals(MethodCollection.onStartAttack);
	}

	public abstract void onAttackStart(L2Character target);
}
