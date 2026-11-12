package com.lucas.murder.game;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.region.Cuboid;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.Set;

public final class ArenaMap {

    private final MurderPlugin plugin;
    private final Arena arena;
    private MapView view;
    private java.awt.Color[][] image;
    private Cuboid renderedFor;

    public ArenaMap(MurderPlugin plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    public ItemStack item() {
        MapView map = view();
        if (map == null) {
            return null;
        }
        ItemStack stack = plugin.items().build("map", Material.FILLED_MAP);
        if (stack.getType() != Material.FILLED_MAP) {
            stack.setType(Material.FILLED_MAP);
        }
        if (stack.getItemMeta() instanceof MapMeta meta) {
            meta.setMapView(map);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private MapView view() {
        Cuboid bounds = arena.bounds();
        World world = bounds == null ? null : Bukkit.getWorld(bounds.world());
        if (world == null) {
            return null;
        }
        if (view == null) {
            view = Bukkit.createMap(world);
            view.getRenderers().forEach(view::removeRenderer);
            view.addRenderer(new Renderer());
            view.setTrackingPosition(false);
            view.setUnlimitedTracking(false);
            view.setLocked(true);
        }
        if (renderedFor != bounds) {
            Location center = bounds.center(world);
            view.setWorld(world);
            view.setCenterX(center.getBlockX());
            view.setCenterZ(center.getBlockZ());
            view.setScale(scaleFor(Math.max(bounds.width(), bounds.depth())));
            image = null;
            renderedFor = bounds;
        }
        return view;
    }

    private static MapView.Scale scaleFor(int size) {
        if (size <= 128) {
            return MapView.Scale.CLOSEST;
        }
        if (size <= 256) {
            return MapView.Scale.CLOSE;
        }
        if (size <= 512) {
            return MapView.Scale.NORMAL;
        }
        return size <= 1024 ? MapView.Scale.FAR : MapView.Scale.FARTHEST;
    }

    private java.awt.Color[][] render() {
        Cuboid bounds = arena.bounds();
        World world = Bukkit.getWorld(bounds.world());
        java.awt.Color[][] pixels = new java.awt.Color[128][128];
        if (world == null) {
            return pixels;
        }
        int blocksPerPixel = 1 << view.getScale().ordinal();
        for (int px = 0; px < 128; px++) {
            for (int pz = 0; pz < 128; pz++) {
                int x = view.getCenterX() + (px - 64) * blocksPerPixel;
                int z = view.getCenterZ() + (pz - 64) * blocksPerPixel;
                if (x < bounds.minX() || x > bounds.maxX() || z < bounds.minZ() || z > bounds.maxZ()) {
                    continue;
                }
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                for (int y = bounds.maxY(); y >= bounds.minY(); y--) {
                    Block block = world.getBlockAt(x, y, z);
                    Material type = block.getType();
                    if (type.isAir() || type == Material.BARRIER || type == Material.LIGHT
                            || type.name().endsWith("GLASS") || type.name().endsWith("GLASS_PANE")) {
                        continue;
                    }
                    org.bukkit.Color color = block.getBlockData().getMapColor();
                    pixels[px][pz] = new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue());
                    break;
                }
            }
        }
        return pixels;
    }

    private byte coordinate(double blocks, int center) {
        int blocksPerPixel = 1 << view.getScale().ordinal();
        int value = (int) Math.round((blocks - center) * 2D / blocksPerPixel);
        return (byte) Math.max(-128, Math.min(127, value));
    }

    private byte direction(float yaw) {
        return (byte) (Math.round(((yaw % 360 + 360) % 360) / 22.5F) & 15);
    }

    private final class Renderer extends MapRenderer {

        private final Set<MapCanvas> drawn = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

        private Renderer() {
            super(true);
        }

        @Override
        public void render(MapView map, MapCanvas canvas, Player player) {
            if (image == null) {
                image = ArenaMap.this.render();
                drawn.clear();
            }
            if (drawn.add(canvas)) {
                for (int x = 0; x < 128; x++) {
                    for (int z = 0; z < 128; z++) {
                        if (image[x][z] != null) {
                            canvas.setPixelColor(x, z, image[x][z]);
                        }
                    }
                }
            }
            MapCursorCollection cursors = new MapCursorCollection();
            if (arena.revealed()) {
                for (GamePlayer participant : arena.participants()) {
                    Player other = participant.player();
                    if (other == null || other.equals(player) || !participant.alive() || participant.spectator()) {
                        continue;
                    }
                    Location location = other.getLocation();
                    cursors.addCursor(new MapCursor(coordinate(location.getX(), map.getCenterX()),
                            coordinate(location.getZ(), map.getCenterZ()), direction(location.getYaw()),
                            MapCursor.Type.RED_MARKER, true));
                }
            }
            Location own = player.getLocation();
            cursors.addCursor(new MapCursor(coordinate(own.getX(), map.getCenterX()),
                    coordinate(own.getZ(), map.getCenterZ()), direction(own.getYaw()),
                    MapCursor.Type.PLAYER, true));
            canvas.setCursors(cursors);
        }
    }
}
