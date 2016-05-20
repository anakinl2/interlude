package ai;

import l2d.game.ai.Fighter;
import l2d.game.model.L2Character;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Player;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.tables.ItemTable;

/**
 * При смерти дропает кучу хербов.
 */
public class BehemothDragon extends Fighter
{
	public BehemothDragon(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtDead()
	{
		L2NpcInstance actor = getActor();
		if(actor == null)
			return;
		L2Player killer = null;
		L2Character MostHated = actor.getMostHated();
		if(MostHated != null && MostHated instanceof L2Playable)
			killer = MostHated.getPlayer();
		for(int i = 0; i < 10; i++)
		{
			ItemTable.getInstance().createItem(8604).dropToTheGround(killer, actor); // Greater Herb of Mana
			ItemTable.getInstance().createItem(8601).dropToTheGround(killer, actor); // Greater Herb of Life
		}
		super.onEvtDead();
	}
}