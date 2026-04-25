package org.xiyu.create_stressbound.content.link;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class LinkAnchor {
    private static final String KIND_KEY = "Kind";
    private static final String DIMENSION_KEY = "Dimension";
    private static final String POSITION_KEY = "Pos";
    private static final String ENDPOINT_KEY = "EndpointId";
    private static final String ENTITY_KEY = "EntityId";
    private static final String EXTERNAL_KEY = "ExternalId";

    private final AnchorKind kind;
    private final ResourceLocation dimensionId;
    private final BlockPos pos;
    private final UUID endpointId;
    private final UUID entityId;
    private final String externalId;

    private LinkAnchor(AnchorKind kind, ResourceLocation dimensionId, BlockPos pos, UUID endpointId, UUID entityId, String externalId) {
        this.kind = kind;
        this.dimensionId = dimensionId;
        this.pos = pos.immutable();
        this.endpointId = endpointId;
        this.entityId = entityId;
        this.externalId = externalId == null ? "" : externalId;
    }

    public static LinkAnchor staticBlock(ResourceKey<Level> dimension, BlockPos pos, UUID endpointId) {
        return new LinkAnchor(AnchorKind.STATIC_BLOCK, dimension.location(), pos, endpointId, null, "");
    }

    public static LinkAnchor staticBlock(ResourceKey<Level> dimension, BlockPos pos) {
        return staticBlock(dimension, pos, null);
    }

    public static LinkAnchor runtime(AnchorKind kind, ResourceKey<Level> dimension, BlockPos pos, UUID endpointId, UUID entityId, String externalId) {
        return new LinkAnchor(kind, dimension.location(), pos, endpointId, entityId, externalId);
    }

    public static LinkAnchor load(CompoundTag tag) {
        AnchorKind kind = AnchorKind.valueOf(tag.getString(KIND_KEY));
        ResourceLocation dimensionId = ResourceLocation.parse(tag.getString(DIMENSION_KEY));
        BlockPos pos = BlockPos.of(tag.getLong(POSITION_KEY));
        UUID endpointId = tag.hasUUID(ENDPOINT_KEY) ? tag.getUUID(ENDPOINT_KEY) : null;
        UUID entityId = tag.hasUUID(ENTITY_KEY) ? tag.getUUID(ENTITY_KEY) : null;
        String externalId = tag.getString(EXTERNAL_KEY);
        return new LinkAnchor(kind, dimensionId, pos, endpointId, entityId, externalId);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(KIND_KEY, kind.name());
        tag.putString(DIMENSION_KEY, dimensionId.toString());
        tag.putLong(POSITION_KEY, pos.asLong());
        if (endpointId != null) {
            tag.putUUID(ENDPOINT_KEY, endpointId);
        }
        if (entityId != null) {
            tag.putUUID(ENTITY_KEY, entityId);
        }
        if (!externalId.isBlank()) {
            tag.putString(EXTERNAL_KEY, externalId);
        }
        return tag;
    }

    public AnchorKind kind() {
        return kind;
    }

    public ResourceLocation dimensionId() {
        return dimensionId;
    }

    public ResourceKey<Level> dimensionKey() {
        return ResourceKey.create(Registries.DIMENSION, dimensionId);
    }

    public BlockPos pos() {
        return pos;
    }

    public Optional<UUID> endpointId() {
        return Optional.ofNullable(endpointId);
    }

    public Optional<UUID> entityId() {
        return Optional.ofNullable(entityId);
    }

    public String externalId() {
        return externalId;
    }

    public boolean isStaticBlock() {
        return kind == AnchorKind.STATIC_BLOCK;
    }

    public LinkAnchor asStatic(ResourceKey<Level> dimension, BlockPos newPos) {
        return staticBlock(dimension, newPos, endpointId);
    }

    public String key() {
        if (endpointId != null) {
            return "endpoint|" + endpointId;
        }
        return kind.name() + "|" + dimensionId + "|" + pos.asLong() + "|"
            + (entityId == null ? "" : entityId) + "|" + externalId;
    }

    public String describe() {
        return kind.name().toLowerCase() + "@" + dimensionId + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ()
            + (endpointId == null ? "" : " [" + endpointId + "]");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LinkAnchor that)) {
            return false;
        }
        return kind == that.kind
            && Objects.equals(dimensionId, that.dimensionId)
            && Objects.equals(pos, that.pos)
            && Objects.equals(endpointId, that.endpointId)
            && Objects.equals(entityId, that.entityId)
            && Objects.equals(externalId, that.externalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, dimensionId, pos, endpointId, entityId, externalId);
    }
}
