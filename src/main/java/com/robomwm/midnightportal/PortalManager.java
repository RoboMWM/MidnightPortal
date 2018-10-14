package com.robomwm.midnightportal;

import com.google.common.collect.Table;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Map;

/**
 * Created on 10/14/2018.
 *
 * @author RoboMWM
 */
public class PortalManager
{
    private Plugin plugin;
    private File storageFile; //TODO: may not be necessary (since we always want to find a portal...?)
    private Table<World, World, Double> enabledWorlds;
    private YamlConfiguration storedPortals;

    public PortalManager(Plugin plugin, Table<World, World, Double> enabledWorlds)
    {
        storageFile = new File(plugin.getDataFolder(), "portal.data");
        storedPortals = YamlConfiguration.loadConfiguration(storageFile);
        this.enabledWorlds = enabledWorlds;
    }

    private void saveStoredPortals()
    {
        final File storageFile = this.storageFile;
        final String dataToSave = storedPortals.saveToString();
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                try
                {
                    storedPortals.save(storageFile);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Finds the corresponding location, given an initial location
     * Maintains y value
     * @param location
     */
    public Location findOtherSide(Location location)
    {
        World world = location.getWorld();

        Map<World, Double> destinations = enabledWorlds.columnMap().get(world);

        //Check first column for world
        if (destinations == null)
            return null;

        //Maybe we'll support multiple destination worlds in the future
        World otherWorld = destinations.keySet().iterator().next();
        double multiplier = enabledWorlds.get(world, otherWorld);
        double y = location.getY();

        location.multiply(multiplier).setWorld(otherWorld);
        location.setY(y);

        return location;
    }

}
