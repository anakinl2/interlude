package com.lineage.ext.listeners.events.L2Object;

import java.util.Collection;

import com.lineage.ext.listeners.PropertyType;
import com.lineage.ext.listeners.events.PropertyEvent;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Territory;

/**
 * @author Death
 * Date: 22/8/2007
 * Time: 12:57:10
 */
public class TerritoryChangeEvent implements PropertyEvent
{
	private final Collection<L2Territory> enter;
	private final Collection<L2Territory> exit;
	private final L2Object object;

	public TerritoryChangeEvent(Collection<L2Territory> enter, Collection<L2Territory> exit, L2Object object)
	{
		this.enter = enter;
		this.exit = exit;
		this.object = object;
	}

	@Override
	public L2Object getObject()
	{
		return object;
	}

	/**
	 * Возврщает список территорий с которых вышел объект
	 * @return список удаленных территорий
	 */
	@Override
	public Collection<L2Territory> getOldValue()
	{
		return exit;
	}

	/**
	 * Возвразает список территорой в которые вошел объект
	 * @return список добавленых территорий
	 */
	@Override
	public Collection<L2Territory> getNewValue()
	{
		return enter;
	}

	@Override
	public PropertyType getProperty()
	{
		return PropertyType.TERRITORY_CHANGED;
	}
}
