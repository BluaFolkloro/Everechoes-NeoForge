package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.Objects;
import java.util.UUID;

public record PlayerAddress(UUID playerId) implements Address {
    public PlayerAddress {
        Objects.requireNonNull(playerId, "playerId");
    }
}
