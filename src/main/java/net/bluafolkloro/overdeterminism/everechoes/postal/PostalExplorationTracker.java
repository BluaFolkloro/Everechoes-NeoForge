package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PostalExplorationTracker {
    private static final Map<UUID, PostalChunk> LAST_CHUNK = new HashMap<>();

    private PostalExplorationTracker() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        PostalChunk current = PostalChunk.at(level.dimension().location(), player.blockPosition());
        if (!current.equals(LAST_CHUNK.put(player.getUUID(), current))) {
            PostalNetwork.get(level).recordExplored(current);
        }
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_CHUNK.remove(event.getEntity().getUUID());
    }
}
