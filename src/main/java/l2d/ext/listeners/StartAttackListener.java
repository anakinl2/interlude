package l2d.ext.listeners;

import l2d.ext.listeners.events.MethodEvent;
import l2d.game.model.L2Character;

/**
 * User: Death
 */
public abstract class StartAttackListener implements MethodInvokeListener, MethodCollection
{
	@Override
	public final void methodInvoked(MethodEvent e)
	{
		onAttackStart((L2Character) e.getArgs()[0]);
	}

	@Override
	public final boolean accept(MethodEvent event)
	{
		return event.getMethodName().equals(onStartAttack);
	}

	public abstract void onAttackStart(L2Character target);
}
