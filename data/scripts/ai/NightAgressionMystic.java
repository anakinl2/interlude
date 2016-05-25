package ai;

import com.lineage.ext.listeners.DayNightChangeListener;
import com.lineage.ext.listeners.PropertyType;
import com.lineage.game.GameTimeController;
import com.lineage.game.ai.Mystic;
import com.lineage.game.model.L2Character;
import com.lineage.game.model.instances.L2NpcInstance;

/**
 * АИ для мобов, меняющих агресивность в ночное время.<BR>
 * Наследуется на прямую от Mystic.
 */
public class NightAgressionMystic extends Mystic
{
	public NightAgressionMystic(L2Character actor)
	{
		super(actor);
		GameTimeController.getInstance().getListenerEngine().addPropertyChangeListener(PropertyType.GAME_TIME_CONTROLLER_DAY_NIGHT_CHANGE, new NightAgressionDayNightListener());
	}

	private class NightAgressionDayNightListener extends DayNightChangeListener
	{
		private NightAgressionDayNightListener()
		{
			if(GameTimeController.getInstance().isNowNight())
				switchToNight();
			else
				switchToDay();
		}

		/**
		 * Вызывается, когда на сервере наступает ночь
		 */
		@Override
		public void switchToNight()
		{
			L2NpcInstance actor = getActor();
			if(actor != null)
				actor.setAggroRange(-1);
		}

		/**
		 * Вызывается, когда на сервере наступает день
		 */
		@Override
		public void switchToDay()
		{
			L2NpcInstance actor = getActor();
			if(actor != null)
				actor.setAggroRange(0);
		}
	}
}