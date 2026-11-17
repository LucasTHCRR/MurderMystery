package dev.paperstream.murder.hook.npc;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import dev.paperstream.murder.MurderPlugin;
import dev.paperstream.murder.hook.NpcProvider;
import dev.paperstream.murder.npc.MurderNpc;
import dev.paperstream.murder.util.Tags;
import dev.paperstream.murder.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.EulerAngle;

import java.util.UUID;

public final class InternalNpcProvider implements NpcProvider {

    private final MurderPlugin plugin;

    public InternalNpcProvider(MurderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "Internal";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean spawn(MurderNpc npc) {
        Location location = npc.location();
        if (location == null || location.getWorld() == null) {
            return false;
        }
        despawn(npc);
        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class, entity -> {
            entity.setInvulnerable(plugin.settings().getBoolean(null, "npcs.invulnerable", true));
            entity.setGravity(plugin.settings().getBoolean(null, "npcs.gravity", false));
            entity.setBasePlate(false);
            entity.setArms(true);
            entity.setCanPickupItems(false);
            entity.setPersistent(true);
            entity.setCustomNameVisible(false);
            entity.setGlowing(plugin.settings().getBoolean(null, "npcs.glowing", false));
            entity.setRightArmPose(new EulerAngle(Math.toRadians(-15), 0, Math.toRadians(10)));
        });
        stand.getPersistentDataContainer().set(Tags.NPC_ID, PersistentDataType.INTEGER, npc.id());
        Tags.tag(stand, "", Tags.KIND_NPC);
        npc.handle(stand);
        setSkin(npc);
        setEquipment(npc);
        setName(npc);
        setPose(npc);
        return true;
    }

    @Override
    public void despawn(MurderNpc npc) {
        if (npc.handle() instanceof ArmorStand stand) {
            stand.remove();
        }
        npc.handle(null);

        Location location = npc.location();
        if (location != null && location.getWorld() != null && location.getWorld().isChunkLoaded(
                location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            for (Entity entity : location.getWorld().getNearbyEntities(location, 2, 2, 2)) {
                if (entity.getType() == EntityType.ARMOR_STAND && idOf(entity) != null
                        && idOf(entity) == npc.id()) {
                    entity.remove();
                }
            }
        }
    }

    @Override
    public void setSkin(MurderNpc npc) {
        if (!(npc.handle() instanceof ArmorStand stand)) {
            return;
        }
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta meta) {
            if (npc.skin().valid()) {
                PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), null);
                profile.setProperty(new ProfileProperty("textures", npc.skin().value(),
                        npc.skin().signature() == null ? "" : npc.skin().signature()));
                meta.setPlayerProfile(profile);
            } else if (npc.skin().owner() != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(npc.skin().owner()));
            }
            head.setItemMeta(meta);
        }
        EntityEquipment equipment = stand.getEquipment();
        if (equipment != null) {
            equipment.setHelmet(head);
        }
    }

    @Override
    public void setPose(MurderNpc npc) {
        if (!(npc.handle() instanceof ArmorStand stand)) {
            return;
        }
        switch (npc.pose()) {
            case SITTING -> stand.setRightLegPose(new EulerAngle(Math.toRadians(-90), 0, 0));
            case SNEAKING -> stand.setBodyPose(new EulerAngle(Math.toRadians(20), 0, 0));
            case LYING -> stand.setBodyPose(new EulerAngle(Math.toRadians(90), 0, 0));
            default -> stand.setBodyPose(new EulerAngle(0, 0, 0));
        }
    }

    @Override
    public void setEquipment(MurderNpc npc) {
        if (!(npc.handle() instanceof ArmorStand stand)) {
            return;
        }
        EntityEquipment equipment = stand.getEquipment();
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
        if (!(npc.handle() instanceof ArmorStand stand)) {
            return;
        }

        if (npc.name() != null && !npc.name().isEmpty()) {
            stand.customName(Text.of(npc.name()));
        }
        stand.setCustomNameVisible(false);
    }

    @Override
    public void teleport(MurderNpc npc, Location location) {
        if (npc.handle() instanceof ArmorStand stand) {
            stand.teleport(location);
        }
    }

    @Override
    public boolean supports(Feature feature) {

        return feature == Feature.SKIN || feature == Feature.EQUIPMENT || feature == Feature.POSE;
    }

    @Override
    public Integer npcIdOf(Entity entity) {
        return idOf(entity);
    }

    private Integer idOf(Entity entity) {
        return entity.getPersistentDataContainer().get(Tags.NPC_ID, PersistentDataType.INTEGER);
    }
}
