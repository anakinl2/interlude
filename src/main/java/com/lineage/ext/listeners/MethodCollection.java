package com.lineage.ext.listeners;

/**
 * @author Death
 */
public interface MethodCollection
{
	public static final String ReduceCurrentHp = "L2Character.ReduceCurrentHp";
	public static final String L2ZoneObjectEnter = "L2Zone.onZoneEnter";
	public static final String L2ZoneObjectLeave = "L2Zone.onZoneLeave";
	public static final String AbstractAInotifyEvent = "AbstractAI.notifyEvent";
	public static final String AbstractAIsetIntention = "AbstractAI.setIntention";
	public static final String onStartAttack = "L2Character.doAttack";
	public static final String onStartCast = "L2Character.doCast";
	public static final String onDecay = "L2Character.onDecay";
	public static final String doDie = "L2Character.doDie";
}
