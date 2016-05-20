package ai;

import java.util.concurrent.ScheduledFuture;

import com.lineage.ext.scripts.Functions;
import l2d.game.ThreadPoolManager;
import l2d.game.ai.CtrlEvent;
import l2d.game.ai.Fighter;
import l2d.game.geodata.GeoEngine;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;
import com.lineage.util.Location;
import com.lineage.util.Rnd;

/**
 * AI для Ol Mahum Guard ID: 20058
 */
public class OlMahumGuard extends Fighter
{
	private L2Character _attacker;
	static final String[] flood =
		{
			"I'll be back",
			"You are stronger than expected"
		};

	public OlMahumGuard(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return;
		if(attacker != null && !actor.isAfraid() && actor.getCurrentHpPercents() < 50)
		{
			_attacker = attacker;
			Functions.npcShout(actor, flood[Rnd.get(flood.length)]);

			// Удаляем все задания
			clearTasks();

			actor.breakAttack();
			actor.breakCast(true);
			actor.stopMove();
			actor.startFear();

			int posX = actor.getX();
			int posY = actor.getY();
			int posZ = actor.getZ();

			int signx = posX < attacker.getX() ? -1 : 1;
			int signy = posY < attacker.getY() ? -1 : 1;

			int range = 1000;

			posX += Math.round(signx * range);
			posY += Math.round(signy * range);
			posZ = GeoEngine.getHeight(posX, posY, posZ);

			actor.setRunning();
			actor.moveToLocation(new Location(posX, posY, posZ), 0, true);

			startEndFearTask();
		}
		else
			super.onEvtAttacked(attacker, damage);
	}

	@Override
	protected void onEvtDead()
	{
		_attacker = null;
		L2NpcInstance actor = getActor();
		if(actor != null)
			actor.stopFear();
		if(_endFearTask != null)
			_endFearTask.cancel(true);
		_endFearTask = null;
		super.onEvtDead();
	}

	@Override
	public void removeActor()
	{
		_attacker = null;
		super.removeActor();
	}

	@SuppressWarnings("unchecked")
	private ScheduledFuture _endFearTask;

	public void startEndFearTask()
	{
		if(_endFearTask != null)
			_endFearTask.cancel(true);
		_endFearTask = ThreadPoolManager.getInstance().scheduleAi(new EndFearTask(), 10000, false);
	}

	public class EndFearTask implements Runnable
	{
		public void run()
		{
			L2NpcInstance actor = getActor();
			if(actor != null)
				actor.stopFear();
			_endFearTask = null;
			if(_attacker != null)
				notifyEvent(CtrlEvent.EVT_AGGRESSION, _attacker, 100);
		}
	}

	@Override
	public void checkAggression(L2Character target)
	{}

	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{}
}