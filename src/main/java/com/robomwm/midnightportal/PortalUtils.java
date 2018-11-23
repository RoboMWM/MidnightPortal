package com.robomwm.midnightportal;

import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainTasks;
import com.google.common.collect.Table;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.EndGateway;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created on 10/14/2018.
 *
 * @author RoboMWM
 */
public class PortalUtils
{
    private Plugin plugin;
    private Table<World, World, Double> enabledWorlds;
    private Material portalFrameMaterial;

    public PortalUtils(Plugin plugin, Table<World, World, Double> enabledWorlds, Material portalFrameMaterial)
    {
        this.plugin = plugin;
        this.enabledWorlds = enabledWorlds;
        this.portalFrameMaterial = portalFrameMaterial;
    }

    public boolean isEnabledWorld(World world)
    {
        return enabledWorlds.columnMap().get(world) != null;
    }

    /**
     * Finds the corresponding portal exit location, given an initial portal location
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

    /**
     * Finds or creates a portal.
     * @param location the location to find a portal near (Searches its chunk), or creates one here if none found.
     * @return the Taskchain. Portal location exists in the TaskChain#getTaskData("location");
     */
    public TaskChain findOrCreatePortal(Location location)
    {
        TaskChain chain = MidnightPortal.newChain();
        chain.async(() ->
        {
            try
            {
                Chunk chunk = location.getWorld().getChunkAtAsync(location).get();
                chain.setTaskData("chunk", chunk);
            }
            catch (InterruptedException | ExecutionException e)
            {
                chain.abort();
            }
        }).sync(() ->
        {
            Chunk chunk = (Chunk)chain.getTaskData("chunk");
            chain.setTaskData("snapshot", chunk.getChunkSnapshot());
        }).async(() ->
        {
            ChunkSnapshot snapshot = (ChunkSnapshot)chain.getTaskData("snapshot");
            chain.setTaskData("locations", getBlockLocationsofType(snapshot, Material.END_GATEWAY));
        }).sync(() ->
        {
            Collection<Location> locations = (Collection<Location>)chain.getTaskData("locations");
            Chunk chunk = (Chunk)chain.getTaskData("chunk");
            for (Location portalLocation : locations)
            {
                EndGateway gateway = (EndGateway)chunk.getBlock(portalLocation.getBlockX(), portalLocation.getBlockY(), portalLocation.getBlockZ()).getState(false);
                if (gateway.isExactTeleport() && gateway.getExitLocation().equals(gateway.getLocation()))
                {
                    chain.setTaskData("location", gateway.getLocation());
                    return;
                }
            }

            //None found, build one
            location.setY(location.getWorld().getHighestBlockYAt(location));
            buildPortalFrame(location.getBlock());
            chain.setTaskData("location", location);
        });
        return chain;
    }

    public TaskChain teleportPlayer(Player player, Location location)
    {
        Location otherSide = findOtherSide(location);
        if (otherSide == null)
            return null;
        TaskChain chain = findOrCreatePortal(otherSide);
        chain.sync(() ->
        {
            Location destination = (Location)chain.getTaskData("location");
            destination.add(0.5, -1, 0.5);
            player.teleport(destination);
            plugin.getLogger().info("Teleporting " + player.getName() + " to " + destination);
        });
        return chain;
    }

    public Collection<Location> getBlockLocationsofType(ChunkSnapshot snapshot, Material material)
    {
        Set<Location> blockLocations = new HashSet<>();
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++)
                for (int y = 0; y < 256; y++)
                    if (snapshot.getBlockType(x, y, z) == material)
                        blockLocations.add(new Location(null, x, y, z));
        return blockLocations;
    }

    /**
     * Is a portal frame, given the base (bottom) block
     * @param block
     * @return
     */
    public boolean isPortalFrame(Block block)
    {
        Material frameMaterial = block.getType();
        Block side1;
        Block side2;

        //First check bottom
        if (block.getType() != frameMaterial)
            return false;

        //Then check top
        if (block.getRelative(0, 3, 0).getType() != frameMaterial)
            return false;

        //Check East-West sides
        side1 = block.getRelative(1, 1, 0);
        side2 = block.getRelative(-1, 1, 0);
        if (isPortalFrameSide(frameMaterial, side1) && isPortalFrameSide(frameMaterial, side2))
            return true;

        //Check North-South sides
        side1 = block.getRelative(0, 1, 1);
        side2 = block.getRelative(0, 1, -1);
        if (isPortalFrameSide(frameMaterial, side1) && isPortalFrameSide(frameMaterial, side2))
            return true;
        return false;
    }

    public void buildPortalFrame(Block block)
    {
        plugin.getLogger().info("Building portal at " + block.getLocation());

        //set bottom
        block.setType(portalFrameMaterial);

        //set top
        block.getRelative(0, 3, 0).setType(portalFrameMaterial);

        //set East-West sides
        for (int i = 0; i < 4; i++)
        {
            //East
            block.getRelative(1, i, 0).setType(portalFrameMaterial);
            //West
            block.getRelative(-1, i, 0).setType(portalFrameMaterial);
        }

        //add gateway blocks
        block = block.getRelative(BlockFace.UP);
        block.setType(Material.END_GATEWAY);
        block.getRelative(BlockFace.UP).setType(Material.END_GATEWAY);

        //set exit location so we know this is a MidnightPortal
        EndGateway gateway = (EndGateway)block.getState(false);
        gateway.setExitLocation(gateway.getLocation());
        gateway.setExactTeleport(true);
        gateway = (EndGateway)block.getRelative(BlockFace.UP).getState(false);
        gateway.setExitLocation(gateway.getLocation());
        gateway.setExactTeleport(true);
    }

    private boolean isPortalFrameSide(Material material, Block block)
    {
        if (block.getType() != material)
            return false;
        block = block.getRelative(BlockFace.UP);
        if (block.getType() != material)
            return false;
        return true;
    }

    public boolean breakPortal(Block block)
    {
        if (block.getType() != portalFrameMaterial)
            return false;

        for (BlockFace face : new BlockFace[]{BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.DOWN, BlockFace.UP})
        {
            Block relative = block.getRelative(face);
            if (relative.getType() == Material.END_GATEWAY)
            {
                for (BlockFace otherFace : new BlockFace[]{BlockFace.UP, BlockFace.DOWN})
                {
                    if (relative.getRelative(otherFace).getType() == Material.END_GATEWAY)
                        relative.getRelative(otherFace).setType(Material.AIR);
                }
                relative.setType(Material.AIR);
                return true;
            }
        }
        return false;
    }
}
