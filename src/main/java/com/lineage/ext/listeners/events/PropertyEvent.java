package com.lineage.ext.listeners.events;

import com.lineage.ext.listeners.PropertyCollection;

public interface PropertyEvent
{
	public Object getObject();

	public Object getOldValue();

	public Object getNewValue();

	public PropertyCollection getProperty();
}
