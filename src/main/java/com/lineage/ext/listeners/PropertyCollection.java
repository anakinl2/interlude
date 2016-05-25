package com.lineage.ext.listeners;

public enum PropertyCollection {
    HitPoints("L2Character.HitPoints"),
    TerritoryChanged("L2Object.TerritoryChangeEvent"),
    HeroChatLaunched("Say2.HeroChatLaunched"),
    ShoutChatLaunched("Say2.ShoutChatLaunched"),
    TradeChatLaunched("Say2.TradeChatLaunched"),
    ZoneEnteredNoLandingFlying("L2Zone.EnteredNoLandingOnWywern"),
    GameTimeControllerDayNightChange("GameTimeController.DayNightChange");

    String command;

    PropertyCollection(String command) {
        this.command = command;
    }
}
