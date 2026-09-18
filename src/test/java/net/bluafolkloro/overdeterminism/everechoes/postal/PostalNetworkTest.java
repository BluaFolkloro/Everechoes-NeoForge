package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
    void overlappingDistrictsAreAllReturnedAndDoNotCreateProxyLinks() {
        PostalNetwork network = new PostalNetwork();
        UUID firstHub = register(network, 0, 0);
        UUID secondHub = register(network, 10, 0);
        PostalDistrict first = network.createDistrict(firstHub, PostalActionContext.empty()).orElseThrow();
        PostalDistrict second = network.createDistrict(secondHub, PostalActionContext.empty()).orElseThrow();
        PostalDomain firstDomain = network.establishAndJoin(first.districtId(), "ev", PostalActionContext.empty(), 0L).orElseThrow();
        PostalDomain secondDomain = network.establishAndJoin(second.districtId(), "ab", PostalActionContext.empty(), 0L).orElseThrow();

        List<PostalDistrict> covering = network.districtsAt(OVERWORLD, new BlockPos(0, 64, 0));
        assertEquals(2, covering.size());
        assertTrue(covering.stream().anyMatch(district -> district.districtId().equals(first.districtId())));
        assertTrue(covering.stream().anyMatch(district -> district.districtId().equals(second.districtId())));
        assertTrue(network.membership(first.districtId()).orElseThrow().domainId().equals(firstDomain.domainId()));
        assertTrue(network.membership(second.districtId()).orElseThrow().domainId().equals(secondDomain.domainId()));
        assertNotEquals(firstDomain.domainId(), secondDomain.domainId());
    }

    @Test
    void coveragePolicyRejectsOutsideAndImplicitJoins() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        UUID far = register(network, 300, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        assertTrue(network.joinDistrictAsCollection(far, district.districtId(), context(far, 300, 0)).isEmpty());

        NodeJoinRequest implicit = new NodeJoinRequest(far, district.districtId(), context(far, 10, 0), false);
        assertFalse(CoveredNodeAdmissionPolicy.INSTANCE.canAdmit(network, implicit).allowed());
    }

    @Test
    void mappedCoverageControlsDiscoveryAndAdmission() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        UUID firstCollection = register(network, 120, 0);
        UUID secondCollection = register(network, 230, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        mapLine(network, district.districtId(), 0, 14);
        network.joinDistrictAsCollection(firstCollection, district.districtId(), context(firstCollection, 120, 0)).orElseThrow();

        assertTrue(network.joinableDistrictsAt(secondCollection, context(secondCollection, 230, 0)).stream()
                .anyMatch(candidate -> candidate.districtId().equals(district.districtId())));
        assertTrue(network.joinDistrictAsCollection(secondCollection, district.districtId(), context(secondCollection, 230, 0)).isPresent());
    }

    @Test
    void coverageMustBeExploredConnectedAndKeepEveryNode() {
        PostalNetwork network = new PostalNetwork();
        UUID firstHub = register(network, 0, 0);
        UUID secondHub = register(network, 48, 0);
        PostalDistrict first = network.createDistrict(firstHub, PostalActionContext.empty()).orElseThrow();
        PostalDistrict second = network.createDistrict(secondHub, PostalActionContext.empty()).orElseThrow();
        PostalChunk chunk0 = new PostalChunk(OVERWORLD, 0, 0);
        PostalChunk chunk1 = new PostalChunk(OVERWORLD, 1, 0);
        PostalChunk chunk2 = new PostalChunk(OVERWORLD, 2, 0);
        PostalChunk chunk3 = new PostalChunk(OVERWORLD, 3, 0);

        assertEquals(
                "message.everechoes.coverage.disconnected",
                network.validateCoverageReplacement(first.districtId(), Set.of(chunk0, chunk2), PostalActionContext.empty()).reasonKey()
        );
        assertEquals(
                "message.everechoes.coverage.unexplored",
                network.validateCoverageReplacement(first.districtId(), Set.of(chunk0, chunk1), PostalActionContext.empty()).reasonKey()
        );

        network.recordExplored(chunk1);
        network.recordExplored(chunk2);
        int originalRevision = network.coverage(first.districtId()).orElseThrow().revision();
        network.replaceDistrictCoverage(first.districtId(), Set.of(chunk0, chunk1, chunk2), originalRevision, PostalActionContext.empty()).orElseThrow();
        assertEquals(
                "message.everechoes.coverage.stale_revision",
                network.validateCoverageReplacement(first.districtId(), Set.of(chunk0, chunk1), originalRevision, PostalActionContext.empty()).reasonKey()
        );
        network.recordExplored(chunk3);
        assertTrue(network.replaceDistrictCoverage(second.districtId(), Set.of(chunk1, chunk2, chunk3), PostalActionContext.empty()).isPresent());
        assertEquals(2, network.districtsAt(chunk1).size());

        UUID collection = register(network, 20, 0);
        network.joinDistrictAsCollection(collection, first.districtId(), context(collection, 20, 0)).orElseThrow();
        assertTrue(network.replaceDistrictCoverage(first.districtId(), Set.of(chunk0), PostalActionContext.empty()).isEmpty());
    }

    @Test
    void domainCoverageIsDerivedFromActiveServedDistricts() {
        PostalNetwork network = new PostalNetwork();
        UUID firstHub = register(network, 0, 0);
        UUID secondHub = register(network, 48, 0);
        PostalDistrict first = network.createDistrict(firstHub, PostalActionContext.empty()).orElseThrow();
        PostalDistrict second = network.createDistrict(secondHub, PostalActionContext.empty()).orElseThrow();
        PostalDomain domain = network.establishAndJoin(first.districtId(), "ev", PostalActionContext.empty(), 0L).orElseThrow();
        network.requestJoin(second.districtId(), domain.domainId(), 0L).orElseThrow();

        assertEquals(2, network.domainCoverage(domain.domainId()).size());
        network.leaveDomain(second.districtId()).orElseThrow();
        assertEquals(Set.of(new PostalChunk(OVERWORLD, 0, 0)), network.domainCoverage(domain.domainId()));
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
        assertTrue(network.district(district.districtId()).isEmpty());
        assertTrue(network.coverage(district.districtId()).isEmpty());
        assertEquals(MembershipState.DETACHED, network.membership(district.districtId()).orElseThrow().state());
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
                new DeniedMembershipPolicy(),
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
        assertTrue(network.establishDomain("EV").isEmpty());
    }

    @Test
    void activeDomainCannotRetireUntilEveryDistrictDetaches() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        PostalDomain domain = network.establishAndJoin(district.districtId(), "ev", PostalActionContext.empty(), 0L).orElseThrow();

        assertTrue(network.retireDomain(domain.domainId()).isEmpty());
        assertTrue(network.isActiveDeliveryDistrict(district.districtId()));

        network.leaveDomain(district.districtId()).orElseThrow();
        assertTrue(network.retireDomain(domain.domainId()).isPresent());
        assertFalse(network.isActiveDeliveryDistrict(district.districtId()));
    }

    @Test
    void unknownSavedMembershipPolicyFailsClosed() {
        PostalNetwork original = new PostalNetwork();
        UUID hub = register(original, 0, 0);
        PostalDistrict district = original.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        PostalDomain domain = original.establishDomain("ev").orElseThrow();
        CompoundTag tag = original.save(new CompoundTag(), null);
        tag.getList("domains", Tag.TAG_COMPOUND).getCompound(0).putString("membershipPolicyId", "missing-policy");

        PostalNetwork loaded = PostalNetwork.load(tag, null);
        assertTrue(loaded.requestJoin(district.districtId(), domain.domainId(), 0L).isEmpty());
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
        assertEquals(Set.of(new PostalChunk(OVERWORLD, 0, 0)), loaded.coverage(district.districtId()).orElseThrow().chunks());
        assertTrue(loaded.isExplored(new PostalChunk(OVERWORLD, 0, 0)));
        assertEquals(List.of(district.districtId()), loaded.districtsAt(new PostalChunk(OVERWORLD, 0, 0)).stream().map(PostalDistrict::districtId).toList());
    }

    @Test
    void overlappingCoverageSurvivesSaveAndLoad() {
        PostalNetwork original = new PostalNetwork();
        UUID firstHub = register(original, 0, 0);
        UUID secondHub = register(original, 10, 0);
        PostalDistrict first = original.createDistrict(firstHub, PostalActionContext.empty()).orElseThrow();
        PostalDistrict second = original.createDistrict(secondHub, PostalActionContext.empty()).orElseThrow();
        PostalChunk shared = new PostalChunk(OVERWORLD, 0, 0);
        assertEquals(2, original.districtsAt(shared).size());

        PostalNetwork loaded = PostalNetwork.load(original.save(new CompoundTag(), null), null);
        List<UUID> loadedIds = loaded.districtsAt(shared).stream().map(PostalDistrict::districtId).toList();
        assertEquals(2, loadedIds.size());
        assertTrue(loadedIds.contains(first.districtId()));
        assertTrue(loadedIds.contains(second.districtId()));
        loaded.rebuildCoverageIndex();
        assertEquals(2, loaded.districtsAt(shared).size());
    }

    @Test
    void coverageIndexTracksReplaceDeleteAndRebuild() {
        PostalNetwork network = new PostalNetwork();
        UUID firstHub = register(network, 0, 0);
        UUID secondHub = register(network, 48, 0);
        PostalDistrict first = network.createDistrict(firstHub, PostalActionContext.empty()).orElseThrow();
        PostalDistrict second = network.createDistrict(secondHub, PostalActionContext.empty()).orElseThrow();
        PostalChunk chunk0 = new PostalChunk(OVERWORLD, 0, 0);
        PostalChunk chunk1 = new PostalChunk(OVERWORLD, 1, 0);
        PostalChunk chunk2 = new PostalChunk(OVERWORLD, 2, 0);
        PostalChunk chunk3 = new PostalChunk(OVERWORLD, 3, 0);
        network.recordExplored(chunk1);
        network.recordExplored(chunk2);
        network.recordExplored(chunk3);
        network.replaceDistrictCoverage(first.districtId(), Set.of(chunk0, chunk1, chunk2), PostalActionContext.empty()).orElseThrow();
        network.replaceDistrictCoverage(second.districtId(), Set.of(chunk1, chunk2, chunk3), PostalActionContext.empty()).orElseThrow();

        assertTrue(network.districtsAt(chunk0).stream().anyMatch(district -> district.districtId().equals(first.districtId())));
        assertFalse(network.districtsAt(chunk0).stream().anyMatch(district -> district.districtId().equals(second.districtId())));
        network.replaceDistrictCoverage(first.districtId(), Set.of(chunk0, chunk1), PostalActionContext.empty()).orElseThrow();
        assertFalse(network.districtsAt(chunk2).stream().anyMatch(district -> district.districtId().equals(first.districtId())));
        assertTrue(network.districtsAt(chunk2).stream().anyMatch(district -> district.districtId().equals(second.districtId())));

        network.removeNode(firstHub);
        assertTrue(network.coverage(first.districtId()).isEmpty());
        assertTrue(network.districtsAt(chunk1).stream().anyMatch(district -> district.districtId().equals(second.districtId())));
        assertFalse(network.districtsAt(chunk1).stream().anyMatch(district -> district.districtId().equals(first.districtId())));

        Set<UUID> before = network.districtsAt(chunk1).stream().map(PostalDistrict::districtId).collect(java.util.stream.Collectors.toSet());
        network.rebuildCoverageIndex();
        network.rebuildCoverageIndex();
        assertEquals(before, network.districtsAt(chunk1).stream().map(PostalDistrict::districtId).collect(java.util.stream.Collectors.toSet()));

        List<PostalDistrict> snapshot = network.districtsAt(chunk1);
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> snapshot.add(second));
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
        return new PostalActionContext(null, nodeId, UUID.randomUUID(), OVERWORLD, new BlockPos(x, 64, z));
    }

    private static void mapLine(PostalNetwork network, UUID districtId, int firstChunkX, int lastChunkX) {
        Set<PostalChunk> chunks = new LinkedHashSet<>();
        for (int x = firstChunkX; x <= lastChunkX; x++) {
            PostalChunk chunk = new PostalChunk(OVERWORLD, x, 0);
            network.recordExplored(chunk);
            chunks.add(chunk);
        }
        network.replaceDistrictCoverage(districtId, chunks, PostalActionContext.empty()).orElseThrow();
    }

    private static final class PendingMembershipPolicy implements DomainMembershipPolicy {
        @Override
        public String policyId() {
            return "pending-test";
        }

        @Override
        public PolicyDecision canJoin(PostalNetwork network, DomainJoinRequest request) {
            return PolicyDecision.allow();
        }

        @Override
        public boolean autoActivate() {
            return false;
        }
    }

    private static final class DeniedMembershipPolicy implements DomainMembershipPolicy {
        @Override
        public String policyId() {
            return "denied-test";
        }

        @Override
        public PolicyDecision canJoin(PostalNetwork network, DomainJoinRequest request) {
            return PolicyDecision.deny("denied");
        }
    }
}
