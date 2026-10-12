package net.bluafolkloro.overdeterminism.everechoes.postal;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 在区内已占用投递码集合上分配下一个空闲码，不改写投递区。
 */
public final class DeliveryCodeAllocator {
    /**
     * 禁止实例化。
     */
    private DeliveryCodeAllocator() {
    }

    /**
     * 按 {@link PostalCodes#deliveryAt(int)} 顺序取第一个未被占用的规范化投递码；区内 4000 个码用尽则空。
     * 占用集按规范化比较，忽略空值与非法码，不修改入参。
     */
    public static Optional<String> allocate(Set<String> usedInDistrict) {
        Set<String> used = new HashSet<>();
        if (usedInDistrict != null) {
            for (String raw : usedInDistrict) {
                PostalCodes.canonicalDelivery(raw).ifPresent(used::add);
            }
        }
        int capacity = PostalCodes.deliveryCapacity();
        for (int index = 0; index < capacity; index++) {
            Optional<String> candidate = PostalCodes.deliveryAt(index);
            if (candidate.isPresent() && !used.contains(candidate.get())) {
                return candidate;
            }
        }
        return Optional.empty();
    }
}
