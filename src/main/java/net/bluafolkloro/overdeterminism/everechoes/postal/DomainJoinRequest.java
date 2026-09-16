package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.UUID;

public record DomainJoinRequest(UUID districtId, UUID domainId, PostalActionContext context) {
}
