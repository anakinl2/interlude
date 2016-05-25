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
	public final boolean accept(PropertyCollection property)
	{
		return PropertyCollection.GameTimeControllerDayNightChange.equals(property);
	}

	/**
	 * Возвращает свойство даного листенера
	 *
	 * @return свойство
	 */
	@Override
	public final PropertyCollection getPropery()
	{
		return PropertyCollection.GameTimeControllerDayNightChange;
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
