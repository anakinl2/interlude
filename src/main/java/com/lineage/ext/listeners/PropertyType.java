package com.lineage.ext.listeners;

public enum PropertyType {
    HIT_POINTS("L2Character.HIT_POINTS"),
    TERRITORY_CHANGED("L2Object.TerritoryChangeEvent"),
    HERO_CHAT_LAUNCHED("Say2.HERO_CHAT_LAUNCHED"),
    SHOUT_CHAT_LAUNCHED("Say2.SHOUT_CHAT_LAUNCHED"),
    TRADE_CHAT_LAUNCHED("Say2.TRADE_CHAT_LAUNCHED"),
    ZONE_ENTERED_NO_LANDING_FLYING("L2Zone.EnteredNoLandingOnWywern"),
    GAME_TIME_CONTROLLER_DAY_NIGHT_CHANGE("GameTimeController.DayNightChange");

    String command;

    PropertyType(String command) {
        this.command = command;
    }
}
