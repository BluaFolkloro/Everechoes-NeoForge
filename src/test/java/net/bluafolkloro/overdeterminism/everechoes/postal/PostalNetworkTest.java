package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostalNetworkTest {
    private static final ResourceLocation OVERWORLD = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    @Test
    void domainHasNoOwnerAndSeparatesInternalIdFromDisplayCode() {
        PostalNetwork network = new PostalNetwork();
        PostalDomain first = network.establishDomain("ev").orElseThrow();
        PostalDomain second = network.establishDomain("ab").orElseThrow();

        assertFalse(first.hasOwner());
        assertEquals("EV", first.domainCode());
        assertNotEquals(first.domainId(), second.domainId());
        assertNotEquals(first.domainId().toString(), first.domainCode());
        assertEquals(DomainLifecycle.DORMANT, first.lifecycle());
    }

    @Test
    void districtCanContainMultiplePostBoxesWithoutNewNumber() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        UUID collection = register(network, 10, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        network.joinDistrictAsCollection(collection, district.districtId(), context(collection, 10, 0)).orElseThrow();

        PostalDomain domain = network.establishAndJoin(district.districtId(), "ev", PostalActionContext.empty(), 0L).orElseThrow();
        assertEquals("1", network.membership(district.districtId()).orElseThrow().districtCode());
        assertEquals(2, network.nodesOf(district.districtId()).size());
        assertEquals(NodeRole.HUB, network.node(hub).orElseThrow().role());
        assertEquals(NodeRole.COLLECTION, network.node(collection).orElseThrow().role());
        assertEquals(2, network.domain(domain.domainId()).orElseThrow().nextDistrict());
    }

    @Test
    void nearbyPolicyRejectsFarAndImplicitJoins() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        UUID far = register(network, 300, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        assertTrue(network.joinDistrictAsCollection(far, district.districtId(), context(far, 300, 0)).isEmpty());

        NodeJoinRequest implicit = new NodeJoinRequest(far, district.districtId(), context(far, 10, 0), false);
        assertFalse(NearbyNodeAdmissionPolicy.INSTANCE.canAdmit(network, implicit).allowed());
    }

    @Test
    void collectionLeavingDoesNotDetachDistrict() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        UUID collection = register(network, 8, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        network.joinDistrictAsCollection(collection, district.districtId(), context(collection, 8, 0));
        network.establishAndJoin(district.districtId(), "ev", PostalActionContext.empty(), 0L);

        network.leaveDistrict(collection);
        assertTrue(network.isActiveDeliveryDistrict(district.districtId()));
        assertEquals(NodeRole.HUB, network.node(hub).orElseThrow().role());
    }

    @Test
    void hubRemovalPromotesCollectionAndLastNodeStopsService() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        UUID collection = register(network, 8, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        network.joinDistrictAsCollection(collection, district.districtId(), context(collection, 8, 0));
        network.establishAndJoin(district.districtId(), "ev", PostalActionContext.empty(), 0L);

        network.removeNode(hub);
        assertEquals(NodeRole.HUB, network.node(collection).orElseThrow().role());
        assertTrue(network.isActiveDeliveryDistrict(district.districtId()));

        network.removeNode(collection);
        assertFalse(network.isServedDistrict(district.districtId()));
        assertFalse(network.isActiveDeliveryDistrict(district.districtId()));
    }

    @Test
    void pendingCanBeApprovedOrRejected() {
        PostalNetwork network = new PostalNetwork(
                OpenDomainPolicies.CREATION,
                new PendingMembershipPolicy(),
                OpenDomainPolicies.EXIT
        );
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        PostalDomain domain = network.establishDomain("ev").orElseThrow();
        DomainMembership pending = network.requestJoin(district.districtId(), domain.domainId(), 0L).orElseThrow();
        assertEquals(MembershipState.PENDING, pending.state());
        assertFalse(pending.isDeliveryEndpoint());

        assertEquals(MembershipState.ACTIVE, network.approveJoin(district.districtId()).orElseThrow().state());
        network.requestLeave(district.districtId());
        network.completeLeave(district.districtId());

        network.requestJoin(district.districtId(), domain.domainId(), 1L);
        assertEquals(MembershipState.DETACHED, network.rejectJoin(district.districtId()).orElseThrow().state());
    }

    @Test
    void leaveDomainUsesServiceAndOpenPolicyCompletesImmediately() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        PostalDomain domain = network.establishAndJoin(district.districtId(), "ev", PostalActionContext.empty(), 0L).orElseThrow();
        DomainMembership left = network.leaveDomain(district.districtId()).orElseThrow();
        assertEquals(MembershipState.DETACHED, left.state());
        assertEquals(DomainLifecycle.DORMANT, network.domain(domain.domainId()).orElseThrow().lifecycle());
    }

    @Test
    void establishAndJoinFailureLeavesNoDomain() {
        PostalNetwork network = new PostalNetwork(
                OpenDomainPolicies.CREATION,
                (ignored, request) -> PolicyDecision.deny("denied"),
                OpenDomainPolicies.EXIT
        );
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        assertTrue(network.establishAndJoin(district.districtId(), "ev", PostalActionContext.empty(), 0L).isEmpty());
        assertTrue(network.liveDomainCodes().isEmpty());
    }

    @Test
    void retireDomainMakesHistorical() {
        PostalNetwork network = new PostalNetwork();
        PostalDomain domain = network.establishDomain("ev").orElseThrow();
        assertEquals(DomainLifecycle.HISTORICAL, network.retireDomain(domain.domainId()).orElseThrow().lifecycle());
        assertTrue(network.findLiveDomainByCode("EV").isEmpty());
    }

    @Test
    void saveAndLoadPreservesDistrictNodesAndMembership() {
        PostalNetwork original = new PostalNetwork();
        UUID hub = register(original, 0, 0);
        PostalDistrict district = original.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        original.establishAndJoin(district.districtId(), "ev", PostalActionContext.empty(), 99L);

        CompoundTag tag = original.save(new CompoundTag(), null);
        assertEquals(PostalNetwork.SCHEMA_VERSION, tag.getInt("schemaVersion"));
        PostalNetwork loaded = PostalNetwork.load(tag, null);
        assertEquals(1, loaded.nodesOf(district.districtId()).size());
        assertEquals(NodeRole.HUB, loaded.node(hub).orElseThrow().role());
        assertEquals("1", loaded.membership(district.districtId()).orElseThrow().districtCode());
        assertEquals("EV", loaded.findLiveDomainByCode("EV").orElseThrow().domainCode());
    }

    @Test
    void oldSchemaLoadsAsEmptyNetwork() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schemaVersion", 3);
        PostalNetwork network = PostalNetwork.load(tag, null);
        assertTrue(network.liveDomainCodes().isEmpty());
        CompoundTag saved = network.save(new CompoundTag(), null);
        assertEquals(PostalNetwork.SCHEMA_VERSION, saved.getInt("schemaVersion"));
    }

    @Test
    void districtsDoNotReuseActiveCodes() {
        PostalNetwork network = new PostalNetwork();
        UUID firstHub = register(network, 0, 0);
        UUID secondHub = register(network, 20, 0);
        PostalDistrict first = network.createDistrict(firstHub, PostalActionContext.empty()).orElseThrow();
        PostalDistrict second = network.createDistrict(secondHub, PostalActionContext.empty()).orElseThrow();
        PostalDomain domain = network.establishAndJoin(first.districtId(), "ev", PostalActionContext.empty(), 0L).orElseThrow();
        network.requestJoin(second.districtId(), domain.domainId(), 0L);
        assertEquals("1", network.membership(first.districtId()).orElseThrow().districtCode());
        assertEquals("2", network.membership(second.districtId()).orElseThrow().districtCode());
    }

    private static UUID register(PostalNetwork network, int x, int z) {
        UUID nodeId = UUID.randomUUID();
        network.registerNode(nodeId, OVERWORLD, new BlockPos(x, 64, z));
        return nodeId;
    }

    private static PostalActionContext context(UUID nodeId, int x, int z) {
        return new PostalActionContext(null, nodeId, OVERWORLD, new BlockPos(x, 64, z));
    }

    private static final class PendingMembershipPolicy implements DomainMembershipPolicy {
        @Override
        public PolicyDecision canJoin(PostalNetwork network, DomainJoinRequest request) {
            return PolicyDecision.allow();
        }

        @Override
        public boolean autoActivate() {
            return false;
        }
    }
}
