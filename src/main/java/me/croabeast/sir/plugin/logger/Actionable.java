package me.croabeast.sir.plugin.logger;


interface Actionable {

    void act();

    default Runnable toRunnable() {
        return this::act;
    }
}
