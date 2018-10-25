package com.robomwm.midnightportal;

import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.robomwm.midnightportal.listener.PortalLighter;
import com.robomwm.midnightportal.listener.Teleporter;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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

        //Default config
        getConfig().addDefault("portalFrameMaterial", "BLACK_WOOL");
        Map<String, Double> worldExample = new HashMap<>();
        Map<String, Double> netherExample = new HashMap<>();
        worldExample.put("world_nether", 8D);
        netherExample.put("world", 0.125D);
        Map<String, Map<String, Double>> enabledWorldsExample = new LinkedHashMap<>();
        enabledWorldsExample.put("world", worldExample);
        enabledWorldsExample.put("world_nether", netherExample);
        getConfig().addDefault("enabledWorlds", enabledWorldsExample);
        getConfig().options().copyDefaults(true);
        saveConfig();

        Material portalFrameMaterial = Material.matchMaterial(getConfig().getString("portalFrameMaterial"));
        if (portalFrameMaterial == null)
        {
            getLogger().severe("Invalid portalFrameMaterial specified.");
            getPluginLoader().disablePlugin(this);
            return;
        }

        ConfigurationSection section = getConfig().getConfigurationSection("enabledWorlds");
        Table<World, World, Double> worlds = HashBasedTable.create();

        for (String worldKey : section.getKeys(false))
        {
            ConfigurationSection worldSection = section.getConfigurationSection(worldKey);
            String otherWorldKey = worldSection.getKeys(false).iterator().next();
            double multiplicationFactor = worldSection.getDouble(otherWorldKey);
            World world = getServer().getWorld(worldKey);
            World otherWorld = getServer().getWorld(otherWorldKey);
            if (world == null || otherWorld == null)
            {
                getLogger().warning(worldKey + " or " + otherWorldKey + " is not a valid/loaded world.");
                break;
            }
            worlds.put(world, otherWorld, multiplicationFactor);
        }

        PortalUtils portalUtils = new PortalUtils(this, worlds, portalFrameMaterial);
        new Teleporter(this, portalUtils);
        new PortalLighter(this, portalUtils, portalFrameMaterial);
    }
}
