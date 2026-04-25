# Create Stressbound

Create Stressbound 是一个面向 Minecraft 1.21.1 / NeoForge / Create 6.0.x 的 Create 附属模组。它提供“应力发送端 / 应力接收端”这一组方块，用于在静态 Create 动力网络、Create Contraption、Create 火车以及 Create Aeronautics 风格的移动结构之间建立服务端友好的远程应力链路。

## 功能特性

- 通过 `Stress Transmitter` 和 `Stress Receiver` 建立远程 Create 应力传输。
- 支持静态 Create 动力网络。
- 支持 Create Contraption、Create Train 的运行时锚点桥接。
- 对 Create Aeronautics / Simulated 提供启发式运行时锚点支持。
- 支持工程师护目镜 HUD，显示链路状态、远程转速、预留/获得的 SU 预算。
- 提供 `Kinetic Binder` 绑定器，右键即可在世界内绑定或清除链路。
- 服务端友好：链路数量、单发送端接收端数量、评估间隔、应力预算都可配置。
- 内置英文和简体中文文本，配置注释采用 `English | 中文` 双语格式。

## 依赖

- Minecraft `1.21.1`
- NeoForge `21.1.x`
- Create `6.0.10`
- Java `21`

可选兼容目标：

- Create Aeronautics / Simulated
- Create 自带 Contraption
- Create 自带火车

## 基本用法

1. 将 `Stress Transmitter` 放在已有动力的 Create 传动网络旁。
2. 将 `Stress Receiver` 放在目标传动网络旁。
3. 用 `Kinetic Binder` 右键发送端。
4. 再用同一个绑定器右键接收端。
5. 戴上 Create 工程师护目镜查看链路状态、远程转速和 SU 预算。

快捷操作：

- 潜行右键接收端：清除该接收端链路。
- 潜行右键空气：清除绑定器中记录的发送端。

## 命令

以下命令需要权限等级 2：

```mcfunction
/stressbound links list
/stressbound links remove <uuid>
/stressbound links removeplayer <player>
/stressbound links setstress <uuid> <su>
/stressbound links setallstress <su>
/stressbound compat
```

高转速测试时可以直接提高已有链路预算：

```mcfunction
/stressbound links setallstress 8192
```

如果还不够，可以继续提高：

```mcfunction
/stressbound links setallstress 16384
```

## 配置

配置文件位置：

```text
config/create_stressbound-common.toml
```

常用配置：

- `defaultRequestedStress`：新接收端默认预留的 SU 预算。
- `maxStressPerLink`：单条链路的 SU 上限，设为 `-1` 表示不限制。
- `strictOverloadMode`：启用后，一个发送端下总预留超过供给时会整体停机。
- `evaluationIntervalTicks`：服务端重新计算链路预算的间隔。
- `maxLinksPerPlayer`：单玩家最多拥有的活动链路数量。
- `maxReceiversPerTransmitter`：单个发送端最多连接的接收端数量。

注意：已有链路会把 `RequestedStress` 存进世界数据。修改 `defaultRequestedStress` 只影响新链路；旧链路请重新绑定，或使用：

```mcfunction
/stressbound links setallstress <su>
```

## 兼容说明

当前支持：

- 静态 Create 动力网络：支持。
- Create Contraption：运行时锚点支持。
- Create 火车：运行时锚点支持。
- Create Aeronautics / Simulated：启发式运行时锚点支持。
- Valkyrien Skies：可检测，但尚未实现完整运行时桥接。

为了保持服务端友好，移动中的接收端不会在实体移动时强行改写 Create 动力网络；链路会保留，并在结构重新变回静态方块后恢复输出。

## 构建

Windows：

```powershell
./gradlew.bat build
```

Linux / macOS：

```bash
./gradlew build
```

构建产物：

```text
build/libs/create_stressbound-1.0-SNAPSHOT.jar
```

## License

All Rights Reserved.
