package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.Objects;

public record LetterBoxAddress(String postalCode) implements Address {
    public LetterBoxAddress {
        Objects.requireNonNull(postalCode, "postalCode");
        if (postalCode.isBlank()) {
            throw new IllegalArgumentException("postalCode cannot be blank");
        }
    }
}
