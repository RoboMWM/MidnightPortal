package com.robomwm.midnightportal.listener;

import co.aikar.taskchain.TaskChain;
import com.destroystokyo.paper.event.player.PlayerTeleportEndGatewayEvent;
import com.robomwm.midnightportal.PortalUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
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

    public Teleporter(Plugin plugin, PortalUtils portalUtils)
    {
        this.portalUtils = portalUtils;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    private void onEnterEndPortalBlock(PlayerTeleportEndGatewayEvent event)
    {
        //MidnightPortals contain End blocks that teleport to themself
        //(unfortunately not possible to set exitLocation to another world)
        if (!event.getGateway().isExactTeleport() || !event.getGateway().getExitLocation().equals(event.getGateway().getLocation()))
            return;

        //May need to event#setCanceled?
        //Prevent player from being teleported twice if they move while the async chunk load stuff is happening
        if (pendingTeleportees.contains(event.getPlayer()))
            return;

        //TODO: check if teleporting to endgateway block causes infinite teleportation.
        //if so, check if player#setPortalCooldown works for this (probably not)

        pendingTeleportees.add(event.getPlayer());

        TaskChain chain = portalUtils.teleportPlayer(event.getPlayer(), event.getGateway().getLocation());
        chain.execute(() ->
                pendingTeleportees.remove(event.getPlayer()));

    }
}
