package ai;

import com.lineage.ext.scripts.Functions;
import l2d.game.ai.DefaultAI;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.tables.SkillTable;
import com.lineage.util.Rnd;

public class EvilNpc extends DefaultAI
{
	private long _lastAction;
	private static final String[] _txt = {
			"Leave me alone!",
			"I will  revenge, then you'll ask me to forgive!",
			"you will have trouble!",
			"I will complain on you, you will be arrested!" };

	public EvilNpc(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage)
	{
		L2NpcInstance actor = getActor();
		if(actor == null || attacker == null || attacker.getPlayer() == null)
			return;

		actor.startAttackStanceTask();

		// Ругаемся и кастуем скилл не чаще, чем раз в 3 секунды
		if(System.currentTimeMillis() - _lastAction > 3000)
		{
			int chance = Rnd.get(0, 100);
			if(chance < 2)
			{
				attacker.getPlayer().setKarma(attacker.getPlayer().getKarma() + 5);
				attacker.sendChanges();
			}
			else if(chance < 4)
				actor.doCast(SkillTable.getInstance().getInfo(4578, 1), attacker, true); // Petrification
			else
				actor.doCast(SkillTable.getInstance().getInfo(4185, 7), attacker, true); // Sleep

			Functions.npcShout(actor, attacker.getName() + ", " + _txt[Rnd.get(_txt.length)]);
			_lastAction = System.currentTimeMillis();
		}
	}
}