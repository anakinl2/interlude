package ai;

import com.lineage.game.ai.Fighter;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.instances.L2NpcInstance;

/**
 * @author PaInKiLlEr
 *         AI мобов Imperial Slave, спавнятся из АИ ImperialGravekeeper.
 *         Деспавнятся при простое более 10(?) минут
 *         Не используют функцию Random Walk
 *         ID: 27180
 */
public class ImperialSlave extends Fighter
{
	private long _wait_timeout = 0;
	private boolean _wait = false;
	private static final int DESPAWN_TIME = 10 * 60 * 1000; // 10 min

	public ImperialSlave(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected boolean thinkActive()
	{
		L2NpcInstance actor = getActor();
		if(actor == null || actor.isDead())
			return true;

		if(_def_think)
		{
			doTask();
			_wait = false;
			return true;
		}

		if( !_wait)
		{
			_wait = true;
			_wait_timeout = System.currentTimeMillis() + DESPAWN_TIME;
		}

		if(_wait_timeout != 0 && _wait && _wait_timeout < System.currentTimeMillis())
		{
			actor.deleteMe();
			return true;
		}

		return super.thinkActive();
	}

	@Override
	protected boolean randomWalk()
	{
		return false;
	}
}