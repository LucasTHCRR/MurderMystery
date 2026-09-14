package com.lucas.murder.npc;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.util.Tags;
import com.lucas.murder.util.Text;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.List;

public final class Hologram {

    private final MurderPlugin plugin;
    private final List<TextDisplay> lines = new ArrayList<>();
    private Location base;

    public Hologram(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    public void render(Location location, List<String> texts) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        if (!plugin.settings().feature("holograms")) {
            return;
        }
        this.base = location;
        if (lines.size() != texts.size()) {
            remove();
        }
        double height = plugin.settings().getDouble(null, "npcs.hologram-line-height", 0.25);
        double offset = plugin.settings().getDouble(null, "npcs.hologram-offset-y", 2.1);

        for (int i = 0; i < texts.size(); i++) {

            double y = offset + (texts.size() - 1 - i) * height;
            Location lineLocation = location.clone().add(0, y, 0);
            TextDisplay display;
            if (i < lines.size() && lines.get(i).isValid()) {
                display = lines.get(i);
                display.teleport(lineLocation);
            } else {
                display = location.getWorld().spawn(lineLocation, TextDisplay.class, entity -> {
                    entity.setBillboard(Display.Billboard.CENTER);
                    entity.setShadowed(false);
                    entity.setSeeThrough(false);
                    entity.setPersistent(false);
                    entity.setViewRange(
                            (float) (plugin.settings().getInt(null, "npcs.view-distance", 48) / 64.0));
                });
                Tags.tag(display, "", Tags.KIND_HOLOGRAM);
                if (i < lines.size()) {
                    lines.set(i, display);
                } else {
                    lines.add(display);
                }
            }
            display.text(Text.of(texts.get(i)));
        }
    }

    public void remove() {
        for (TextDisplay display : lines) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        lines.clear();
    }

    public Location base() {
        return base;
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }
}
