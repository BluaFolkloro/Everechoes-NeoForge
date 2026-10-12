package net.bluafolkloro.overdeterminism.everechoes.network;

import net.bluafolkloro.overdeterminism.everechoes.Everechoes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 封装客户端与服务端之间的网络载荷。
 */
public record PostBoxConfigPayload(/** 保存 {@code action} 对应的状态或依赖。 */ Action action, /** 保存 {@code pos} 对应的状态或依赖。 */ BlockPos pos, /** 保存 {@code domainCode} 对应的状态或依赖。 */ String domainCode, /** 保存 {@code targetDistrictId} 对应的状态或依赖。 */ @Nullable UUID targetDistrictId) implements CustomPacketPayload {
    /**
     * 定义 {@code TYPE} 常量。
     */
    public static final CustomPacketPayload.Type<PostBoxConfigPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Everechoes.MODID, "post_box_config"));

    /**
     * 定义 {@code STREAM_CODEC} 常量。
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, PostBoxConfigPayload> STREAM_CODEC =
            StreamCodec.of(PostBoxConfigPayload::write, PostBoxConfigPayload::read);

    /**
     * 封装一组受限且稳定的业务状态。
     */
    public enum Action {
        /**
         * 定义 {@code CREATE_DISTRICT} 常量。
         */
        CREATE_DISTRICT,
        /**
         * 定义 {@code JOIN_DISTRICT} 常量。
         */
        JOIN_DISTRICT,
        /**
         * 定义 {@code ESTABLISH_DOMAIN} 常量。
         */
        ESTABLISH_DOMAIN,
        /**
         * 定义 {@code JOIN_DOMAIN} 常量。
         */
        JOIN_DOMAIN,
        /**
         * 定义 {@code LEAVE_DOMAIN} 常量。
         */
        LEAVE_DOMAIN,
        /**
         * 定义 {@code LEAVE_DISTRICT} 常量。
         */
        LEAVE_DISTRICT
    }

    /**
     * 执行 {@code type} 对应的业务操作。
     */
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 保存或编码 {@code write} 对应的业务操作。
     */
    private static void write(RegistryFriendlyByteBuf buffer, PostBoxConfigPayload payload) {
        buffer.writeEnum(payload.action);
        buffer.writeBlockPos(payload.pos);
        buffer.writeUtf(payload.domainCode, 8);
        buffer.writeBoolean(payload.targetDistrictId != null);
        if (payload.targetDistrictId != null) {
            buffer.writeUUID(payload.targetDistrictId);
        }
    }

    /**
     * 获取或解析 {@code read} 对应的业务操作。
     */
    private static PostBoxConfigPayload read(RegistryFriendlyByteBuf buffer) {
        Action action = buffer.readEnum(Action.class);
        BlockPos pos = buffer.readBlockPos();
        String domainCode = buffer.readUtf(8);
        UUID target = buffer.readBoolean() ? buffer.readUUID() : null;
        return new PostBoxConfigPayload(action, pos, domainCode, target);
    }
}
