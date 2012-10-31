package com.github.ribesg.magicegg;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;


public class ME_Listener implements Listener {

    private final MagicEgg plugin;

    public ME_Listener(final MagicEgg instance) {
        this.plugin = instance;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        final Block b = event.getBlock();
        final Material type = b.getType();
        if (type == Material.CAULDRON && b.getRelative(BlockFace.DOWN).getType() == Material.DRAGON_EGG && this.plugin.magicBenches.contains(b.getLocation())) {
            this.plugin.magicBenches.remove(b.getLocation());
        } else if (type == Material.DRAGON_EGG && b.getRelative(BlockFace.UP).getType() == Material.CAULDRON && this.plugin.magicBenches.contains(b.getRelative(BlockFace.UP).getLocation())) {
            this.plugin.magicBenches.remove(b.getRelative(BlockFace.UP).getLocation());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Block b = event.getBlock();
        final Material type = b.getType();
        if (type == Material.CAULDRON && b.getRelative(BlockFace.DOWN).getType() == Material.DRAGON_EGG && !this.plugin.magicBenches.contains(b.getLocation())) {
            this.plugin.magicBenches.add(b.getLocation());
        } else if (type == Material.DRAGON_EGG && b.getRelative(BlockFace.UP).getType() == Material.CAULDRON && !this.plugin.magicBenches.contains(b.getRelative(BlockFace.UP).getLocation())) {
            this.plugin.magicBenches.add(b.getRelative(BlockFace.UP).getLocation());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final Action a = event.getAction();
        if (a.equals(Action.RIGHT_CLICK_BLOCK) || a.equals(Action.LEFT_CLICK_BLOCK)) {
            final Material type = event.getClickedBlock().getType();
            if (type == Material.DRAGON_EGG) {
                event.setCancelled(event.getClickedBlock().getRelative(BlockFace.UP).getType() == Material.CAULDRON);
            }
        }
    }
}
