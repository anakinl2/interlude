package ai;

import com.lineage.ext.scripts.Functions;
import com.lineage.game.ai.Fighter;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.L2Player;
import com.lineage.game.model.instances.L2NpcInstance;

/**
 * @author PaInKiLlEr
 *         AI рейдбосса Tiberias
 *         любит поговорить после смерти
 */

public class Tiberias extends Fighter
{
	private L2Player self;
	L2Player player = (L2Player) self;
	L2NpcInstance actor = getActor();

	private static String[] _entxt = {"Your skills are impressive, I think that you can pass. Take a key and leave this place."};

	private static String[] _rutxt = {"Твои навыки впечатляют. Я полагаю, что ты можешь пройти. Бери ключ и покинь это место."};

	public Tiberias(L2Character actor)
	{
		super(actor);
	}

	/**
	 * Реплика перед гибелью
	 */
	@Override
	protected void onEvtDead()
	{
		if(player.getVar("lang@").equalsIgnoreCase("ru"))
			Functions.npcShout(actor, _rutxt[1]);
		else
			Functions.npcShout(actor, _entxt[1]);
		super.onEvtDead();
	}
}