package ru.mounts.plugin;

import org.bukkit.entity.EntityType;

/**
 * Три типа животных, которые умеет призывать плагин.
 * Все три реализуют org.bukkit.entity.AbstractHorse, поэтому
 * обрабатываются одним и тем же кодом в MountCommand.
 */
public enum MountType {

    HORSE("horse", EntityType.HORSE, "Лошадь"),
    DONKEY("donkey", EntityType.DONKEY, "Осёл"),
    CAMEL("camel", EntityType.CAMEL, "Верблюд");

    private final String commandName;
    private final EntityType entityType;
    private final String displayName;

    MountType(String commandName, EntityType entityType, String displayName) {
        this.commandName = commandName;
        this.entityType = entityType;
        this.displayName = displayName;
    }

    public String getCommandName() {
        return commandName;
    }

    /** Имя команды для изменения кулдауна, например "cdhorse" для лошади. */
    public String getCooldownCommandName() {
        return "cd" + commandName;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public String getDisplayName() {
        return displayName;
    }
}
