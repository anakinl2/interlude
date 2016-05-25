package com.lineage.ext.listeners.events;

import com.lineage.ext.listeners.PropertyType;

public interface PropertyEvent
{
	public Object getObject();

	public Object getOldValue();

	public Object getNewValue();

	public PropertyType getProperty();
}
