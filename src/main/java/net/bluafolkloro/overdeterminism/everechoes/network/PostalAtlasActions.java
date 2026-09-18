package net.bluafolkloro.overdeterminism.everechoes.network;

import net.bluafolkloro.overdeterminism.everechoes.block.entity.PostBoxBlockEntity;
import net.bluafolkloro.overdeterminism.everechoes.menu.PostalAtlasMenu;
import net.bluafolkloro.overdeterminism.everechoes.postal.DistrictCoverage;
import net.bluafolkloro.overdeterminism.everechoes.postal.PolicyDecision;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalActionContext;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalAtlasLimits;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalAtlasSnapshot;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalChunk;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PostalAtlasActions {
    private PostalAtlasActions() {
    }

    public static void handleRequest(PostalAtlasRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> handleRequestOnMain(payload, context));
    }

    public static void handleSync(PostalAtlasSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof PostalAtlasMenu menu) {
                switch (payload.action()) {
                    case WINDOW -> {
                        if (payload.window() != null) {
                            menu.replaceWindow(payload.window());
                        }
                    }
                    case RESULT -> menu.applyResult(
                            payload.allowed(),
                            payload.reasonKey(),
                            payload.revision(),
                            payload.savedCoveragePacked()
                    );
                }
            }
        });
    }

    private static void handleRequestOnMain(PostalAtlasRequestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.containerMenu instanceof PostalAtlasMenu menu)) {
            player.displayClientMessage(Component.translatable("message.everechoes.atlas.not_open"), true);
            return;
        }
        if (!menu.stillValid(player) || !(player.level() instanceof ServerLevel level)) {
            player.displayClientMessage(Component.translatable("message.everechoes.atlas.not_open"), true);
            return;
        }
        if (!(level.getBlockEntity(menu.pos()) instanceof PostBoxBlockEntity postBox)) {
            player.displayClientMessage(Component.translatable("message.everechoes.atlas.not_open"), true);
            return;
        }
        if (!menu.districtId().equals(postBox.districtId()) || !menu.nodeId().equals(postBox.nodeId())) {
            player.displayClientMessage(Component.translatable("message.everechoes.atlas.not_open"), true);
            return;
        }

        PostalNetwork network = PostalNetwork.get(level);
        UUID districtId = menu.districtId();
        ResourceLocation dimension = level.dimension().location();
        switch (payload.action()) {
            case PAN -> handlePan(player, menu, network, districtId, dimension, payload);
            case SUBMIT -> handleSubmit(player, menu, network, postBox, districtId, dimension, payload);
        }
    }

    private static void handlePan(
            ServerPlayer player,
            PostalAtlasMenu menu,
            PostalNetwork network,
            UUID districtId,
            ResourceLocation dimension,
            PostalAtlasRequestPayload payload
    ) {
        network.atlasWindow(
                districtId,
                dimension,
                payload.originX(),
                payload.originZ(),
                PostalAtlasLimits.WINDOW_SIZE,
                PostalAtlasLimits.WINDOW_SIZE,
                false
        ).ifPresent(snapshot -> {
            menu.replaceWindow(snapshot);
            PacketDistributor.sendToPlayer(player, PostalAtlasSyncPayload.window(snapshot));
        });
    }

    private static void handleSubmit(
            ServerPlayer player,
            PostalAtlasMenu menu,
            PostalNetwork network,
            PostBoxBlockEntity postBox,
            UUID districtId,
            ResourceLocation dimension,
            PostalAtlasRequestPayload payload
    ) {
        Set<Long> packed = payload.packedChunks();
        if (packed.size() > PostalAtlasLimits.MAX_SUBMIT_CHUNKS) {
            deny(player, menu, "message.everechoes.atlas.too_many");
            return;
        }
        if (payload.dimension() != null && !payload.dimension().equals(dimension)) {
            deny(player, menu, "message.everechoes.coverage.multiple_dimensions");
            return;
        }

        Set<PostalChunk> chunks = new LinkedHashSet<>();
        for (long packedChunk : packed) {
            chunks.add(PostalChunk.unpack(dimension, packedChunk));
        }

        PostalActionContext actionContext = postBox.actionContext(player.getUUID());
        Optional<DistrictCoverage> updated = network.replaceDistrictCoverage(
                districtId,
                chunks,
                payload.expectedRevision(),
                actionContext
        );
        if (updated.isEmpty()) {
            PolicyDecision decision = network.validateCoverageReplacement(
                    districtId,
                    chunks,
                    payload.expectedRevision(),
                    actionContext
            );
            String reason = decision.reasonKey() == null ? "message.everechoes.coverage.invalid" : decision.reasonKey();
            deny(player, menu, reason);
            return;
        }

        DistrictCoverage coverage = updated.get();
        PostalAtlasSnapshot current = menu.snapshot();
        PostalAtlasSnapshot snapshot = network.atlasWindow(
                districtId,
                dimension,
                current.originX(),
                current.originZ(),
                PostalAtlasLimits.WINDOW_SIZE,
                PostalAtlasLimits.WINDOW_SIZE,
                true
        ).orElse(null);
        Set<Long> saved = new LinkedHashSet<>();
        for (PostalChunk chunk : coverage.chunks()) {
            saved.add(chunk.packed());
        }
        if (snapshot != null) {
            menu.replaceWindow(snapshot);
            saved = snapshot.savedCoveragePacked();
        }
        PacketDistributor.sendToPlayer(
                player,
                PostalAtlasSyncPayload.result(true, "", coverage.revision(), saved)
        );
    }

    private static void deny(ServerPlayer player, PostalAtlasMenu menu, String reasonKey) {
        player.displayClientMessage(Component.translatable(reasonKey), true);
        PacketDistributor.sendToPlayer(
                player,
                PostalAtlasSyncPayload.result(false, reasonKey, menu.snapshot().revision(), Set.of())
        );
    }
}
