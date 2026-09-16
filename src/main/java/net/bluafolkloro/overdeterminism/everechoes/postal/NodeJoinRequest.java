package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.UUID;

public record NodeJoinRequest(UUID nodeId, UUID districtId, PostalActionContext context, boolean explicit) {
}
