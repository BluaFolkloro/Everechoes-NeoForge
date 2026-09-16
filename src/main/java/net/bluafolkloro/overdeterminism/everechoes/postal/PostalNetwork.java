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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PostalNetwork extends SavedData {
    public static final int SCHEMA_VERSION = 4;
    private static final String DATA_NAME = "everechoes_postal_network";

    private final Map<UUID, PostalDomain> domains = new LinkedHashMap<>();
    private final Map<UUID, PostalDistrict> districts = new LinkedHashMap<>();
    private final Map<UUID, DomainMembership> memberships = new LinkedHashMap<>();
    private final Map<UUID, PostBoxNode> nodes = new LinkedHashMap<>();
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
                NearbyNodeAdmissionPolicy.INSTANCE,
                PostalPolicies.interaction(PostalPolicies.ALLOW_CONFIG)
        );
    }

    public PostalNetwork(
            DomainCreationPolicy creationPolicy,
            DomainMembershipPolicy membershipPolicy,
            DomainExitPolicy exitPolicy
    ) {
        this(creationPolicy, membershipPolicy, exitPolicy, NearbyNodeAdmissionPolicy.INSTANCE, PostalPolicies.interaction(PostalPolicies.ALLOW_CONFIG));
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

    public Optional<PostalDistrict> findDistrictByCode(UUID domainId, String districtCode) {
        return PostalCodes.canonicalDistrict(districtCode).flatMap(code -> memberships.values().stream()
                .filter(membership -> membership.domainId().equals(domainId) && membership.districtCode().equals(code))
                .filter(DomainMembership::isDeliveryEndpoint)
                .map(membership -> districts.get(membership.districtId()))
                .findFirst());
    }

    public boolean isActiveDeliveryDistrict(UUID districtId) {
        DomainMembership membership = memberships.get(districtId);
        PostalDistrict district = districts.get(districtId);
        return membership != null
                && membership.isDeliveryEndpoint()
                && district != null
                && !district.isEmpty()
                && hubOf(districtId).isPresent();
    }

    public boolean isServedDistrict(UUID districtId) {
        PostalDistrict district = districts.get(districtId);
        return district != null && !district.isEmpty() && hubOf(districtId).isPresent();
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
        UUID districtId = UUID.randomUUID();
        PostalDistrict district = new PostalDistrict(districtId, List.of(hubNodeId), 1);
        districts.put(districtId, district);
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
            districts.put(districtId, updated);
            if (node.role() == NodeRole.HUB) {
                promoteHub(updated);
            }
            refreshDistrictService(districtId);
        }
        setDirty();
    }

    public List<PostalDistrict> nearbyJoinableDistricts(ResourceLocation dimension, BlockPos position) {
        List<PostalDistrict> result = new ArrayList<>();
        long rangeSq = (long) PostalCodes.NEARBY_NODE_RANGE * PostalCodes.NEARBY_NODE_RANGE;
        for (PostalDistrict district : districts.values()) {
            if (district.isEmpty()) {
                continue;
            }
            boolean nearby = nodesOf(district.districtId()).stream().anyMatch(node ->
                    node.dimension().equals(dimension) && node.position().distSqr(position) <= rangeSq);
            if (nearby) {
                result.add(district);
            }
        }
        return result;
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
                PostalPolicies.OPEN,
                PostalPolicies.OPEN,
                PostalPolicies.OPEN
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
                PostalPolicies.OPEN,
                PostalPolicies.OPEN,
                PostalPolicies.OPEN
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
        if (!membershipPolicy.canJoin(this, new DomainJoinRequest(districtId, domainId, context)).allowed()) {
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
        MembershipState state = membershipPolicy.autoActivate() ? MembershipState.ACTIVE : MembershipState.PENDING;
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
        if (exitPolicy.canCompleteExit(this, leaving.get()).allowed()) {
            return completeLeave(districtId);
        }
        return leaving;
    }

    public Optional<DomainMembership> requestLeave(UUID districtId) {
        DomainMembership membership = memberships.get(districtId);
        if (membership == null) {
            return Optional.empty();
        }
        if (!exitPolicy.canRequestExit(this, membership).allowed()) {
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
        if (!exitPolicy.canCompleteExit(this, membership).allowed()) {
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
        return network;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("schemaVersion", SCHEMA_VERSION);
        writeDomains(tag);
        writeDistricts(tag);
        writeMemberships(tag);
        writeNodes(tag);
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
        }
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

    private static String defaultPolicy(String policyId) {
        return policyId == null || policyId.isBlank() ? PostalPolicies.OPEN : policyId;
    }
}
