package dev.paperstream.murder.hook;

import dev.paperstream.murder.MurderPlugin;
import dev.paperstream.murder.hook.npc.CitizensNpcProvider;
import dev.paperstream.murder.hook.npc.InternalNpcProvider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NpcProviderRegistry {

    private final MurderPlugin plugin;
    private final Map<String, NpcProvider> providers = new LinkedHashMap<>();
    private NpcProvider active;

    public NpcProviderRegistry(MurderPlugin plugin) {
        this.plugin = plugin;
        providers.put("CITIZENS", new CitizensNpcProvider(plugin));
        providers.put("INTERNAL", new InternalNpcProvider(plugin));
    }

    public void detect() {
        String configured = plugin.settings().getString(null, "npcs.provider", "AUTO");
        if (!"AUTO".equalsIgnoreCase(configured)) {
            NpcProvider provider = providers.get(configured.toUpperCase(Locale.ROOT));
            if (provider != null && provider.isAvailable()) {
                active = provider;
                plugin.getLogger().info("NPC provider: " + provider.name());
                return;
            }
            plugin.getLogger().warning("NPC provider " + configured
                    + " is not available - falling back to the internal one.");
            active = providers.get("INTERNAL");
            return;
        }
        List<String> priority = plugin.settings().getStringList(null, "npcs.provider-priority");
        if (priority.isEmpty()) {
            priority = List.of("CITIZENS", "INTERNAL");
        }
        for (String name : priority) {
            NpcProvider provider = providers.get(name.toUpperCase(Locale.ROOT));
            if (provider != null && provider.isAvailable()) {
                active = provider;
                plugin.getLogger().info("NPC provider: " + provider.name());
                return;
            }
        }
        active = providers.get("INTERNAL");
        plugin.getLogger().info("NPC provider: " + active.name());
    }

    public NpcProvider active() {
        if (active == null) {
            detect();
        }
        return active;
    }

    public NpcProvider provider(String name) {
        return name == null ? null : providers.get(name.toUpperCase(Locale.ROOT));
    }

    public boolean switchTo(String name) {
        NpcProvider provider = provider(name);
        if (provider == null || !provider.isAvailable()) {
            return false;
        }
        active = provider;
        return true;
    }

    public Map<String, NpcProvider> providers() {
        return providers;
    }

    public String names() {
        return String.join(", ", providers.keySet());
    }
}
