package org.xiyu.create_stressbound.content.link;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

public final class StressLinkRecord {
    private static final String ID_KEY = "Id";
    private static final String OWNER_KEY = "Owner";
    private static final String TRANSMITTER_KEY = "Transmitter";
    private static final String RECEIVER_KEY = "Receiver";
    private static final String REQUESTED_STRESS_KEY = "RequestedStress";
    private static final String CREATED_AT_KEY = "CreatedAt";

    private final UUID id;
    private final UUID owner;
    private final LinkAnchor transmitter;
    private final LinkAnchor receiver;
    private final int requestedStress;
    private final long createdAt;

    public StressLinkRecord(UUID id, UUID owner, LinkAnchor transmitter, LinkAnchor receiver, int requestedStress, long createdAt) {
        this.id = id;
        this.owner = owner;
        this.transmitter = transmitter;
        this.receiver = receiver;
        this.requestedStress = requestedStress;
        this.createdAt = createdAt;
    }

    public static StressLinkRecord load(CompoundTag tag) {
        return new StressLinkRecord(
            tag.getUUID(ID_KEY),
            tag.getUUID(OWNER_KEY),
            LinkAnchor.load(tag.getCompound(TRANSMITTER_KEY)),
            LinkAnchor.load(tag.getCompound(RECEIVER_KEY)),
            tag.getInt(REQUESTED_STRESS_KEY),
            tag.getLong(CREATED_AT_KEY)
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(ID_KEY, id);
        tag.putUUID(OWNER_KEY, owner);
        tag.put(TRANSMITTER_KEY, transmitter.save());
        tag.put(RECEIVER_KEY, receiver.save());
        tag.putInt(REQUESTED_STRESS_KEY, requestedStress);
        tag.putLong(CREATED_AT_KEY, createdAt);
        return tag;
    }

    public UUID id() {
        return id;
    }

    public UUID owner() {
        return owner;
    }

    public LinkAnchor transmitter() {
        return transmitter;
    }

    public LinkAnchor receiver() {
        return receiver;
    }

    public int requestedStress() {
        return requestedStress;
    }

    public long createdAt() {
        return createdAt;
    }

    public StressLinkRecord withRequestedStress(int newRequestedStress) {
        return new StressLinkRecord(id, owner, transmitter, receiver, newRequestedStress, createdAt);
    }

    public StressLinkRecord withTransmitter(LinkAnchor newTransmitter) {
        return new StressLinkRecord(id, owner, newTransmitter, receiver, requestedStress, createdAt);
    }

    public StressLinkRecord withReceiver(LinkAnchor newReceiver) {
        return new StressLinkRecord(id, owner, transmitter, newReceiver, requestedStress, createdAt);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StressLinkRecord that)) {
            return false;
        }
        return requestedStress == that.requestedStress
            && createdAt == that.createdAt
            && Objects.equals(id, that.id)
            && Objects.equals(owner, that.owner)
            && Objects.equals(transmitter, that.transmitter)
            && Objects.equals(receiver, that.receiver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, owner, transmitter, receiver, requestedStress, createdAt);
    }
}
