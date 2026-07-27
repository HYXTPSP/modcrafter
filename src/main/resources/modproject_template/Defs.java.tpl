package {{PACKAGE}};

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** pack.json 数据模型(由 ModCrafter 模组工坊导出) */
public final class Defs {

    public static class Pack {
        public String id = "";
        public String name = "";
        public List<ItemD> items = new ArrayList<>();
        public List<BlockD> blocks = new ArrayList<>();
        public List<EventD> events = new ArrayList<>();
    }

    public static class ItemD {
        public String id = "";
        public String name = "";
        public List<String> tooltip = new ArrayList<>();
        public String type = "ITEM";
        public String armorMaterial = "IRON";
        public String armorTexMode = "VANILLA";
        public String armorColor = "#FF5555";
        public String model = "";
        public int maxCount = 64;
        public int maxDamage = 0;
        public String rarity = "COMMON";
        public boolean fireproof = false;
        public boolean glint = false;
        public FoodD food = null;
        public ToolD tool = null;

        public boolean isTool() {
            return "SWORD".equals(type) || "PICKAXE".equals(type)
                || "AXE".equals(type) || "SHOVEL".equals(type) || "HOE".equals(type);
        }
    }

    public static class FoodD {
        public int nutrition = 4;
        public float saturation = 0.3f;
        public boolean alwaysEdible = false;
    }

    public static class ToolD {
        public String material = "IRON";
        public float attackDamage = 3.0f;
        public float attackSpeed = -2.4f;
    }

    public static class BlockD {
        public String id = "";
        public String name = "";
        public float hardness = 3.0f;
        public float resistance = 6.0f;
        public int luminance = 0;
        public boolean requiresTool = false;
        public String sound = "STONE";
        public boolean transparent = false;
        public float slipperiness = 0.6f;
        public OreGenD oreGen = null;
        public String textureMode = "SINGLE";
        public String facingMode = "NONE";
    }

    public static class OreGenD {
        public boolean enabled = false;
        public int veinSize = 6;
        public int veinsPerChunk = 8;
        public int minY = -60;
        public int maxY = 40;
    }

    public static class EventD {
        public String id = "";
        public String trigger = "ITEM_USE";
        public String target = "";
        public List<ActionD> actions = new ArrayList<>();
    }

    public static class ActionD {
        public String type = "MESSAGE";
        public String text = "";
        public String effect = "minecraft:speed";
        public int duration = 200;
        public int amplifier = 0;
        public float power = 2.0f;
        public boolean breakBlocks = false;
        public String command = "";
        public String item = "";
        public int count = 1;
        public String sound = "minecraft:entity.experience_orb.pickup";
        public float amount = 4.0f;
        public int seconds = 3;
        public String entity = "minecraft:zombie";
        public int xp = 10;
        public float dx = 0;
        public float dy = 10;
        public float dz = 0;
        public String weather = "RAIN";
        public String particle = "minecraft:heart";
    }

    public static Pack load() {
        try (InputStream in = Defs.class.getResourceAsStream("/packdata/pack.json")) {
            if (in == null) return new Pack();
            Pack pack = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Pack.class);
            if (pack == null) return new Pack();
            if (pack.items == null) pack.items = new ArrayList<>();
            if (pack.blocks == null) pack.blocks = new ArrayList<>();
            if (pack.events == null) pack.events = new ArrayList<>();
            return pack;
        } catch (Exception e) {
            return new Pack();
        }
    }

    /** 体素模型方块的包围盒(导出时预计算): blockId -> [minX,minY,minZ,maxX,maxY,maxZ] 0-16 */
    public static java.util.Map<String, double[]> loadShapes() {
        try (InputStream in = Defs.class.getResourceAsStream("/packdata/shapes.json")) {
            if (in == null) return new java.util.HashMap<>();
            java.util.Map<String, double[]> map = new Gson().fromJson(
                new InputStreamReader(in, StandardCharsets.UTF_8),
                new com.google.gson.reflect.TypeToken<java.util.Map<String, double[]>>() {
                }.getType());
            return map == null ? new java.util.HashMap<>() : map;
        } catch (Exception e) {
            return new java.util.HashMap<>();
        }
    }

    private Defs() {
    }
}
