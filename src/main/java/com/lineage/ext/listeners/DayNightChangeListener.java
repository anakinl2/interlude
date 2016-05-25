package com.lineage.ext.listeners;

import com.lineage.ext.listeners.events.PropertyEvent;
import com.lineage.game.GameTimeController;

/**
 * @Author: Death
 * @Date: 23/11/2007
 * @Time: 8:56:19
 */
public abstract class DayNightChangeListener implements PropertyChangeListener
{

	/**
	 * Вызывается при смене состояния
	 *
	 * @param event передаваемое событие
	 */
	@Override
	public final void propertyChanged(PropertyEvent event)
	{
		if(((GameTimeController) event.getObject()).isNowNight())
			switchToNight();
		else
			switchToDay();
	}

	@Override
	public final boolean accept(PropertyType property)
	{
		return PropertyType.GAME_TIME_CONTROLLER_DAY_NIGHT_CHANGE.equals(property);
	}

	/**
	 * Возвращает свойство даного листенера
	 *
	 * @return свойство
	 */
	@Override
	public final PropertyType getPropery()
	{
		return PropertyType.GAME_TIME_CONTROLLER_DAY_NIGHT_CHANGE;
	}

	/**
	 * Вызывается когда на сервер наступает ночь
	 */
	public abstract void switchToNight();

	/**
	 * Вызывается когда на сервере наступает день
	 */
	public abstract void switchToDay();
}
