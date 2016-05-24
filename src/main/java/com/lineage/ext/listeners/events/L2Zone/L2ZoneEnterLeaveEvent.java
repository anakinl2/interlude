package com.lineage.ext.listeners.events.L2Zone;

import com.lineage.ext.listeners.MethodCollection;
import com.lineage.ext.listeners.events.DefaultMethodInvokeEvent;
import com.lineage.game.model.L2Object;
import com.lineage.game.model.L2Zone;

/**
 * @Author: Death
 * @Date: 18/9/2007
 * @Time: 9:30:13
 */
public class L2ZoneEnterLeaveEvent extends DefaultMethodInvokeEvent
{
	public L2ZoneEnterLeaveEvent(MethodCollection methodName, L2Zone owner, L2Object[] args)
	{
		super(methodName, owner, args);
	}

	@Override
	public L2Zone getOwner()
	{
		return (L2Zone) super.getOwner();
	}

	@Override
	public L2Object[] getArgs()
	{
		return (L2Object[]) super.getArgs();
	}
}
