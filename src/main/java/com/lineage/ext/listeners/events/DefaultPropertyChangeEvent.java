package com.lineage.ext.listeners.events;

import com.lineage.ext.listeners.PropertyCollection;

public class DefaultPropertyChangeEvent implements PropertyEvent
{
	private final PropertyCollection event;
	private final Object actor;
	private final Object oldV;
	private final Object newV;

	public DefaultPropertyChangeEvent(PropertyCollection event, Object actor, Object oldV, Object newV)
	{
		this.event = event;
		this.actor = actor;
		this.oldV = oldV;
		this.newV = newV;
	}

	@Override
	public Object getObject()
	{
		return actor;
	}

	@Override
	public Object getOldValue()
	{
		return oldV;
	}

	@Override
	public Object getNewValue()
	{
		return newV;
	}

	@Override
	public PropertyCollection getProperty()
	{
		return event;
	}
}
