package com.github.ribesg.magicegg.util;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
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
                            final Result res = this.plugin.randomizer.randomize(itemStack); // The itemStack/item is now modified
                            byte data = 0; // Wool color
                            float ratio = 1f; // Volume & pitch of thunder sound
                            switch (res) {
                                case CLEAN:
                                    data = 15; // Black
                                    ratio = 0.2f;
                                    break;
                                case LOSS:
                                    data = 14; // Red
                                    ratio = 0.4f;
                                    break;
                                case NONE:
                                    data = 0; // White
                                    ratio = 0.6f;
                                    break;
                                case BOOST:
                                    data = 4; // Yellow
                                    ratio = 0.8f;
                                    break;
                                case OVERBOOST:
                                    data = 5; // Green
                                    ratio = 1f;
                                    break;
                            }
                            this.plugin.magicBenches.remove(cauldronLoc);
                            cauldronLoc.getWorld().playSound(cauldronLoc, Sound.AMBIENCE_THUNDER, ratio, ratio * 2f);
                            cauldronLoc.subtract(0, 1, 0); // Is now Egg location
                            cauldronLoc.getWorld().playEffect(item.getLocation(), Effect.ENDER_SIGNAL, 0);
                            if (cauldronLoc.getBlock().getType() != Material.DRAGON_EGG) {
                                // Prevent possible cheat : if the player remove the egg before this happen, we remove every enchants
                                // As the interact event is cancelled in ME_Listener, this should never happen.
                                for (final Enchantment e : itemStack.getEnchantments().keySet()) {
                                    itemStack.removeEnchantment(e);
                                }
                                cauldronLoc.getWorld().playSound(cauldronLoc, Sound.ENDERDRAGON_DEATH, 1.0f, 1.5f);
                            }
                            cauldronLoc.getBlock().setType(Material.AIR); // Delete the Egg
                            this.plugin.fakeWool(cauldronLoc, data, this.plugin.getNearbyPlayers(item, 10, 5, 10)); // Wool color effect
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
