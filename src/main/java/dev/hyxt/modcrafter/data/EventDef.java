package dev.hyxt.modcrafter.data;

import java.util.ArrayList;
import java.util.List;

/** 事件定义: 触发器 + 目标元素 + 动作列表 */
public class EventDef {
    public String id = "";

    /**
     * 触发器类型:
     *  ITEM_USE           右键使用物品
     *  ITEM_ATTACK_ENTITY 手持物品攻击实体
     *  ITEM_EATEN         吃下食物(仅 FOOD 类型物品)
     *  BLOCK_BREAK        方块被挖掘
     *  BLOCK_USE          右键点击方块
     *  BLOCK_STEPPED_ON   实体踩在方块上
     */
    public String trigger = "ITEM_USE";

    /** 目标元素完整 id,如 "mypack:ruby" */
    public String target = "";

    public List<ActionDef> actions = new ArrayList<>();
}
