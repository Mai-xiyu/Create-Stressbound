package org.xiyu.create_stressbound;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = CreateStressbound.MODID)
public final class StressboundConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ALLOW_CROSS_DIMENSION = BUILDER
        .comment("Whether receivers may bind to transmitters in another dimension. | 是否允许接收端绑定到其他维度的发送端。")
        .define("general.allowCrossDimensionTransmission", false);

    private static final ModConfigSpec.BooleanValue REQUIRE_LOADED_CHUNKS = BUILDER
        .comment("When enabled, both endpoints must be loaded for the link to activate. | 启用后，链路两端都必须加载才会生效。")
        .define("general.requireLoadedChunks", true);

    private static final ModConfigSpec.BooleanValue TRANSMITTER_POWERED_STOPS = BUILDER
        .comment("If true, redstone throttles transmitter output from full at signal 0 to stopped at signal 15. | 为 true 时，发送端会按红石强度调制输出：信号 0 为满输出，信号 15 为停止。")
        .define("redstone.transmitterPoweredStops", true);

    private static final ModConfigSpec.BooleanValue RECEIVER_POWERED_STOPS = BUILDER
        .comment("If true, redstone throttles receiver output from full at signal 0 to stopped at signal 15. | 为 true 时，接收端会按红石强度调制输出：信号 0 为满输出，信号 15 为停止。")
        .define("redstone.receiverPoweredStops", true);

    private static final ModConfigSpec.BooleanValue STRICT_OVERLOAD_MODE = BUILDER
        .comment("If true, a transmitter shuts down all attached receivers when the reserved budget exceeds supply. | 为 true 时，当预留应力超过供给时，发送端会关闭所有连接的接收端。")
        .define("balance.strictOverloadMode", true);

    private static final ModConfigSpec.IntValue MAX_LINKS_PER_PLAYER = BUILDER
        .comment("Maximum number of active links a single player may own. | 单个玩家最多可拥有的活动链路数量。")
        .defineInRange("limits.maxLinksPerPlayer", 64, 1, 4096);

    private static final ModConfigSpec.IntValue MAX_RECEIVERS_PER_TRANSMITTER = BUILDER
        .comment("Maximum number of receivers attached to one transmitter. | 单个发送端最多可连接的接收端数量。")
        .defineInRange("limits.maxReceiversPerTransmitter", 8, 1, 256);

    private static final ModConfigSpec.IntValue MAX_STRESS_PER_LINK = BUILDER
        .comment("Hard cap for one receiver's reserved SU budget. Use -1 for unlimited. | 单个接收端预留 SU 的硬上限，设为 -1 表示无限制。")
        .defineInRange("limits.maxStressPerLink", 512, -1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue DEFAULT_REQUESTED_STRESS = BUILDER
        .comment("Default reserved SU budget assigned to a new receiver. | 新接收端默认预留的 SU 预算。")
        .defineInRange("limits.defaultRequestedStress", 256, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue EVALUATION_INTERVAL_TICKS = BUILDER
        .comment("Base interval for recomputing link state and stress budgets. Redstone control may use a faster interval. | 服务端重新计算链路状态和应力预算的基础间隔；红石控制可使用更快间隔。")
        .defineInRange("server.evaluationIntervalTicks", 20, 1, 200);

    private static final ModConfigSpec.IntValue REDSTONE_EVALUATION_INTERVAL_TICKS = BUILDER
        .comment("When redstone throttling is enabled, links are recomputed at least this often. Use 1 for stable feedback builds. | 启用红石调速时，链路至少按此间隔重算；不倒翁等反馈结构建议为 1。")
        .defineInRange("server.redstoneEvaluationIntervalTicks", 1, 1, 20);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean allowCrossDimensionTransmission;
    public static boolean requireLoadedChunks;
    public static boolean transmitterPoweredStops;
    public static boolean receiverPoweredStops;
    public static boolean strictOverloadMode;
    public static int maxLinksPerPlayer;
    public static int maxReceiversPerTransmitter;
    public static int maxStressPerLink;
    public static int defaultRequestedStress;
    public static int evaluationIntervalTicks;
    public static int redstoneEvaluationIntervalTicks;

    private StressboundConfig() {
    }

    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        allowCrossDimensionTransmission = ALLOW_CROSS_DIMENSION.get();
        requireLoadedChunks = REQUIRE_LOADED_CHUNKS.get();
        transmitterPoweredStops = TRANSMITTER_POWERED_STOPS.get();
        receiverPoweredStops = RECEIVER_POWERED_STOPS.get();
        strictOverloadMode = STRICT_OVERLOAD_MODE.get();
        maxLinksPerPlayer = MAX_LINKS_PER_PLAYER.get();
        maxReceiversPerTransmitter = MAX_RECEIVERS_PER_TRANSMITTER.get();
        maxStressPerLink = MAX_STRESS_PER_LINK.get();
        defaultRequestedStress = DEFAULT_REQUESTED_STRESS.get();
        evaluationIntervalTicks = EVALUATION_INTERVAL_TICKS.get();
        redstoneEvaluationIntervalTicks = REDSTONE_EVALUATION_INTERVAL_TICKS.get();
    }

    public static int clampRequestedStress(int requestedStress) {
        int clamped = Math.max(1, requestedStress);
        if (maxStressPerLink > 0) {
            clamped = Math.min(clamped, maxStressPerLink);
        }
        return clamped;
    }

    public static int clampRedstoneSignal(int signal) {
        return Math.max(0, Math.min(15, signal));
    }

    public static float redstoneOutputScale(int signal) {
        return (15 - clampRedstoneSignal(signal)) / 15.0F;
    }

    public static int scaleStressByRedstone(int stress, int signal) {
        float scale = redstoneOutputScale(signal);
        if (stress <= 0 || scale <= 0.0F) {
            return 0;
        }
        return Math.max(1, Math.round(stress * scale));
    }
}
