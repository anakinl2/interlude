package ai;

import com.lineage.game.ai.DefaultAI;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Spawn;
import com.lineage.game.model.instances.L2NpcInstance;
import com.lineage.game.tables.SpawnTable;

/**
 * @author PaInKiLlEr
 *         AI для Holy Brazier в Monastery of Silence.
 *         После убийства статуи исчезают все мобы.
 *         Не использует рандом валк.
 *         Не использует рандом анимацию.
 *         TODO: удаляются наглухо после убийства статуи, не респавнятся
 *         Выполнено специально для L2Dream.su
 */
public class HolyBrazier extends DefaultAI
{
	public HolyBrazier(L2Character actor)
	{
		super(actor);
	}

	protected void onEvtDead()
	{
		L2NpcInstance actor = getActor();
		L2Spawn spawn = actor.getSpawn();
		if(spawn.getLocation() != 9142)
		{
			SpawnTable.getInstance().deleteSpawn(spawn, false);
			spawn.setRespawnDelay(60);
		}
	}

	@Override
	protected boolean randomWalk()
	{
		return false;
	}

	@Override
	protected boolean randomAnimation()
	{
		return false;
	}
}