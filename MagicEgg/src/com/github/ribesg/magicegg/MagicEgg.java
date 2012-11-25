package com.github.ribesg.magicegg;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.github.ribesg.magicegg.util.EnchantmentRandomizer;
import com.github.ribesg.magicegg.util.ItemCheckerTask;
import com.github.ribesg.magicegg.util.ItemEnchantingTask;


public class MagicEgg extends JavaPlugin {

    private String                              directory;
    private File                                f_config;
    private File                                f_magicBenchesDB;

    private final long                          period = 60;

    // Data
    public ConcurrentNavigableMap<String, Item> buffer;          // Items spotted in cauldrons
    public CopyOnWriteArrayList<Location>       magicBenches;    // Location of cauldrons

    // Tools
    public EnchantmentRandomizer                randomizer;
    private ME_Listener                         listener;

    @Override
    public void onEnable() {
        this.directory = "plugins" + File.separator + "MagicEgg";
        this.f_config = new File(this.directory + File.separator + "config.yml");
        this.f_magicBenchesDB = new File(this.directory + File.separator + "magicBenchesDB.yml");
        this.buffer = new ConcurrentSkipListMap<String, Item>();
        this.magicBenches = new CopyOnWriteArrayList<Location>();
        this.listener = new ME_Listener(this);
        if (!new File(this.directory).exists()) {
            new File(this.directory).mkdir();
        }
        if (!this.f_config.exists()) {
            this.newConfig();
        }
        if (!this.f_magicBenchesDB.exists()) {
            try {
                this.f_magicBenchesDB.createNewFile();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        this.load();

        this.getServer().getPluginManager().registerEvents(this.listener, this);

        // This task is async because it have to handle every item Entity on the server, this could cause lag
        this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new ItemCheckerTask(this), 100, this.period);

        // Those tasks are sync because they need access to not-thread-safe methods/Objects
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new ItemEnchantingTask(this), 100 + this.period / 2, this.period);
        for (int i = 1; i < 10; i += 3) {
            this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {

                @Override
                public void run() {
                    for (final Location location : MagicEgg.this.magicBenches) {
                        final Location loc = location.clone();
                        loc.getWorld().playEffect(loc, Effect.SMOKE, 4);
                        loc.subtract(0, 1, 0);
                        loc.getWorld().playEffect(loc, Effect.SMOKE, 1);
                        loc.getWorld().playEffect(loc, Effect.SMOKE, 3);
                        loc.getWorld().playEffect(loc, Effect.SMOKE, 5);
                        loc.getWorld().playEffect(loc, Effect.SMOKE, 7);
                    }
                }
            }, i, 40);
        }
    }

    @Override
    public void onDisable() {
        this.getServer().getScheduler().cancelTasks(this);
        this.save();
    }

    // ---------------------- //
    // Config related methods //
    // ---------------------- //

    private void newConfig() {
        try {
            this.f_config.createNewFile();
            final FileWriter f = new FileWriter(this.f_config);
            f.write("#Sum should be equals to 1 !\n");
            f.write("cleanEnchProbability: 0.05\n");
            f.write("enchLossProbability: 0.2\n");
            f.write("noChangeProbability: 0.25\n");
            f.write("enchBoostProbability: 0.40\n");
            f.write("enchOverBoostProbability: 0.10\n");
            f.flush();
            f.close();
        } catch (final Exception e) {
            e.printStackTrace();
            this.getPluginLoader().disablePlugin(this);
        }
    }

    private void load() {
        try {
            final YamlConfiguration config = new YamlConfiguration();
            config.load(this.f_config);
            final float cleanEnch = (float) config.getDouble("cleanEnchProbability", 0.05);
            final float enchLoss = (float) config.getDouble("enchLossProbability", 0.2);
            final float noChange = (float) config.getDouble("noChangeProbability", 0.25);
            final float enchBoost = (float) config.getDouble("enchBoostProbability", 0.45);
            final float enchOverBoost = (float) config.getDouble("enchOverBoostProbability", 0.05);
            if (this.randomizer == null) {
                this.randomizer = new EnchantmentRandomizer(cleanEnch, enchLoss, noChange, enchBoost, enchOverBoost);
            } else {
                synchronized (this.randomizer) {
                    this.randomizer = new EnchantmentRandomizer(cleanEnch, enchLoss, noChange, enchBoost, enchOverBoost);
                }
            }
            final YamlConfiguration cfg = new YamlConfiguration();
            cfg.load(this.f_magicBenchesDB);
            try {
                for (final String s : cfg.getKeys(false)) {
                    final ConfigurationSection section = cfg.getConfigurationSection(s);
                    final String worldName = section.getString("worldName");
                    final Vector vector = section.getVector("vector");
                    final World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        this.magicBenches.add(vector.toLocation(world));
                    }
                }
                final Iterator<Location> it = this.magicBenches.iterator();
                while (it.hasNext()) {
                    final Location loc = it.next();
                    if (loc.getWorld() == null || loc.getBlock().getType() != Material.CAULDRON || loc.getBlock().getRelative(BlockFace.DOWN).getType() != Material.DRAGON_EGG) {
                        it.remove();
                    }
                }
            } catch (final Exception e) {
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            if (!this.f_magicBenchesDB.exists()) {
                this.f_magicBenchesDB.createNewFile();
            }
            final YamlConfiguration cfg = new YamlConfiguration();
            int i = 0;
            for (final Location loc : this.magicBenches) {
                final ConfigurationSection s = cfg.createSection("bench" + i++);
                s.set("worldname", loc.getWorld().getName());
                s.set("vector", loc.toVector());
            }
            cfg.save(this.f_magicBenchesDB);
        } catch (final Exception e) {
            e.printStackTrace();
            this.getPluginLoader().disablePlugin(this);
        }
    }

    // --------------------- //
    // Tools & Utils methods //
    // --------------------- //

    public void fakeWool(final Location blockLocation, final byte woolData, final List<Player> nearbyPlayers) {
        new BukkitRunnable() {
            Random rand  = new Random();
            int    loops = 30;

            @Override
            public void run() {
                if (this.loops > 1) {
                    byte b = woolData;
                    while (b == woolData) {
                        b = (byte) this.rand.nextInt(16);
                    }
                    for (final Player p : nearbyPlayers) {
                        p.sendBlockChange(blockLocation, 35, b);
                    }
                    this.loops--;
                } else {
                    for (final Player p : nearbyPlayers) {
                        p.sendBlockChange(blockLocation, 35, woolData);
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(this, 1,1);
        new BukkitRunnable() {

            @Override
            public void run() {
                for (final Player p : nearbyPlayers) {
                    p.sendBlockChange(blockLocation, 0, (byte) 0);
                }
            }
        }.runTaskLater(this, 80);
    }

    public List<Player> getNearbyPlayers(final Entity e, final double x, final double y, final double z) {
        final List<Player> result = new ArrayList<Player>();
        for (final Entity nearbyEntity : e.getNearbyEntities(x, y, z)) {
            if (nearbyEntity.getType() == EntityType.PLAYER) {
                result.add((Player) nearbyEntity);
            }
        }
        return result;
    }
}
