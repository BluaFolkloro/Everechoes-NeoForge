package net.bluafolkloro.overdeterminism.everechoes.network;

import net.bluafolkloro.overdeterminism.everechoes.letter.LetterActions;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostBoxConfigActions;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 封装该模块的相关职责。
 */
public final class ModNetworking {
    /**
     * 创建并初始化当前对象。
     */
    private ModNetworking() {
    }

    /**
     * 注册 {@code register} 对应的业务操作。
     */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                LetterActionPayload.TYPE,
                LetterActionPayload.STREAM_CODEC,
                LetterActions::handle
        );
        registrar.playToServer(
                PostBoxConfigPayload.TYPE,
                PostBoxConfigPayload.STREAM_CODEC,
                PostBoxConfigActions::handle
        );
        registrar.playToServer(
                PostalAtlasRequestPayload.TYPE,
                PostalAtlasRequestPayload.STREAM_CODEC,
                PostalAtlasActions::handleRequest
        );
        registrar.playToClient(
                PostalAtlasSyncPayload.TYPE,
                PostalAtlasSyncPayload.STREAM_CODEC,
                PostalAtlasActions::handleSync
        );
    }
}
