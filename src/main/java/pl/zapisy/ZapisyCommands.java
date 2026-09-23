package pl.zapisy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ZapisyCommands implements CommandExecutor {

    private final ZapisyManager manager;
    private final TurniejManager turniej;

    public ZapisyCommands(ZapisyManager manager, TurniejManager turniej) {
        this.manager = manager;
        this.turniej = turniej;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        return switch (command.getName().toLowerCase()) {
            case "zapisz" -> zapisz(sender);
            case "wypisz" -> wypisz(sender);
            case "zapisani" -> zapisani(sender);
            case "resetlisty" -> resetlisty(sender);
            case "zamknijzapisy" -> zamknijzapisy(sender);
            case "otworzzapisy" -> otworzzapisy(sender);
            case "stworzturniej" -> stworzturniej(sender);
            case "wygral" -> wygral(sender, args);
            case "turniej" -> pokazTurniej(sender);
            default -> false;
        };
    }

    private boolean zapisz(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Ta komenda jest tylko dla graczy.", NamedTextColor.RED));
            return true;
        }

        if (!manager.isOpen()) {
            player.sendMessage(Component.text("Zapisy są obecnie zamknięte.", NamedTextColor.RED));
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

    private boolean resetlisty(CommandSender sender) {
        int ile = manager.size();
        manager.clear();
        sender.sendMessage(Component.text(
                "Wyczyszczono listę zapisanych (" + ile + " graczy).", NamedTextColor.GREEN));
        return true;
    }

    private boolean zamknijzapisy(CommandSender sender) {
        manager.setOpen(false);
        Bukkit.broadcast(Component.text("Zapisy zostały zamknięte.", NamedTextColor.RED));
        return true;
    }

    private boolean otworzzapisy(CommandSender sender) {
        manager.setOpen(true);
        Bukkit.broadcast(Component.text("Zapisy zostały otwarte! Wpisz /zapisz, aby dołączyć.", NamedTextColor.GREEN));
        return true;
    }

    private boolean stworzturniej(CommandSender sender) {
        List<String> gracze = manager.names();
        String wynik = turniej.start(gracze);

        if (wynik == null) {
            sender.sendMessage(Component.text(
                    "Potrzeba co najmniej 2 zapisanych graczy, żeby stworzyć turniej.", NamedTextColor.RED));
            return true;
        }

        Bukkit.broadcast(legacy(wynik));
        return true;
    }

    private boolean wygral(CommandSender sender, String[] args) {
        if (!turniej.istnieje()) {
            sender.sendMessage(Component.text("Nie ma jeszcze utworzonego turnieju.", NamedTextColor.RED));
            return true;
        }
        if (turniej.jestZakonczony()) {
            sender.sendMessage(Component.text("Turniej jest już zakończony.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(Component.text("Użycie: /wygral <gracz>", NamedTextColor.YELLOW));
            return true;
        }

        String wynik = turniej.zglosZwyciezce(args[0]);
        if (wynik == null) {
            sender.sendMessage(Component.text(
                    "Nie znaleziono nierozegranego meczu tego gracza w bieżącej rundzie.", NamedTextColor.RED));
            return true;
        }

        Bukkit.broadcast(legacy(wynik));
        return true;
    }

    private boolean pokazTurniej(CommandSender sender) {
        if (!turniej.istnieje()) {
            sender.sendMessage(Component.text("Nie ma jeszcze utworzonego turnieju.", NamedTextColor.RED));
            return true;
        }
        sender.sendMessage(legacy(turniej.renderuj()));
        return true;
    }

    private Component legacy(String tekst) {
        return LegacyComponentSerializer.legacySection().deserialize(tekst);
    }
}
