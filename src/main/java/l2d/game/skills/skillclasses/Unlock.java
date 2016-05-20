package l2d.game.skills.skillclasses;

import javolution.util.FastList;
import com.lineage.ext.multilang.CustomMessage;
import l2d.game.cache.Msg;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.L2Skill;
import l2d.game.model.instances.L2ChestInstance;
import l2d.game.model.instances.L2DoorInstance;
import l2d.game.serverpackets.SystemMessage;
import l2d.game.templates.StatsSet;
import com.lineage.util.Rnd;

public class Unlock extends L2Skill
{
	final int _unlockPower;

	public Unlock(StatsSet set)
	{
		super(set);
		_unlockPower = set.getInteger("unlockPower", 0) + 100;
	}

	@Override
	public boolean checkCondition(L2Character activeChar, L2Character target, boolean forceUse, boolean dontMove, boolean first)
	{
		if(target instanceof L2ChestInstance && target.isDead())
		{
			activeChar.sendPacket(Msg.INVALID_TARGET);
			return false;
		}

		if(target instanceof L2ChestInstance && activeChar.isPlayer())
			return super.checkCondition(activeChar, target, forceUse, dontMove, first);

		if(!(target instanceof L2DoorInstance) || _unlockPower == 0)
		{
			activeChar.sendPacket(Msg.INVALID_TARGET);
			return false;
		}

		L2DoorInstance door = (L2DoorInstance) target;

		if(door.isOpen())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.IT_IS_NOT_LOCKED));
			return false;
		}

		if(!door.isUnlockable())
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_UNABLE_TO_UNLOCK_THE_DOOR));
			return false;
		}

		if(door.key > 0) // ключ не подходит к двери
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_UNABLE_TO_UNLOCK_THE_DOOR));
			return false;
		}

		if(_unlockPower - door.getLevel() * 100 < 0) // Дверь слишком высокого уровня
		{
			activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_UNABLE_TO_UNLOCK_THE_DOOR));
			return false;
		}

		return super.checkCondition(activeChar, target, forceUse, dontMove, first);
	}

	@Override
	public void useSkill(L2Character activeChar, FastList<L2Character> targets)
	{
		for(FastList.Node<L2Character> n = targets.head(), end = targets.tail(); (n = n.getNext()) != end;)
		{
			L2Character targ = n.getValue();
			if(targ instanceof L2DoorInstance)
			{
				L2DoorInstance target = (L2DoorInstance) targ;
				if(!target.isOpen() && (target.key > 0 || Rnd.chance(_unlockPower - target.level * 100)))
				{
					target.openMe();
					target.onOpen();
				}
				else
					activeChar.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_UNLOCK_THE_DOOR));
				return;
			}

			L2ChestInstance target = (L2ChestInstance) targ;

			if(target.isDead())
				return;

			if(target.isFake())
			{
				target.onOpen((L2Player) activeChar);
				return;
			}

			double chance = getActivateRate();
			double levelmod = (double) getMagicLevel() - target.getLevel();
			chance += levelmod * getLevelModifier();

			if(chance < 0)
				chance = 1;

			if(chance < 100)
				activeChar.sendMessage(new CustomMessage("l2d.game.skills.skillclasses.Unlock.Chance", activeChar).addString(getName()).addNumber((int) chance));

			if(Rnd.chance(chance))
			{
				activeChar.sendMessage(new CustomMessage("l2d.game.skills.skillclasses.Unlock.Success", activeChar));
				target.onOpen((L2Player) activeChar);
			}
			else
			{
				activeChar.sendPacket(new SystemMessage(SystemMessage.S1_HAS_FAILED).addString(getName()));
				target.doDie(activeChar);
			}
		}
	}
}