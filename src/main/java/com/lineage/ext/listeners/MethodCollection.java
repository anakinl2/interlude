package com.lineage.ext.listeners;

/**
 * @author Death
 */
public enum MethodCollection {
    ReduceCurrentHp("L2Character.ReduceCurrentHp"),
    L2ZoneObjectEnter("L2Zone.onZoneEnter"),
    L2ZoneObjectLeave("L2Zone.onZoneLeave"),
    AbstractAInotifyEvent("AbstractAI.notifyEvent"),
    AbstractAIsetIntention("AbstractAI.setIntention"),
    onStartAttack("L2Character.doAttack"),
    onStartCast("L2Character.doCast"),
    onDecay("L2Character.onDecay"),
    doDie("L2Character.doDie");

    String command;

    MethodCollection(String command) {
        this.command = command;
    }
}
