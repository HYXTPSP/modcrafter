# {{PACK_NAME}}

这是由 **ModCrafter 模组工坊** 在游戏内制作并导出的独立 Fabric 模组工程。
它不依赖 ModCrafter,可以单独编译、分发。

## 构建方法

需要 Java 21。在本目录执行:

```
./gradlew build          # Linux / macOS
gradlew.bat build        # Windows
```

首次构建会自动下载 Gradle 与依赖,需要联网,耐心等待。
构建产物在 `build/libs/{{PACK_ID}}-{{PACK_VERSION}}.jar`
(不要用带 `-sources` 后缀的那个)。

## 安装

把 jar 放进 Minecraft 1.21.1 (Fabric) 的 `mods` 文件夹,
同时需要安装 [Fabric API](https://modrinth.com/mod/fabric-api)。

## 内容

- 物品/方块: 见 `src/main/resources/packdata/pack.json`
- 配方/掉落: 见 `src/main/resources/data/`
- 贴图/模型: 见 `src/main/resources/assets/`

你可以直接修改这些 JSON 与贴图后重新构建。
