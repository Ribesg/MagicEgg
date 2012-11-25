package com.github.ribesg.magicegg.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;

import com.github.ribesg.magicegg.MagicEgg;


// This task is called ASYNC
// This task look at every dropped item and add it to a buffer if it's in a Cauldron
public class ItemCheckerTask implements Runnable {

    private final MagicEgg         plugin;
    private final Collection<Item> items = new ArrayList<Item>();

    public ItemCheckerTask(final MagicEgg instance) {
        this.plugin = instance;
    }

    @Override
    public void run() {
        try {
            for (final World w : this.plugin.getServer().getWorlds()) {
                this.items.clear();
                synchronized (w) {
                    while (true) {
                        try {
                            this.items.addAll(w.getEntitiesByClass(Item.class));
                            break;
                        } catch (final ConcurrentModificationException e) {
                            this.items.clear();
                            Thread.sleep(1);
                        }
                    }
                }
                for (final Item i : this.items) {
                    if (i.isValid()) {
                        final Block b = i.getWorld().getBlockAt(i.getLocation());
                        if (b.getTypeId() == 118) { // Cauldron
                            final Location blockLocation = i.getWorld().getBlockAt(i.getLocation()).getLocation();
                            if (!this.plugin.buffer.containsValue(i) && !this.plugin.buffer.containsKey(blockLocation.toString())) {
                                this.plugin.buffer.put(blockLocation.toString(), i);
                                i.setPickupDelay(100);
                            }
                        }
                        // ------------------------------------------------------------- //
                        // "FIX" FOR 1.4 BUG OF CAULDRON NO LONGER ABLE TO CONTAIN ITEMS //
                        // ------------------------------------------------------------- //
                        else if (b.getTypeId() == 44 && b.getData() == (byte) 4) { // Brick slab
                            final Location blockLocation = i.getWorld().getBlockAt(i.getLocation()).getLocation();
                            if (!this.plugin.buffer.containsValue(i) && !this.plugin.buffer.containsKey(blockLocation.toString())) {
                                this.plugin.buffer.put(blockLocation.toString(), i);
                                i.setPickupDelay(100);
                            }
                        }
                        // ---------- //
                        // END OF FIX //
                        // ---------- //
                    }
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
