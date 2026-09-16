package net.bluafolkloro.overdeterminism.everechoes.postal;

import javax.annotation.Nullable;

public record PolicyDecision(boolean allowed, @Nullable String reasonKey) {
    public static PolicyDecision allow() {
        return new PolicyDecision(true, null);
    }

    public static PolicyDecision deny(String reasonKey) {
        return new PolicyDecision(false, reasonKey);
    }
}
