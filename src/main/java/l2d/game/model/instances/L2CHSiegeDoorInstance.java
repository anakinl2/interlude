package l2d.game.model.instances;

import l2d.game.model.L2Character;
import l2d.game.templates.L2CharTemplate;

public class L2CHSiegeDoorInstance extends L2DoorInstance
{
	public L2CHSiegeDoorInstance(int objectId, L2CharTemplate template, int doorId, String name, boolean unlockable, boolean showHp)
	{
		super(objectId, template, doorId, name, unlockable, showHp);
	}

	@Override
	public void doDie(L2Character killer)
	{
		super.doDie(killer);
	}

	@Override
	public boolean isInvul()
	{
		return false;
	}
}