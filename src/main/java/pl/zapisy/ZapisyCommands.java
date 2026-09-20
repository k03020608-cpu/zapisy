package pl.zapisy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ZapisyCommands implements CommandExecutor {

    private final ZapisyManager manager;

    public ZapisyCommands(ZapisyManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        // Uprawnienia sprawdza serwer na podstawie plugin.yml (pole "permission"),
        // więc tutaj kod wykona się tylko dla graczy z odpowiednią permisją.
        return switch (command.getName().toLowerCase()) {
            case "zapisz" -> zapisz(sender);
            case "wypisz" -> wypisz(sender);
            case "zapisani" -> zapisani(sender);
            default -> false;
        };
    }

    private boolean zapisz(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Ta komenda jest tylko dla graczy.", NamedTextColor.RED));
            return true;
        }

        if (manager.add(player)) {
            player.sendMessage(Component.text("Zapisano Cię na listę!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Już jesteś zapisany.", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean wypisz(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Ta komenda jest tylko dla graczy.", NamedTextColor.RED));
            return true;
        }

        if (manager.remove(player)) {
            player.sendMessage(Component.text("Wypisano Cię z listy.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Nie jesteś zapisany.", NamedTextColor.YELLOW));
        }
        return true;
    }

    private boolean zapisani(CommandSender sender) {
        List<String> names = manager.names();

        if (names.isEmpty()) {
            sender.sendMessage(Component.text("Nikt nie jest jeszcze zapisany.", NamedTextColor.GRAY));
            return true;
        }

        Component list = Component.join(
                JoinConfiguration.commas(true),
                names.stream().map(name -> Component.text(name, NamedTextColor.WHITE)).toList()
        );

        sender.sendMessage(
                Component.text("Zapisani (" + names.size() + "): ", NamedTextColor.GOLD).append(list)
        );
        return true;
    }
}
