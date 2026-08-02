package ru.mounts.plugin;

import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MountsPlugin extends JavaPlugin {

    private static final long DEFAULT_COOLDOWN_SECONDS = 300L;

    private NamespacedKey ownerKey;
    private final CooldownManager cooldownManager = new CooldownManager();
    private final Map<MountType, Long> cooldownSeconds = new EnumMap<>(MountType.class);

    // playerId -> (тип животного -> UUID сущности, которую он призвал последней)
    // Используется, чтобы при повторном вызове команды удалить старое животное.
    // Хранится только в памяти (см. README про ограничения после рестарта).
    private final Map<UUID, Map<MountType, UUID>> activeMounts = new HashMap<>();

    @Override
    public void onEnable() {
        this.ownerKey = new NamespacedKey(this, "mount_owner");

        saveDefaultConfig();
        loadCooldownSettings();

        for (MountType type : MountType.values()) {
            PluginCommand summonCommand = getCommand(type.getCommandName());
            if (summonCommand != null) {
                summonCommand.setExecutor(new MountCommand(this, type));
            } else {
                getLogger().warning("Команда /" + type.getCommandName() + " не найдена в plugin.yml!");
            }

            PluginCommand cooldownCommand = getCommand(type.getCooldownCommandName());
            if (cooldownCommand != null) {
                cooldownCommand.setExecutor(new CooldownConfigCommand(this, type));
            } else {
                getLogger().warning("Команда /" + type.getCooldownCommandName() + " не найдена в plugin.yml!");
            }
        }

        getServer().getPluginManager().registerEvents(new OwnershipListener(this), this);
        getServer().getPluginManager().registerEvents(new SecretGamemodeListener(), this);

        getLogger().info("MountsPlugin включён.");
    }

    private void loadCooldownSettings() {
        for (MountType type : MountType.values()) {
            long seconds = getConfig().getLong("cooldown-seconds." + type.getCommandName(), DEFAULT_COOLDOWN_SECONDS);
            cooldownSeconds.put(type, Math.max(0L, seconds));
        }
    }

    /** Текущая длительность кулдауна для команды призыва этого типа, в секундах. */
    public long getCooldownSeconds(MountType type) {
        return cooldownSeconds.getOrDefault(type, DEFAULT_COOLDOWN_SECONDS);
    }

    /** Меняет длительность кулдауна для команды призыва этого типа и сохраняет в config.yml. */
    public void setCooldownSeconds(MountType type, long seconds) {
        long safeSeconds = Math.max(0L, seconds);
        cooldownSeconds.put(type, safeSeconds);
        getConfig().set("cooldown-seconds." + type.getCommandName(), safeSeconds);
        saveConfig();
    }

    @Override
    public void onDisable() {
        getLogger().info("MountsPlugin выключен.");
    }

    public NamespacedKey getOwnerKey() {
        return ownerKey;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public Map<UUID, Map<MountType, UUID>> getActiveMounts() {
        return activeMounts;
    }
}
