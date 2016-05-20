package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.game.ai.CtrlIntention;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.templates.StatsSet;
import com.lineage.util.GArray;
import com.lineage.util.Rnd;

/**
 * Switch Снимает таргет. Для использования требуется кинжал. От автора: в пве переключает моба на другого игрока поблизости \ снимает таргет если игрока поблизости нет, в пвп просто снимает таргет и
 * сбрасывает касты скиллов.
 */
public class Switch extends L2Skill
{
	boolean unggroid;

	public Switch(StatsSet set)
	{
		super(set);
		unggroid = set.getBool("unaggroing", true);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		L2Player player;
		L2Character toAttack;
		L2Character target;

		if(!activeChar.isPlayer())
			return;
		player = activeChar.getPlayer();

		if(player.getTarget() == null || !(player.getTarget() instanceof L2Character))
			return;
		target = (L2Character) player.getTarget();
		if(!player.isGM())
		{
			int skilldiff = target.getLevel() - getMagicLevel();
			int lvldiff = target.getLevel() - player.getLevel();
			if(skilldiff > 10 || skilldiff > 5 && Rnd.chance(30) || Rnd.chance(Math.abs(lvldiff) * 2))
				return;
		}

		GArray<L2Character> arounds = new GArray<L2Character>();
		for(L2Character ch : target.getAroundCharacters(900, 200/* На "глазок" */))
			if(ch != activeChar && ch.isPlayer())
				arounds.add(ch);
		if(arounds.isEmpty())
		{
			target.setTarget(null);
			target.abortAttack();
			target.abortCast();
			return;
		}

		toAttack = arounds.get(Rnd.get(arounds.size()));

		if(target.isMonster())
		{
			target.setRunning();
			target.getAI().Attack(toAttack, true);
			target.startConfused();
			if(unggroid)
			{
				target.getAI().setGlobalAggro(-10);
				target.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
			}
		}
		else if(target.isPlayer())
		{
			if(target.getPvpFlag() > 0 || target.getKarma() > 0)
			{
				target.setTarget(null);
				target.abortAttack();
				target.abortCast();
			}
			return;
		}
	}

}
