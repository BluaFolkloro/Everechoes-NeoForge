package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.Objects;

public record PostBoxAddress(String postalCode) implements Address {
    public PostBoxAddress {
        postalCode = Objects.requireNonNull(postalCode, "postalCode cannot be null").strip();
        if (postalCode.isEmpty()) {
            throw new IllegalArgumentException("postalCode cannot be blank");
        }
    }
}
