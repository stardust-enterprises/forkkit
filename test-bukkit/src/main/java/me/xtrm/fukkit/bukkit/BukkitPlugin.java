package me.xtrm.fukkit.bukkit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author xtrm
 */
public class BukkitPlugin extends JavaPlugin implements Listener {
    private static final Boolean FUKKIT_DEBUG =
            Boolean.parseBoolean(System.getProperty("fukkit.debug", "false"));
    private static final Boolean ENABLE_TEST =
            Boolean.parseBoolean(System.getProperty("fukkit.test", "false"));
    public static final Map<UUID, Integer> map = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info("Hello fukkit bukkit :3");

        if (FUKKIT_DEBUG) {
            ClassLoader loader = getClass().getClassLoader();
            int i = 0;
            do {
                String padding = new String(new char[i]).replace('\0', ' ');
                String name;
                if (loader == null) {
                    name = "(builtin)";
                } else {
                    name = loader.getClass().getName() + " (" + Integer.toHexString(loader.getClass().hashCode()) + ")";
                    loader = loader.getParent();
                }
                getLogger().info(padding + "- " + name);
                i++;
            } while (loader != null);
        }

        try {
            Class.forName("unknown/clazz/Name");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        if (ENABLE_TEST) {
            getServer().getPluginManager().registerEvents(this, this);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (ENABLE_TEST) {
            Player player = event.getPlayer();
            int rand = (int) (Math.random() * 10000);
            map.put(player.getUniqueId(), rand);
            player.sendMessage("Your random number is " + rand);
        }
    }
}
