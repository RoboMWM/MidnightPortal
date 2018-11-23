package com.robomwm.midnightportal.listener;

import co.aikar.taskchain.TaskChain;
import com.destroystokyo.paper.event.player.PlayerTeleportEndGatewayEvent;
import com.robomwm.midnightportal.PortalUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created on 10/16/2018.
 *
 * @author RoboMWM
 */
public class Teleporter implements Listener
{
    private PortalUtils portalUtils;
    private Set<Player> pendingTeleportees = ConcurrentHashMap.newKeySet();
    private Set<Player> postTeleportees = new HashSet<>();
    private Plugin plugin;

    public Teleporter(Plugin plugin, PortalUtils portalUtils)
    {
        this.plugin = plugin;
        this.portalUtils = portalUtils;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    private void onEnterEndPortalBlock(PlayerTeleportEndGatewayEvent event)
    {
        //Prevent player from being teleported twice if they move while the async chunk load stuff is happening
        //Also prevents player from endlessly teleporting back and forth between two portals.
        if (pendingTeleportees.contains(event.getPlayer()) || postTeleportees.contains(event.getPlayer()))
        {
            event.setCancelled(true);
            return;
        }

        //MidnightPortals contain End blocks that teleports stuff to itself
        //(unfortunately not possible to set exitLocation to another world)
        if (!event.getGateway().isExactTeleport() || !event.getGateway().getExitLocation().equals(event.getGateway().getLocation()))
            return;

        pendingTeleportees.add(event.getPlayer());

        TaskChain chain = portalUtils.teleportPlayer(event.getPlayer(), event.getGateway().getLocation());
        chain.execute(() ->
                {
                    pendingTeleportees.remove(event.getPlayer());
                    postTeleportees.add(event.getPlayer());
                });
    }

    @EventHandler(ignoreCancelled = true)
    private void removeCooldown(PlayerMoveEvent event)
    {
        if (!postTeleportees.contains(event.getPlayer()))
            return;
        Player player = event.getPlayer();
        if (player.getLocation().getBlock().getType() != Material.END_GATEWAY)
            postTeleportees.remove(event.getPlayer());
    }
}
