package ru.smole;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public class GunGaleTest3 extends JavaPlugin implements Listener {

    private static int COOLDOWN_SECONDS;

    private static Cache<UUID, Long> CLICK_COOLDOWN;
    private static final Cache<UUID, Location> CONFIRM_TIME = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMillis(500))
            .build();

    @Override
    public void onEnable() {
        COOLDOWN_SECONDS = getConfig().getInt("cooldown");

        CLICK_COOLDOWN = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(COOLDOWN_SECONDS))
                .build();

        getServer().getPluginManager().registerEvents(this, this);

        Objects.requireNonNull(getCommand("check")).setExecutor(this);
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        var player = event.getPlayer();

        if (!player.isSneaking() || event.getAction() != Action.RIGHT_CLICK_BLOCK || !event.hasItem()) return;

        var block = Objects.requireNonNull(event.getClickedBlock());
        var item = Objects.requireNonNull(event.getItem());

        var type = item.getType();
        var typeName = type.name();

        if (!typeName.contains("DYE")) return;

        var color = typeName.split("_")[0];
        var blockTypeName = block.getType().name();

        var coloredBlockTypeName = color + blockTypeName.substring(blockTypeName.split("_")[0].length());
        var coloredMaterial = Material.getMaterial(coloredBlockTypeName);

        UUID uuid;
        if (coloredMaterial == null) return;

        Long cooldown;
        if ((cooldown = CLICK_COOLDOWN.getIfPresent(uuid = player.getUniqueId())) != null) {
            var stringSeconds = getSecondsByMillis(cooldown - System.currentTimeMillis());

            player.sendMessage("Please, wait %s seconds!".formatted(stringSeconds));
            return;
        }

        var location = block.getLocation();

        if (Objects.equals(CONFIRM_TIME.getIfPresent(uuid), location)) {
            block.setType(coloredMaterial);
            CONFIRM_TIME.invalidate(uuid);
            CLICK_COOLDOWN.put(uuid, System.currentTimeMillis() + (COOLDOWN_SECONDS * 1000L));
            return;
        }

        CONFIRM_TIME.put(uuid, location);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var uuid = event.getPlayer().getUniqueId();

        CLICK_COOLDOWN.invalidate(uuid);
        CONFIRM_TIME.invalidate(uuid);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ggtest.stats")) return false;

        if (args.length == 1) {
            var targetName = args[0];
            var target = Bukkit.getPlayer(targetName);

            if (target == null) return false;

            var targetCooldown = CLICK_COOLDOWN.getIfPresent(target.getUniqueId());

            if (targetCooldown == null) {
                sender.sendMessage("Target player isn't have a cooldown.");
                return false;
            }

            var stringSeconds = getSecondsByMillis(targetCooldown - System.currentTimeMillis());

            sender.sendMessage("%s has cooldown on %s seconds.".formatted(target.getName(), stringSeconds));
            return true;
        }

        return false;
    }

    private static String getSecondsByMillis(long millis) {
        return String.valueOf(Duration.ofMillis(millis).toSeconds());
    }
}
