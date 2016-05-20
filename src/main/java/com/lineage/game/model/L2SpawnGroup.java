package com.lineage.game.model;

import com.lineage.game.model.instances.L2NpcInstance;

public abstract class L2SpawnGroup
{
	public abstract void OnNpcDeleted(L2Spawn spawn, L2NpcInstance npc);

	public abstract void OnNpcCreated(L2Spawn spawn, L2NpcInstance npc);
}
