bash

cat /home/claude/Zapisy_v2/src/main/java/pl/zapisy/TurniejManager.java
Output

package pl.zapisy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Prosty turniej pucharowy (single elimination).
 * Runda 1 powstaje z losowo potasowanej listy zapisanych graczy.
 * Gdy liczba graczy w rundzie jest nieparzysta, ostatni dostaje "wolny los"
 * (bye) i automatycznie przechodzi dalej bez walki.
 */
public final class TurniejManager {

    private final JavaPlugin plugin;
    private final File file;

    private final List<List<Mecz>> rundy = new ArrayList<>();
    private boolean zakonczony = false;
    private String mistrz = null;

    public TurniejManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "turniej.yml");
    }

    public boolean istnieje() {
        return !rundy.isEmpty();
    }

    public boolean jestZakonczony() {
        return zakonczony;
    }

    /** Tworzy nowy turniej z podanej listy graczy. Zwraca null, jeśli graczy jest za mało. */
    public String start(List<String> gracze) {
        if (gracze.size() < 2) {
            return null;
        }

        List<String> potasowani = new ArrayList<>(gracze);
        Collections.shuffle(potasowani);

        rundy.clear();
        zakonczony = false;
        mistrz = null;
        rundy.add(paruj(potasowani));

        // Jeśli pierwsza runda składała się z samych "wolnych losów" (bardzo mało graczy),
        // od razu przejdź dalej, aż będzie coś do rozegrania albo zostanie mistrz.
        domykajRundy();

        save();
        return renderuj();
    }

    /** Zgłasza zwycięzcę bieżącego meczu danego gracza. Zwraca null, jeśli nie znaleziono takiego meczu. */
    public String zglosZwyciezce(String nazwaGracza) {
        if (rundy.isEmpty() || zakonczony) {
            return null;
        }

        List<Mecz> biezaca = rundy.get(rundy.size() - 1);
        Mecz znaleziony = null;
        String poprawnaNazwa = null;

        for (Mecz m : biezaca) {
            if (m.zwyciezca != null) {
                continue;
            }
            if (nazwaGracza.equalsIgnoreCase(m.gracz1)) {
                znaleziony = m;
                poprawnaNazwa = m.gracz1;
                break;
            }
            if (m.gracz2 != null && nazwaGracza.equalsIgnoreCase(m.gracz2)) {
                znaleziony = m;
                poprawnaNazwa = m.gracz2;
                break;
            }
        }

        if (znaleziony == null) {
            return null;
        }

        znaleziony.zwyciezca = poprawnaNazwa;
        domykajRundy();
        save();
        return renderuj();
    }

    public void wyczysc() {
        rundy.clear();
        zakonczony = false;
        mistrz = null;
        if (file.exists()) {
            file.delete();
        }
    }

    public String renderuj() {
        StringBuilder sb = new StringBuilder();
        sb.append("§6§l=== Turniej ===\n");

        for (int i = 0; i < rundy.size(); i++) {
            sb.append("§e§lRunda ").append(i + 1).append(":\n");
            for (Mecz m : rundy.get(i)) {
                sb.append("§7 - ");
                if (m.gracz2 == null) {
                    sb.append(m.gracz1).append(" §a(wolny los)\n");
                } else if (m.zwyciezca != null) {
                    sb.append(m.gracz1).append(" §7vs §7").append(m.gracz2)
                            .append(" §a-> zwycięzca: ").append(m.zwyciezca).append("\n");
                } else {
                    sb.append(m.gracz1).append(" §7vs §7").append(m.gracz2).append(" §7(do rozegrania)\n");
                }
            }
        }

        if (zakonczony) {
            sb.append("§6§lMistrz turnieju: §f").append(mistrz);
        }

        return sb.toString();
    }

    // ---------- Logika wewnętrzna ----------

    private List<Mecz> paruj(List<String> nazwy) {
        List<Mecz> mecze = new ArrayList<>();
        for (int i = 0; i < nazwy.size(); i += 2) {
            String p1 = nazwy.get(i);
            String p2 = (i + 1 < nazwy.size()) ? nazwy.get(i + 1) : null;
            mecze.add(new Mecz(p1, p2));
        }
        return mecze;
    }

    /** Jeśli bieżąca runda ma już wszystkich zwycięzców, tworzy kolejną rundę (albo kończy turniej). */
    private void domykajRundy() {
        while (!rundy.isEmpty()) {
            List<Mecz> biezaca = rundy.get(rundy.size() - 1);
            boolean wszystkieRozstrzygniete = biezaca.stream().allMatch(m -> m.zwyciezca != null);

            if (!wszystkieRozstrzygniete) {
                return;
            }

            List<String> zwyciezcy = new ArrayList<>();
            for (Mecz m : biezaca) {
                zwyciezcy.add(m.zwyciezca);
            }

            if (zwyciezcy.size() == 1) {
                zakonczony = true;
                mistrz = zwyciezcy.get(0);
                return;
            }

            rundy.add(paruj(zwyciezcy));
        }
    }

    // ---------- Zapis / odczyt ----------

    private static final class Mecz {
        String gracz1;
        String gracz2;
        String zwyciezca;

        Mecz(String gracz1, String gracz2) {
            this.gracz1 = gracz1;
            this.gracz2 = gracz2;
            // "Wolny los" (brak przeciwnika) od razu liczy się jako zwycięstwo.
            this.zwyciezca = (gracz2 == null) ? gracz1 : null;
        }
    }

    public void load() {
        rundy.clear();
        zakonczony = false;
        mistrz = null;

        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        zakonczony = yaml.getBoolean("zakonczony", false);
        mistrz = yaml.getString("mistrz", null);

        ConfigurationSection rundySection = yaml.getConfigurationSection("rundy");
        if (rundySection == null) {
            return;
        }

        for (String key : rundySection.getKeys(false)) {
            ConfigurationSection rundaSection = rundySection.getConfigurationSection(key);
            if (rundaSection == null) {
                continue;
            }
            List<Mecz> mecze = new ArrayList<>();
            for (String meczKey : rundaSection.getKeys(false)) {
                ConfigurationSection m = rundaSection.getConfigurationSection(meczKey);
                if (m == null) {
                    continue;
                }
                Mecz mecz = new Mecz(m.getString("gracz1"), m.getString("gracz2", null));
                mecz.zwyciezca = m.getString("zwyciezca", null);
                mecze.add(mecz);
            }
            rundy.add(mecze);
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("zakonczony", zakonczony);
        yaml.set("mistrz", mistrz);

        for (int r = 0; r < rundy.size(); r++) {
            List<Mecz> mecze = rundy.get(r);
            for (int m = 0; m < mecze.size(); m++) {
                Mecz mecz = mecze.get(m);
                String path = "rundy.runda" + r + ".mecz" + m;
                yaml.set(path + ".gracz1", mecz.gracz1);
                yaml.set(path + ".gracz2", mecz.gracz2);
                yaml.set(path + ".zwyciezca", mecz.zwyciezca);
            }
        }

        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie udało się zapisać turniej.yml: " + e.getMessage());
        }
    }
}
