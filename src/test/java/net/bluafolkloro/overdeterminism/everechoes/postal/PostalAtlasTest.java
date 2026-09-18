package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostalAtlasTest {
    private static final ResourceLocation OVERWORLD = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
    private static final ResourceLocation NETHER = ResourceLocation.fromNamespaceAndPath("minecraft", "the_nether");

    @Test
    void atlasLimitsAndSchemaMatchContract() {
        assertEquals(21, PostalAtlasLimits.WINDOW_SIZE);
        assertEquals(441, PostalAtlasLimits.MAX_WINDOW_CELLS);
        assertEquals(PostalAtlasLimits.WINDOW_SIZE * PostalAtlasLimits.WINDOW_SIZE, PostalAtlasLimits.MAX_WINDOW_CELLS);
        assertEquals(DistrictCoverage.MAX_CHUNKS, PostalAtlasLimits.MAX_SUBMIT_CHUNKS);
        assertEquals(-1875000, PostalAtlasLimits.MIN_CHUNK);
        assertEquals(1874999, PostalAtlasLimits.MAX_CHUNK);
        assertTrue(PostalAtlasLimits.isLegalChunk(PostalAtlasLimits.MIN_CHUNK, PostalAtlasLimits.MIN_CHUNK));
        assertTrue(PostalAtlasLimits.isLegalChunk(PostalAtlasLimits.MAX_CHUNK, PostalAtlasLimits.MAX_CHUNK));
        assertTrue(PostalAtlasLimits.isLegalChunk(PostalAtlasLimits.MIN_CHUNK, PostalAtlasLimits.MAX_CHUNK));
        assertFalse(PostalAtlasLimits.isLegalChunk(PostalAtlasLimits.MIN_CHUNK - 1, 0));
        assertFalse(PostalAtlasLimits.isLegalChunk(PostalAtlasLimits.MAX_CHUNK + 1, 0));
        assertEquals(5, PostalNetwork.SCHEMA_VERSION);
    }

    @Test
    void expandAndShrinkCoverageIncrementsRevisionAndOwnedPackedFollows() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        PostalChunk chunk0 = new PostalChunk(OVERWORLD, 0, 0);
        PostalChunk chunk1 = new PostalChunk(OVERWORLD, 1, 0);
        PostalChunk chunk2 = new PostalChunk(OVERWORLD, 2, 0);
        int originalRevision = network.coverage(district.districtId()).orElseThrow().revision();

        DistrictCoverage expanded = network.replaceDistrictCoverage(
                district.districtId(),
                Set.of(chunk0, chunk1, chunk2),
                PostalActionContext.empty()
        ).orElseThrow();
        assertEquals(originalRevision + 1, expanded.revision());
        PostalAtlasSnapshot expandedWindow = atlas(network, district.districtId(), 0, 0, true);
        assertEquals(expanded.revision(), expandedWindow.revision());
        assertEquals(Set.of(chunk0.packed(), chunk1.packed(), chunk2.packed()), expandedWindow.ownedPacked());

        DistrictCoverage shrunk = network.replaceDistrictCoverage(
                district.districtId(),
                Set.of(chunk0, chunk1),
                PostalActionContext.empty()
        ).orElseThrow();
        assertEquals(expanded.revision() + 1, shrunk.revision());
        PostalAtlasSnapshot shrunkWindow = atlas(network, district.districtId(), 0, 0, true);
        assertEquals(shrunk.revision(), shrunkWindow.revision());
        assertEquals(Set.of(chunk0.packed(), chunk1.packed()), shrunkWindow.ownedPacked());
        assertFalse(shrunkWindow.ownedPacked().contains(chunk2.packed()));
    }

    @Test
    void neverVisitedLegalChunksCanBeAddedWhenConnected() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        PostalChunk chunk0 = new PostalChunk(OVERWORLD, 0, 0);
        PostalChunk chunk1 = new PostalChunk(OVERWORLD, 1, 0);
        PostalChunk far = new PostalChunk(OVERWORLD, 100, 0);

        assertTrue(network.validateCoverageReplacement(
                district.districtId(),
                Set.of(chunk0, chunk1),
                PostalActionContext.empty()
        ).allowed());
        network.replaceDistrictCoverage(district.districtId(), Set.of(chunk0, chunk1), PostalActionContext.empty()).orElseThrow();

        mapLine(network, district.districtId(), 0, 100);
        assertTrue(network.coverage(district.districtId()).orElseThrow().chunks().contains(far));
        assertEquals(101, network.coverage(district.districtId()).orElseThrow().chunks().size());
    }

    @Test
    void legalOverlapIsAllowedAndAppearsInOwnedAndForeignPacked() {
        PostalNetwork network = new PostalNetwork();
        UUID firstHub = register(network, 0, 0);
        UUID secondHub = register(network, 48, 0);
        PostalDistrict first = network.createDistrict(firstHub, PostalActionContext.empty()).orElseThrow();
        PostalDistrict second = network.createDistrict(secondHub, PostalActionContext.empty()).orElseThrow();
        PostalChunk chunk0 = new PostalChunk(OVERWORLD, 0, 0);
        PostalChunk chunk1 = new PostalChunk(OVERWORLD, 1, 0);
        PostalChunk chunk2 = new PostalChunk(OVERWORLD, 2, 0);
        PostalChunk chunk3 = new PostalChunk(OVERWORLD, 3, 0);

        assertTrue(network.validateCoverageReplacement(
                first.districtId(),
                Set.of(chunk0, chunk1, chunk2),
                PostalActionContext.empty()
        ).allowed());
        network.replaceDistrictCoverage(first.districtId(), Set.of(chunk0, chunk1, chunk2), PostalActionContext.empty()).orElseThrow();
        assertTrue(network.validateCoverageReplacement(
                second.districtId(),
                Set.of(chunk1, chunk2, chunk3),
                PostalActionContext.empty()
        ).allowed());
        network.replaceDistrictCoverage(second.districtId(), Set.of(chunk1, chunk2, chunk3), PostalActionContext.empty()).orElseThrow();
        assertEquals(2, network.districtsAt(chunk1).size());

        PostalAtlasSnapshot firstWindow = atlas(network, first.districtId(), 0, 0, true);
        assertTrue(firstWindow.ownedPacked().contains(chunk1.packed()));
        assertTrue(firstWindow.foreignPacked().contains(chunk1.packed()));
        assertTrue(firstWindow.ownedPacked().contains(chunk0.packed()));
        assertFalse(firstWindow.foreignPacked().contains(chunk0.packed()));

        PostalAtlasSnapshot secondWindow = atlas(network, second.districtId(), 0, 0, true);
        assertTrue(secondWindow.ownedPacked().contains(chunk1.packed()));
        assertTrue(secondWindow.foreignPacked().contains(chunk1.packed()));
        assertTrue(secondWindow.ownedPacked().contains(chunk3.packed()));
        assertFalse(secondWindow.foreignPacked().contains(chunk3.packed()));
    }

    @Test
    void coverageReplacementRejectsDisconnectedEmptyExcludesNodeAndTooLarge() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        PostalChunk chunk0 = new PostalChunk(OVERWORLD, 0, 0);
        PostalChunk chunk1 = new PostalChunk(OVERWORLD, 1, 0);
        PostalChunk chunk2 = new PostalChunk(OVERWORLD, 2, 0);

        assertEquals(
                "message.everechoes.coverage.disconnected",
                network.validateCoverageReplacement(district.districtId(), Set.of(chunk0, chunk2), PostalActionContext.empty()).reasonKey()
        );
        assertTrue(network.validateCoverageReplacement(
                district.districtId(),
                Set.of(chunk0, chunk1),
                PostalActionContext.empty()
        ).allowed());
        assertEquals(
                "message.everechoes.coverage.empty",
                network.validateCoverageReplacement(district.districtId(), Set.of(), PostalActionContext.empty()).reasonKey()
        );
        assertEquals(
                "message.everechoes.coverage.excludes_node",
                network.validateCoverageReplacement(district.districtId(), Set.of(chunk1), PostalActionContext.empty()).reasonKey()
        );

        Set<PostalChunk> tooLarge = new LinkedHashSet<>();
        for (int x = 0; x <= PostalAtlasLimits.MAX_SUBMIT_CHUNKS; x++) {
            tooLarge.add(new PostalChunk(OVERWORLD, x, 0));
        }
        assertEquals(PostalAtlasLimits.MAX_SUBMIT_CHUNKS + 1, tooLarge.size());
        assertEquals(
                "message.everechoes.coverage.too_large",
                network.validateCoverageReplacement(district.districtId(), tooLarge, PostalActionContext.empty()).reasonKey()
        );
    }

    @Test
    void outOfBoundsChunksAreRejectedAndInclusiveBoundsAreLegal() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        PostalChunk node = new PostalChunk(OVERWORLD, 0, 0);

        assertEquals(
                "message.everechoes.coverage.out_of_bounds",
                network.validateCoverageReplacement(
                        district.districtId(),
                        Set.of(node, new PostalChunk(OVERWORLD, PostalAtlasLimits.MIN_CHUNK - 1, 0)),
                        PostalActionContext.empty()
                ).reasonKey()
        );
        assertEquals(
                "message.everechoes.coverage.out_of_bounds",
                network.validateCoverageReplacement(
                        district.districtId(),
                        Set.of(node, new PostalChunk(OVERWORLD, PostalAtlasLimits.MAX_CHUNK + 1, 0)),
                        PostalActionContext.empty()
                ).reasonKey()
        );
        assertEquals(
                "message.everechoes.coverage.out_of_bounds",
                network.validateCoverageReplacement(
                        district.districtId(),
                        Set.of(node, new PostalChunk(OVERWORLD, Integer.MAX_VALUE, 0)),
                        PostalActionContext.empty()
                ).reasonKey()
        );

        PostalNetwork minBound = new PostalNetwork();
        UUID minHub = registerChunk(minBound, PostalAtlasLimits.MIN_CHUNK, 0);
        PostalDistrict minDistrict = minBound.createDistrict(minHub, PostalActionContext.empty()).orElseThrow();
        PostalChunk minChunk = new PostalChunk(OVERWORLD, PostalAtlasLimits.MIN_CHUNK, 0);
        PostalChunk minNeighbor = new PostalChunk(OVERWORLD, PostalAtlasLimits.MIN_CHUNK + 1, 0);
        assertTrue(minBound.validateCoverageReplacement(
                minDistrict.districtId(),
                Set.of(minChunk, minNeighbor),
                PostalActionContext.empty()
        ).allowed());
        minBound.replaceDistrictCoverage(
                minDistrict.districtId(),
                Set.of(minChunk, minNeighbor),
                PostalActionContext.empty()
        ).orElseThrow();

        PostalNetwork maxBound = new PostalNetwork();
        UUID maxHub = registerChunk(maxBound, PostalAtlasLimits.MAX_CHUNK, 0);
        PostalDistrict maxDistrict = maxBound.createDistrict(maxHub, PostalActionContext.empty()).orElseThrow();
        PostalChunk maxChunk = new PostalChunk(OVERWORLD, PostalAtlasLimits.MAX_CHUNK, 0);
        PostalChunk maxNeighbor = new PostalChunk(OVERWORLD, PostalAtlasLimits.MAX_CHUNK - 1, 0);
        assertTrue(maxBound.validateCoverageReplacement(
                maxDistrict.districtId(),
                Set.of(maxChunk, maxNeighbor),
                PostalActionContext.empty()
        ).allowed());
    }

    @Test
    void netherProposedCoverageAgainstOverworldDistrictIsMultipleDimensions() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        PostalChunk netherOrigin = new PostalChunk(NETHER, 0, 0);
        PostalChunk netherNeighbor = new PostalChunk(NETHER, 1, 0);
        PostalChunk overworldNode = new PostalChunk(OVERWORLD, 0, 0);

        assertEquals(
                "message.everechoes.coverage.multiple_dimensions",
                network.validateCoverageReplacement(
                        district.districtId(),
                        Set.of(netherOrigin, netherNeighbor),
                        PostalActionContext.empty()
                ).reasonKey()
        );
        assertEquals(
                "message.everechoes.coverage.multiple_dimensions",
                network.validateCoverageReplacement(
                        district.districtId(),
                        Set.of(overworldNode, netherOrigin),
                        PostalActionContext.empty()
                ).reasonKey()
        );
    }

    @Test
    void staleExpectedRevisionIsRejected() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        PostalChunk chunk0 = new PostalChunk(OVERWORLD, 0, 0);
        PostalChunk chunk1 = new PostalChunk(OVERWORLD, 1, 0);
        int originalRevision = network.coverage(district.districtId()).orElseThrow().revision();
        network.replaceDistrictCoverage(
                district.districtId(),
                Set.of(chunk0, chunk1),
                originalRevision,
                PostalActionContext.empty()
        ).orElseThrow();

        assertEquals(
                "message.everechoes.coverage.stale_revision",
                network.validateCoverageReplacement(
                        district.districtId(),
                        Set.of(chunk0),
                        originalRevision,
                        PostalActionContext.empty()
                ).reasonKey()
        );
        assertTrue(network.replaceDistrictCoverage(
                district.districtId(),
                Set.of(chunk0),
                originalRevision,
                PostalActionContext.empty()
        ).isEmpty());
        assertEquals(Set.of(chunk0, chunk1), network.coverage(district.districtId()).orElseThrow().chunks());
    }

    @Test
    void forgedOrMissingDistrictIdDoesNotChangeOtherDistrict() {
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
        Set<PostalChunk> secondChunks = network.coverage(second.districtId()).orElseThrow().chunks();
        int secondRevision = network.coverage(second.districtId()).orElseThrow().revision();

        UUID missing = UUID.randomUUID();
        assertEquals(
                "message.everechoes.coverage.unknown_district",
                network.validateCoverageReplacement(missing, secondChunks, PostalActionContext.empty()).reasonKey()
        );
        assertTrue(network.replaceDistrictCoverage(missing, secondChunks, PostalActionContext.empty()).isEmpty());
        assertEquals(secondChunks, network.coverage(second.districtId()).orElseThrow().chunks());
        assertEquals(secondRevision, network.coverage(second.districtId()).orElseThrow().revision());

        assertEquals(
                "message.everechoes.coverage.excludes_node",
                network.validateCoverageReplacement(first.districtId(), secondChunks, PostalActionContext.empty()).reasonKey()
        );
        assertTrue(network.replaceDistrictCoverage(first.districtId(), secondChunks, PostalActionContext.empty()).isEmpty());
        assertEquals(secondChunks, network.coverage(second.districtId()).orElseThrow().chunks());
        assertEquals(secondRevision, network.coverage(second.districtId()).orElseThrow().revision());

        network.replaceDistrictCoverage(
                first.districtId(),
                Set.of(chunk0, chunk1, chunk2, chunk3),
                PostalActionContext.empty()
        ).orElseThrow();
        assertEquals(secondChunks, network.coverage(second.districtId()).orElseThrow().chunks());
        assertEquals(secondRevision, network.coverage(second.districtId()).orElseThrow().revision());
        assertEquals(2, network.districtsAt(chunk3).size());
    }

    @Test
    void saveAndReloadKeepsCoverageAndIgnoresLegacyExploredChunks() {
        PostalNetwork original = new PostalNetwork();
        UUID firstHub = register(original, 0, 0);
        UUID secondHub = register(original, 48, 0);
        PostalDistrict first = original.createDistrict(firstHub, PostalActionContext.empty()).orElseThrow();
        PostalDistrict second = original.createDistrict(secondHub, PostalActionContext.empty()).orElseThrow();
        PostalChunk chunk0 = new PostalChunk(OVERWORLD, 0, 0);
        PostalChunk chunk1 = new PostalChunk(OVERWORLD, 1, 0);
        PostalChunk chunk2 = new PostalChunk(OVERWORLD, 2, 0);
        PostalChunk chunk3 = new PostalChunk(OVERWORLD, 3, 0);
        original.replaceDistrictCoverage(first.districtId(), Set.of(chunk0, chunk1, chunk2), PostalActionContext.empty()).orElseThrow();
        original.replaceDistrictCoverage(second.districtId(), Set.of(chunk1, chunk2, chunk3), PostalActionContext.empty()).orElseThrow();

        CompoundTag tag = original.save(new CompoundTag(), null);
        assertEquals(PostalNetwork.SCHEMA_VERSION, tag.getInt("schemaVersion"));
        assertEquals(5, tag.getInt("schemaVersion"));
        assertFalse(tag.contains("exploredChunks"));
        ListTag coverages = tag.getList("coverages", Tag.TAG_COMPOUND);
        for (int index = 0; index < coverages.size(); index++) {
            assertFalse(coverages.getCompound(index).contains("exploredChunks"));
        }

        ListTag leftoverExplored = new ListTag();
        CompoundTag leftoverGroup = new CompoundTag();
        leftoverGroup.putString("dimension", OVERWORLD.toString());
        leftoverGroup.putLongArray("positions", new long[] {chunk1.packed(), new PostalChunk(OVERWORLD, 50, 0).packed()});
        leftoverExplored.add(leftoverGroup);
        tag.put("exploredChunks", leftoverExplored);

        PostalNetwork loaded = PostalNetwork.load(tag, null);
        assertEquals(Set.of(chunk0, chunk1, chunk2), loaded.coverage(first.districtId()).orElseThrow().chunks());
        assertEquals(Set.of(chunk1, chunk2, chunk3), loaded.coverage(second.districtId()).orElseThrow().chunks());
        assertEquals(2, loaded.districtsAt(chunk1).size());
        PostalAtlasSnapshot window = atlas(loaded, first.districtId(), 0, 0, true);
        assertTrue(window.ownedPacked().contains(chunk1.packed()));
        assertTrue(window.foreignPacked().contains(chunk1.packed()));

        PostalChunk neverVisited = new PostalChunk(OVERWORLD, 4, 0);
        loaded.replaceDistrictCoverage(
                second.districtId(),
                Set.of(chunk1, chunk2, chunk3, neverVisited),
                PostalActionContext.empty()
        ).orElseThrow();
        assertTrue(loaded.coverage(second.districtId()).orElseThrow().chunks().contains(neverVisited));
        CompoundTag resaved = loaded.save(new CompoundTag(), null);
        assertFalse(resaved.contains("exploredChunks"));
    }

    @Test
    void atlasWindowClipsOwnedPackedAndDoesNotRequireServerLevel() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        PostalChunk inside = new PostalChunk(OVERWORLD, 0, 0);
        PostalChunk justOutside = new PostalChunk(OVERWORLD, PostalAtlasLimits.WINDOW_SIZE, 0);
        PostalChunk far = new PostalChunk(OVERWORLD, 100, 0);
        mapLine(network, district.districtId(), 0, 100);

        PostalAtlasSnapshot window = atlas(network, district.districtId(), 0, 0, false);
        assertEquals(PostalAtlasLimits.WINDOW_SIZE, window.width());
        assertEquals(PostalAtlasLimits.WINDOW_SIZE, window.height());
        assertTrue(window.ownedPacked().contains(inside.packed()));
        assertFalse(window.ownedPacked().contains(justOutside.packed()));
        assertFalse(window.ownedPacked().contains(far.packed()));
        assertTrue(window.ownedPacked().size() <= PostalAtlasLimits.MAX_WINDOW_CELLS);
        assertEquals(101, window.savedCoverageSize());
        assertTrue(window.savedCoveragePacked().isEmpty());
    }

    @Test
    void invalidAtlasWindowSizesAreEmptyAndTwentyOneIsAccepted() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        UUID districtId = district.districtId();

        assertTrue(network.atlasWindow(districtId, OVERWORLD, 0, 0, 0, PostalAtlasLimits.WINDOW_SIZE, false).isEmpty());
        assertTrue(network.atlasWindow(districtId, OVERWORLD, 0, 0, PostalAtlasLimits.WINDOW_SIZE, 0, false).isEmpty());
        assertTrue(network.atlasWindow(districtId, OVERWORLD, 0, 0, 22, PostalAtlasLimits.WINDOW_SIZE, false).isEmpty());
        assertTrue(network.atlasWindow(districtId, OVERWORLD, 0, 0, PostalAtlasLimits.WINDOW_SIZE, 22, false).isEmpty());
        assertTrue(network.atlasWindow(districtId, OVERWORLD, 0, 0, 1_000_000, 1_000_000, false).isEmpty());
        assertTrue(network.atlasWindow(UUID.randomUUID(), OVERWORLD, 0, 0, PostalAtlasLimits.WINDOW_SIZE, PostalAtlasLimits.WINDOW_SIZE, false).isEmpty());
        assertTrue(network.atlasWindow(
                districtId,
                OVERWORLD,
                0,
                0,
                PostalAtlasLimits.WINDOW_SIZE,
                PostalAtlasLimits.WINDOW_SIZE,
                false
        ).isPresent());
    }

    @Test
    void includeSavedCoverageFalseLeavesPackedEmptyButKeepsFullSize() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        mapLine(network, district.districtId(), 0, 25);
        int coverageSize = network.coverage(district.districtId()).orElseThrow().chunks().size();
        assertEquals(26, coverageSize);

        PostalAtlasSnapshot hidden = atlas(network, district.districtId(), 0, 0, false);
        assertTrue(hidden.savedCoveragePacked().isEmpty());
        assertEquals(coverageSize, hidden.savedCoverageSize());
        assertEquals(DistrictCoverage.MAX_CHUNKS, hidden.maxChunks());

        PostalAtlasSnapshot included = atlas(network, district.districtId(), 0, 0, true);
        assertEquals(coverageSize, included.savedCoveragePacked().size());
        assertEquals(coverageSize, included.savedCoverageSize());
        assertTrue(included.savedCoveragePacked().contains(new PostalChunk(OVERWORLD, 25, 0).packed()));
        assertFalse(included.ownedPacked().contains(new PostalChunk(OVERWORLD, 25, 0).packed()));
    }

    @Test
    void denyingInteractionPolicyBlocksCoverageReplacement() {
        PostalNetwork network = new PostalNetwork(
                OpenDomainPolicies.CREATION,
                OpenDomainPolicies.MEMBERSHIP,
                OpenDomainPolicies.EXIT,
                CoveredNodeAdmissionPolicy.INSTANCE,
                new DeniedInteractionPolicy()
        );
        PolicyDecision decision = network.validateCoverageReplacement(
                UUID.randomUUID(),
                Set.of(new PostalChunk(OVERWORLD, 0, 0)),
                PostalActionContext.empty()
        );
        assertFalse(decision.allowed());
        assertEquals("denied", decision.reasonKey());
        assertTrue(network.replaceDistrictCoverage(
                UUID.randomUUID(),
                Set.of(new PostalChunk(OVERWORLD, 0, 0)),
                PostalActionContext.empty()
        ).isEmpty());
    }

    @Test
    void nodePackedIncludesOutOfWindowCollectionWhileHubPackedIsClipped() {
        PostalNetwork network = new PostalNetwork();
        UUID hub = register(network, 0, 0);
        PostalDistrict district = network.createDistrict(hub, PostalActionContext.empty()).orElseThrow();
        mapLine(network, district.districtId(), 0, 25);
        UUID collection = register(network, 400, 0);
        network.joinDistrictAsCollection(collection, district.districtId(), context(collection, 400, 0)).orElseThrow();
        PostalChunk hubChunk = new PostalChunk(OVERWORLD, 0, 0);
        PostalChunk collectionChunk = new PostalChunk(OVERWORLD, 25, 0);

        PostalAtlasSnapshot originWindow = atlas(network, district.districtId(), 0, 0, true);
        assertTrue(originWindow.nodePacked().contains(hubChunk.packed()));
        assertTrue(originWindow.nodePacked().contains(collectionChunk.packed()));
        assertTrue(originWindow.hubPacked().contains(hubChunk.packed()));
        assertFalse(originWindow.hubPacked().contains(collectionChunk.packed()));
        assertFalse(originWindow.collectionPacked().contains(collectionChunk.packed()));

        PostalAtlasSnapshot panned = atlas(network, district.districtId(), 10, 0, true);
        assertTrue(panned.nodePacked().contains(hubChunk.packed()));
        assertTrue(panned.nodePacked().contains(collectionChunk.packed()));
        assertFalse(panned.hubPacked().contains(hubChunk.packed()));
        assertTrue(panned.collectionPacked().contains(collectionChunk.packed()));
    }

    private static PostalAtlasSnapshot atlas(
            PostalNetwork network,
            UUID districtId,
            int originX,
            int originZ,
            boolean includeSavedCoverage
    ) {
        return network.atlasWindow(
                districtId,
                OVERWORLD,
                originX,
                originZ,
                PostalAtlasLimits.WINDOW_SIZE,
                PostalAtlasLimits.WINDOW_SIZE,
                includeSavedCoverage
        ).orElseThrow();
    }

    private static UUID register(PostalNetwork network, int x, int z) {
        UUID nodeId = UUID.randomUUID();
        network.registerNode(nodeId, OVERWORLD, new BlockPos(x, 64, z));
        return nodeId;
    }

    private static UUID registerChunk(PostalNetwork network, int chunkX, int chunkZ) {
        return register(network, chunkX * 16, chunkZ * 16);
    }

    private static PostalActionContext context(UUID nodeId, int x, int z) {
        return new PostalActionContext(null, nodeId, UUID.randomUUID(), OVERWORLD, new BlockPos(x, 64, z));
    }

    private static void mapLine(PostalNetwork network, UUID districtId, int firstChunkX, int lastChunkX) {
        Set<PostalChunk> chunks = new LinkedHashSet<>();
        for (int x = firstChunkX; x <= lastChunkX; x++) {
            chunks.add(new PostalChunk(OVERWORLD, x, 0));
        }
        network.replaceDistrictCoverage(districtId, chunks, PostalActionContext.empty()).orElseThrow();
    }

    private static final class DeniedInteractionPolicy implements InteractionPolicy {
        @Override
        public String policyId() {
            return "denied-interaction-test";
        }

        @Override
        public PolicyDecision canSubmit(PostalActionContext context) {
            return PolicyDecision.deny("denied");
        }
    }
}
