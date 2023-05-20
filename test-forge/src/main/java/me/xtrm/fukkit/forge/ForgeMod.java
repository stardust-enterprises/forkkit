package me.xtrm.fukkit.forge;

import me.xtrm.fukkit.bukkit.BukkitPlugin;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author xtrm
 */
@Mod("fukkittest")
public class ForgeMod {
    private static final Boolean FUKKIT_DEBUG =
            Boolean.parseBoolean(System.getProperty("fukkit.debug", "false"));
    private static final Boolean ENABLE_TEST =
            Boolean.parseBoolean(System.getProperty("fukkit.test", "false"));
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeMod.class);

    public ForgeMod() {
        LOGGER.info("Hello fukkit forge :3");

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
                System.out.println(padding + "- " + name);
                i++;
            } while (loader != null);
        }

        if (ENABLE_TEST) {
            MinecraftForge.EVENT_BUS.register(this);
        }
    }

    private final Set<UUID> displayed = new HashSet<>();

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent tickEvent) {
        if (ENABLE_TEST) {
            if (tickEvent.side.isClient()) return;
            for (Player it : tickEvent.world.players()) {
                UUID uuid = it.getUUID();
                if (displayed.contains(uuid)) return;
                if (!BukkitPlugin.map.containsKey(uuid)) return;
                displayed.add(uuid);
                it.sendMessage(new TextComponent("Your random number (from bukkit) is " + BukkitPlugin.map.get(uuid)), UUID.randomUUID());
            }
        }
    }
}
