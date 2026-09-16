package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.bluafolkloro.overdeterminism.everechoes.block.entity.PostBoxBlockEntity;
import net.bluafolkloro.overdeterminism.everechoes.menu.PostBoxConfigMenu;
import net.bluafolkloro.overdeterminism.everechoes.network.PostBoxConfigPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class PostBoxConfigActions {
    private PostBoxConfigActions() {
    }

    public static void handle(PostBoxConfigPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.containerMenu instanceof PostBoxConfigMenu menu) || !menu.pos().equals(payload.pos())) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(level.getBlockEntity(payload.pos()) instanceof PostBoxBlockEntity postBox)) {
            return;
        }
        if (!menu.stillValid(player)) {
            return;
        }

        PostalNetwork network = PostalNetwork.get(level);
        String domainId = switch (payload.action()) {
            case CREATE -> createDomain(player, network, payload.domainCode());
            case SELECT -> selectDomain(player, network, payload.domainCode());
        };
        if (domainId == null) {
            return;
        }

        if (!postBox.assignToDomain(level, domainId)) {
            player.displayClientMessage(Component.translatable("message.everechoes.post_box.district_full"), true);
            return;
        }

        player.displayClientMessage(Component.translatable(
                "message.everechoes.post_box.bound",
                PostalCodes.formatDistrict(postBox.domainId(), postBox.districtId())
        ), true);
        player.closeContainer();
    }

    private static String createDomain(ServerPlayer player, PostalNetwork network, String raw) {
        if (PostalCodes.canonicalDomain(raw).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.everechoes.post_box.invalid_domain"), true);
            return null;
        }

        return network.createDomain(raw).orElseGet(() -> {
            player.displayClientMessage(Component.translatable("message.everechoes.post_box.domain_exists"), true);
            return null;
        });
    }

    private static String selectDomain(ServerPlayer player, PostalNetwork network, String raw) {
        String domainId = PostalCodes.canonicalDomain(raw).orElse(null);
        if (domainId == null || !network.hasDomain(domainId)) {
            player.displayClientMessage(Component.translatable("message.everechoes.post_box.unknown_domain"), true);
            return null;
        }
        return domainId;
    }
}
