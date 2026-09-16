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

        postBox.syncFromNetwork(level);
        PostalNetwork network = PostalNetwork.get(level);
        PostalActionContext actionContext = postBox.actionContext();
        boolean changed = switch (payload.action()) {
            case CREATE_DISTRICT -> createDistrict(player, network, postBox, actionContext);
            case JOIN_NEARBY -> joinNearby(player, network, postBox, actionContext, payload.targetDistrictId());
            case ESTABLISH_DOMAIN -> establishDomain(player, network, postBox, actionContext, payload.domainCode());
            case JOIN_DOMAIN -> joinDomain(player, network, postBox, actionContext, payload.domainCode());
            case LEAVE_DOMAIN -> leaveDomain(player, network, postBox);
            case LEAVE_DISTRICT -> leaveDistrict(player, network, postBox);
        };
        if (changed) {
            postBox.syncFromNetwork(level);
            player.closeContainer();
        }
    }

    private static boolean createDistrict(ServerPlayer player, PostalNetwork network, PostBoxBlockEntity postBox, PostalActionContext context) {
        if (network.createDistrict(postBox.nodeId(), context).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.everechoes.post_box.district_create_failed"), true);
            return false;
        }
        player.displayClientMessage(Component.translatable("message.everechoes.post_box.district_created"), true);
        return true;
    }

    private static boolean joinNearby(
            ServerPlayer player,
            PostalNetwork network,
            PostBoxBlockEntity postBox,
            PostalActionContext context,
            java.util.UUID targetDistrictId
    ) {
        if (targetDistrictId == null || network.joinDistrictAsCollection(postBox.nodeId(), targetDistrictId, context).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.everechoes.post_box.join_denied"), true);
            return false;
        }
        player.displayClientMessage(Component.translatable("message.everechoes.post_box.node_joined"), true);
        return true;
    }

    private static boolean establishDomain(
            ServerPlayer player,
            PostalNetwork network,
            PostBoxBlockEntity postBox,
            PostalActionContext context,
            String domainCode
    ) {
        if (postBox.districtId() == null) {
            player.displayClientMessage(Component.translatable("message.everechoes.post_box.need_district"), true);
            return false;
        }
        if (network.establishAndJoin(postBox.districtId(), domainCode, context, System.currentTimeMillis()).isEmpty()) {
            if (PostalCodes.canonicalDomain(domainCode).isEmpty()) {
                player.displayClientMessage(Component.translatable("message.everechoes.post_box.invalid_domain"), true);
            } else if (network.findLiveDomainByCode(domainCode).isPresent()) {
                player.displayClientMessage(Component.translatable("message.everechoes.post_box.domain_exists"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.everechoes.post_box.join_denied"), true);
            }
            return false;
        }
        player.displayClientMessage(Component.translatable("message.everechoes.post_box.bound", postBoxOutward(network, postBox)), true);
        return true;
    }

    private static boolean joinDomain(
            ServerPlayer player,
            PostalNetwork network,
            PostBoxBlockEntity postBox,
            PostalActionContext context,
            String domainCode
    ) {
        if (postBox.districtId() == null) {
            player.displayClientMessage(Component.translatable("message.everechoes.post_box.need_district"), true);
            return false;
        }
        PostalDomain domain = network.findLiveDomainByCode(domainCode).orElse(null);
        if (domain == null) {
            player.displayClientMessage(Component.translatable("message.everechoes.post_box.unknown_domain"), true);
            return false;
        }
        DomainMembership membership = network.requestJoin(postBox.districtId(), domain.domainId(), context, System.currentTimeMillis()).orElse(null);
        if (membership == null) {
            player.displayClientMessage(Component.translatable("message.everechoes.post_box.join_denied"), true);
            return false;
        }
        if (membership.state() == MembershipState.PENDING) {
            player.displayClientMessage(Component.translatable("gui.everechoes.post_box.pending"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.everechoes.post_box.bound", postBoxOutward(network, postBox)), true);
        }
        return true;
    }

    private static boolean leaveDomain(ServerPlayer player, PostalNetwork network, PostBoxBlockEntity postBox) {
        if (postBox.districtId() == null) {
            return false;
        }
        DomainMembership membership = network.leaveDomain(postBox.districtId()).orElse(null);
        if (membership == null) {
            player.displayClientMessage(Component.translatable("message.everechoes.post_box.leave_invalid"), true);
            return false;
        }
        if (membership.state() == MembershipState.LEAVING) {
            player.displayClientMessage(Component.translatable("message.everechoes.post_box.leaving"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.everechoes.post_box.left"), true);
        }
        return true;
    }

    private static boolean leaveDistrict(ServerPlayer player, PostalNetwork network, PostBoxBlockEntity postBox) {
        if (network.leaveDistrict(postBox.nodeId()).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.everechoes.post_box.leave_invalid"), true);
            return false;
        }
        player.displayClientMessage(Component.translatable("message.everechoes.post_box.node_left"), true);
        return true;
    }

    private static String postBoxOutward(PostalNetwork network, PostBoxBlockEntity postBox) {
        if (postBox.districtId() == null) {
            return "";
        }
        DomainMembership membership = network.membership(postBox.districtId()).orElse(null);
        PostalDomain domain = membership == null ? null : network.domain(membership.domainId()).orElse(null);
        if (membership == null || domain == null) {
            return "";
        }
        return PostalCodes.formatOutward(domain.domainCode(), membership.districtCode());
    }
}
