package l2d.game.skills.effects;

import com.lineage.ext.listeners.MethodCollection;
import com.lineage.ext.listeners.reduceHp.ReduceCurrentHpListener;
import l2d.game.model.L2Character;
import l2d.game.model.L2Effect;
import l2d.game.model.L2Skill;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.skills.Env;

public final class EffectCurseOfLifeFlow extends L2Effect
{
	private CurseOfLifeFlowListener _listener;

	private int _damage = 0;

	public EffectCurseOfLifeFlow(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		_listener = new CurseOfLifeFlowListener();
		_effected.addMethodInvokeListener(MethodCollection.ReduceCurrentHp, _listener);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		_effected.removeMethodInvokeListener(MethodCollection.ReduceCurrentHp, _listener);
		_listener = null;
	}

	@Override
	public boolean onActionTime()
	{
		if(_effected.isDead() || _effector.isDead())
			return false;

		if(_effector.isCurrentHpFull())
			return true;

		if(_damage <= 0)
			return true;

		double max_heal = calc();
		double heal = Math.min(_damage, max_heal);
		double newHp = Math.min(_effector.getCurrentHp() + heal, _effector.getMaxHp());

		_effector.sendPacket(new SystemMessage(SystemMessage.S1_HPS_HAVE_BEEN_RESTORED).addNumber((int) (newHp - _effector.getCurrentHp())));
		_effector.setCurrentHp(newHp, false);

		return true;
	}

	private class CurseOfLifeFlowListener extends ReduceCurrentHpListener
	{
		@Override
		public void onReduceCurrentHp(L2Character actor, double damage, L2Character attacker, L2Skill skill, boolean awake, boolean standUp, boolean directHp)
		{
			if(_effector == attacker)
				_damage += damage;
		}
	}
}