package ru.mounts.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Обрабатывает /cdhorse, /cddonkey, /cdcamel — задаёт длительность кулдауна
 * (в секундах) для соответствующей команды призыва. Значение сохраняется
 * в config.yml и переживает перезапуск сервера.
 */
public class CooldownConfigCommand implements CommandExecutor {

    private final MountsPlugin plugin;
    private final MountType type;

    public CooldownConfigCommand(MountsPlugin plugin, MountType type) {
        this.plugin = plugin;
        this.type = type;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("§cИспользование: /" + label + " <секунды>");
            return true;
        }

        long seconds;
        try {
            seconds = Long.parseLong(args[0]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§cУкажите целое число секунд, например: /" + label + " 120");
            return true;
        }

        if (seconds < 0) {
            sender.sendMessage("§cКулдаун не может быть отрицательным.");
            return true;
        }

        plugin.setCooldownSeconds(type, seconds);
        sender.sendMessage("§aКулдаун для команды /" + type.getCommandName()
                + " установлен: " + seconds + " сек.");
        return true;
    }
}
