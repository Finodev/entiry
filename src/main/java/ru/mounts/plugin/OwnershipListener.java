package ru.mounts.plugin;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerLeashEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.UUID;

/**
 * ВАЖНО: в ванильном Minecraft приручённую лошадь/осла может оседлать ЛЮБОЙ
 * игрок (приручение не ограничивает посадку владельцем), а верблюда вообще
 * не нужно приручать — сесть на осёдланного верблюда может кто угодно.
 * Поэтому ограничение "может сесть только призвавший" реализовано здесь
 * полностью через свою систему (PersistentDataContainer), а не через
 * стандартное приручение.
 */
public class OwnershipListener implements Listener {

    private final MountsPlugin plugin;

    public OwnershipListener(MountsPlugin plugin) {
        this.plugin = plugin;
    }

    private UUID getOwnerId(Entity entity) {
        String raw = entity.getPersistentDataContainer()
                .get(plugin.getOwnerKey(), PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    // Главная защита: запрещаем садиться верхом всем, кроме владельца.
    // Это относится и ко второму месту у верблюда — его тоже нельзя занять чужому игроку.
    @EventHandler(ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {
        UUID ownerId = getOwnerId(event.getMount());
        if (ownerId == null) {
            return; // это животное не призвано плагином
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!player.getUniqueId().equals(ownerId)) {
            event.setCancelled(true);
            player.sendMessage("§cЭто чужое призванное животное — сесть может только тот, кто его призвал.");
        }
    }

    // Дополнительно блокируем любое взаимодействие (открытие инвентаря животного,
    // седловка, кормление и т.д.) для всех, кроме владельца.
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        UUID ownerId = getOwnerId(event.getRightClicked());
        if (ownerId == null) {
            return;
        }
        if (!event.getPlayer().getUniqueId().equals(ownerId)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cЭто чужое призванное животное — взаимодействовать может только хозяин.");
        }
    }

    // Не даём чужим игрокам взять животное на поводок.
    @EventHandler(ignoreCancelled = true)
    public void onLeash(PlayerLeashEntityEvent event) {
        UUID ownerId = getOwnerId(event.getEntity());
        if (ownerId == null) {
            return;
        }
        if (!event.getPlayer().getUniqueId().equals(ownerId)) {
            event.setCancelled(true);
        }
    }
}
