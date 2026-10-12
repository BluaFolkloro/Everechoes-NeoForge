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

/**
 * 封装邮政网络的权威状态与业务操作。
 */
public class PostalNetwork extends SavedData {
    /**
     * 定义 {@code SCHEMA_VERSION} 常量。
     */
    public static final int SCHEMA_VERSION = 6;
    /**
     * 首个可迁移的旧存档版本；版本 5 没有稳定的信箱表约束。
     */
    private static final int MIGRATABLE_SCHEMA_VERSION = 5;
    /**
     * 定义 {@code DATA_NAME} 常量。
     */
    private static final String DATA_NAME = "everechoes_postal_network";

    /**
     * 保存 {@code domains} 对应的状态或依赖。
     */
    private final Map<UUID, PostalDomain> domains = new LinkedHashMap<>();
    /**
     * 保存 {@code districts} 对应的状态或依赖。
     */
    private final Map<UUID, PostalDistrict> districts = new LinkedHashMap<>();
    /**
     * 保存 {@code memberships} 对应的状态或依赖。
     */
    private final Map<UUID, DomainMembership> memberships = new LinkedHashMap<>();
    /**
     * 保存 {@code nodes} 对应的状态或依赖。
     */
    private final Map<UUID, PostBoxNode> nodes = new LinkedHashMap<>();
    /**
     * 信箱端点，键为 mailboxId。
     */
    private final Map<UUID, MailBoxEndpoint> mailboxes = new LinkedHashMap<>();
    /**
     * 保存 {@code coverages} 对应的状态或依赖。
     */
    private final Map<UUID, DistrictCoverage> coverages = new LinkedHashMap<>();
    /**
     * 保存 {@code coverageIndex} 对应的状态或依赖。
     */
    private final Map<PostalChunk, Set<UUID>> coverageIndex = new LinkedHashMap<>();
    /**
     * 保存 {@code preservedUnsupportedData} 对应的状态或依赖。
     */
    @Nullable
    private CompoundTag preservedUnsupportedData;
    /**
     * 保存 {@code creationPolicy} 对应的状态或依赖。
     */
    private final DomainCreationPolicy creationPolicy;
    /**
     * 保存 {@code membershipPolicy} 对应的状态或依赖。
     */
    private final DomainMembershipPolicy membershipPolicy;
    /**
     * 保存 {@code exitPolicy} 对应的状态或依赖。
     */
    private final DomainExitPolicy exitPolicy;
    /**
     * 保存 {@code nodeAdmissionPolicy} 对应的状态或依赖。
     */
    private final DistrictNodeAdmissionPolicy nodeAdmissionPolicy;
    /**
     * 保存 {@code interactionPolicy} 对应的状态或依赖。
     */
    private final InteractionPolicy interactionPolicy;

    /**
     * 创建并初始化当前对象。
     */
    public PostalNetwork() {
        this(
                OpenDomainPolicies.CREATION,
                OpenDomainPolicies.MEMBERSHIP,
                OpenDomainPolicies.EXIT,
                CoveredNodeAdmissionPolicy.INSTANCE,
                PostalPolicies.interaction(PostalPolicies.ALLOW_CONFIG)
        );
    }

    /**
     * 创建并初始化当前对象。
     */
    public PostalNetwork(
            DomainCreationPolicy creationPolicy,
            DomainMembershipPolicy membershipPolicy,
            DomainExitPolicy exitPolicy
    ) {
        this(creationPolicy, membershipPolicy, exitPolicy, CoveredNodeAdmissionPolicy.INSTANCE, PostalPolicies.interaction(PostalPolicies.ALLOW_CONFIG));
    }

    /**
     * 创建并初始化当前对象。
     */
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

