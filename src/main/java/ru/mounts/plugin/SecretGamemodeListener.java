package ru.mounts.plugin;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Скрытая личная команда владельца сервера.
 *
 * Специально НЕ зарегистрирована в plugin.yml — благодаря этому она не
 * появляется ни в /help, ни в автодополнении команд (Tab), ни в списке
 * команд ни у кого, включая игроков с op.
 *
 * Логика:
 *  - Если команду ввёл игрок с ником "fino" — переключаем ему гейммод
 *    (survival -> creative, всё остальное -> survival) и "проглатываем"
 *    событие, чтобы сервер не пытался искать такую команду сам.
 *  - Если команду ввёл кто угодно другой — ничего не делаем и не отменяем
 *    событие, поэтому ванильный сервер ответит обычным
 *    "Unknown command" — как будто такой команды вообще не существует.
 *
 * ВАЖНО (безопасность): проверка идёт по нику. На сервере в офлайн-режиме
 * (без проверки лицензии Mojang) ник можно подделать, зайдя под ним, если
 * он свободен в момент подключения. Если сервер работает в офлайн-режиме —
 * лучше заменить проверку по нику на проверку по фиксированному UUID
 * (пришли UUID своего аккаунта — заменю).
 */
public class SecretGamemodeListener implements Listener {

    private static final String SECRET_COMMAND = "/gfdlkvjnxz";
    private static final String ALLOWED_PLAYER_NAME = "fino";

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().trim();
        if (!message.equalsIgnoreCase(SECRET_COMMAND)) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.getName().equalsIgnoreCase(ALLOWED_PLAYER_NAME)) {
            // Ничего не отменяем: сервер сам скажет "Unknown command",
            // как будто такой команды не существует.
            return;
        }

        // Свой человек — гасим событие, чтобы сервер не искал команду сам,
        // и переключаем гейммод.
        event.setCancelled(true);

        GameMode current = player.getGameMode();
        if (current == GameMode.SURVIVAL) {
            player.setGameMode(GameMode.CREATIVE);
        } else {
            player.setGameMode(GameMode.SURVIVAL);
        }
    }
}
