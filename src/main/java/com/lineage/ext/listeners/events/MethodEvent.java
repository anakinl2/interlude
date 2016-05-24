package com.lineage.ext.listeners.events;

import com.lineage.ext.listeners.MethodCollection;

/**
 * @author Death
 */
public interface MethodEvent {
    public Object[] getArgs();

    public MethodCollection getMethodName();
    public Object getOwner();
}
