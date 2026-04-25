package org.xiyu.create_stressbound.content.link;

public enum AnchorKind {
    STATIC_BLOCK(true),
    CREATE_CONTRAPTION(true),
    CREATE_TRAIN(true),
    AERONAUTICS_CRAFT(true),
    VALKYRIEN_SHIP(false);

    private final boolean runtimeImplemented;

    AnchorKind(boolean runtimeImplemented) {
        this.runtimeImplemented = runtimeImplemented;
    }

    public boolean isRuntimeImplemented() {
        return runtimeImplemented;
    }
}
