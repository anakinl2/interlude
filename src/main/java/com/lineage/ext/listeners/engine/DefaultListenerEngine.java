package com.lineage.ext.listeners.engine;

import com.lineage.ext.listeners.MethodCollection;
import com.lineage.ext.listeners.MethodInvokeListener;
import com.lineage.ext.listeners.PropertyChangeListener;
import com.lineage.ext.listeners.PropertyCollection;
import com.lineage.ext.listeners.events.DefaultMethodInvokeEvent;
import com.lineage.ext.listeners.events.DefaultPropertyChangeEvent;
import com.lineage.ext.listeners.events.MethodEvent;
import com.lineage.ext.listeners.events.PropertyEvent;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Death
 */
public class DefaultListenerEngine<T> implements ListenerEngine<T>
{
	protected LinkedBlockingQueue<PropertyChangeListener> propertyChangeListeners;
	protected ConcurrentHashMap<PropertyCollection, LinkedBlockingQueue<PropertyChangeListener>> mappedPropertyChangeListeners;
	protected HashMap<PropertyCollection, Object> properties;

	protected LinkedBlockingQueue<MethodInvokeListener> methodInvokedListeners;
	protected ConcurrentHashMap<MethodCollection, LinkedBlockingQueue<MethodInvokeListener>> mappedMethodInvokedListeners;

	private final T owner;

	public DefaultListenerEngine(T owner)
	{
		this.owner = owner;
	}

	@Override
	public T getOwner()
	{
		return owner;
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
		if(propertyChangeListeners == null)
			propertyChangeListeners = new LinkedBlockingQueue<PropertyChangeListener>();

		propertyChangeListeners.add(listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener)
	{
		if(propertyChangeListeners == null)
			return;

		propertyChangeListeners.remove(listener);
	}

	@Override
	public void addPropertyChangeListener(PropertyCollection value, PropertyChangeListener listener)
	{
		if(mappedPropertyChangeListeners == null)
			mappedPropertyChangeListeners = new ConcurrentHashMap<>();

		LinkedBlockingQueue<PropertyChangeListener> listeners = mappedPropertyChangeListeners.get(value);

		if(listeners == null)
		{
			listeners = new LinkedBlockingQueue<>();
			mappedPropertyChangeListeners.put(value, listeners);
		}

		listeners.add(listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyCollection value, PropertyChangeListener listener)
	{
		if(mappedPropertyChangeListeners == null)
			return;

		LinkedBlockingQueue<PropertyChangeListener> listeners = mappedPropertyChangeListeners.get(value);

		if(listeners == null)
			return;

		listeners.remove(listener);
	}

	@Override
	public void firePropertyChanged(PropertyCollection value, T source, Object oldValue, Object newValue)
	{
		firePropertyChanged(new DefaultPropertyChangeEvent(value, source, oldValue, newValue));
	}

	@Override
	public void firePropertyChanged(PropertyEvent event)
	{
		if(propertyChangeListeners != null)
			for(PropertyChangeListener l : propertyChangeListeners)
				if(l.accept(event.getProperty()))
					l.propertyChanged(event);

		if(mappedPropertyChangeListeners == null)
			return;

		LinkedBlockingQueue<PropertyChangeListener> listeners = mappedPropertyChangeListeners.get(event.getProperty());

		if(listeners == null)
			return;

		for(PropertyChangeListener l : listeners)
			l.propertyChanged(event);
	}

	@Override
	public void addProperty(PropertyCollection property, Object value)
	{
		if(properties == null)
			properties = new HashMap<>();

		Object old = properties.get(property);
		properties.put(property, value);

		firePropertyChanged(property, getOwner(), old, value);
	}

	@Override
	public Object getProperty(PropertyCollection property)
	{
		if(properties == null)
			return null;

		return properties.get(property);
	}

	@Override
	public void addMethodInvokedListener(MethodInvokeListener listener)
	{
		if(methodInvokedListeners == null)
			methodInvokedListeners = new LinkedBlockingQueue<>();

		methodInvokedListeners.add(listener);
	}

	@Override
	public void removeMethodInvokedListener(MethodInvokeListener listener)
	{
		if(methodInvokedListeners == null)
			return;

		methodInvokedListeners.remove(listener);
	}

	@Override
	public void addMethodInvokedListener(MethodCollection methodName, MethodInvokeListener listener)
	{
		if(mappedMethodInvokedListeners == null)
			mappedMethodInvokedListeners = new ConcurrentHashMap<>();

		LinkedBlockingQueue<MethodInvokeListener> listeners = mappedMethodInvokedListeners.get(methodName);

		if(listeners == null)
		{
			listeners = new LinkedBlockingQueue<MethodInvokeListener>();
			mappedMethodInvokedListeners.put(methodName, listeners);
		}

		listeners.add(listener);
	}

	@Override
	public void removeMethodInvokedListener(MethodCollection methodName, MethodInvokeListener listener)
	{
		if(mappedMethodInvokedListeners == null)
			return;

		LinkedBlockingQueue<MethodInvokeListener> a = mappedMethodInvokedListeners.get(methodName);

		if(a == null)
			return;

		a.remove(listener);
	}

	@Override
	public void fireMethodInvoked(MethodEvent event)
	{
		if(methodInvokedListeners != null)
			for(MethodInvokeListener listener : methodInvokedListeners)
				if(listener.accept(event))
					listener.methodInvoked(event);

		if(mappedMethodInvokedListeners == null)
			return;

		LinkedBlockingQueue<MethodInvokeListener> list = mappedMethodInvokedListeners.get(event.getMethodName());

		if(list == null)
			return;

		for(MethodInvokeListener lsr : list)
			if(lsr.accept(event))
				lsr.methodInvoked(event);
	}

	@Override
	public void fireMethodInvoked(MethodCollection methodName, T source, Object[] args)
	{
		fireMethodInvoked(new DefaultMethodInvokeEvent(methodName, source, args));
	}
}
