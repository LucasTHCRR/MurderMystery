package dev.paperstream.murder.hook.corpse;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;

final class MannequinBody {

    private MannequinBody() {
    }

    static Entity spawn(Location spot, Player victim, boolean skin) {
        return spot.getWorld().spawn(spot, Mannequin.class, entity -> {
            if (skin) {
                entity.setProfile(ResolvableProfile.resolvableProfile(victim.getPlayerProfile()));
            }
            entity.setPose(Mannequin.validPoses().contains(Pose.SLEEPING) ? Pose.SLEEPING : Pose.SWIMMING, true);
            entity.setImmovable(true);
            entity.setInvulnerable(true);
            entity.setAI(false);
            entity.setSilent(true);
            entity.setCollidable(false);
            entity.setCustomNameVisible(false);
            entity.setDescription(null);
            entity.setPersistent(false);
        });
    }
}
