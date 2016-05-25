package com.lineage.ext.listeners.events;

import com.lineage.ext.listeners.MethodType;

/**
 * @author Death
 */
public interface MethodEvent {
    public Object[] getArgs();

    public MethodType getMethodName();
    public Object getOwner();
}
