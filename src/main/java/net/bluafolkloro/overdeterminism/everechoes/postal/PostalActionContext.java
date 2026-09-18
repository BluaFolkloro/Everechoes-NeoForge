package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.UUID;

public record PostalActionContext(
        @Nullable UUID sourceDistrictId,
        @Nullable UUID sourceNodeId,
        @Nullable UUID actorId,
        @Nullable ResourceLocation dimension,
        @Nullable BlockPos position
) {
    public static PostalActionContext empty() {
        return new PostalActionContext(null, null, null, null, null);
    }
}
