package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import l2d.game.model.L2Character;
import l2d.game.model.L2Skill;
import l2d.game.templates.StatsSet;

public class TeleportNpc extends L2Skill
{
	public TeleportNpc(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character target = n.getValue();
			if(target == null || target.isDead())
				continue;

			getEffects(activeChar, target, getActivateRate() > 0, false);
			target.abortAttack();
			target.abortCast();
			target.stopMove();
			int x = activeChar.getX();
			int y = activeChar.getY();
			int z = activeChar.getZ();
			int h = activeChar.getHeading();
			int range = (int) (activeChar.getColRadius() + target.getColRadius());
			int hyp = (int) Math.sqrt(range * range / 2);
			if(h < 16384)
			{
				x += hyp;
				y += hyp;
			}
			else if(h > 16384 && h <= 32768)
			{
				x -= hyp;
				y += hyp;
			}
			else if(h < 32768 && h <= 49152)
			{
				x -= hyp;
				y -= hyp;
			}
			else if(h > 49152)
			{
				x += hyp;
				y -= hyp;
			}
			target.setXYZ(x, y, z);
			target.validateLocation(true);
		}
	}
}