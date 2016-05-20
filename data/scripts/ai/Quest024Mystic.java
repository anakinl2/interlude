package ai;

import l2d.game.ai.Mystic;
import l2d.game.instancemanager.QuestManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2Player;
import l2d.game.model.L2World;
import l2d.game.model.quest.Quest;

public class Quest024Mystic extends Mystic
{
	private final String myEvent;

	public Quest024Mystic(L2Character actor)
	{
		super(actor);
		myEvent = "playerInMobRange_" + actor.getNpcId();
	}

	@Override
	protected boolean thinkActive()
	{
		Quest q = QuestManager.getQuest(24);
		if(q != null)
			for(L2Player player : L2World.getAroundPlayers(getActor(), 900, 200))
				player.processQuestEvent(q.getName(), myEvent);

		return super.thinkActive();
	}
}