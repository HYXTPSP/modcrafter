package dev.hyxt.modcrafter.data;

/** 自定义方块定义 */
public class BlockDef {
    public String id = "";
    public String name = "";
    /** 同 ItemDef.texture: preset:xxx / custom:xxx (SINGLE 模式使用) */
    public String texture = "preset:block_stone";

    /** 贴图/模型模式: SINGLE(单一贴图) / PER_FACE(六面贴图) / MODEL(体素模型) */
    public String textureMode = "SINGLE";

    /** PER_FACE 模式的六面贴图 */
    public FacesDef faces = new FacesDef();

    /** MODEL 模式的体素模型名 */
    public String model = "";

    /** 朝向: NONE / HORIZONTAL(放置时面向玩家,水平四向) / ALL(六向,像投掷器) */
    public String facingMode = "NONE";

    public static class FacesDef {
        public String up = "preset:block_white";
        public String down = "preset:block_white";
        public String north = "preset:block_white";
        public String south = "preset:block_white";
        public String east = "preset:block_white";
        public String west = "preset:block_white";
    }

    public float hardness = 3.0f;
    public float resistance = 6.0f;
    /** 0-15 */
    public int luminance = 0;

    /** 是否需要正确工具才掉落 */
    public boolean requiresTool = false;
    /** 挖掘工具类型: pickaxe / axe / shovel / hoe */
    public String toolType = "pickaxe";
    /** 工具等级: NONE(木) / STONE / IRON / DIAMOND */
    public String toolLevel = "NONE";

    /** STONE / WOOD / METAL / GLASS / GRASS / SAND / WOOL */
    public String sound = "STONE";

    /** 透明方块(贴图透明部分镂空,如玻璃/栅栏样式贴图) */
    public boolean transparent = false;
    /** 滑度: 0.6=普通, 0.98=冰, 0.989=蓝冰 */
    public float slipperiness = 0.6f;

    public DropDef drop = new DropDef();

    /** 矿石世界生成(仅主世界地下) */
    public OreGenDef oreGen = new OreGenDef();

    public static class OreGenDef {
        public boolean enabled = false;
        /** 每条矿脉的矿石数 */
        public int veinSize = 6;
        /** 每个区块的矿脉数 */
        public int veinsPerChunk = 8;
        public int minY = -60;
        public int maxY = 40;
    }

    public static class DropDef {
        /** SELF(掉自己) / NONE(不掉落) / CUSTOM(掉指定物品) */
        public String mode = "SELF";
        public String item = "";
        public int min = 1;
        public int max = 1;
    }
}
