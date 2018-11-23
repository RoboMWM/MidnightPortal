package com.robomwm.midnightportal.listener;

import com.robomwm.midnightportal.PortalUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.EndGateway;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created on 10/24/2018.
 *
 * Create and destroy portals
 *
 * @author RoboMWM
 */
public class PortalLighter implements Listener
{
    private Material portalFrameMaterial;
    private PortalUtils portalUtils;

    public PortalLighter(Plugin plugin, PortalUtils portalUtils, Material portalFrameMaterial)
    {
        this.portalUtils = portalUtils;
        this.portalFrameMaterial = portalFrameMaterial;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    private void onPlaceFire(PlayerInteractEvent event)
    {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (event.getBlockFace() != BlockFace.UP)
            return;
        if (event.getItem().getType() != Material.FLINT_AND_STEEL)
            return;

        Block block = event.getClickedBlock();
        if (!portalUtils.isEnabledWorld(block.getWorld()))
            return;
        if (block.getType() != portalFrameMaterial)
            return;

        if (!portalUtils.isPortalFrame(block))
            return;

        event.setCancelled(true);

        //set gateway block
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

    @EventHandler(ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent event)
    {
        portalUtils.breakPortal(event.getBlock());
    }
}
