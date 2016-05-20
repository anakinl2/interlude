package ai;

import l2d.ext.listeners.L2ZoneEnterLeaveListener;
import l2d.game.ai.Fighter;
import l2d.game.instancemanager.ZoneManager;
import l2d.game.model.L2Character;
import l2d.game.model.L2Object;
import l2d.game.model.L2Playable;
import l2d.game.model.L2Zone;
import l2d.game.model.L2Zone.ZoneType;
import l2d.game.model.instances.L2NpcInstance;

/**
 * @author PaInKiLlEr
 *         AI для РБ Anais в Monastery of Silence.
 *         Агрится на игроков.
 *         Видит через саулент мув.
 *         При выходе из комнаты делает телепортацию на место.
 *         Выполнено специально для L2Dream.su
 */
public class Anais extends Fighter
{
	private static L2Zone _zone;
	private static ZoneListener _zoneListener = new ZoneListener();

	public Anais(L2Character actor)
	{
		super(actor);
	}

	public void init()
	{
		_zone = ZoneManager.getInstance().getZoneById(ZoneType.epic, 702001, false);
		_zone.getListenerEngine().addMethodInvokedListener(_zoneListener);
	}

	@Override
	protected boolean maybeMoveToHome()
	{
		L2NpcInstance actor = getActor();
		if(actor != null && getZone().checkIfInZone(actor))
			teleportHome();
		return false;
	}

	public static class ZoneListener extends L2ZoneEnterLeaveListener
	{
		@Override
		public void objectEntered(L2Zone zone, L2Object object)
		{}

		@Override
		public void objectLeaved(L2Zone zone, L2Object object)
		{}
	}

	public static L2Zone getZone()
	{
		return _zone;
	}

	@Override
	public boolean isSilentMoveNotVisible(L2Playable target)
	{
		return true;
	}

	@Override
	protected void onEvtAggression(L2Character target, int aggro)
	{}

	public void onReload()
	{
		getZone().getListenerEngine().removeMethodInvokedListener(_zoneListener);
	}
}