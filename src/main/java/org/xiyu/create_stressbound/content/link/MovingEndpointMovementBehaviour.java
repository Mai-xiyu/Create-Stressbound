package org.xiyu.create_stressbound.content.link;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.server.level.ServerLevel;

public final class MovingEndpointMovementBehaviour implements MovementBehaviour {
    private final EndpointRole role;

    public MovingEndpointMovementBehaviour(EndpointRole role) {
        this.role = role;
    }

    @Override
    public void startMoving(MovementContext context) {
        capture(context);
    }

    @Override
    public void tick(MovementContext context) {
        capture(context);
    }

    private void capture(MovementContext context) {
        if (!(context.world instanceof ServerLevel serverLevel)) {
            return;
        }
        MovingEndpointRegistry.get(serverLevel.getServer()).capture(context, role);
    }
}
