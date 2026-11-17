package dev.paperstream.murder.hook;

import dev.paperstream.murder.MurderPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class HookManager {

    private static final String[] KNOWN = {
            "PlaceholderAPI", "Vault", "Citizens", "AdvancedReplay", "WorldEdit", "WorldGuard",
            "FancyNpcs", "ZNPCsPlus", "Adyeshach", "EasyNPC", "NPCLib", "PlayerNPC", "ServersNPC",
            "CorpseReborn", "Corpse", "Corpses", "DeadBodies", "SimpleCorpse"
    };

    private final MurderPlugin plugin;
    private final Map<String, Boolean> detected = new LinkedHashMap<>();
    private Method placeholderApiSet;

    public HookManager(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    public void detect() {
        detected.clear();
        for (String name : KNOWN) {
            detected.put(name, Bukkit.getPluginManager().isPluginEnabled(name));
        }
        if (isEnabled("PlaceholderAPI")) {
            try {
                Class<?> api = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                placeholderApiSet = api.getMethod("setPlaceholders", Player.class, String.class);
            } catch (Throwable throwable) {
                placeholderApiSet = null;
            }
        }
    }

    public Map<String, Boolean> detected() {
        return detected;
    }

    public boolean isEnabled(String name) {
        String mode = plugin.settings().getString(null, "hooks." + name.toLowerCase(Locale.ROOT), "AUTO");
        if ("false".equalsIgnoreCase(mode)) {
            return false;
        }
        return Boolean.TRUE.equals(detected.get(name));
    }

    public String applyPlaceholderApi(Player player, String text) {
        if (placeholderApiSet == null || !isEnabled("PlaceholderAPI")) {
            return text.replace("%papi_", "%");
        }
        try {

            String converted = text.replace("%papi_", "%");
            Object result = placeholderApiSet.invoke(null, player, converted);
            return result == null ? text : String.valueOf(result);
        } catch (Throwable throwable) {
            return text;
        }
    }
}
