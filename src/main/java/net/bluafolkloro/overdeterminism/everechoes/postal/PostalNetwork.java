package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PostalNetwork extends SavedData {
    public static final int SCHEMA_VERSION = 5;
    private static final String DATA_NAME = "everechoes_postal_network";

    private final Map<UUID, PostalDomain> domains = new LinkedHashMap<>();
    private final Map<UUID, PostalDistrict> districts = new LinkedHashMap<>();
    private final Map<UUID, DomainMembership> memberships = new LinkedHashMap<>();
    private final Map<UUID, PostBoxNode> nodes = new LinkedHashMap<>();
    private final Map<UUID, DistrictCoverage> coverages = new LinkedHashMap<>();
    private final Map<PostalChunk, Set<UUID>> coverageIndex = new LinkedHashMap<>();
    private final DomainCreationPolicy creationPolicy;
    private final DomainMembershipPolicy membershipPolicy;
    private final DomainExitPolicy exitPolicy;
    private final DistrictNodeAdmissionPolicy nodeAdmissionPolicy;
    private final InteractionPolicy interactionPolicy;

    public PostalNetwork() {
        this(
                OpenDomainPolicies.CREATION,
                OpenDomainPolicies.MEMBERSHIP,
                OpenDomainPolicies.EXIT,
                CoveredNodeAdmissionPolicy.INSTANCE,
                PostalPolicies.interaction(PostalPolicies.ALLOW_CONFIG)
        );
    }

    public PostalNetwork(
            DomainCreationPolicy creationPolicy,
            DomainMembershipPolicy membershipPolicy,
            DomainExitPolicy exitPolicy
    ) {
        this(creationPolicy, membershipPolicy, exitPolicy, CoveredNodeAdmissionPolicy.INSTANCE, PostalPolicies.interaction(PostalPolicies.ALLOW_CONFIG));
    }

    public PostalNetwork(
            DomainCreationPolicy creationPolicy,
            DomainMembershipPolicy membershipPolicy,
            DomainExitPolicy exitPolicy,
            DistrictNodeAdmissionPolicy nodeAdmissionPolicy,
            InteractionPolicy interactionPolicy
    ) {
        this.creationPolicy = creationPolicy;
        this.membershipPolicy = membershipPolicy;
        this.exitPolicy = exitPolicy;
        this.nodeAdmissionPolicy = nodeAdmissionPolicy;
        this.interactionPolicy = interactionPolicy;
    }

    public static PostalNetwork get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PostalNetwork::new, PostalNetwork::load),
                DATA_NAME
        );
    }

    public Optional<PostalDomain> domain(UUID domainId) {
        return Optional.ofNullable(domains.get(domainId));
    }

    public Optional<PostalDistrict> district(UUID districtId) {
        return Optional.ofNullable(districts.get(districtId));
    }

    public Optional<DomainMembership> membership(UUID districtId) {
        return Optional.ofNullable(memberships.get(districtId));
    }

    public Optional<PostBoxNode> node(UUID nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    public Optional<DistrictCoverage> coverage(UUID districtId) {
        return Optional.ofNullable(coverages.get(districtId));
    }

    public List<PostalDistrict> districtsAt(ResourceLocation dimension, BlockPos position) {
        return districtsAt(PostalChunk.at(dimension, position));
    }

    public List<PostalDistrict> districtsAt(PostalChunk chunk) {
        Set<UUID> ids = coverageIndex.get(chunk);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<PostalDistrict> result = new ArrayList<>();
        for (UUID districtId : Set.copyOf(ids)) {
            PostalDistrict district = districts.get(districtId);
            if (district != null) {
                result.add(district);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<PostalDistrict> joinableDistrictsAt(UUID nodeId, PostalActionContext context) {
        List<PostalDistrict> joinable = new ArrayList<>();
        for (CoverageMatch match : coverageMatches(nodeId, context)) {
            if (match.joinable()) {
                joinable.add(match.district());
            }
        }
        return Collections.unmodifiableList(joinable);
    }

    public List<CoverageMatch> coverageMatches(UUID nodeId, PostalActionContext context) {
        if (context.dimension() == null || context.position() == null) {
            return List.of();
        }
        PostBoxNode node = nodes.get(nodeId);
        List<CoverageMatch> matches = new ArrayList<>();
        for (PostalDistrict district : districtsAt(context.dimension(), context.position())) {
            if (node == null) {
                matches.add(new CoverageMatch(district, false, "message.everechoes.post_box.unknown_district"));
                continue;
            }
            if (node.districtId() != null) {
                matches.add(new CoverageMatch(district, false, "message.everechoes.post_box.node_already_assigned"));
                continue;
            }
            PolicyDecision interaction = interactionPolicy.canSubmit(context);
            if (!interaction.allowed()) {
                matches.add(new CoverageMatch(district, false, interaction.reasonKey()));
                continue;
            }
            PolicyDecision admission = nodeAdmissionPolicy.canAdmit(
                    this,
                    new NodeJoinRequest(nodeId, district.districtId(), context, true)
            );
            matches.add(new CoverageMatch(district, admission.allowed(), admission.reasonKey()));
        }
        return Collections.unmodifiableList(matches);
    }

    public void rebuildCoverageIndex() {
        coverageIndex.clear();
        for (DistrictCoverage coverage : coverages.values()) {
            indexCoverage(coverage);
        }
    }

    public record CoverageMatch(PostalDistrict district, boolean joinable, @Nullable String reasonKey) {
    }

    public Optional<PostalAtlasSnapshot> atlasWindow(
            UUID districtId,
            ResourceLocation dimension,
            int originX,
            int originZ,
            int width,
            int height,
            boolean includeSavedCoverage
    ) {
        if (!districts.containsKey(districtId)) {
            return Optional.empty();
        }
        DistrictCoverage coverage = coverages.get(districtId);
        if (coverage == null) {
            return Optional.empty();
        }
        if (width < 1
                || height < 1
                || width > PostalAtlasLimits.MAX_WINDOW_SIDE
                || height > PostalAtlasLimits.MAX_WINDOW_SIDE
                || width * height > PostalAtlasLimits.MAX_WINDOW_CELLS) {
            return Optional.empty();
        }
        if (dimension == null) {
            return Optional.empty();
        }

        Set<Long> ownedPacked = new LinkedHashSet<>();
        Set<Long> foreignPacked = new LinkedHashSet<>();
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < height; dz++) {
                int x = originX + dx;
                int z = originZ + dz;
                if (!PostalAtlasLimits.isLegalChunk(x, z)) {
                    continue;
                }
                PostalChunk cell = new PostalChunk(dimension, x, z);
                long packed = cell.packed();
                Set<UUID> owners = coverageIndex.get(cell);
                if (owners == null || owners.isEmpty()) {
                    continue;
                }
                if (owners.contains(districtId)) {
                    ownedPacked.add(packed);
                }
                for (UUID ownerId : owners) {
                    if (!ownerId.equals(districtId)) {
                        foreignPacked.add(packed);
                        break;
                    }
                }
            }
        }

        Set<Long> hubPacked = new LinkedHashSet<>();
        Set<Long> collectionPacked = new LinkedHashSet<>();
        Set<Long> nodePacked = new LinkedHashSet<>();
        int maxX = originX + width;
        int maxZ = originZ + height;
        for (PostBoxNode node : nodesOf(districtId)) {
            PostalChunk nodeChunk = PostalChunk.at(node.dimension(), node.position());
            long packed = nodeChunk.packed();
            nodePacked.add(packed);
            if (!node.dimension().equals(dimension)) {
                continue;
            }
            if (nodeChunk.x() < originX || nodeChunk.x() >= maxX || nodeChunk.z() < originZ || nodeChunk.z() >= maxZ) {
                continue;
            }
            if (node.role() == NodeRole.HUB) {
                hubPacked.add(packed);
            } else if (node.role() == NodeRole.COLLECTION) {
                collectionPacked.add(packed);
            }
        }

        Set<Long> savedCoveragePacked = Set.of();
        if (includeSavedCoverage) {
            savedCoveragePacked = new LinkedHashSet<>();
            for (PostalChunk chunk : coverage.chunks()) {
                savedCoveragePacked.add(chunk.packed());
            }
        }

        String domainCode = "";
        String districtCode = "";
        DomainMembership membership = memberships.get(districtId);
        if (membership != null && membership.isDeliveryEndpoint()) {
            PostalDomain domain = domains.get(membership.domainId());
            if (domain != null && domain.lifecycle() != DomainLifecycle.HISTORICAL) {
                domainCode = domain.domainCode();
                districtCode = membership.districtCode();
            }
        }

        return Optional.of(new PostalAtlasSnapshot(
                districtId,
                dimension,
                coverage.revision(),
                domainCode,
                districtCode,
                originX,
                originZ,
                width,
                height,
                ownedPacked,
                foreignPacked,
                hubPacked,
                collectionPacked,
                savedCoveragePacked,
                nodePacked,
                coverage.chunks().size(),
                DistrictCoverage.MAX_CHUNKS
        ));
    }

    public Set<PostalChunk> domainCoverage(UUID domainId) {
        Set<PostalChunk> result = new LinkedHashSet<>();
        for (DomainMembership membership : memberships.values()) {
            if (!membership.domainId().equals(domainId)
                    || !membership.isDeliveryEndpoint()
                    || !isServedDistrict(membership.districtId())) {
                continue;
            }
            DistrictCoverage coverage = coverages.get(membership.districtId());
            if (coverage != null) {
                result.addAll(coverage.chunks());
            }
        }
        return Set.copyOf(result);
    }

    public List<PostBoxNode> nodesOf(UUID districtId) {
        PostalDistrict district = districts.get(districtId);
        if (district == null) {
            return List.of();
        }
        List<PostBoxNode> result = new ArrayList<>();
        for (UUID nodeId : district.nodeIds()) {
            PostBoxNode node = nodes.get(nodeId);
            if (node != null) {
                result.add(node);
            }
        }
        return result;
    }

    public Optional<PostBoxNode> hubOf(UUID districtId) {
        return nodesOf(districtId).stream().filter(node -> node.role() == NodeRole.HUB).findFirst();
    }

    public List<String> liveDomainCodes() {
        return domains.values().stream()
                .filter(domain -> domain.lifecycle() != DomainLifecycle.HISTORICAL)
                .map(PostalDomain::domainCode)
                .toList();
    }

    public Optional<PostalDomain> findLiveDomainByCode(String rawCode) {
        return PostalCodes.canonicalDomain(rawCode).flatMap(code -> domains.values().stream()
                .filter(domain -> domain.lifecycle() != DomainLifecycle.HISTORICAL)
                .filter(domain -> domain.domainCode().equals(code))
                .findFirst());
    }

    public Optional<PostalDomain> findDomainByCode(String rawCode) {
        return PostalCodes.canonicalDomain(rawCode).flatMap(code -> domains.values().stream()
                .filter(domain -> domain.domainCode().equals(code))
                .findFirst());
    }

    public Optional<PostalDistrict> findDistrictByCode(UUID domainId, String districtCode) {
        return PostalCodes.canonicalDistrict(districtCode).flatMap(code -> memberships.values().stream()
                .filter(membership -> membership.domainId().equals(domainId) && membership.districtCode().equals(code))
                .filter(membership -> isActiveDeliveryDistrict(membership.districtId()))
                .map(membership -> districts.get(membership.districtId()))
                .findFirst());
    }

    public boolean isActiveDeliveryDistrict(UUID districtId) {
        DomainMembership membership = memberships.get(districtId);
        PostalDomain domain = membership == null ? null : domains.get(membership.domainId());
        return membership != null
                && membership.isDeliveryEndpoint()
                && domain != null
                && domain.lifecycle() == DomainLifecycle.ACTIVE
                && isServedDistrict(districtId);
    }

    public boolean isServedDistrict(UUID districtId) {
        PostalDistrict district = districts.get(districtId);
        DistrictCoverage coverage = coverages.get(districtId);
        return district != null
                && !district.isEmpty()
                && coverage != null
                && hubOf(districtId).isPresent()
                && nodesOf(districtId).stream().allMatch(node ->
                coverage.contains(PostalChunk.at(node.dimension(), node.position())));
    }

    public MailBoxAddress resolveAddress(MailBoxAddress display) {
        PostalDomain domain = findLiveDomainByCode(display.domainCode()).orElse(null);
        if (domain == null) {
            return display;
        }
        PostalDistrict district = findDistrictByCode(domain.domainId(), display.districtCode()).orElse(null);
        return display.resolved(domain.domainId(), district == null ? null : district.districtId(), null);
    }

    public PostBoxNode registerNode(UUID nodeId, ResourceLocation dimension, BlockPos position) {
        PostBoxNode existing = nodes.get(nodeId);
        PostBoxNode node = existing == null
                ? new PostBoxNode(nodeId, null, NodeRole.COLLECTION, dimension, position, NodeState.REGISTERED)
                : existing.withPosition(dimension, position);
        nodes.put(nodeId, node);
        setDirty();
        return node;
    }

    public Optional<PostalDistrict> createDistrict(UUID hubNodeId, PostalActionContext context) {
        if (!interactionPolicy.canSubmit(context).allowed()) {
            return Optional.empty();
        }
        PostBoxNode node = nodes.get(hubNodeId);
        if (node == null || node.districtId() != null) {
            return Optional.empty();
        }
        PostalChunk initialChunk = PostalChunk.at(node.dimension(), node.position());
        UUID districtId = UUID.randomUUID();
        PostalDistrict district = new PostalDistrict(districtId, List.of(hubNodeId), 1);
        DistrictCoverage coverage = new DistrictCoverage(
                districtId,
                Set.of(initialChunk),
                1
        );
        districts.put(districtId, district);
        coverages.put(districtId, coverage);
        indexCoverage(coverage);
        nodes.put(hubNodeId, node.withDistrict(districtId, NodeRole.HUB));
        setDirty();
        return Optional.of(district);
    }

    public Optional<PostBoxNode> joinDistrictAsCollection(UUID nodeId, UUID districtId, PostalActionContext context) {
        if (!interactionPolicy.canSubmit(context).allowed()) {
            return Optional.empty();
        }
        NodeJoinRequest request = new NodeJoinRequest(nodeId, districtId, context, true);
        if (!nodeAdmissionPolicy.canAdmit(this, request).allowed()) {
            return Optional.empty();
        }
        PostalDistrict district = districts.get(districtId);
        PostBoxNode node = nodes.get(nodeId);
        districts.put(districtId, district.adding(nodeId));
        PostBoxNode joined = node.withDistrict(districtId, NodeRole.COLLECTION);
        nodes.put(nodeId, joined);
        setDirty();
        return Optional.of(joined);
    }

    public Optional<DistrictCoverage> replaceDistrictCoverage(
            UUID districtId,
            Collection<PostalChunk> proposedChunks,
            PostalActionContext context
    ) {
        DistrictCoverage current = coverages.get(districtId);
        if (current == null) {
            return Optional.empty();
        }
        return replaceDistrictCoverage(districtId, proposedChunks, current.revision(), context);
    }

    public Optional<DistrictCoverage> replaceDistrictCoverage(
            UUID districtId,
            Collection<PostalChunk> proposedChunks,
            int expectedRevision,
            PostalActionContext context
    ) {
        PolicyDecision validation = validateCoverageReplacement(districtId, proposedChunks, expectedRevision, context);
        if (!validation.allowed()) {
            return Optional.empty();
        }
        Set<PostalChunk> proposed = Set.copyOf(proposedChunks);
        DistrictCoverage current = coverages.get(districtId);
        DistrictCoverage updated = current.withChunks(proposed);
        unindexCoverage(current);
        coverages.put(districtId, updated);
        indexCoverage(updated);
        setDirty();
        return Optional.of(updated);
    }

    public PolicyDecision validateCoverageReplacement(
            UUID districtId,
            Collection<PostalChunk> proposedChunks,
            PostalActionContext context
    ) {
        DistrictCoverage current = coverages.get(districtId);
        int expectedRevision = current == null ? -1 : current.revision();
        return validateCoverageReplacement(districtId, proposedChunks, expectedRevision, context);
    }

    public PolicyDecision validateCoverageReplacement(
            UUID districtId,
            Collection<PostalChunk> proposedChunks,
            int expectedRevision,
            PostalActionContext context
    ) {
        PolicyDecision interaction = interactionPolicy.canSubmit(context);
        if (!interaction.allowed()) {
            return interaction;
        }
        if (!districts.containsKey(districtId)) {
            return PolicyDecision.deny("message.everechoes.coverage.unknown_district");
        }
        DistrictCoverage current = coverages.get(districtId);
        if (current == null || current.revision() != expectedRevision) {
            return PolicyDecision.deny("message.everechoes.coverage.stale_revision");
        }
        if (proposedChunks == null || proposedChunks.isEmpty()) {
            return PolicyDecision.deny("message.everechoes.coverage.empty");
        }
        if (proposedChunks.size() > DistrictCoverage.MAX_CHUNKS) {
            return PolicyDecision.deny("message.everechoes.coverage.too_large");
        }
        if (proposedChunks.stream().anyMatch(Objects::isNull)) {
            return PolicyDecision.deny("message.everechoes.coverage.invalid");
        }
        Set<PostalChunk> proposed = Set.copyOf(proposedChunks);
        if (proposed.stream().anyMatch(chunk -> !PostalAtlasLimits.isLegalChunk(chunk.x(), chunk.z()))) {
            return PolicyDecision.deny("message.everechoes.coverage.out_of_bounds");
        }
        if (proposed.stream().map(PostalChunk::dimension).distinct().count() != 1
                || !current.dimension().equals(proposed.iterator().next().dimension())) {
            return PolicyDecision.deny("message.everechoes.coverage.multiple_dimensions");
        }
        if (!DistrictCoverage.isValidShape(proposed)) {
            return PolicyDecision.deny("message.everechoes.coverage.disconnected");
        }
        boolean excludesNode = nodesOf(districtId).stream()
                .map(node -> PostalChunk.at(node.dimension(), node.position()))
                .anyMatch(chunk -> !proposed.contains(chunk));
        if (excludesNode) {
            return PolicyDecision.deny("message.everechoes.coverage.excludes_node");
        }
        return PolicyDecision.allow();
    }

    public Optional<PostBoxNode> leaveDistrict(UUID nodeId) {
        PostBoxNode node = nodes.get(nodeId);
        if (node == null || node.districtId() == null || node.role() == NodeRole.HUB) {
            return Optional.empty();
        }
        UUID districtId = node.districtId();
        PostalDistrict district = districts.get(districtId);
        if (district != null) {
            districts.put(districtId, district.removing(nodeId));
        }
        PostBoxNode detached = node.withoutDistrict();
        nodes.put(nodeId, detached);
        refreshDistrictService(districtId);
        setDirty();
        return Optional.of(detached);
    }

    public void removeNode(UUID nodeId) {
        PostBoxNode node = nodes.remove(nodeId);
        if (node == null || node.districtId() == null) {
            setDirty();
            return;
        }
        UUID districtId = node.districtId();
        PostalDistrict district = districts.get(districtId);
        if (district != null) {
            PostalDistrict updated = district.removing(nodeId);
            if (updated.isEmpty()) {
                removeEmptyDistrict(updated);
            } else {
                districts.put(districtId, updated);
            }
            if (!updated.isEmpty() && node.role() == NodeRole.HUB) {
                promoteHub(updated);
            }
            if (!updated.isEmpty()) {
                refreshDistrictService(districtId);
            }
        }
        setDirty();
    }

    public boolean covers(UUID districtId, ResourceLocation dimension, BlockPos position) {
        DistrictCoverage coverage = coverages.get(districtId);
        return coverage != null && coverage.contains(PostalChunk.at(dimension, position));
    }

    public Optional<PostalDomain> establishAndJoin(UUID districtId, String rawCode, PostalActionContext context, long joinedAt) {
        if (!interactionPolicy.canSubmit(context).allowed()) {
            return Optional.empty();
        }
        PostalDistrict district = districts.get(districtId);
        if (district == null || district.isEmpty()) {
            return Optional.empty();
        }
        DomainMembership existing = memberships.get(districtId);
        if (existing != null && existing.state() != MembershipState.DETACHED) {
            return Optional.empty();
        }
        Optional<String> domainCode = PostalCodes.canonicalDomain(rawCode);
        if (domainCode.isEmpty()) {
            return Optional.empty();
        }
        PolicyDecision create = creationPolicy.canCreate(this, new DomainCreationRequest(domainCode.get(), context));
        if (!create.allowed()) {
            return Optional.empty();
        }
        PostalDomain domain = new PostalDomain(
                UUID.randomUUID(),
                domainCode.get(),
                DomainLifecycle.DORMANT,
                1,
                creationPolicy.policyId(),
                membershipPolicy.policyId(),
                exitPolicy.policyId()
        );
        domains.put(domain.domainId(), domain);
        Optional<DomainMembership> membership = requestJoin(districtId, domain.domainId(), context, joinedAt);
        if (membership.isEmpty()) {
            domains.remove(domain.domainId());
            setDirty();
            return Optional.empty();
        }
        return Optional.of(domains.get(domain.domainId()));
    }

    public Optional<PostalDomain> establishDomain(String rawCode) {
        return establishDomain(rawCode, PostalActionContext.empty());
    }

    public Optional<PostalDomain> establishDomain(String rawCode, PostalActionContext context) {
        Optional<String> domainCode = PostalCodes.canonicalDomain(rawCode);
        if (domainCode.isEmpty()) {
            return Optional.empty();
        }
        if (!creationPolicy.canCreate(this, new DomainCreationRequest(domainCode.get(), context)).allowed()) {
            return Optional.empty();
        }
        PostalDomain domain = new PostalDomain(
                UUID.randomUUID(),
                domainCode.get(),
                DomainLifecycle.DORMANT,
                1,
                creationPolicy.policyId(),
                membershipPolicy.policyId(),
                exitPolicy.policyId()
        );
        domains.put(domain.domainId(), domain);
        setDirty();
        return Optional.of(domain);
    }

    public Optional<DomainMembership> requestJoin(UUID districtId, UUID domainId, long joinedAt) {
        return requestJoin(districtId, domainId, PostalActionContext.empty(), joinedAt);
    }

    public Optional<DomainMembership> requestJoin(UUID districtId, UUID domainId, PostalActionContext context, long joinedAt) {
        PostalDomain domain = domains.get(domainId);
        PostalDistrict district = districts.get(districtId);
        if (domain == null || district == null || district.isEmpty()) {
            return Optional.empty();
        }
        DomainMembership existing = memberships.get(districtId);
        if (existing != null && existing.domainId().equals(domainId) && existing.state() == MembershipState.ACTIVE) {
            return Optional.of(existing);
        }
        if (existing != null && existing.state() != MembershipState.DETACHED) {
            return Optional.empty();
        }
        DomainMembershipPolicy policy = membershipPolicyFor(domain).orElse(null);
        if (policy == null || !policy.canJoin(this, new DomainJoinRequest(districtId, domainId, context)).allowed()) {
            return Optional.empty();
        }
        String districtCode;
        PostalDomain updated = domain;
        if (existing != null && existing.domainId().equals(domainId)) {
            districtCode = existing.districtCode();
        } else {
            int next = nextFreeDistrict(domain);
            if (next < 0) {
                return Optional.empty();
            }
            districtCode = PostalCodes.nextNumericDistrict(next);
            updated = domain.withNextDistrict(Math.max(domain.nextDistrict(), next + 1));
        }
        domains.put(domainId, updated);
        MembershipState state = policy.autoActivate() ? MembershipState.ACTIVE : MembershipState.PENDING;
        DomainMembership membership = new DomainMembership(
                districtId,
                domainId,
                state,
                existing == null ? 1 : existing.revision() + 1,
                joinedAt,
                districtCode
        );
        memberships.put(districtId, membership);
        refreshLifecycle(domainId);
        setDirty();
        return Optional.of(membership);
    }

    public Optional<DomainMembership> approveJoin(UUID districtId) {
        DomainMembership membership = memberships.get(districtId);
        if (membership == null || membership.state() != MembershipState.PENDING) {
            return Optional.empty();
        }
        DomainMembership active = membership.withState(MembershipState.ACTIVE);
        memberships.put(districtId, active);
        refreshLifecycle(membership.domainId());
        setDirty();
        return Optional.of(active);
    }

    public Optional<DomainMembership> rejectJoin(UUID districtId) {
        DomainMembership membership = memberships.get(districtId);
        if (membership == null || membership.state() != MembershipState.PENDING) {
            return Optional.empty();
        }
        DomainMembership detached = membership.withState(MembershipState.DETACHED);
        memberships.put(districtId, detached);
        refreshLifecycle(membership.domainId());
        setDirty();
        return Optional.of(detached);
    }

    public Optional<DomainMembership> leaveDomain(UUID districtId) {
        Optional<DomainMembership> leaving = requestLeave(districtId);
        if (leaving.isEmpty()) {
            return Optional.empty();
        }
        PostalDomain domain = domains.get(leaving.get().domainId());
        DomainExitPolicy policy = domain == null ? null : exitPolicyFor(domain).orElse(null);
        if (policy != null && policy.canCompleteExit(this, leaving.get()).allowed()) {
            return completeLeave(districtId);
        }
        return leaving;
    }

    public Optional<DomainMembership> requestLeave(UUID districtId) {
        DomainMembership membership = memberships.get(districtId);
        if (membership == null) {
            return Optional.empty();
        }
        PostalDomain domain = domains.get(membership.domainId());
        DomainExitPolicy policy = domain == null ? null : exitPolicyFor(domain).orElse(null);
        if (policy == null || !policy.canRequestExit(this, membership).allowed()) {
            return Optional.empty();
        }
        DomainMembership leaving = membership.withState(MembershipState.LEAVING);
        memberships.put(districtId, leaving);
        refreshLifecycle(membership.domainId());
        setDirty();
        return Optional.of(leaving);
    }

    public Optional<DomainMembership> completeLeave(UUID districtId) {
        DomainMembership membership = memberships.get(districtId);
        if (membership == null) {
            return Optional.empty();
        }
        PostalDomain domain = domains.get(membership.domainId());
        DomainExitPolicy policy = domain == null ? null : exitPolicyFor(domain).orElse(null);
        if (policy == null || !policy.canCompleteExit(this, membership).allowed()) {
            return Optional.empty();
        }
        DomainMembership detached = membership.withState(MembershipState.DETACHED);
        memberships.put(districtId, detached);
        refreshLifecycle(membership.domainId());
        setDirty();
        return Optional.of(detached);
    }

    public Optional<PostalDomain> retireDomain(UUID domainId) {
        PostalDomain domain = domains.get(domainId);
        if (domain == null) {
            return Optional.empty();
        }
        boolean hasAttachedDistrict = memberships.values().stream()
                .anyMatch(membership -> membership.domainId().equals(domainId)
                        && membership.state() != MembershipState.DETACHED);
        if (hasAttachedDistrict) {
            return Optional.empty();
        }
        PostalDomain historical = domain.withLifecycle(DomainLifecycle.HISTORICAL);
        domains.put(domainId, historical);
        setDirty();
        return Optional.of(historical);
    }

    public static PostalNetwork load(CompoundTag tag, HolderLookup.Provider registries) {
        PostalNetwork network = new PostalNetwork();
        if (tag.getInt("schemaVersion") != SCHEMA_VERSION) {
            network.setDirty();
            return network;
        }
        readDomains(network, tag);
        readDistricts(network, tag);
        readMemberships(network, tag);
        readNodes(network, tag);
        readCoverages(network, tag);
        return network;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("schemaVersion", SCHEMA_VERSION);
        writeDomains(tag);
        writeDistricts(tag);
        writeMemberships(tag);
        writeNodes(tag);
        writeCoverages(tag);
        return tag;
    }

    private void promoteHub(PostalDistrict district) {
        for (UUID nodeId : district.nodeIds()) {
            PostBoxNode node = nodes.get(nodeId);
            if (node != null) {
                nodes.put(nodeId, node.withRole(NodeRole.HUB));
                return;
            }
        }
    }

    private void removeEmptyDistrict(PostalDistrict district) {
        UUID districtId = district.districtId();
        DomainMembership membership = memberships.get(districtId);
        if (membership != null && membership.state() != MembershipState.DETACHED) {
            memberships.put(districtId, membership.withState(MembershipState.DETACHED));
            refreshLifecycle(membership.domainId());
        }
        districts.remove(districtId);
        DistrictCoverage coverage = coverages.remove(districtId);
        unindexCoverage(coverage);
    }

    private void indexCoverage(DistrictCoverage coverage) {
        for (PostalChunk chunk : coverage.chunks()) {
            coverageIndex.computeIfAbsent(chunk, ignored -> new LinkedHashSet<>()).add(coverage.districtId());
        }
    }

    private void unindexCoverage(@Nullable DistrictCoverage coverage) {
        if (coverage == null) {
            return;
        }
        for (PostalChunk chunk : coverage.chunks()) {
            Set<UUID> owners = coverageIndex.get(chunk);
            if (owners == null) {
                continue;
            }
            owners.remove(coverage.districtId());
            if (owners.isEmpty()) {
                coverageIndex.remove(chunk);
            }
        }
    }

    private void refreshDistrictService(UUID districtId) {
        PostalDistrict district = districts.get(districtId);
        if (district == null) {
            return;
        }
        if (district.isEmpty()) {
            DomainMembership membership = memberships.get(districtId);
            if (membership != null && membership.isDeliveryEndpoint()) {
                leaveDomain(districtId);
            }
            if (membership != null) {
                refreshLifecycle(membership.domainId());
            }
        }
    }

    private Optional<DomainMembershipPolicy> membershipPolicyFor(PostalDomain domain) {
        if (membershipPolicy.policyId().equals(domain.membershipPolicyId())) {
            return Optional.of(membershipPolicy);
        }
        return PostalPolicies.findMembership(domain.membershipPolicyId());
    }

    private Optional<DomainExitPolicy> exitPolicyFor(PostalDomain domain) {
        if (exitPolicy.policyId().equals(domain.exitPolicyId())) {
            return Optional.of(exitPolicy);
        }
        return PostalPolicies.findExit(domain.exitPolicyId());
    }

    private void refreshLifecycle(UUID domainId) {
        PostalDomain domain = domains.get(domainId);
        if (domain == null || domain.lifecycle() == DomainLifecycle.HISTORICAL) {
            return;
        }
        boolean hasActive = memberships.values().stream()
                .anyMatch(membership -> membership.domainId().equals(domainId)
                        && membership.isDeliveryEndpoint()
                        && isServedDistrict(membership.districtId()));
        domains.put(domainId, domain.withLifecycle(hasActive ? DomainLifecycle.ACTIVE : DomainLifecycle.DORMANT));
    }

    private List<String> usedDistrictCodes(UUID domainId) {
        return memberships.values().stream()
                .filter(membership -> membership.domainId().equals(domainId))
                .filter(membership -> membership.state() != MembershipState.DETACHED)
                .map(DomainMembership::districtCode)
                .toList();
    }

    private int nextFreeDistrict(PostalDomain domain) {
        List<String> used = usedDistrictCodes(domain.domainId());
        for (int value = 1; value <= PostalCodes.DISTRICT_MAX; value++) {
            if (!used.contains(Integer.toString(value))) {
                return value;
            }
        }
        return -1;
    }

    private void writeDomains(CompoundTag tag) {
        ListTag list = new ListTag();
        for (PostalDomain domain : domains.values()) {
            CompoundTag domainTag = new CompoundTag();
            domainTag.putUUID("domainId", domain.domainId());
            domainTag.putString("domainCode", domain.domainCode());
            domainTag.putString("lifecycle", domain.lifecycle().name());
            domainTag.putInt("nextDistrict", domain.nextDistrict());
            domainTag.putString("creationPolicyId", domain.creationPolicyId());
            domainTag.putString("membershipPolicyId", domain.membershipPolicyId());
            domainTag.putString("exitPolicyId", domain.exitPolicyId());
            list.add(domainTag);
        }
        tag.put("domains", list);
    }

    private void writeDistricts(CompoundTag tag) {
        ListTag list = new ListTag();
        for (PostalDistrict district : districts.values()) {
            CompoundTag districtTag = new CompoundTag();
            districtTag.putUUID("districtId", district.districtId());
            districtTag.putInt("revision", district.revision());
            ListTag nodesTag = new ListTag();
            for (UUID nodeId : district.nodeIds()) {
                CompoundTag nodeRef = new CompoundTag();
                nodeRef.putUUID("nodeId", nodeId);
                nodesTag.add(nodeRef);
            }
            districtTag.put("nodeIds", nodesTag);
            list.add(districtTag);
        }
        tag.put("districts", list);
    }

    private void writeMemberships(CompoundTag tag) {
        ListTag list = new ListTag();
        for (DomainMembership membership : memberships.values()) {
            CompoundTag membershipTag = new CompoundTag();
            membershipTag.putUUID("districtId", membership.districtId());
            membershipTag.putUUID("domainId", membership.domainId());
            membershipTag.putString("state", membership.state().name());
            membershipTag.putInt("revision", membership.revision());
            membershipTag.putLong("joinedAt", membership.joinedAt());
            membershipTag.putString("districtCode", membership.districtCode());
            list.add(membershipTag);
        }
        tag.put("memberships", list);
    }

    private void writeNodes(CompoundTag tag) {
        ListTag list = new ListTag();
        for (PostBoxNode node : nodes.values()) {
            CompoundTag nodeTag = new CompoundTag();
            nodeTag.putUUID("nodeId", node.nodeId());
            if (node.districtId() != null) {
                nodeTag.putUUID("districtId", node.districtId());
            }
            nodeTag.putString("role", node.role().name());
            nodeTag.putString("dimension", node.dimension().toString());
            nodeTag.putInt("x", node.position().getX());
            nodeTag.putInt("y", node.position().getY());
            nodeTag.putInt("z", node.position().getZ());
            nodeTag.putString("state", node.state().name());
            list.add(nodeTag);
        }
        tag.put("nodes", list);
    }

    private void writeCoverages(CompoundTag tag) {
        ListTag list = new ListTag();
        for (DistrictCoverage coverage : coverages.values()) {
            CompoundTag coverageTag = new CompoundTag();
            coverageTag.putUUID("districtId", coverage.districtId());
            coverageTag.putInt("revision", coverage.revision());
            coverageTag.put("chunks", writeChunkGroups(coverage.chunks()));
            list.add(coverageTag);
        }
        tag.put("coverages", list);
    }

    private static ListTag writeChunkGroups(Collection<PostalChunk> chunks) {
        Map<ResourceLocation, List<Long>> byDimension = new LinkedHashMap<>();
        for (PostalChunk chunk : chunks) {
            byDimension.computeIfAbsent(chunk.dimension(), ignored -> new ArrayList<>()).add(chunk.packed());
        }
        ListTag groups = new ListTag();
        for (Map.Entry<ResourceLocation, List<Long>> entry : byDimension.entrySet()) {
            CompoundTag group = new CompoundTag();
            group.putString("dimension", entry.getKey().toString());
            group.putLongArray("positions", entry.getValue().stream().mapToLong(Long::longValue).toArray());
            groups.add(group);
        }
        return groups;
    }

    private static void readDomains(PostalNetwork network, CompoundTag tag) {
        ListTag list = tag.getList("domains", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag domainTag = list.getCompound(index);
            UUID domainId = domainTag.getUUID("domainId");
            PostalCodes.canonicalDomain(domainTag.getString("domainCode")).ifPresent(code -> network.domains.put(
                    domainId,
                    new PostalDomain(
                            domainId,
                            code,
                            DomainLifecycle.valueOf(domainTag.getString("lifecycle")),
                            Math.max(1, domainTag.getInt("nextDistrict")),
                            defaultPolicy(domainTag.getString("creationPolicyId")),
                            defaultPolicy(domainTag.getString("membershipPolicyId")),
                            defaultPolicy(domainTag.getString("exitPolicyId"))
                    )
            ));
        }
    }

    private static void readDistricts(PostalNetwork network, CompoundTag tag) {
        ListTag list = tag.getList("districts", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag districtTag = list.getCompound(index);
            List<UUID> nodeIds = new ArrayList<>();
            ListTag nodesTag = districtTag.getList("nodeIds", Tag.TAG_COMPOUND);
            for (int nodeIndex = 0; nodeIndex < nodesTag.size(); nodeIndex++) {
                nodeIds.add(nodesTag.getCompound(nodeIndex).getUUID("nodeId"));
            }
            UUID districtId = districtTag.getUUID("districtId");
            network.districts.put(districtId, new PostalDistrict(districtId, nodeIds, Math.max(1, districtTag.getInt("revision"))));
        }
    }

    private static void readMemberships(PostalNetwork network, CompoundTag tag) {
        ListTag list = tag.getList("memberships", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag membershipTag = list.getCompound(index);
            UUID districtId = membershipTag.getUUID("districtId");
            network.memberships.put(districtId, new DomainMembership(
                    districtId,
                    membershipTag.getUUID("domainId"),
                    MembershipState.valueOf(membershipTag.getString("state")),
                    Math.max(1, membershipTag.getInt("revision")),
                    membershipTag.getLong("joinedAt"),
                    membershipTag.getString("districtCode")
            ));
        }
    }

    private static void readNodes(PostalNetwork network, CompoundTag tag) {
        ListTag list = tag.getList("nodes", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag nodeTag = list.getCompound(index);
            UUID nodeId = nodeTag.getUUID("nodeId");
            UUID districtId = nodeTag.hasUUID("districtId") ? nodeTag.getUUID("districtId") : null;
            network.nodes.put(nodeId, new PostBoxNode(
                    nodeId,
                    districtId,
                    NodeRole.valueOf(nodeTag.getString("role")),
                    ResourceLocation.parse(nodeTag.getString("dimension")),
                    new BlockPos(nodeTag.getInt("x"), nodeTag.getInt("y"), nodeTag.getInt("z")),
                    NodeState.valueOf(nodeTag.getString("state"))
            ));
        }
    }

    private static void readCoverages(PostalNetwork network, CompoundTag tag) {
        ListTag list = tag.getList("coverages", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag coverageTag = list.getCompound(index);
            UUID districtId = coverageTag.getUUID("districtId");
            Set<PostalChunk> chunks = readChunkGroups(coverageTag.getList("chunks", Tag.TAG_COMPOUND));
            if (!network.districts.containsKey(districtId)
                    || !DistrictCoverage.isValidShape(chunks)
                    || network.nodesOf(districtId).stream()
                    .map(node -> PostalChunk.at(node.dimension(), node.position()))
                    .anyMatch(chunk -> !chunks.contains(chunk))) {
                continue;
            }
            DistrictCoverage coverage = new DistrictCoverage(
                    districtId,
                    chunks,
                    Math.max(1, coverageTag.getInt("revision"))
            );
            network.coverages.put(districtId, coverage);
            network.indexCoverage(coverage);
        }
    }

    private static Set<PostalChunk> readChunkGroups(ListTag groups) {
        Set<PostalChunk> chunks = new LinkedHashSet<>();
        for (int index = 0; index < groups.size(); index++) {
            CompoundTag group = groups.getCompound(index);
            ResourceLocation dimension = ResourceLocation.tryParse(group.getString("dimension"));
            if (dimension == null) {
                continue;
            }
            for (long packed : group.getLongArray("positions")) {
                chunks.add(PostalChunk.unpack(dimension, packed));
            }
        }
        return chunks;
    }

    private static String defaultPolicy(String policyId) {
        return policyId == null || policyId.isBlank() ? PostalPolicies.OPEN : policyId;
    }
}
