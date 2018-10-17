package com.robomwm.midnightportal;

import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created on 10/10/2018.
 *
 * @author RoboMWM
 */
public class MidnightPortal extends JavaPlugin
{
    private static TaskChainFactory taskChainFactory;
    public static <T> TaskChain<T> newChain() {
        return taskChainFactory.newChain();
    }
    public static <T> TaskChain<T> newSharedChain(String name) {
        return taskChainFactory.newSharedChain(name);
    }

    @Override
    public void onEnable()
    {
        taskChainFactory = BukkitTaskChainFactory.create(this);
    }
}
