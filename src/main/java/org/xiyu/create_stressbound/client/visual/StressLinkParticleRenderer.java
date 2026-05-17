package org.xiyu.create_stressbound.client.visual;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.xiyu.create_stressbound.client.StressboundClientConfig;
import org.xiyu.create_stressbound.content.link.ReceiverStatus;
import org.xiyu.create_stressbound.registry.StressboundItems;

public final class StressLinkParticleRenderer {

    private static final float BEAM_STEP = 1.0F;
    private static final float GLOW_RADIUS = 0.4F;
    private static final float LINE_WIDTH = 0.06F;
    private static final double MAX_RENDER_DISTANCE_SQR = 96.0D * 96.0D;
    private static final int MAX_BEAM_SEGMENTS = 96;

    private StressLinkParticleRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) return;

        boolean holdingBinder = player.getItemInHand(InteractionHand.MAIN_HAND).is(StressboundItems.KINETIC_BINDER.get())
            || player.getItemInHand(InteractionHand.OFF_HAND).is(StressboundItems.KINETIC_BINDER.get());
        if (!StressboundClientConfig.shouldRenderLinks(player, holdingBinder)) return;

        List<StressLinkVisualData.LinkVisual> links = StressLinkVisualData.getLinks(level);
        if (links.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        Camera camera = event.getCamera();
        float animationTime = level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(true);

        poseStack.pushPose();
        try {
            poseStack.translate(-cam.x, -cam.y, -cam.z);

            Matrix4f m = poseStack.last().pose();
            Vector3f right = new Vector3f(camera.getLeftVector());
            Vector3f up = new Vector3f(camera.getUpVector());

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);

            try {
                for (StressLinkVisualData.LinkVisual link : links) {
                    renderBeam(m, right, up, link, cam, animationTime);
                }
            } finally {
                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();
            }
        } finally {
            poseStack.popPose();
        }
    }

    private static void renderBeam(Matrix4f m, Vector3f right, Vector3f up,
                                   StressLinkVisualData.LinkVisual link, Vec3 cameraPos, float animationTime) {
        Vec3 start = link.transmitterPos();
        Vec3 end = link.receiverPos();
        if (start.distanceToSqr(cameraPos) > MAX_RENDER_DISTANCE_SQR
            && end.distanceToSqr(cameraPos) > MAX_RENDER_DISTANCE_SQR) {
            return;
        }
        float[] c = statusColor(link.status(), link.color());
        float dist = (float) start.distanceTo(end);
        int segments = Math.min(MAX_BEAM_SEGMENTS, Math.max(2, Mth.ceil(dist / BEAM_STEP)));

        float halfW = LINE_WIDTH * 0.5F;

        Tesselator tesselator = Tesselator.getInstance();
        var buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float) (i + 1) / segments;
            Vec3 p0 = start.lerp(end, t0);
            Vec3 p1 = start.lerp(end, t1);

            float pulse0 = 0.6F + 0.4F * Mth.sin((t0 + animationTime / 60.0F) * Mth.TWO_PI * 3);
            float pulse1 = 0.6F + 0.4F * Mth.sin((t1 + animationTime / 60.0F) * Mth.TWO_PI * 3);

            float r0 = c[0] * pulse0, g0 = c[1] * pulse0, b0 = c[2] * pulse0;
            float r1 = c[0] * pulse1, g1 = c[1] * pulse1, b1 = c[2] * pulse1;

            float x0 = (float) p0.x, y0 = (float) p0.y, z0 = (float) p0.z;
            float x1 = (float) p1.x, y1 = (float) p1.y, z1 = (float) p1.z;

            // Cross-section: 3 quads at 0°, 60°, 120° for cylindrical look
            addBeamQuad(buffer, m, x0, y0, z0, x1, y1, z1, right, up, halfW, 0, r0, g0, b0, r1, g1, b1);
            addBeamQuad(buffer, m, x0, y0, z0, x1, y1, z1, right, up, halfW, Mth.TWO_PI / 3, r0, g0, b0, r1, g1, b1);
            addBeamQuad(buffer, m, x0, y0, z0, x1, y1, z1, right, up, halfW, Mth.TWO_PI * 2 / 3, r0, g0, b0, r1, g1, b1);
        }

        MeshData meshData = buffer.buildOrThrow();
        BufferUploader.drawWithShader(meshData);

        // Endpoint rings
        renderRing(m, right, up, start, c, 0.25F);
        renderRing(m, right, up, end, c, 0.25F);
    }

    private static void addBeamQuad(VertexConsumer buffer, Matrix4f m,
                                     float x0, float y0, float z0,
                                     float x1, float y1, float z1,
                                     Vector3f right, Vector3f up, float halfW, float angle,
                                     float r0, float g0, float b0,
                                     float r1, float g1, float b1) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        float dx = right.x * cos + up.x * sin;
        float dy = right.y * cos + up.y * sin;
        float dz = right.z * cos + up.z * sin;

        buffer.addVertex(m, x0 - dx * halfW, y0 - dy * halfW, z0 - dz * halfW).setColor(r0, g0, b0, 1.0F);
        buffer.addVertex(m, x0 + dx * halfW, y0 + dy * halfW, z0 + dz * halfW).setColor(r0, g0, b0, 1.0F);
        buffer.addVertex(m, x1 + dx * halfW, y1 + dy * halfW, z1 + dz * halfW).setColor(r1, g1, b1, 1.0F);
        buffer.addVertex(m, x1 - dx * halfW, y1 - dy * halfW, z1 - dz * halfW).setColor(r1, g1, b1, 1.0F);
    }

    private static void renderRing(Matrix4f m, Vector3f right, Vector3f up,
                                   Vec3 center, float[] c, float radius) {
        float halfW = LINE_WIDTH * 0.3F;
        int segs = 16;

        Tesselator tesselator = Tesselator.getInstance();
        var buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < segs; i++) {
            float a0 = Mth.TWO_PI * i / segs;
            float a1 = Mth.TWO_PI * (i + 1) / segs;

            float cx0 = (float) (center.x + Math.cos(a0) * radius);
            float cy0 = (float) (center.y + Math.sin(a0) * radius);
            float cz0 = (float) center.z;
            float cx1 = (float) (center.x + Math.cos(a1) * radius);
            float cy1 = (float) (center.y + Math.sin(a1) * radius);
            float cz1 = (float) center.z;

            // XY ring quad
            buffer.addVertex(m, cx0 - up.x * halfW, cy0 - up.y * halfW, cz0 - up.z * halfW).setColor(c[0], c[1], c[2], 1.0F);
            buffer.addVertex(m, cx0 + up.x * halfW, cy0 + up.y * halfW, cz0 + up.z * halfW).setColor(c[0], c[1], c[2], 1.0F);
            buffer.addVertex(m, cx1 + up.x * halfW, cy1 + up.y * halfW, cz1 + up.z * halfW).setColor(c[0], c[1], c[2], 1.0F);
            buffer.addVertex(m, cx1 - up.x * halfW, cy1 - up.y * halfW, cz1 - up.z * halfW).setColor(c[0], c[1], c[2], 1.0F);

            // XZ ring quad
            float zx0 = (float) (center.x + Math.cos(a0) * radius);
            float zz0 = (float) (center.z + Math.sin(a0) * radius);
            float zx1 = (float) (center.x + Math.cos(a1) * radius);
            float zz1 = (float) (center.z + Math.sin(a1) * radius);
            float cy = (float) center.y;

            buffer.addVertex(m, zx0 - up.x * halfW, cy - up.y * halfW, zz0 - up.z * halfW).setColor(c[0], c[1], c[2], 0.7F);
            buffer.addVertex(m, zx0 + up.x * halfW, cy + up.y * halfW, zz0 + up.z * halfW).setColor(c[0], c[1], c[2], 0.7F);
            buffer.addVertex(m, zx1 + up.x * halfW, cy + up.y * halfW, zz1 + up.z * halfW).setColor(c[0], c[1], c[2], 0.7F);
            buffer.addVertex(m, zx1 - up.x * halfW, cy - up.y * halfW, zz1 - up.z * halfW).setColor(c[0], c[1], c[2], 0.7F);
        }

        MeshData meshData = buffer.buildOrThrow();
        BufferUploader.drawWithShader(meshData);
    }

    private static void renderGlowRing(Matrix4f m, Vector3f right, Vector3f up,
                                        Vec3 center, ReceiverStatus status, int color, Vec3 cameraPos, float animationTime) {
        if (center.distanceToSqr(cameraPos) > MAX_RENDER_DISTANCE_SQR) {
            return;
        }
        float[] c = statusColor(status, color);
        float t = (animationTime % 40.0F) / 40.0F;
        float r = GLOW_RADIUS * (0.7F + 0.3F * Mth.sin(t * Mth.TWO_PI));

        float halfW = LINE_WIDTH * 0.3F;
        int segs = 20;

        Tesselator tesselator = Tesselator.getInstance();
        var buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < segs; i++) {
            float a0 = Mth.TWO_PI * i / segs + t * Mth.TWO_PI;
            float a1 = Mth.TWO_PI * (i + 1) / segs + t * Mth.TWO_PI;

            float x0 = (float) (center.x + Math.cos(a0) * r);
            float y0 = (float) (center.y + 0.5 + Math.sin(a0) * r * 0.3);
            float z0 = (float) (center.z + Math.sin(a0) * r);
            float x1 = (float) (center.x + Math.cos(a1) * r);
            float y1 = (float) (center.y + 0.5 + Math.sin(a1) * r * 0.3);
            float z1 = (float) (center.z + Math.sin(a1) * r);

            buffer.addVertex(m, x0 - right.x * halfW, y0 - right.y * halfW, z0 - right.z * halfW).setColor(c[0], c[1], c[2], 0.8F);
            buffer.addVertex(m, x0 + right.x * halfW, y0 + right.y * halfW, z0 + right.z * halfW).setColor(c[0], c[1], c[2], 0.8F);
            buffer.addVertex(m, x1 + right.x * halfW, y1 + right.y * halfW, z1 + right.z * halfW).setColor(c[0], c[1], c[2], 0.8F);
            buffer.addVertex(m, x1 - right.x * halfW, y1 - right.y * halfW, z1 - right.z * halfW).setColor(c[0], c[1], c[2], 0.8F);
        }

        MeshData meshData = buffer.buildOrThrow();
        BufferUploader.drawWithShader(meshData);
    }

    private static float[] statusColor(ReceiverStatus status, int color) {
        float brightness = switch (status) {
            case ACTIVE -> 1.0F;
            case OVERLOADED -> 0.92F;
            case TRANSMITTER_DISABLED, RECEIVER_DISABLED -> 0.35F;
            case IDLE -> 0.62F;
            default -> 0.75F;
        };
        int rgb = color & 0xFFFFFF;
        return new float[]{
            ((rgb >> 16) & 0xFF) / 255.0F * brightness,
            ((rgb >> 8) & 0xFF) / 255.0F * brightness,
            (rgb & 0xFF) / 255.0F * brightness
        };
    }
}
