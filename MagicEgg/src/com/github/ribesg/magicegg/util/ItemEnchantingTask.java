package com.github.ribesg.magicegg.util;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import com.github.ribesg.magicegg.MagicEgg;
import com.github.ribesg.magicegg.util.EnchantmentRandomizer.Result;


// This task is called SYNC
// This task take items from the buffer and apply our effect on it
public class ItemEnchantingTask implements Runnable {
    private final MagicEgg plugin;

    public ItemEnchantingTask(final MagicEgg instance) {
        this.plugin = instance;
    }

    @Override
    public void run() {
        try {
            while (this.plugin.buffer.size() > 0) {
                final Item item = this.plugin.buffer.pollFirstEntry().getValue();
                if (item.isValid()) {
                    final Location cauldronLoc = item.getLocation().getBlock().getLocation().clone();
                    if (this.plugin.magicBenches.contains(cauldronLoc)) { // So now we act !
                        final ItemStack itemStack = item.getItemStack();
                        if (itemStack.getEnchantments().size() != 0) {
                            final Result res = this.plugin.randomizer.randomize(itemStack);

                            // id => Color of effect
                            int id = 49;
                            switch (res) {
                                case CLEAN:
                                    id = 20;
                                    break;
                                case LOSS:
                                    id = 10;
                                    break;
                                case NONE:
                                    id = 49;
                                    break;
                                case BOOST:
                                    id = 41;
                                    break;
                                case OVERBOOST:
                                    id = 57;
                                    break;
                            }

                            this.plugin.magicBenches.remove(cauldronLoc);
                            cauldronLoc.getWorld().playEffect(cauldronLoc, Effect.EXTINGUISH, 0);
                            cauldronLoc.getWorld().playEffect(cauldronLoc, Effect.MOBSPAWNER_FLAMES, 0);
                            cauldronLoc.subtract(0, 1, 0); // Is now Egg location
                            cauldronLoc.getWorld().playEffect(cauldronLoc, Effect.ENDER_SIGNAL, 0);
                            cauldronLoc.getWorld().playEffect(cauldronLoc, Effect.STEP_SOUND, id);
                            cauldronLoc.getWorld().playEffect(cauldronLoc, Effect.STEP_SOUND, id);
                            cauldronLoc.getWorld().playEffect(cauldronLoc, Effect.STEP_SOUND, id);
                            cauldronLoc.getWorld().playEffect(cauldronLoc, Effect.STEP_SOUND, id);
                            cauldronLoc.getWorld().playEffect(cauldronLoc, Effect.STEP_SOUND, id);
                            if (cauldronLoc.getBlock().getType() != Material.DRAGON_EGG) {
                                // Prevent possible cheat : if the player remove the egg before this happen, we delete his item (MUAHAHA)
                                item.remove();
                            }
                            cauldronLoc.getBlock().setType(Material.AIR); // Delete the Egg
                        }
                    }
                }
                if (item.isValid()) {
                    item.setPickupDelay(0);
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
