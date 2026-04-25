package org.xiyu.create_stressbound.content.link;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public final class StressLinkSavedData extends SavedData {
    private static final String DATA_NAME = "create_stressbound_links";
    private static final String LINKS_KEY = "Links";
    private static final Factory<StressLinkSavedData> FACTORY =
        new Factory<>(StressLinkSavedData::new, StressLinkSavedData::load, DataFixTypes.LEVEL);

    private final Map<UUID, StressLinkRecord> links = new LinkedHashMap<>();
    private final Map<String, UUID> receiverIndex = new LinkedHashMap<>();
    private final Map<String, List<UUID>> transmitterIndex = new LinkedHashMap<>();
    private final Map<UUID, Integer> ownerCounts = new LinkedHashMap<>();

    public static StressLinkSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static StressLinkSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        StressLinkSavedData data = new StressLinkSavedData();
        ListTag linksTag = tag.getList(LINKS_KEY, Tag.TAG_COMPOUND);
        for (Tag linkTag : linksTag) {
            StressLinkRecord record = StressLinkRecord.load((CompoundTag) linkTag);
            data.links.put(record.id(), record);
        }
        data.rebuildIndexes();
        return data;
    }

    public Collection<StressLinkRecord> all() {
        return Collections.unmodifiableCollection(links.values());
    }

    public Optional<StressLinkRecord> get(UUID id) {
        return Optional.ofNullable(links.get(id));
    }

    public Optional<StressLinkRecord> findByReceiver(LinkAnchor receiver) {
        UUID id = receiverIndex.get(receiver.key());
        return id == null ? Optional.empty() : get(id);
    }

    public List<StressLinkRecord> findByTransmitter(LinkAnchor transmitter) {
        List<UUID> ids = transmitterIndex.getOrDefault(transmitter.key(), List.of());
        List<StressLinkRecord> results = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            StressLinkRecord record = links.get(id);
            if (record != null) {
                results.add(record);
            }
        }
        return results;
    }

    public int countByOwner(UUID owner) {
        return ownerCounts.getOrDefault(owner, 0);
    }

    public void put(StressLinkRecord record) {
        findByReceiver(record.receiver()).ifPresent(existing -> links.remove(existing.id()));
        links.put(record.id(), record);
        rebuildIndexes();
        setDirty();
    }

    public boolean remove(UUID id) {
        if (links.remove(id) == null) {
            return false;
        }
        rebuildIndexes();
        setDirty();
        return true;
    }

    public boolean removeByReceiver(LinkAnchor receiver) {
        UUID id = receiverIndex.get(receiver.key());
        return id != null && remove(id);
    }

    public int removeByOwner(UUID owner) {
        List<UUID> idsToRemove = new ArrayList<>();
        for (StressLinkRecord record : links.values()) {
            if (record.owner().equals(owner)) {
                idsToRemove.add(record.id());
            }
        }
        idsToRemove.forEach(links::remove);
        if (!idsToRemove.isEmpty()) {
            rebuildIndexes();
            setDirty();
        }
        return idsToRemove.size();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag listTag = new ListTag();
        for (StressLinkRecord record : links.values()) {
            listTag.add(record.save());
        }
        tag.put(LINKS_KEY, listTag);
        return tag;
    }

    private void rebuildIndexes() {
        receiverIndex.clear();
        transmitterIndex.clear();
        ownerCounts.clear();

        for (StressLinkRecord record : links.values()) {
            receiverIndex.put(record.receiver().key(), record.id());
            transmitterIndex.computeIfAbsent(record.transmitter().key(), ignored -> new ArrayList<>()).add(record.id());
            ownerCounts.merge(record.owner(), 1, Integer::sum);
        }
    }
}
