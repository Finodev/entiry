package ru.mounts.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Хранит время окончания кулдауна для каждого игрока отдельно по каждой
 * команде (лошадь/осёл/верблюд имеют независимые кулдауны).
 *
 * Хранится только в памяти: после перезапуска сервера все кулдауны сбрасываются.
 */
public class CooldownManager {

    private final Map<UUID, Map<MountType, Long>> expiryTimestamps = new HashMap<>();

    /**
     * @return сколько миллисекунд осталось до конца кулдауна.
     * 0, если кулдауна нет или он уже прошёл.
     */
    public long getRemainingMillis(UUID playerId, MountType type) {
        Map<MountType, Long> playerCooldowns = expiryTimestamps.get(playerId);
        if (playerCooldowns == null) {
            return 0L;
        }
        Long expiresAt = playerCooldowns.get(type);
        if (expiresAt == null) {
            return 0L;
        }
        return Math.max(0L, expiresAt - System.currentTimeMillis());
    }

    public void setCooldown(UUID playerId, MountType type, long durationMillis) {
        expiryTimestamps
                .computeIfAbsent(playerId, key -> new HashMap<>())
                .put(type, System.currentTimeMillis() + durationMillis);
    }
}
