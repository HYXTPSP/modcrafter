package dev.hyxt.modcrafter.runtime.content;

import net.minecraft.block.Block;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

/**
 * 体素包围盒 -> 旋转后的 VoxelShape。
 * 基准模型面向北;数学与 blockstate 旋转(east=y90, south=y180, west=y270, up=x270, down=x90)一致。
 */
public final class ShapeUtil {

    private ShapeUtil() {
    }

    /** bounds: [minX,minY,minZ,maxX,maxY,maxZ] 0-16 坐标 */
    public static VoxelShape shapeFor(double[] b, Direction facing) {
        double[] p1 = transform(b[0], b[1], b[2], facing);
        double[] p2 = transform(b[3], b[4], b[5], facing);
        return Block.createCuboidShape(
            Math.min(p1[0], p2[0]), Math.min(p1[1], p2[1]), Math.min(p1[2], p2[2]),
            Math.max(p1[0], p2[0]), Math.max(p1[1], p2[1]), Math.max(p1[2], p2[2]));
    }

    private static double[] transform(double x, double y, double z, Direction facing) {
        return switch (facing) {
            case EAST -> new double[]{16 - z, y, x};
            case SOUTH -> new double[]{16 - x, y, 16 - z};
            case WEST -> new double[]{z, y, 16 - x};
            case UP -> new double[]{x, 16 - z, y};
            case DOWN -> new double[]{x, z, 16 - y};
            default -> new double[]{x, y, z}; // NORTH
        };
    }

    public static boolean isFullCube(double[] b) {
        return b[0] <= 0 && b[1] <= 0 && b[2] <= 0 && b[3] >= 16 && b[4] >= 16 && b[5] >= 16;
    }
}
