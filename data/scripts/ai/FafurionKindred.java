package ai;

import l2d.Config;
import l2d.game.ThreadPoolManager;
import l2d.game.ai.Fighter;
import l2d.game.model.L2Character;
import l2d.game.model.instances.L2ItemInstance;
import l2d.game.model.instances.L2NpcInstance;
import l2d.game.tables.ItemTable;
import l2d.util.Location;
import l2d.util.Rnd;

import java.util.concurrent.ScheduledFuture;

/**
 * AI моба Fafurion Kindred
 * - после смерти Dark Water Dragon спаунится синий дракон - фафурион (дроп с него нужен для прохождение инстансов)
 * HP постепенно убывает (его надо хилить и нельзя дать умереть)
 * Every three seconds reduces Fafurions hp like it is poisoned (500 hp)
 * Fafurion Kindred disappears after two minutes
 */
public class FafurionKindred extends Fighter
{
	private ScheduledFuture<?> _poisonTimer;
	private ScheduledFuture<?> _despawnTimer;
	private final int DROPS[] = {9691, // Water Dragon Scale
		9700 // Water Dragon Detractor
			};

	public FafurionKindred(L2Character actor)
	{
		super(actor);
		startDespawnTimer();
		startPoisonTimer();
	}

	public void onTimer(String event)
	{
		if(event.equals("Poison"))
		{
			L2NpcInstance npc = this.getActor();
			// reduce current hp
			npc.reduceCurrentHp(500.0, npc, null, true, true, true, false);
			if(npc.getCurrentHp() > 0)
				startPoisonTimer();
		}
		else if(event.equals("fafurion_despawn"))
		{
			// drop item and disappear
			L2NpcInstance npc = this.getActor();
			int itemId = DROPS[Rnd.get(DROPS.length)]; // random item from script-drop
			L2ItemInstance item = ItemTable.getInstance().createItem(itemId);
			item.setCount((int) Config.RATE_QUESTS_DROP); // учтем рейты
			item.dropToTheGround(npc, new Location(this.getActor().getX(), this.getActor().getY(), this.getActor().getZ()));
			npc.deleteMe();
			// stop timers if any
			if(_poisonTimer != null)
			{
				_poisonTimer.cancel(false);
				_poisonTimer = null;
			}
			if(_despawnTimer != null)
			{
				_despawnTimer.cancel(false);
				_despawnTimer = null;
			}
		}
	}

	public void startPoisonTimer()
	{
		// poison - every 3 sec
		_poisonTimer = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTimerTask("Poison", this), 3000);
	}

	public void startDespawnTimer()
	{
		// despawn - 2 min
		_despawnTimer = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleTimerTask("fafurion_despawn", this), 120000);
	}

	private class ScheduleTimerTask implements Runnable
	{
		private String _name;
		private FafurionKindred _caller;

		public ScheduleTimerTask(String name, FafurionKindred classPtr)
		{
			_name = name;
			_caller = classPtr;
		}

		public void run()
		{
			_caller.onTimer(_name);
		}
	}

	@Override
	protected void onEvtDead()
	{
		// stop timers if any
		if(_poisonTimer != null)
		{
			_poisonTimer.cancel(false);
			_poisonTimer = null;
		}
		if(_despawnTimer != null)
		{
			_despawnTimer.cancel(false);
			_despawnTimer = null;
		}
		super.onEvtDead();
	}

	@Override
	protected boolean randomWalk()
	{
		return true;
	}
}