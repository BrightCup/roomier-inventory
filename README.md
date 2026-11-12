# Roomier Inventory

一个面向 Minecraft **1.21.1 / NeoForge** 的轻量物品栏扩展模组。不改配方、不加方块，只把原版物品栏放大并加一排工具架。

## 功能

- 主物品栏从 9×3 扩到 **10×5**（27 → 50 格）
- 物品栏左侧浮动 **6 个工具槽**，只接受剑、斧、镐、锹、锄
- 箱子、木桶、潜影盒、末影箱等 `ChestMenu` 界面同样显示扩展格
- 界面过高时可用鼠标滚轮上下滚动

## 实现要点

`Inventory.getItem(int)` 是按 `compartments`（items → armor → offhand）顺序累加索引解析的，所以不能简单地扩大 `items` 数组。本项目在 offhand 之后追加了第 4 个 compartment：

| 索引 | 内容 |
| --- | --- |
| 0-35 | 原版 items（快捷栏 + 主栏） |
| 36-39 | armor |
| 40 | offhand |
| 41-63 | 新增的 23 格 |
| 64-69 | 工具槽 |

这样原版索引全部保持原义，存档、死亡掉落、重生复制都能直接复用原版逻辑。扩展格在 NBT 里使用 `Slot` 200+ 偏移，避开 armor 的 100 和 offhand 的 150。

## 构建

需要 JDK 21。

```bash
./gradlew build
```

产物在 `build/libs/roomier-inventory-1.0.0.jar`。

> 仓库未包含 `gradle/wrapper/gradle-wrapper.jar`（二进制文件未随源码提交）。首次构建前先执行一次 `gradle wrapper --gradle-version 9.2.1`，或直接用 IntelliJ IDEA 导入该 Gradle 项目。

国内网络如果拉不到 `maven.neoforged.net`，可参考项目外的 `neoforge-mirror.init.gradle` 思路，把仓库重定向到 `https://neoforged.forgecdn.net/releases`。

## 已知限制

- 服务端也需要安装（菜单槽位数在服务端定义）。
- 工作台、熔炉、漏斗等其它容器界面尚未接入。
- 与重绘物品栏界面的模组（如 Slate UI 的 Custom 布局）存在渲染层冲突，测试时建议切到 Vanilla 布局。

## 许可证

MIT
