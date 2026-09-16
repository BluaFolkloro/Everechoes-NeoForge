package net.bluafolkloro.overdeterminism.everechoes.network;

import net.bluafolkloro.overdeterminism.everechoes.letter.LetterActions;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostBoxConfigActions;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private ModNetworking() {
    }

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
    }
}