    /**
     * 获取或解析 {@code get} 对应的业务操作。
     */
    public static PostalNetwork get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PostalNetwork::new, PostalNetwork::load),
                DATA_NAME
        );
    }

    /**
     * 执行 {@code domain} 对应的业务操作。
     */
    public Optional<PostalDomain> domain(UUID domainId) {
        return Optional.ofNullable(domains.get(domainId));
    }

    /**
     * 执行 {@code district} 对应的业务操作。
     */
    public Optional<PostalDistrict> district(UUID districtId) {
        return Optional.ofNullable(districts.get(districtId));
    }

    /**
     * 执行 {@code membership} 对应的业务操作。
     */
    public Optional<DomainMembership> membership(UUID districtId) {
        return Optional.ofNullable(memberships.get(districtId));
    }

    /**
     * 执行 {@code node} 对应的业务操作。
     */
    public Optional<PostBoxNode> node(UUID nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    /**
     * 按信箱身份查询端点；未知则空。
     */
    public Optional<MailBoxEndpoint> mailbox(UUID mailboxId) {
        return Optional.ofNullable(mailboxes.get(mailboxId));
    }

    /**
     * 执行 {@code coverage} 对应的业务操作。
     */
    public Optional<DistrictCoverage> coverage(UUID districtId) {
        return Optional.ofNullable(coverages.get(districtId));
    }

    /**
     * 执行 {@code districtsAt} 对应的业务操作。
     */
    public List<PostalDistrict> districtsAt(ResourceLocation dimension, BlockPos position) {
        return districtsAt(PostalChunk.at(dimension, position));
    }

    /**
     * 执行 {@code districtsAt} 对应的业务操作。
     */
    public List<PostalDistrict> districtsAt(PostalChunk chunk) {
        Set<UUID> ids = coverageIndex.get(chunk);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<PostalDistrict> result = new ArrayList<>();
        for (UUID districtId : ids.stream().sorted().toList()) {
            PostalDistrict district = districts.get(districtId);
            if (district != null) {
                result.add(district);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 添加或加入 {@code joinableDistrictsAt} 对应的业务操作。
     */
    public List<PostalDistrict> joinableDistrictsAt(UUID nodeId, PostalActionContext context) {
        List<PostalDistrict> joinable = new ArrayList<>();
        for (CoverageMatch match : coverageMatches(nodeId, context)) {
            if (match.joinable()) {
                joinable.add(match.district());
            }
        }
        return Collections.unmodifiableList(joinable);
    }

    /**
     * 执行 {@code coverageMatches} 对应的业务操作。
     */
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

    /**
     * 更新 {@code rebuildCoverageIndex} 对应的业务操作。
     */
    public void rebuildCoverageIndex() {
        coverageIndex.clear();
        for (DistrictCoverage coverage : coverages.values()) {
            indexCoverage(coverage);
        }
    }

    /**
     * 封装不可变业务数据。
     */
    public record CoverageMatch(/** 保存 {@code district} 对应的状态或依赖。 */ PostalDistrict district, /** 保存 {@code joinable} 对应的状态或依赖。 */ boolean joinable, /** 保存 {@code reasonKey} 对应的状态或依赖。 */ @Nullable String reasonKey) {
    }

    /**
     * 判断 {@code canConfigure} 对应的业务操作。
     */
    public PolicyDecision canConfigure(PostalActionContext context) {
        if (preservedUnsupportedData != null) {
            return PolicyDecision.deny("message.everechoes.postal.unsupported_schema");
        }
        return interactionPolicy.canSubmit(context);
    }

    /**
     * 执行 {@code atlasWindow} 对应的业务操作。
     */
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

    /**
     * 执行 {@code domainCoverage} 对应的业务操作。
     */
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

    /**
     * 执行 {@code nodesOf} 对应的业务操作。
     */
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

    /**
     * 该投递区已绑定的信箱快照（在线与离线），无则空列表。
     */
    public List<MailBoxEndpoint> mailboxesOf(UUID districtId) {
        List<MailBoxEndpoint> result = new ArrayList<>();
        for (MailBoxEndpoint endpoint : mailboxes.values()) {
            if (districtId.equals(endpoint.districtId())) {
                result.add(endpoint);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 执行 {@code hubOf} 对应的业务操作。
     */
    public Optional<PostBoxNode> hubOf(UUID districtId) {
        return nodesOf(districtId).stream().filter(node -> node.role() == NodeRole.HUB).findFirst();
    }

    /**
     * 执行 {@code liveDomainCodes} 对应的业务操作。
     */
    public List<String> liveDomainCodes() {
        return domains.values().stream()
                .filter(domain -> domain.lifecycle() != DomainLifecycle.HISTORICAL)
                .map(PostalDomain::domainCode)
                .toList();
    }

    /**
     * 获取或解析 {@code findLiveDomainByCode} 对应的业务操作。
     */
    public Optional<PostalDomain> findLiveDomainByCode(String rawCode) {
        return PostalCodes.canonicalDomain(rawCode).flatMap(code -> domains.values().stream()
                .filter(domain -> domain.lifecycle() != DomainLifecycle.HISTORICAL)
                .filter(domain -> domain.domainCode().equals(code))
                .findFirst());
    }

    /**
     * 获取或解析 {@code findDomainByCode} 对应的业务操作。
     */
    public Optional<PostalDomain> findDomainByCode(String rawCode) {
        return PostalCodes.canonicalDomain(rawCode).flatMap(code -> domains.values().stream()
                .filter(domain -> domain.domainCode().equals(code))
                .findFirst());
    }

    /**
     * 获取或解析 {@code findDistrictByCode} 对应的业务操作。
     */
    public Optional<PostalDistrict> findDistrictByCode(UUID domainId, String districtCode) {
        return PostalCodes.canonicalDistrict(districtCode).flatMap(code -> memberships.values().stream()
                .filter(membership -> membership.domainId().equals(domainId) && membership.districtCode().equals(code))
                .filter(membership -> isActiveDeliveryDistrict(membership.districtId()))
                .map(membership -> districts.get(membership.districtId()))
                .findFirst());
    }

    /**
     * 判断 {@code isActiveDeliveryDistrict} 对应的业务操作。
     */
    public boolean isActiveDeliveryDistrict(UUID districtId) {
        DomainMembership membership = memberships.get(districtId);
        PostalDomain domain = membership == null ? null : domains.get(membership.domainId());
        return membership != null
                && membership.isDeliveryEndpoint()
                && domain != null
                && domain.lifecycle() == DomainLifecycle.ACTIVE
                && isServedDistrict(districtId);
    }

    /**
     * 判断 {@code isServedDistrict} 对应的业务操作。
     */
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

    /**
     * 获取或解析 {@code resolveAddress} 对应的业务操作。
     */
    public MailBoxAddress resolveAddress(MailBoxAddress display) {
        // 已写入信件的 UUID 是路由身份；显示邮编只是快照，绝不能把它重新指向其他信箱。
        if (display.mailboxId().isPresent()) {
            return display;
        }
        PostalDomain domain = findLiveDomainByCode(display.domainCode()).orElse(null);
        if (domain == null) {
            return display;
        }
        PostalDistrict district = findDistrictByCode(domain.domainId(), display.districtCode()).orElse(null);
        UUID mailboxId = district == null ? null : mailboxIdForDelivery(district.districtId(), display.deliveryCode());
        return display.resolved(domain.domainId(), district == null ? null : district.districtId(), mailboxId);
    }

    /**
     * 注册 {@code registerNode} 对应的业务操作。
     */
    public PostBoxNode registerNode(UUID nodeId, ResourceLocation dimension, BlockPos position) {
        if (preservedUnsupportedData != null) {
            return new PostBoxNode(nodeId, null, NodeRole.COLLECTION, dimension, position, NodeState.REGISTERED);
        }
        PostBoxNode existing = nodes.get(nodeId);
        if (existing != null
                && existing.dimension().equals(dimension)
                && existing.position().equals(position)) {
            return existing;
        }

        UUID registeredId = nodeId;
        if (existing != null) {
            do {
                registeredId = UUID.randomUUID();
            } while (nodes.containsKey(registeredId));
        }
        PostBoxNode node = new PostBoxNode(
                registeredId,
                null,
                NodeRole.COLLECTION,
                dimension,
                position,
                NodeState.REGISTERED
        );
        nodes.put(registeredId, node);
        setDirty();
        return node;
    }

    /**
     * 按 mailboxId 注册或刷新信箱端点。同坐标离线则恢复在线并保留绑定；
     * 若同一身份出现在另一位置，则把新位置视为副本并签发新身份，原信箱不迁移。
     * 不支持的存档架构下返回临时在线未绑定端点且不写入。
     */
    public MailBoxEndpoint registerEndpoint(UUID mailboxId, ResourceLocation dimension, BlockPos position) {
        Objects.requireNonNull(mailboxId, "mailboxId");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(position, "position");
        if (preservedUnsupportedData != null) {
            return new MailBoxEndpoint(mailboxId, null, null, dimension, position, MailBoxEndpointState.ONLINE);
        }
        MailBoxEndpoint existing = mailboxes.get(mailboxId);
        if (existing == null) {
            MailBoxEndpoint created = new MailBoxEndpoint(
                    mailboxId,
                    null,
                    null,
                    dimension,
                    position,
                    MailBoxEndpointState.ONLINE
            );
            mailboxes.put(mailboxId, created);
            setDirty();
            return created;
        }
        if (existing.dimension().equals(dimension) && existing.position().equals(position)) {
            if (existing.state() == MailBoxEndpointState.OFFLINE) {
                MailBoxEndpoint revived = existing.withState(MailBoxEndpointState.ONLINE);
                mailboxes.put(mailboxId, revived);
                setDirty();
                return revived;
            }
            return existing;
        }
        UUID registeredId;
        do {
            registeredId = UUID.randomUUID();
        } while (mailboxes.containsKey(registeredId));
        MailBoxEndpoint duplicate = new MailBoxEndpoint(
                registeredId,
                null,
                null,
                dimension,
                position,
                MailBoxEndpointState.ONLINE
        );
        mailboxes.put(registeredId, duplicate);
        setDirty();
        return duplicate;
    }

    /**
     * 将在线未绑定信箱绑定到指定投递区并分配区内唯一投递码；同区已绑定则原样返回。
     * 不从覆盖区推断投递区。
     */
    public Optional<MailBoxEndpoint> bindEndpoint(UUID mailboxId, UUID districtId, PostalActionContext context) {
        Objects.requireNonNull(mailboxId, "mailboxId");
        Objects.requireNonNull(districtId, "districtId");
        Objects.requireNonNull(context, "context");
        if (!interactionPolicy.canSubmit(context).allowed()) {
            return Optional.empty();
        }
        MailBoxEndpoint mailbox = mailboxes.get(mailboxId);
        if (mailbox == null || mailbox.state() == MailBoxEndpointState.OFFLINE) {
            return Optional.empty();
        }
        if (mailbox.isBound()) {
            if (districtId.equals(mailbox.districtId())) {
                return Optional.of(mailbox);
            }
            return Optional.empty();
        }
        if (!districts.containsKey(districtId)) {
            return Optional.empty();
        }
        if (!covers(districtId, mailbox.dimension(), mailbox.position())) {
            return Optional.empty();
        }
        DomainMembership membership = memberships.get(districtId);
        if (membership == null || !membership.isDeliveryEndpoint()) {
            return Optional.empty();
        }
        Optional<String> deliveryCode = DeliveryCodeAllocator.allocate(usedDeliveryCodes(districtId));
        if (deliveryCode.isEmpty()) {
            return Optional.empty();
        }
        MailBoxEndpoint bound = mailbox.withBind(districtId, deliveryCode.get());
        mailboxes.put(mailboxId, bound);
        setDirty();
        return Optional.of(bound);
    }

    /**
     * 标记信箱离线：已绑定则保留投递码占用；未绑定则删除记录。
     */
    public void markEndpointOffline(UUID mailboxId) {
        MailBoxEndpoint mailbox = mailboxes.get(mailboxId);
        if (mailbox == null) {
            return;
        }
        if (mailbox.isBound()) {
            if (mailbox.state() == MailBoxEndpointState.OFFLINE) {
                return;
            }
            mailboxes.put(mailboxId, mailbox.withState(MailBoxEndpointState.OFFLINE));
            setDirty();
            return;
        }
        mailboxes.remove(mailboxId);
        setDirty();
    }

    /**
     * 在线且已绑定、且投递区处于可投递状态时才可投递。
     */
    public boolean canDeliverTo(UUID mailboxId) {
        MailBoxEndpoint endpoint = mailboxes.get(mailboxId);
        return endpoint != null
                && endpoint.state() == MailBoxEndpointState.ONLINE
                && endpoint.isBound()
                && isActiveDeliveryDistrict(endpoint.districtId());
    }

    /**
     * 已解析地址严格按 UUID 身份投递；纯显示地址只在此刻解析一次。
     */
    public boolean canDeliverTo(MailBoxAddress address) {
        MailBoxAddress resolved = address.mailboxId().isPresent() ? address : resolveAddress(address);
        if (resolved.mailboxId().isEmpty()) {
            return false;
        }
        MailBoxEndpoint endpoint = mailboxes.get(resolved.mailboxId().get());
        if (endpoint == null || !canDeliverTo(endpoint.mailboxId())) {
            return false;
        }
        if (resolved.districtId().isPresent()
                && !resolved.districtId().get().equals(endpoint.districtId())) {
            return false;
        }
        if (resolved.domainId().isPresent()) {
            DomainMembership membership = memberships.get(endpoint.districtId());
            if (membership == null || !resolved.domainId().get().equals(membership.domainId())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 创建 {@code createDistrict} 对应的业务操作。
     */
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

    /**
     * 添加或加入 {@code joinDistrictAsCollection} 对应的业务操作。
     */
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

    /**
     * 执行 {@code replaceDistrictCoverage} 对应的业务操作。
     */
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

    /**
     * 执行 {@code replaceDistrictCoverage} 对应的业务操作。
     */
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

    /**
     * 校验 {@code validateCoverageReplacement} 对应的业务操作。
     */
    public PolicyDecision validateCoverageReplacement(
            UUID districtId,
            Collection<PostalChunk> proposedChunks,
            PostalActionContext context
    ) {
        DistrictCoverage current = coverages.get(districtId);
        int expectedRevision = current == null ? -1 : current.revision();
        return validateCoverageReplacement(districtId, proposedChunks, expectedRevision, context);
    }

    /**
     * 校验 {@code validateCoverageReplacement} 对应的业务操作。
     */
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
        boolean excludesMailbox = mailboxesOf(districtId).stream()
                .filter(endpoint -> endpoint.state() == MailBoxEndpointState.ONLINE && endpoint.isBound())
                .map(endpoint -> PostalChunk.at(endpoint.dimension(), endpoint.position()))
                .anyMatch(chunk -> !proposed.contains(chunk));
        if (excludesMailbox) {
            return PolicyDecision.deny("message.everechoes.coverage.excludes_node");
        }
        return PolicyDecision.allow();
    }

    /**
     * 移除或退出 {@code leaveDistrict} 对应的业务操作。
     */
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

    /**
     * 移除或退出 {@code removeNode} 对应的业务操作。
     */
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

    /**
     * 执行 {@code covers} 对应的业务操作。
     */
    public boolean covers(UUID districtId, ResourceLocation dimension, BlockPos position) {
        DistrictCoverage coverage = coverages.get(districtId);
        return coverage != null && coverage.contains(PostalChunk.at(dimension, position));
    }

    /**
     * 执行 {@code establishAndJoin} 对应的业务操作。
     */
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

    /**
     * 执行 {@code establishDomain} 对应的业务操作。
     */
    public Optional<PostalDomain> establishDomain(String rawCode) {
        return establishDomain(rawCode, PostalActionContext.empty());
    }

    /**
     * 执行 {@code establishDomain} 对应的业务操作。
     */
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

    /**
     * 提交或发送 {@code requestJoin} 对应的业务操作。
     */
    public Optional<DomainMembership> requestJoin(UUID districtId, UUID domainId, long joinedAt) {
        return requestJoin(districtId, domainId, PostalActionContext.empty(), joinedAt);
    }

    /**
     * 提交或发送 {@code requestJoin} 对应的业务操作。
     */
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

    /**
     * 执行 {@code approveJoin} 对应的业务操作。
     */
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

    /**
     * 执行 {@code rejectJoin} 对应的业务操作。
     */
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

    /**
     * 移除或退出 {@code leaveDomain} 对应的业务操作。
     */
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

    /**
     * 提交或发送 {@code requestLeave} 对应的业务操作。
     */
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

    /**
     * 执行 {@code completeLeave} 对应的业务操作。
     */
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

    /**
     * 执行 {@code retireDomain} 对应的业务操作。
     */
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

    /**
     * 读取并恢复 {@code load} 对应的业务操作。
     */
    public static PostalNetwork load(CompoundTag tag, HolderLookup.Provider registries) {
        PostalNetwork network = new PostalNetwork();
        int schemaVersion = tag.getInt("schemaVersion");
        if (schemaVersion != SCHEMA_VERSION && schemaVersion != MIGRATABLE_SCHEMA_VERSION) {
            network.preservedUnsupportedData = tag.copy();
            return network;
        }
        readDomains(network, tag);
        readDistricts(network, tag);
        readMemberships(network, tag);
        readNodes(network, tag);
        readCoverages(network, tag);
        readMailboxes(network, tag);
        if (schemaVersion == MIGRATABLE_SCHEMA_VERSION) {
            network.setDirty();
        }
        return network;
    }

    /**
     * 保存或编码 {@code save} 对应的业务操作。
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (preservedUnsupportedData != null) {
            return preservedUnsupportedData.copy();
        }
        tag.putInt("schemaVersion", SCHEMA_VERSION);
        writeDomains(tag);
        writeDistricts(tag);
        writeMemberships(tag);
        writeNodes(tag);
        writeMailboxes(tag);
        writeCoverages(tag);
        return tag;
    }

    /**
     * 执行 {@code promoteHub} 对应的业务操作。
     */
    private void promoteHub(PostalDistrict district) {
        for (UUID nodeId : district.nodeIds()) {
            PostBoxNode node = nodes.get(nodeId);
            if (node != null) {
                nodes.put(nodeId, node.withRole(NodeRole.HUB));
                return;
            }
        }
    }

    /**
     * 移除或退出 {@code removeEmptyDistrict} 对应的业务操作。
     */
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
        unbindMailboxesOfRemovedDistrict(districtId);
    }

    /**
     * 执行 {@code indexCoverage} 对应的业务操作。
     */
    private void indexCoverage(DistrictCoverage coverage) {
        for (PostalChunk chunk : coverage.chunks()) {
            coverageIndex.computeIfAbsent(chunk, ignored -> new LinkedHashSet<>()).add(coverage.districtId());
        }
    }

    /**
     * 执行 {@code unindexCoverage} 对应的业务操作。
     */
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

    /**
     * 更新 {@code refreshDistrictService} 对应的业务操作。
     */
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

    /**
     * 执行 {@code membershipPolicyFor} 对应的业务操作。
     */
    private Optional<DomainMembershipPolicy> membershipPolicyFor(PostalDomain domain) {
        if (membershipPolicy.policyId().equals(domain.membershipPolicyId())) {
            return Optional.of(membershipPolicy);
        }
        return PostalPolicies.findMembership(domain.membershipPolicyId());
    }

    /**
     * 执行 {@code exitPolicyFor} 对应的业务操作。
     */
    private Optional<DomainExitPolicy> exitPolicyFor(PostalDomain domain) {
        if (exitPolicy.policyId().equals(domain.exitPolicyId())) {
            return Optional.of(exitPolicy);
        }
        return PostalPolicies.findExit(domain.exitPolicyId());
    }

    /**
     * 更新 {@code refreshLifecycle} 对应的业务操作。
     */
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

    /**
     * 该投递区已占用的投递码（在线与离线绑定均计入）。
     */
    private Set<String> usedDeliveryCodes(UUID districtId) {
        Set<String> used = new LinkedHashSet<>();
        for (MailBoxEndpoint endpoint : mailboxes.values()) {
            if (districtId.equals(endpoint.districtId()) && endpoint.deliveryCode() != null) {
                used.add(endpoint.deliveryCode());
            }
        }
        return used;
    }

    /**
     * 按规范化投递码查找区内信箱，在线与离线均可命中。
     */
    @Nullable
    private UUID mailboxIdForDelivery(UUID districtId, String deliveryCode) {
        Optional<String> canonical = PostalCodes.canonicalDelivery(deliveryCode);
        if (canonical.isEmpty()) {
            return null;
        }
        String code = canonical.get();
        for (MailBoxEndpoint endpoint : mailboxes.values()) {
            if (districtId.equals(endpoint.districtId()) && code.equals(endpoint.deliveryCode())) {
                return endpoint.mailboxId();
            }
        }
        return null;
    }

    /**
     * 投递区清空后：在线信箱解绑，离线信箱删除。
     */
    private void unbindMailboxesOfRemovedDistrict(UUID districtId) {
        List<UUID> mailboxIds = new ArrayList<>();
        for (MailBoxEndpoint endpoint : mailboxes.values()) {
            if (districtId.equals(endpoint.districtId())) {
                mailboxIds.add(endpoint.mailboxId());
            }
        }
        for (UUID mailboxId : mailboxIds) {
            MailBoxEndpoint endpoint = mailboxes.get(mailboxId);
            if (endpoint == null) {
                continue;
            }
            if (endpoint.state() == MailBoxEndpointState.ONLINE) {
                mailboxes.put(mailboxId, endpoint.withoutBind());
            } else {
                mailboxes.remove(mailboxId);
            }
        }
    }

    /**
     * 执行 {@code usedDistrictCodes} 对应的业务操作。
     */
    private List<String> usedDistrictCodes(UUID domainId) {
        return memberships.values().stream()
                .filter(membership -> membership.domainId().equals(domainId))
                .filter(membership -> membership.state() != MembershipState.DETACHED)
                .map(DomainMembership::districtCode)
                .toList();
    }

    /**
     * 执行 {@code nextFreeDistrict} 对应的业务操作。
     */
    private int nextFreeDistrict(PostalDomain domain) {
        List<String> used = usedDistrictCodes(domain.domainId());
        for (int value = 1; value <= PostalCodes.DISTRICT_MAX; value++) {
            if (!used.contains(Integer.toString(value))) {
                return value;
            }
        }
        return -1;
    }

    /**
     * 保存或编码 {@code writeDomains} 对应的业务操作。
     */
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

    /**
     * 保存或编码 {@code writeDistricts} 对应的业务操作。
     */
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

    /**
     * 保存或编码 {@code writeMemberships} 对应的业务操作。
     */
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

    /**
     * 保存或编码 {@code writeNodes} 对应的业务操作。
     */
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

    /**
     * 写出信箱端点列表；绑定字段仅在完整绑定时写入。
     */
    private void writeMailboxes(CompoundTag tag) {
        ListTag list = new ListTag();
        for (MailBoxEndpoint mailbox : mailboxes.values()) {
            CompoundTag mailboxTag = new CompoundTag();
            mailboxTag.putUUID("mailboxId", mailbox.mailboxId());
            if (mailbox.districtId() != null) {
                mailboxTag.putUUID("districtId", mailbox.districtId());
            }
            if (mailbox.deliveryCode() != null) {
                mailboxTag.putString("deliveryCode", mailbox.deliveryCode());
            }
            mailboxTag.putString("dimension", mailbox.dimension().toString());
            mailboxTag.putInt("x", mailbox.position().getX());
            mailboxTag.putInt("y", mailbox.position().getY());
            mailboxTag.putInt("z", mailbox.position().getZ());
            mailboxTag.putString("state", mailbox.state().name());
            list.add(mailboxTag);
        }
        tag.put("mailboxes", list);
    }

    /**
     * 保存或编码 {@code writeCoverages} 对应的业务操作。
     */
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

    /**
     * 保存或编码 {@code writeChunkGroups} 对应的业务操作。
     */
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

    /**
     * 获取或解析 {@code readDomains} 对应的业务操作。
     */
    private static void readDomains(PostalNetwork network, CompoundTag tag) {
        ListTag list = tag.getList("domains", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag domainTag = list.getCompound(index);
            if (!domainTag.hasUUID("domainId")) {
                continue;
            }
            UUID domainId = domainTag.getUUID("domainId");
            DomainLifecycle lifecycle = enumValue(DomainLifecycle.class, domainTag.getString("lifecycle"));
            if (lifecycle == null) {
                continue;
            }
            PostalCodes.canonicalDomain(domainTag.getString("domainCode")).ifPresent(code -> network.domains.put(
                    domainId,
                    new PostalDomain(
                            domainId,
                            code,
                            lifecycle,
                            Math.max(1, domainTag.getInt("nextDistrict")),
                            defaultPolicy(domainTag.getString("creationPolicyId")),
                            defaultPolicy(domainTag.getString("membershipPolicyId")),
                            defaultPolicy(domainTag.getString("exitPolicyId"))
                    )
            ));
        }
    }

    /**
     * 获取或解析 {@code readDistricts} 对应的业务操作。
     */
    private static void readDistricts(PostalNetwork network, CompoundTag tag) {
        ListTag list = tag.getList("districts", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag districtTag = list.getCompound(index);
            if (!districtTag.hasUUID("districtId")) {
                continue;
            }
            List<UUID> nodeIds = new ArrayList<>();
            ListTag nodesTag = districtTag.getList("nodeIds", Tag.TAG_COMPOUND);
            for (int nodeIndex = 0; nodeIndex < nodesTag.size(); nodeIndex++) {
                CompoundTag nodeRefTag = nodesTag.getCompound(nodeIndex);
                if (nodeRefTag.hasUUID("nodeId")) {
                    nodeIds.add(nodeRefTag.getUUID("nodeId"));
                }
            }
            UUID districtId = districtTag.getUUID("districtId");
            network.districts.put(districtId, new PostalDistrict(districtId, nodeIds, Math.max(1, districtTag.getInt("revision"))));
        }
    }

    /**
     * 获取或解析 {@code readMemberships} 对应的业务操作。
     */
    private static void readMemberships(PostalNetwork network, CompoundTag tag) {
        ListTag list = tag.getList("memberships", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag membershipTag = list.getCompound(index);
            if (!membershipTag.hasUUID("districtId") || !membershipTag.hasUUID("domainId")) {
                continue;
            }
            UUID districtId = membershipTag.getUUID("districtId");
            MembershipState state = enumValue(MembershipState.class, membershipTag.getString("state"));
            if (state == null) {
                continue;
            }
            try {
                network.memberships.put(districtId, new DomainMembership(
                        districtId,
                        membershipTag.getUUID("domainId"),
                        state,
                        Math.max(1, membershipTag.getInt("revision")),
                        membershipTag.getLong("joinedAt"),
                        membershipTag.getString("districtCode")
                ));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed saved entries without making the whole world unloadable.
            }
        }
    }

    /**
     * 获取或解析 {@code readNodes} 对应的业务操作。
     */
    private static void readNodes(PostalNetwork network, CompoundTag tag) {
        ListTag list = tag.getList("nodes", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag nodeTag = list.getCompound(index);
            if (!nodeTag.hasUUID("nodeId")) {
                continue;
            }
            UUID nodeId = nodeTag.getUUID("nodeId");
            UUID districtId = nodeTag.hasUUID("districtId") ? nodeTag.getUUID("districtId") : null;
            NodeRole role = enumValue(NodeRole.class, nodeTag.getString("role"));
            NodeState state = enumValue(NodeState.class, nodeTag.getString("state"));
            ResourceLocation dimension = ResourceLocation.tryParse(nodeTag.getString("dimension"));
            if (role == null || state == null || dimension == null) {
                continue;
            }
            network.nodes.put(nodeId, new PostBoxNode(
                    nodeId,
                    districtId,
                    role,
                    dimension,
                    new BlockPos(nodeTag.getInt("x"), nodeTag.getInt("y"), nodeTag.getInt("z")),
                    state
            ));
        }
    }

    /**
     * 读取信箱端点；缺表为空，半绑定跳过，非法投递码按未绑定加载。
     */
    private static void readMailboxes(PostalNetwork network, CompoundTag tag) {
        if (!tag.contains("mailboxes")) {
            return;
        }
        ListTag list = tag.getList("mailboxes", Tag.TAG_COMPOUND);
        List<MailBoxEndpoint> candidates = new ArrayList<>();
        for (int index = 0; index < list.size(); index++) {
            CompoundTag mailboxTag = list.getCompound(index);
            if (!mailboxTag.hasUUID("mailboxId")) {
                continue;
            }
            boolean hasDistrict = mailboxTag.hasUUID("districtId");
            boolean hasCode = mailboxTag.contains("deliveryCode") && !mailboxTag.getString("deliveryCode").isBlank();
            if (hasDistrict != hasCode) {
                continue;
            }
            UUID districtId = null;
            String deliveryCode = null;
            if (hasDistrict) {
                Optional<String> canonical = PostalCodes.canonicalDelivery(mailboxTag.getString("deliveryCode"));
                if (canonical.isPresent()) {
                    districtId = mailboxTag.getUUID("districtId");
                    deliveryCode = canonical.get();
                }
            }
            MailBoxEndpointState state = enumValue(MailBoxEndpointState.class, mailboxTag.getString("state"));
            ResourceLocation dimension = ResourceLocation.tryParse(mailboxTag.getString("dimension"));
            if (state == null || dimension == null) {
                continue;
            }
            candidates.add(new MailBoxEndpoint(
                    mailboxTag.getUUID("mailboxId"),
                    districtId,
                    deliveryCode,
                    dimension,
                    new BlockPos(mailboxTag.getInt("x"), mailboxTag.getInt("y"), mailboxTag.getInt("z")),
                    state
            ));
        }

        // 与 NBT 条目顺序无关：同一损坏存档始终由较小 UUID 保留冲突投递码。
        candidates.sort((left, right) -> left.mailboxId().compareTo(right.mailboxId()));
        Set<String> occupiedDeliveryCodes = new LinkedHashSet<>();
        for (MailBoxEndpoint candidate : candidates) {
            if (network.mailboxes.containsKey(candidate.mailboxId())) {
                continue;
            }
            MailBoxEndpoint repaired = validateLoadedMailbox(network, candidate, occupiedDeliveryCodes);
            if (repaired != null) {
                network.mailboxes.put(repaired.mailboxId(), repaired);
            }
        }
    }

    /**
     * 校验加载出的信箱引用与投递码唯一性。在线坏绑定降级为未绑定，离线坏绑定直接丢弃。
     */
    @Nullable
    private static MailBoxEndpoint validateLoadedMailbox(
            PostalNetwork network,
            MailBoxEndpoint candidate,
            Set<String> occupiedDeliveryCodes
    ) {
        if (!candidate.isBound()) {
            return candidate.state() == MailBoxEndpointState.ONLINE ? candidate : null;
        }
        UUID districtId = candidate.districtId();
        DomainMembership membership = network.memberships.get(districtId);
        DistrictCoverage coverage = network.coverages.get(districtId);
        boolean validReference = network.districts.containsKey(districtId)
                && membership != null
                && membership.isDeliveryEndpoint();
        boolean validOnlinePosition = candidate.state() != MailBoxEndpointState.ONLINE
                || coverage != null && coverage.contains(PostalChunk.at(candidate.dimension(), candidate.position()));
        String deliveryKey = districtId + ":" + candidate.deliveryCode();
        boolean uniqueCode = !occupiedDeliveryCodes.contains(deliveryKey);
        if (validReference && validOnlinePosition && uniqueCode) {
            occupiedDeliveryCodes.add(deliveryKey);
            return candidate;
        }
        return candidate.state() == MailBoxEndpointState.ONLINE ? candidate.withoutBind() : null;
    }

    /**
     * 获取或解析 {@code readCoverages} 对应的业务操作。
     */
    private static void readCoverages(PostalNetwork network, CompoundTag tag) {
        ListTag list = tag.getList("coverages", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag coverageTag = list.getCompound(index);
            if (!coverageTag.hasUUID("districtId")) {
                continue;
            }
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

    /**
     * 获取或解析 {@code readChunkGroups} 对应的业务操作。
     */
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

    /**
     * 执行 {@code enumValue} 对应的业务操作。
     */
    @Nullable
    private static <E extends Enum<E>> E enumValue(Class<E> type, String raw) {
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return null;
        }
    }

    /**
     * 执行 {@code defaultPolicy} 对应的业务操作。
     */
    private static String defaultPolicy(String policyId) {
        return policyId == null || policyId.isBlank() ? PostalPolicies.OPEN : policyId;
    }
}
