package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 封装相关行为的自动化回归测试。
 */
class PostalNetworkTest {
    /**
     * 定义 {@code OVERWORLD} 常量。
     */
    private static final ResourceLocation OVERWORLD = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    /**
     * 定义 {@code NETHER} 常量。
     */
    private static final ResourceLocation NETHER = ResourceLocation.fromNamespaceAndPath("minecraft", "the_nether");

    /**
     * 执行 {@code domainHasNoOwnerAndSeparatesInternalIdFromDisplayCode} 对应的业务操作。
     */
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

    /**
     * 执行 {@code districtCanContainMultiplePostBoxesWithoutNewNumber} 对应的业务操作。
     */
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

    /**
     * 执行 {@code overlappingDistrictsAreAllReturnedAndDoNotCreateProxyLinks} 对应的业务操作。
     */
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

        List<UUID> ordered = covering.stream().map(PostalDistrict::districtId).toList();
        assertEquals(ordered.stream().sorted().toList(), ordered);
    }

    /**
     * 执行 {@code duplicateNodeIdentityMintsANewIdWithoutMovingTheOriginal} 对应的业务操作。
     */
    @Test
    void duplicateNodeIdentityMintsANewIdWithoutMovingTheOriginal() {
        PostalNetwork network = new PostalNetwork();
        UUID originalId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        PostBoxNode original = network.registerNode(originalId, OVERWORLD, new BlockPos(0, 64, 0));
        network.setDirty(false);

        assertEquals(original, network.registerNode(originalId, OVERWORLD, new BlockPos(0, 64, 0)));
        assertFalse(network.isDirty());

        PostBoxNode duplicate = network.registerNode(originalId, OVERWORLD, new BlockPos(32, 64, 0));
        assertNotEquals(originalId, duplicate.nodeId());
        assertEquals(new BlockPos(0, 64, 0), network.node(originalId).orElseThrow().position());
        assertEquals(new BlockPos(32, 64, 0), network.node(duplicate.nodeId()).orElseThrow().position());
        assertTrue(network.isDirty());
    }

    /**
     * 执行 {@code configurationPermissionIsExposedForEveryGuiAction} 对应的业务操作。
     */
    @Test
    void configurationPermissionIsExposedForEveryGuiAction() {
        PostalNetwork denied = new PostalNetwork(
                OpenDomainPolicies.CREATION,
                OpenDomainPolicies.MEMBERSHIP,
                OpenDomainPolicies.EXIT,
                CoveredNodeAdmissionPolicy.INSTANCE,
                context -> PolicyDecision.deny("denied")
        );
        assertFalse(denied.canConfigure(PostalActionContext.empty()).allowed());
        assertEquals("denied", denied.canConfigure(PostalActionContext.empty()).reasonKey());
    }

    /**
     * 执行 {@code coveragePolicyRejectsOutsideAndImplicitJoins} 对应的业务操作。
     */
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

    /**
     * 执行 {@code mappedCoverageControlsDiscoveryAndAdmission} 对应的业务操作。
     */
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

    /**
     * 执行 {@code coverageMustBeConnectedAndKeepEveryNode} 对应的业务操作。
     */
    @Test
    void coverageMustBeConnectedAndKeepEveryNode() {
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
        assertTrue(network.validateCoverageReplacement(
                first.districtId(),
                Set.of(chunk0, chunk1),
                PostalActionContext.empty()
        ).allowed());

        int originalRevision = network.coverage(first.districtId()).orElseThrow().revision();
        network.replaceDistrictCoverage(first.districtId(), Set.of(chunk0, chunk1, chunk2), originalRevision, PostalActionContext.empty()).orElseThrow();
        assertEquals(
                "message.everechoes.coverage.stale_revision",
                network.validateCoverageReplacement(first.districtId(), Set.of(chunk0, chunk1), originalRevision, PostalActionContext.empty()).reasonKey()
        );
        assertTrue(network.replaceDistrictCoverage(second.districtId(), Set.of(chunk1, chunk2, chunk3), PostalActionContext.empty()).isPresent());
        assertEquals(2, network.districtsAt(chunk1).size());

        UUID collection = register(network, 20, 0);
        network.joinDistrictAsCollection(collection, first.districtId(), context(collection, 20, 0)).orElseThrow();
        assertTrue(network.replaceDistrictCoverage(first.districtId(), Set.of(chunk0), PostalActionContext.empty()).isEmpty());
    }

    /**
     * 执行 {@code domainCoverageIsDerivedFromActiveServedDistricts} 对应的业务操作。
     */
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

    /**
     * 执行 {@code collectionLeavingDoesNotDetachDistrict} 对应的业务操作。
     */
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

    /**
     * 执行 {@code hubRemovalPromotesCollectionAndLastNodeStopsService} 对应的业务操作。
     */
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

    /**
     * 执行 {@code pendingCanBeApprovedOrRejected} 对应的业务操作。
     */
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

    /**
     * 移除或退出 {@code leaveDomainUsesServiceAndOpenPolicyCompletesImmediately} 对应的业务操作。
     */
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

    /**
     * 执行 {@code establishAndJoinFailureLeavesNoDomain} 对应的业务操作。
     */
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

    /**
     * 执行 {@code retireDomainMakesHistorical} 对应的业务操作。
     */
    @Test
    void retireDomainMakesHistorical() {
        PostalNetwork network = new PostalNetwork();
        PostalDomain domain = network.establishDomain("ev").orElseThrow();
        assertEquals(DomainLifecycle.HISTORICAL, network.retireDomain(domain.domainId()).orElseThrow().lifecycle());
        assertTrue(network.findLiveDomainByCode("EV").isEmpty());
        assertTrue(network.establishDomain("EV").isEmpty());
    }

    /**
     * 执行 {@code activeDomainCannotRetireUntilEveryDistrictDetaches} 对应的业务操作。
     */
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

    /**
     * 执行 {@code unknownSavedMembershipPolicyFailsClosed} 对应的业务操作。
     */
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

    /**
     * 保存或编码 {@code saveAndLoadPreservesDistrictNodesAndMembership} 对应的业务操作。
     */
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
        assertEquals(List.of(district.districtId()), loaded.districtsAt(new PostalChunk(OVERWORLD, 0, 0)).stream().map(PostalDistrict::districtId).toList());
    }

    /**
     * 执行 {@code overlappingCoverageSurvivesSaveAndLoad} 对应的业务操作。
     */
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

    /**
     * 执行 {@code coverageIndexTracksReplaceDeleteAndRebuild} 对应的业务操作。
     */
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

    /**
     * 执行 {@code unsupportedSchemaLoadsReadOnlyAndPreservesRawData} 对应的业务操作。
     */
    @Test
    void unsupportedSchemaLoadsReadOnlyAndPreservesRawData() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schemaVersion", 3);
        PostalNetwork network = PostalNetwork.load(tag, null);
        assertTrue(network.liveDomainCodes().isEmpty());
        assertFalse(network.isDirty());
        PolicyDecision permission = network.canConfigure(PostalActionContext.empty());
        assertFalse(permission.allowed());
        assertEquals("message.everechoes.postal.unsupported_schema", permission.reasonKey());
        UUID transientNodeId = UUID.randomUUID();
        PostBoxNode transientNode = network.registerNode(transientNodeId, OVERWORLD, BlockPos.ZERO);
        assertEquals(transientNodeId, transientNode.nodeId());
        assertTrue(network.node(transientNodeId).isEmpty());
        assertFalse(network.isDirty());
        CompoundTag saved = network.save(new CompoundTag(), null);
        assertEquals(3, saved.getInt("schemaVersion"));
    }

    /**
     * 执行 {@code malformedEnumValuesAreSkippedInsteadOfCrashingTheWorldLoad} 对应的业务操作。
     */
    @Test
    void malformedEnumValuesAreSkippedInsteadOfCrashingTheWorldLoad() {
        PostalNetwork original = new PostalNetwork();
        UUID hub = register(original, 0, 0);
        PostalDistrict district = original.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        original.establishAndJoin(district.districtId(), "ev", PostalActionContext.empty(), 0L).orElseThrow();

        CompoundTag badDomain = original.save(new CompoundTag(), null).copy();
        badDomain.getList("domains", Tag.TAG_COMPOUND).getCompound(0).putString("lifecycle", "BROKEN");
        assertTrue(PostalNetwork.load(badDomain, null).liveDomainCodes().isEmpty());

        CompoundTag badMembership = original.save(new CompoundTag(), null).copy();
        badMembership.getList("memberships", Tag.TAG_COMPOUND).getCompound(0).putString("state", "BROKEN");
        assertTrue(PostalNetwork.load(badMembership, null).membership(district.districtId()).isEmpty());

        CompoundTag badNode = original.save(new CompoundTag(), null).copy();
        badNode.getList("nodes", Tag.TAG_COMPOUND).getCompound(0).putString("role", "BROKEN");
        assertTrue(PostalNetwork.load(badNode, null).node(hub).isEmpty());
    }

    /**
     * 执行 {@code malformedUuidValuesAreSkippedInsteadOfCrashingTheWorldLoad} 对应的业务操作。
     */
    @Test
    void malformedUuidValuesAreSkippedInsteadOfCrashingTheWorldLoad() {
        PostalNetwork original = new PostalNetwork();
        UUID hub = register(original, 0, 0);
        PostalDistrict district = original.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        original.establishAndJoin(district.districtId(), "ev", PostalActionContext.empty(), 0L).orElseThrow();

        CompoundTag malformed = original.save(new CompoundTag(), null).copy();
        malformed.getList("domains", Tag.TAG_COMPOUND).getCompound(0).putString("domainId", "not-a-uuid");
        malformed.getList("districts", Tag.TAG_COMPOUND).getCompound(0).putString("districtId", "not-a-uuid");
        malformed.getList("memberships", Tag.TAG_COMPOUND).getCompound(0).remove("domainId");
        malformed.getList("nodes", Tag.TAG_COMPOUND).getCompound(0).putInt("nodeId", 1);
        malformed.getList("coverages", Tag.TAG_COMPOUND).getCompound(0).remove("districtId");

        PostalNetwork loaded = PostalNetwork.load(malformed, null);
        assertTrue(loaded.liveDomainCodes().isEmpty());
        assertTrue(loaded.district(district.districtId()).isEmpty());
        assertTrue(loaded.membership(district.districtId()).isEmpty());
        assertTrue(loaded.node(hub).isEmpty());
        assertTrue(loaded.coverage(district.districtId()).isEmpty());
    }

    /**
     * 执行 {@code districtsDoNotReuseActiveCodes} 对应的业务操作。
     */
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

    /**
     * 执行 {@code registerEndpointKeepsMailboxIdAndRevivesOfflineAtSamePos} 对应的业务操作。
     */
    @Test
    void registerEndpointKeepsMailboxIdAndRevivesOfflineAtSamePos() {
        PostalNetwork network = new PostalNetwork();
        UUID mailboxId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        BlockPos origin = new BlockPos(0, 64, 0);

        assertFalse(network.isDirty());
        MailBoxEndpoint created = network.registerEndpoint(mailboxId, OVERWORLD, origin);
        assertEquals(mailboxId, created.mailboxId());
        assertEquals(MailBoxEndpointState.ONLINE, created.state());
        assertFalse(created.isBound());
        assertEquals(origin, created.position());
        assertEquals(OVERWORLD, created.dimension());
        assertSame(created, network.mailbox(mailboxId).orElseThrow());
        assertTrue(network.isDirty());

        network.setDirty(false);
        assertSame(created, network.registerEndpoint(mailboxId, OVERWORLD, origin));
        assertFalse(network.isDirty());

        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        network.establishAndJoin(district.districtId(), "ev", PostalActionContext.empty(), 0L).orElseThrow();
        MailBoxEndpoint bound = network.bindEndpoint(mailboxId, district.districtId(), PostalActionContext.empty()).orElseThrow();
        assertEquals("0AA", bound.deliveryCode());
        network.markEndpointOffline(mailboxId);
        assertEquals(MailBoxEndpointState.OFFLINE, network.mailbox(mailboxId).orElseThrow().state());

        network.setDirty(false);
        MailBoxEndpoint revived = network.registerEndpoint(mailboxId, OVERWORLD, origin);
        assertEquals(mailboxId, revived.mailboxId());
        assertEquals(MailBoxEndpointState.ONLINE, revived.state());
        assertEquals(district.districtId(), revived.districtId());
        assertEquals("0AA", revived.deliveryCode());
        assertTrue(revived.isBound());
        assertTrue(network.isDirty());

        MailBoxEndpoint moved = network.registerEndpoint(mailboxId, NETHER, new BlockPos(32, 70, 16));
        assertEquals(mailboxId, moved.mailboxId());
        assertEquals(NETHER, moved.dimension());
        assertEquals(new BlockPos(32, 70, 16), moved.position());
        assertEquals(district.districtId(), moved.districtId());
        assertEquals("0AA", moved.deliveryCode());
        assertEquals(MailBoxEndpointState.ONLINE, moved.state());

        network.markEndpointOffline(mailboxId);
        MailBoxEndpoint relocatedOffline = network.registerEndpoint(mailboxId, OVERWORLD, new BlockPos(48, 64, 0));
        assertEquals(mailboxId, relocatedOffline.mailboxId());
        assertEquals(OVERWORLD, relocatedOffline.dimension());
        assertEquals(new BlockPos(48, 64, 0), relocatedOffline.position());
        assertEquals(MailBoxEndpointState.OFFLINE, relocatedOffline.state());
        assertEquals(district.districtId(), relocatedOffline.districtId());
        assertEquals("0AA", relocatedOffline.deliveryCode());
    }

    /**
     * 执行 {@code registerEndpointOnUnsupportedSchemaIsEphemeral} 对应的业务操作。
     */
    @Test
    void registerEndpointOnUnsupportedSchemaIsEphemeral() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schemaVersion", 3);
        PostalNetwork network = PostalNetwork.load(tag, null);
        UUID mailboxId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        MailBoxEndpoint ephemeral = network.registerEndpoint(mailboxId, OVERWORLD, BlockPos.ZERO);
        assertEquals(mailboxId, ephemeral.mailboxId());
        assertEquals(MailBoxEndpointState.ONLINE, ephemeral.state());
        assertFalse(ephemeral.isBound());
        assertTrue(network.mailbox(mailboxId).isEmpty());
        assertFalse(network.isDirty());
        CompoundTag saved = network.save(new CompoundTag(), null);
        assertEquals(3, saved.getInt("schemaVersion"));
    }

    /**
     * 执行 {@code bindEndpointRequiresExplicitDistrictAndActiveMembership} 对应的业务操作。
     */
    @Test
    void bindEndpointRequiresExplicitDistrictAndActiveMembership() {
        PostalNetwork denied = new PostalNetwork(
                OpenDomainPolicies.CREATION,
                OpenDomainPolicies.MEMBERSHIP,
                OpenDomainPolicies.EXIT,
                CoveredNodeAdmissionPolicy.INSTANCE,
                context -> PolicyDecision.deny("denied")
        );
        UUID deniedMailbox = UUID.randomUUID();
        denied.registerEndpoint(deniedMailbox, OVERWORLD, new BlockPos(0, 64, 0));
        assertTrue(denied.bindEndpoint(deniedMailbox, UUID.randomUUID(), PostalActionContext.empty()).isEmpty());

        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        network.establishAndJoin(district.districtId(), "ev", PostalActionContext.empty(), 0L).orElseThrow();
        UUID mailboxId = registerMailbox(network, 0, 0);
        assertEquals(1, network.districtsAt(OVERWORLD, new BlockPos(0, 64, 0)).size());

        assertTrue(network.bindEndpoint(UUID.randomUUID(), district.districtId(), PostalActionContext.empty()).isEmpty());

        MailBoxEndpoint first = network.bindEndpoint(mailboxId, district.districtId(), PostalActionContext.empty()).orElseThrow();
        assertEquals("0AA", first.deliveryCode());
        assertEquals(district.districtId(), first.districtId());
        assertTrue(network.isDirty());

        network.setDirty(false);
        assertSame(first, network.bindEndpoint(mailboxId, district.districtId(), PostalActionContext.empty()).orElseThrow());
        assertFalse(network.isDirty());

        UUID secondMailbox = registerMailbox(network, 8, 0);
        MailBoxEndpoint second = network.bindEndpoint(secondMailbox, district.districtId(), PostalActionContext.empty()).orElseThrow();
        assertEquals("0AB", second.deliveryCode());

        network.markEndpointOffline(secondMailbox);
        assertTrue(network.bindEndpoint(secondMailbox, district.districtId(), PostalActionContext.empty()).isEmpty());
        UUID thirdMailbox = registerMailbox(network, 12, 0);
        assertEquals("0AD", network.bindEndpoint(thirdMailbox, district.districtId(), PostalActionContext.empty()).orElseThrow().deliveryCode());
        assertEquals("0AB", network.mailbox(secondMailbox).orElseThrow().deliveryCode());
        assertEquals(MailBoxEndpointState.OFFLINE, network.mailbox(secondMailbox).orElseThrow().state());

        UUID farMailbox = registerMailbox(network, 300, 0);
        assertTrue(network.bindEndpoint(farMailbox, district.districtId(), PostalActionContext.empty()).isEmpty());
        assertTrue(network.bindEndpoint(mailboxId, UUID.randomUUID(), PostalActionContext.empty()).isEmpty());
        assertEquals(district.districtId(), network.mailbox(mailboxId).orElseThrow().districtId());

        PostalNetwork pendingNetwork = new PostalNetwork(
                OpenDomainPolicies.CREATION,
                new PendingMembershipPolicy(),
                OpenDomainPolicies.EXIT
        );
        UUID pendingHub = register(pendingNetwork, 0, 0);
        PostalDistrict pendingDistrict = pendingNetwork.createDistrict(pendingHub, PostalActionContext.empty()).orElseThrow();
        PostalDomain pendingDomain = pendingNetwork.establishDomain("ev").orElseThrow();
        assertEquals(MembershipState.PENDING, pendingNetwork.requestJoin(
                pendingDistrict.districtId(), pendingDomain.domainId(), 0L).orElseThrow().state());
        UUID pendingMailbox = registerMailbox(pendingNetwork, 0, 0);
        assertTrue(pendingNetwork.bindEndpoint(
                pendingMailbox, pendingDistrict.districtId(), PostalActionContext.empty()).isEmpty());

        PostalNetwork unjoined = new PostalNetwork();
        UUID unjoinedHub = register(unjoined, 0, 0);
        PostalDistrict unjoinedDistrict = unjoined.createDistrict(unjoinedHub, PostalActionContext.empty()).orElseThrow();
        UUID unjoinedMailbox = registerMailbox(unjoined, 0, 0);
        assertTrue(unjoined.bindEndpoint(
                unjoinedMailbox, unjoinedDistrict.districtId(), PostalActionContext.empty()).isEmpty());

        UUID secondHub = register(network, 10, 0);
        PostalDistrict overlap = network.createDistrict(secondHub, PostalActionContext.empty()).orElseThrow();
        network.establishAndJoin(overlap.districtId(), "ab", PostalActionContext.empty(), 0L).orElseThrow();
        assertEquals(2, network.districtsAt(OVERWORLD, new BlockPos(0, 64, 0)).size());
        assertTrue(network.bindEndpoint(mailboxId, overlap.districtId(), PostalActionContext.empty()).isEmpty());
        assertEquals(district.districtId(), network.mailbox(mailboxId).orElseThrow().districtId());

        UUID overlapMailbox = registerMailbox(network, 4, 0);
        MailBoxEndpoint boundToOverlap = network.bindEndpoint(
                overlapMailbox, overlap.districtId(), PostalActionContext.empty()).orElseThrow();
        assertEquals(overlap.districtId(), boundToOverlap.districtId());
        assertEquals("0AA", boundToOverlap.deliveryCode());
        assertEquals(district.districtId(), network.mailbox(mailboxId).orElseThrow().districtId());
        assertEquals("0AA", network.mailbox(mailboxId).orElseThrow().deliveryCode());
    }

    /**
     * 执行 {@code markEndpointOfflineRemovesUnboundAndReservesBoundCode} 对应的业务操作。
     */
    @Test
    void markEndpointOfflineRemovesUnboundAndReservesBoundCode() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        network.establishAndJoin(district.districtId(), "ev", PostalActionContext.empty(), 0L).orElseThrow();

        UUID unboundId = registerMailbox(network, 0, 0);
        network.setDirty(false);
        network.markEndpointOffline(unboundId);
        assertTrue(network.mailbox(unboundId).isEmpty());
        assertTrue(network.isDirty());

        UUID boundId = registerMailbox(network, 8, 0);
        MailBoxEndpoint bound = network.bindEndpoint(boundId, district.districtId(), PostalActionContext.empty()).orElseThrow();
        network.markEndpointOffline(boundId);
        MailBoxEndpoint offline = network.mailbox(boundId).orElseThrow();
        assertEquals(MailBoxEndpointState.OFFLINE, offline.state());
        assertEquals(bound.districtId(), offline.districtId());
        assertEquals(bound.deliveryCode(), offline.deliveryCode());

        network.setDirty(false);
        network.markEndpointOffline(UUID.randomUUID());
        assertFalse(network.isDirty());
        assertEquals(MailBoxEndpointState.OFFLINE, network.mailbox(boundId).orElseThrow().state());
    }

    /**
     * 执行 {@code canDeliverToRequiresOnlineBoundActiveDistrict} 对应的业务操作。
     */
    @Test
    void canDeliverToRequiresOnlineBoundActiveDistrict() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        network.establishAndJoin(district.districtId(), "ev", PostalActionContext.empty(), 0L).orElseThrow();

        UUID onlineId = registerMailbox(network, 0, 0);
        network.bindEndpoint(onlineId, district.districtId(), PostalActionContext.empty()).orElseThrow();
        MailBoxAddress address = MailBoxAddress.display("EV", "1", "0AA");
        assertTrue(network.canDeliverTo(onlineId));
        assertTrue(network.canDeliverTo(address));

        UUID unboundId = registerMailbox(network, 8, 0);
        assertFalse(network.canDeliverTo(unboundId));

        UUID offlineId = registerMailbox(network, 12, 0);
        network.bindEndpoint(offlineId, district.districtId(), PostalActionContext.empty()).orElseThrow();
        network.markEndpointOffline(offlineId);
        assertFalse(network.canDeliverTo(offlineId));
        assertFalse(network.canDeliverTo(MailBoxAddress.display("EV", "1", "0AB")));

        network.removeNode(hub);
        assertEquals(MembershipState.DETACHED, network.membership(district.districtId()).orElseThrow().state());
        assertFalse(network.canDeliverTo(onlineId));
        assertFalse(network.canDeliverTo(address));
    }

    /**
     * 执行 {@code resolveAddressFillsMailboxIdForOnlineOrOffline} 对应的业务操作。
     */
    @Test
    void resolveAddressFillsMailboxIdForOnlineOrOffline() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        PostalDomain domain = network.establishAndJoin(district.districtId(), "ev", PostalActionContext.empty(), 0L).orElseThrow();
        UUID mailboxId = registerMailbox(network, 0, 0);
        assertEquals("0AA", network.bindEndpoint(mailboxId, district.districtId(), PostalActionContext.empty())
                .orElseThrow().deliveryCode());
        assertEquals("EV", domain.domainCode());
        assertEquals("1", network.membership(district.districtId()).orElseThrow().districtCode());

        MailBoxAddress online = network.resolveAddress(MailBoxAddress.display("EV", "1", "0AA"));
        assertEquals(mailboxId, online.mailboxId().orElseThrow());
        assertEquals(domain.domainId(), online.domainId().orElseThrow());
        assertEquals(district.districtId(), online.districtId().orElseThrow());

        network.markEndpointOffline(mailboxId);
        MailBoxAddress offline = network.resolveAddress(MailBoxAddress.display("ev", "1", "0aa"));
        assertEquals(mailboxId, offline.mailboxId().orElseThrow());
        assertEquals(domain.domainId(), offline.domainId().orElseThrow());
        assertEquals(district.districtId(), offline.districtId().orElseThrow());

        MailBoxAddress unknown = network.resolveAddress(MailBoxAddress.display("EV", "1", "9ZZ"));
        assertTrue(unknown.mailboxId().isEmpty());
        assertEquals(domain.domainId(), unknown.domainId().orElseThrow());
        assertEquals(district.districtId(), unknown.districtId().orElseThrow());
    }

    /**
     * 执行 {@code coverageReplacementDeniesOnlineBoundMailboxExclusion} 对应的业务操作。
     */
    @Test
    void coverageReplacementDeniesOnlineBoundMailboxExclusion() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        network.establishAndJoin(district.districtId(), "ev", PostalActionContext.empty(), 0L).orElseThrow();
        PostalChunk chunk0 = new PostalChunk(OVERWORLD, 0, 0);
        PostalChunk chunk1 = new PostalChunk(OVERWORLD, 1, 0);
        network.replaceDistrictCoverage(district.districtId(), Set.of(chunk0, chunk1), PostalActionContext.empty()).orElseThrow();

        UUID onlineBound = registerMailbox(network, 20, 0);
        network.bindEndpoint(onlineBound, district.districtId(), PostalActionContext.empty()).orElseThrow();
        assertEquals(
                "message.everechoes.coverage.excludes_node",
                network.validateCoverageReplacement(district.districtId(), Set.of(chunk0), PostalActionContext.empty()).reasonKey()
        );

        network.markEndpointOffline(onlineBound);
        assertTrue(network.validateCoverageReplacement(
                district.districtId(), Set.of(chunk0), PostalActionContext.empty()).allowed());

        UUID unbound = registerMailbox(network, 24, 0);
        assertFalse(network.mailbox(unbound).orElseThrow().isBound());
        assertTrue(network.validateCoverageReplacement(
                district.districtId(), Set.of(chunk0), PostalActionContext.empty()).allowed());

        UUID collection = register(network, 20, 0);
        network.joinDistrictAsCollection(collection, district.districtId(), context(collection, 20, 0)).orElseThrow();
        assertEquals(
                "message.everechoes.coverage.excludes_node",
                network.validateCoverageReplacement(district.districtId(), Set.of(chunk0), PostalActionContext.empty()).reasonKey()
        );
    }

    /**
     * 执行 {@code lastPostBoxRemovalUnbindsOnlineAndDropsOfflineMailboxes} 对应的业务操作。
     */
    @Test
    void lastPostBoxRemovalUnbindsOnlineAndDropsOfflineMailboxes() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        network.establishAndJoin(district.districtId(), "ev", PostalActionContext.empty(), 0L).orElseThrow();

        UUID onlineId = registerMailbox(network, 0, 0);
        UUID offlineId = registerMailbox(network, 8, 0);
        network.bindEndpoint(onlineId, district.districtId(), PostalActionContext.empty()).orElseThrow();
        network.bindEndpoint(offlineId, district.districtId(), PostalActionContext.empty()).orElseThrow();
        network.markEndpointOffline(offlineId);

        PostalDistrict stored = network.district(district.districtId()).orElseThrow();
        assertFalse(stored.nodeIds().contains(onlineId));
        assertFalse(stored.nodeIds().contains(offlineId));
        assertEquals(List.of(hub), stored.nodeIds());

        network.removeNode(hub);
        MailBoxEndpoint unboundOnline = network.mailbox(onlineId).orElseThrow();
        assertFalse(unboundOnline.isBound());
        assertEquals(MailBoxEndpointState.ONLINE, unboundOnline.state());
        assertTrue(network.mailbox(offlineId).isEmpty());
        assertTrue(network.district(district.districtId()).isEmpty());
    }

    /**
     * 执行 {@code saveAndLoadPreservesMailboxesAndSkipsHalfBound} 对应的业务操作。
     */
    @Test
    void saveAndLoadPreservesMailboxesAndSkipsHalfBound() {
        assertEquals(5, PostalNetwork.SCHEMA_VERSION);

        PostalNetwork original = new PostalNetwork();
        UUID hub = register(original, 0, 0);
        PostalDistrict district = original.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        original.establishAndJoin(district.districtId(), "ev", PostalActionContext.empty(), 0L).orElseThrow();
        UUID onlineBound = registerMailbox(original, 0, 0);
        UUID offlineBound = registerMailbox(original, 8, 0);
        UUID unbound = registerMailbox(original, 12, 0);
        original.bindEndpoint(onlineBound, district.districtId(), PostalActionContext.empty()).orElseThrow();
        original.bindEndpoint(offlineBound, district.districtId(), PostalActionContext.empty()).orElseThrow();
        original.markEndpointOffline(offlineBound);

        CompoundTag tag = original.save(new CompoundTag(), null);
        assertEquals(PostalNetwork.SCHEMA_VERSION, tag.getInt("schemaVersion"));
        assertEquals(5, tag.getInt("schemaVersion"));
        PostalNetwork loaded = PostalNetwork.load(tag, null);
        MailBoxEndpoint loadedOnline = loaded.mailbox(onlineBound).orElseThrow();
        assertEquals(MailBoxEndpointState.ONLINE, loadedOnline.state());
        assertEquals(district.districtId(), loadedOnline.districtId());
        assertEquals("0AA", loadedOnline.deliveryCode());
        MailBoxEndpoint loadedOffline = loaded.mailbox(offlineBound).orElseThrow();
        assertEquals(MailBoxEndpointState.OFFLINE, loadedOffline.state());
        assertEquals(district.districtId(), loadedOffline.districtId());
        assertEquals("0AB", loadedOffline.deliveryCode());
        MailBoxEndpoint loadedUnbound = loaded.mailbox(unbound).orElseThrow();
        assertFalse(loadedUnbound.isBound());
        assertEquals(MailBoxEndpointState.ONLINE, loadedUnbound.state());

        CompoundTag missing = tag.copy();
        missing.remove("mailboxes");
        PostalNetwork withoutList = PostalNetwork.load(missing, null);
        assertTrue(withoutList.mailbox(onlineBound).isEmpty());
        assertTrue(withoutList.mailbox(offlineBound).isEmpty());
        assertTrue(withoutList.mailbox(unbound).isEmpty());

        CompoundTag halfBound = tag.copy();
        ListTag mailboxes = halfBound.getList("mailboxes", Tag.TAG_COMPOUND);
        UUID districtOnlyId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        CompoundTag districtOnly = new CompoundTag();
        districtOnly.putUUID("mailboxId", districtOnlyId);
        districtOnly.putUUID("districtId", district.districtId());
        districtOnly.putString("dimension", OVERWORLD.toString());
        districtOnly.putInt("x", 1);
        districtOnly.putInt("y", 64);
        districtOnly.putInt("z", 1);
        districtOnly.putString("state", "ONLINE");
        mailboxes.add(districtOnly);
        UUID codeOnlyId = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff");
        CompoundTag codeOnly = new CompoundTag();
        codeOnly.putUUID("mailboxId", codeOnlyId);
        codeOnly.putString("deliveryCode", "0BA");
        codeOnly.putString("dimension", OVERWORLD.toString());
        codeOnly.putInt("x", 2);
        codeOnly.putInt("y", 64);
        codeOnly.putInt("z", 2);
        codeOnly.putString("state", "ONLINE");
        mailboxes.add(codeOnly);
        PostalNetwork skipped = PostalNetwork.load(halfBound, null);
        assertTrue(skipped.mailbox(districtOnlyId).isEmpty());
        assertTrue(skipped.mailbox(codeOnlyId).isEmpty());
        assertTrue(skipped.mailbox(onlineBound).orElseThrow().isBound());

        CompoundTag invalidDelivery = tag.copy();
        ListTag invalidList = invalidDelivery.getList("mailboxes", Tag.TAG_COMPOUND);
        UUID invalidId = UUID.fromString("cccccccc-dddd-eeee-ffff-000000000000");
        CompoundTag invalid = new CompoundTag();
        invalid.putUUID("mailboxId", invalidId);
        invalid.putUUID("districtId", district.districtId());
        invalid.putString("deliveryCode", "0AC");
        invalid.putString("dimension", OVERWORLD.toString());
        invalid.putInt("x", 3);
        invalid.putInt("y", 64);
        invalid.putInt("z", 3);
        invalid.putString("state", "ONLINE");
        invalidList.add(invalid);
        PostalNetwork invalidLoaded = PostalNetwork.load(invalidDelivery, null);
        MailBoxEndpoint rebound = invalidLoaded.mailbox(invalidId).orElseThrow();
        assertFalse(rebound.isBound());
        assertEquals(MailBoxEndpointState.ONLINE, rebound.state());
        assertEquals(new BlockPos(3, 64, 3), rebound.position());
    }

    /**
     * 注册 {@code register} 对应的业务操作。
     */
    private static UUID register(PostalNetwork network, int x, int z) {
        UUID nodeId = UUID.randomUUID();
        network.registerNode(nodeId, OVERWORLD, new BlockPos(x, 64, z));
        return nodeId;
    }

    /**
     * 注册 {@code registerMailbox} 对应的业务操作。
     */
    private static UUID registerMailbox(PostalNetwork network, int x, int z) {
        UUID mailboxId = UUID.randomUUID();
        network.registerEndpoint(mailboxId, OVERWORLD, new BlockPos(x, 64, z));
        return mailboxId;
    }

    /**
     * 执行 {@code context} 对应的业务操作。
     */
    private static PostalActionContext context(UUID nodeId, int x, int z) {
        return new PostalActionContext(null, nodeId, UUID.randomUUID(), OVERWORLD, new BlockPos(x, 64, z));
    }

    /**
     * 执行 {@code mapLine} 对应的业务操作。
     */
    private static void mapLine(PostalNetwork network, UUID districtId, int firstChunkX, int lastChunkX) {
        Set<PostalChunk> chunks = new LinkedHashSet<>();
        for (int x = firstChunkX; x <= lastChunkX; x++) {
            chunks.add(new PostalChunk(OVERWORLD, x, 0));
        }
        network.replaceDistrictCoverage(districtId, chunks, PostalActionContext.empty()).orElseThrow();
    }

    /**
     * 封装邮政业务的权限与准入策略。
     */
    private static final class PendingMembershipPolicy implements DomainMembershipPolicy {
        /**
         * 执行 {@code policyId} 对应的业务操作。
         */
        @Override
        public String policyId() {
            return "pending-test";
        }

        /**
         * 判断 {@code canJoin} 对应的业务操作。
         */
        @Override
        public PolicyDecision canJoin(PostalNetwork network, DomainJoinRequest request) {
            return PolicyDecision.allow();
        }

        /**
         * 执行 {@code autoActivate} 对应的业务操作。
         */
        @Override
        public boolean autoActivate() {
            return false;
        }
    }

    /**
     * 封装邮政业务的权限与准入策略。
     */
    private static final class DeniedMembershipPolicy implements DomainMembershipPolicy {
        /**
         * 执行 {@code policyId} 对应的业务操作。
         */
        @Override
        public String policyId() {
            return "denied-test";
        }

        /**
         * 判断 {@code canJoin} 对应的业务操作。
         */
        @Override
        public PolicyDecision canJoin(PostalNetwork network, DomainJoinRequest request) {
            return PolicyDecision.deny("denied");
        }
    }
}
