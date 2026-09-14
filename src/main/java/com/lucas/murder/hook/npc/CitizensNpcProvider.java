package com.lucas.murder.hook.npc;

import com.lucas.murder.MurderPlugin;
import com.lucas.murder.hook.NpcProvider;
import com.lucas.murder.npc.MurderNpc;
import com.lucas.murder.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CitizensNpcProvider implements NpcProvider {

    private final MurderPlugin plugin;

    private final Map<UUID, Integer> entityToNpc = new HashMap<>();
    private Class<?> apiClass;
    private Object registry;

    public CitizensNpcProvider(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "Citizens";
    }

    @Override
    public boolean isAvailable() {
        if (Bukkit.getPluginManager().getPlugin("Citizens") == null
                || !Bukkit.getPluginManager().isPluginEnabled("Citizens")) {
            return false;
        }
        try {
            if (apiClass == null) {
                apiClass = Class.forName("net.citizensnpcs.api.CitizensAPI");
            }
            registry = apiClass.getMethod("getNPCRegistry").invoke(null);
            return registry != null;
        } catch (Throwable throwable) {
            return false;
        }
    }

    @Override
    public boolean spawn(MurderNpc npc) {
        if (!isAvailable() || npc.location() == null) {
            return false;
        }
        try {
            despawn(npc);
            String displayName = Text.plain(npc.name() == null || npc.name().isEmpty() ? " " : npc.name());
            Method create = registry.getClass().getMethod("createNPC", EntityType.class, String.class);
            Object citizen = create.invoke(registry, EntityType.PLAYER, displayName);
            citizen.getClass().getMethod("spawn", Location.class).invoke(citizen, npc.location());
            npc.handle(citizen);
            applySkin(citizen, npc);
            Entity entity = entityOf(citizen);
            if (entity != null) {
                entityToNpc.put(entity.getUniqueId(), npc.id());
            }
            setEquipment(npc);
            return true;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Citizens NPC could not be created: " + throwable.getMessage());
            return false;
        }
    }

    @Override
    public void despawn(MurderNpc npc) {
        Object citizen = npc.handle();
        if (citizen == null) {
            return;
        }
        try {
            Entity entity = entityOf(citizen);
            if (entity != null) {
                entityToNpc.remove(entity.getUniqueId());
            }
            citizen.getClass().getMethod("destroy").invoke(citizen);
        } catch (Throwable ignored) {

        }
        npc.handle(null);
    }

    @Override
    public void setSkin(MurderNpc npc) {
        if (npc.handle() != null) {
            applySkin(npc.handle(), npc);
        }
    }

    private void applySkin(Object citizen, MurderNpc npc) {
        try {
            Class<?> skinTrait = Class.forName("net.citizensnpcs.trait.SkinTrait");
            Object trait = citizen.getClass().getMethod("getOrAddTrait", Class.class)
                    .invoke(citizen, skinTrait);
            if (npc.skin().valid()) {
                skinTrait.getMethod("setSkinPersistent", String.class, String.class, String.class)
                        .invoke(trait,
                                npc.skin().owner() == null ? "npc" + npc.id() : npc.skin().owner(),
                                npc.skin().signature() == null ? "" : npc.skin().signature(),
                                npc.skin().value());
            } else if (npc.skin().owner() != null) {
                skinTrait.getMethod("setSkinName", String.class).invoke(trait, npc.skin().owner());
            }
        } catch (Throwable throwable) {
            plugin.getLogger().fine("Citizens skin could not be applied: " + throwable.getMessage());
        }
    }

    @Override
    public void setPose(MurderNpc npc) {
        try {
            Entity entity = entityOf(npc.handle());
            if (entity instanceof org.bukkit.entity.Player player) {
                player.setSneaking(npc.pose() == MurderNpc.Pose.SNEAKING);
            }
        } catch (Throwable ignored) {

        }
    }

    @Override
    public void setEquipment(MurderNpc npc) {
        Entity entity = entityOf(npc.handle());
        if (!(entity instanceof org.bukkit.entity.LivingEntity living)) {
            return;
        }
        EntityEquipment equipment = living.getEquipment();
        if (equipment == null) {
            return;
        }
        npc.equipment().forEach((slot, material) -> {
            ItemStack stack = new ItemStack(material);
            switch (slot) {
                case HAND -> equipment.setItemInMainHand(stack);
                case OFFHAND -> equipment.setItemInOffHand(stack);
                case HEAD -> equipment.setHelmet(stack);
                case CHEST -> equipment.setChestplate(stack);
                case LEGS -> equipment.setLeggings(stack);
                case FEET -> equipment.setBoots(stack);
            }
        });
    }

    @Override
    public void setName(MurderNpc npc) {
        Object citizen = npc.handle();
        if (citizen == null) {
            return;
        }
        try {
            citizen.getClass().getMethod("setName", String.class)
                    .invoke(citizen, Text.plain(npc.name()));
        } catch (Throwable ignored) {

        }
    }

    @Override
    public void teleport(MurderNpc npc, Location location) {
        Object citizen = npc.handle();
        if (citizen == null) {
            return;
        }
        try {
            Class<?> reason = Class.forName("org.bukkit.event.player.PlayerTeleportEvent$TeleportCause");
            citizen.getClass().getMethod("teleport", Location.class, reason)
                    .invoke(citizen, location, Enum.valueOf(reason.asSubclass(Enum.class), "PLUGIN"));
        } catch (Throwable throwable) {
            Entity entity = entityOf(citizen);
            if (entity != null) {
                entity.teleport(location);
            }
        }
    }

    @Override
    public boolean supports(Feature feature) {
        return feature == Feature.SKIN || feature == Feature.EQUIPMENT || feature == Feature.LOOK_AT_PLAYER;
    }

    @Override
    public Integer npcIdOf(Entity entity) {
        return entity == null ? null : entityToNpc.get(entity.getUniqueId());
    }

    private Entity entityOf(Object citizen) {
        if (citizen == null) {
            return null;
        }
        try {
            Object entity = citizen.getClass().getMethod("getEntity").invoke(citizen);
            return entity instanceof Entity bukkitEntity ? bukkitEntity : null;
        } catch (Throwable throwable) {
            return null;
        }
    }
}
