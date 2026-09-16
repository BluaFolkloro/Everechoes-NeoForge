package net.bluafolkloro.overdeterminism.everechoes.postal;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PostalNetwork extends SavedData {
    private static final String DATA_NAME = "everechoes_postal_network";
    private final Map<String, DomainState> domains = new LinkedHashMap<>();

    public static PostalNetwork get(ServerLevel level) {
        MinecraftServer server = level.getServer();
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PostalNetwork::new, PostalNetwork::load),
                DATA_NAME
        );
    }

    public List<String> domainIds() {
        return List.copyOf(domains.keySet());
    }

    public Optional<String> createDomain(String raw) {
        Optional<String> domainId = PostalCodes.canonicalDomain(raw);
        if (domainId.isEmpty() || domains.containsKey(domainId.get())) {
            return Optional.empty();
        }

        domains.put(domainId.get(), new DomainState(domainId.get(), 1));
        setDirty();
        return domainId;
    }

    public boolean hasDomain(String domainId) {
        return domains.containsKey(domainId);
    }

    public Optional<PostalIds> assignDistrict(String domainId, PostalIds current) {
        DomainState state = domains.get(domainId);
        if (state == null) {
            return Optional.empty();
        }

        if (current != null && domainId.equals(current.domainId())) {
            return Optional.of(current);
        }

        if (state.nextDistrict > PostalCodes.DISTRICT_MAX) {
            return Optional.empty();
        }

        String districtId = Integer.toString(state.nextDistrict);
        state.nextDistrict++;
        setDirty();
        return Optional.of(new PostalIds(domainId, districtId));
    }

    public static PostalNetwork load(CompoundTag tag, HolderLookup.Provider registries) {
        PostalNetwork network = new PostalNetwork();
        ListTag list = tag.getList("domains", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag domainTag = list.getCompound(index);
            PostalCodes.canonicalDomain(domainTag.getString("domainId")).ifPresent(domainId -> {
                int nextDistrict = Math.max(1, domainTag.getInt("nextDistrict"));
                network.domains.put(domainId, new DomainState(domainId, nextDistrict));
            });
        }
        return network;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (DomainState state : domains.values()) {
            CompoundTag domainTag = new CompoundTag();
            domainTag.putString("domainId", state.domainId);
            domainTag.putInt("nextDistrict", state.nextDistrict);
            list.add(domainTag);
        }
        tag.put("domains", list);
        return tag;
    }

    public record PostalIds(String domainId, String districtId) {
        public String format() {
            return PostalCodes.formatDistrict(domainId, districtId);
        }
    }

    private static final class DomainState {
        private final String domainId;
        private int nextDistrict;

        private DomainState(String domainId, int nextDistrict) {
            this.domainId = domainId;
            this.nextDistrict = nextDistrict;
        }
    }
}
