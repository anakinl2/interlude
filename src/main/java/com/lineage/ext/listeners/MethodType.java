package com.lineage.ext.listeners;

/**
 * @author Death
 */
public enum MethodType {
    REDUCE_CURRENT_HP("L2Character.REDUCE_CURRENT_HP"),
    ZONE_OBJECT_ENTER("L2Zone.onZoneEnter"),
    ZONE_OBJECT_LEAVE("L2Zone.onZoneLeave"),
    ABSTRACT_AI_NOTIFY_EVENT("AbstractAI.notifyEvent"),
    ABSTRACT_AI_SET_INTENTION("AbstractAI.setIntention"),
    ON_START_ATTACK("L2Character.doAttack"),
    ON_START_CAST("L2Character.doCast"),
    ON_DECAY("L2Character.ON_DECAY"),
    DO_DIE("L2Character.DO_DIE");

    String command;

    MethodType(String command) {
        this.command = command;
    }
}
