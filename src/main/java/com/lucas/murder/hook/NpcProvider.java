package com.lucas.murder.hook;

import com.lucas.murder.npc.MurderNpc;
import org.bukkit.Location;

public interface NpcProvider {

    enum Feature {
        SKIN, POSE, EQUIPMENT, HOLOGRAM, LOOK_AT_PLAYER
    }

    String name();

    boolean isAvailable();

    boolean spawn(MurderNpc npc);

    void despawn(MurderNpc npc);

    void setSkin(MurderNpc npc);

    void setPose(MurderNpc npc);

    void setEquipment(MurderNpc npc);

    void setName(MurderNpc npc);

    void teleport(MurderNpc npc, Location location);

    boolean supports(Feature feature);

    default Integer npcIdOf(org.bukkit.entity.Entity entity) {
        return null;
    }
}
