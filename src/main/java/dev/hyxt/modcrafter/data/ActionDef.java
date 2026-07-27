package dev.hyxt.modcrafter.data;

/**
 * 动作定义。type 决定使用哪些参数字段:
 *  MESSAGE     text                        给玩家发消息(支持 & 颜色符)
 *  GIVE_EFFECT effect,duration,amplifier   给玩家药水效果
 *  EXPLOSION   power,breakBlocks           在玩家/方块处爆炸
 *  LIGHTNING   -                           落雷
 *  COMMAND     command                     以玩家身份执行命令(权限4,支持 {player} {x} {y} {z})
 *  GIVE_ITEM   item,count                  给玩家物品
 *  PLAY_SOUND  sound                       播放声音
 *  LAUNCH      power                       把玩家向上弹射
 *  HEAL        amount                      治疗
 *  DAMAGE      amount                      伤害
 *  SET_FIRE    seconds                     点燃玩家
 *  SPAWN_ENTITY entity,count               在触发位置生成实体
 *  GIVE_XP     xp                          给予经验点
 *  TELEPORT_RELATIVE dx,dy,dz              相对传送玩家
 *  SET_WEATHER weather                     设置天气(CLEAR/RAIN/THUNDER)
 *  PARTICLE    particle,count              在触发位置播放粒子
 */
public class ActionDef {
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
    /** CLEAR / RAIN / THUNDER */
    public String weather = "RAIN";
    public String particle = "minecraft:heart";
}
