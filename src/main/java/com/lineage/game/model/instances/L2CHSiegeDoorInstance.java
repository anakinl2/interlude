package com.lineage.game.model.instances;

import com.lineage.game.model.L2Character;
import com.lineage.game.templates.L2CharTemplate;

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