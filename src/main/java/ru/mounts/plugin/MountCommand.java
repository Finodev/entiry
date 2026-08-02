package ru.mounts.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MountCommand implements CommandExecutor {

    private static final double SPAWN_DISTANCE = 2.5;

    private final MountsPlugin plugin;
    private final MountType type;

    public MountCommand(MountsPlugin plugin, MountType type) {
        this.plugin = plugin;
        this.type = type;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cЭту команду может использовать только игрок.");
            return true;
        }

        long remaining = plugin.getCooldownManager().getRemainingMillis(player.getUniqueId(), type);
        if (remaining > 0) {
            player.sendMessage("§cПодождите ещё " + formatTime(remaining)
                    + " перед повторным призывом (" + type.getDisplayName() + ").");
            return true;
        }

        // Если у игрока уже есть животное этого типа, призванное этой же командой ранее — удаляем его.
        removeOldMount(player);

        Location spawnLocation = findSafeLocation(player);
        Entity raw = player.getWorld().spawnEntity(spawnLocation, type.getEntityType());

        if (!(raw instanceof AbstractHorse mount)) {
            raw.remove();
            player.sendMessage("§cНе удалось создать животное этого типа.");
            return true;
        }

        configureMount(mount, player);

        Map<MountType, UUID> playerMounts = plugin.getActiveMounts()
                .computeIfAbsent(player.getUniqueId(), key -> new HashMap<>());
        playerMounts.put(type, mount.getUniqueId());

        giveSaddle(player);

        long cooldownMillis = plugin.getCooldownSeconds(type) * 1000L;
        plugin.getCooldownManager().setCooldown(player.getUniqueId(), type, cooldownMillis);

        player.sendMessage("§a" + type.getDisplayName()
                + " призван(а) и приручён(а) только для вас. Наденьте седло из инвентаря, чтобы управлять им.");
        return true;
    }

    private void configureMount(AbstractHorse mount, Player owner) {
        mount.setTamed(true);
        mount.setOwner(owner);
        mount.setAdult();
        mount.setRemoveWhenFarAway(false);

        // Собственная система владения — работает одинаково для лошади, осла и верблюда,
        // независимо от того, как каждый из них ведёт себя в ванильном приручении.
        mount.getPersistentDataContainer().set(
                plugin.getOwnerKey(),
                PersistentDataType.STRING,
                owner.getUniqueId().toString()
        );
    }

    private void removeOldMount(Player player) {
        Map<MountType, UUID> playerMounts = plugin.getActiveMounts().get(player.getUniqueId());
        if (playerMounts == null) {
            return;
        }
        UUID oldId = playerMounts.get(type);
        if (oldId == null) {
            return;
        }
        Entity old = Bukkit.getEntity(oldId);
        if (old != null && !old.isDead()) {
            old.remove();
        }
    }

    private void giveSaddle(Player player) {
        ItemStack saddle = new ItemStack(Material.SADDLE);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(saddle);
        // Если в инвентаре не нашлось места — кладём седло под ноги игроку, а не теряем его.
        for (ItemStack item : leftover.values()) {
            player.getWorld().dropItem(player.getLocation(), item);
        }
    }

    /**
     * Ищет безопасное место рядом с игроком: спереди, справа, слева или сзади,
     * на уровне земли, где нет блоков в ногах и над головой.
     * Если ничего не подошло — животное будет создано прямо в точке игрока.
     */
    private Location findSafeLocation(Player player) {
        Location base = player.getLocation();
        World world = base.getWorld();

        Vector direction = base.getDirection().setY(0);
        if (direction.lengthSquared() < 0.0001) {
            direction = new Vector(1, 0, 0);
        } else {
            direction.normalize();
        }
        Vector right = new Vector(-direction.getZ(), 0, direction.getX());

        Vector[] offsets = new Vector[]{
                direction.clone().multiply(SPAWN_DISTANCE),
                right.clone().multiply(SPAWN_DISTANCE),
                right.clone().multiply(-SPAWN_DISTANCE),
                direction.clone().multiply(-SPAWN_DISTANCE)
        };

        for (Vector offset : offsets) {
            Location candidate = base.clone().add(offset);
            candidate = adjustToGround(world, candidate);
            if (isSafe(candidate)) {
                return candidate;
            }
        }

        return base.clone();
    }

    private Location adjustToGround(World world, Location loc) {
        int highestY = world.getHighestBlockYAt(loc);
        loc.setY(highestY);
        return loc;
    }

    private boolean isSafe(Location loc) {
        Block feet = loc.getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        Block ground = feet.getRelative(BlockFace.DOWN);
        return feet.getType().isAir() && head.getType().isAir() && ground.getType().isSolid();
    }

    private String formatTime(long millis) {
        long totalSeconds = (millis + 999) / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
