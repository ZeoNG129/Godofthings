package com.godofthings.waypoint;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 传送点：名称 + 维度 + 精确坐标 + 面对方向（yaw/pitch）+ 置顶标记。
 * 由 /setpoint 指令写入，/point 指令或「神之记录」方块 UI 传送。
 */
public class Waypoint
{
    public String name = "";
    public String dimension = "minecraft:overworld";
    public double x, y, z;
    public float yaw, pitch;
    public boolean pinned;

    public CompoundTag save(CompoundTag tag)
    {
        tag.putString("Name", name);
        tag.putString("Dimension", dimension);
        tag.putDouble("X", x);
        tag.putDouble("Y", y);
        tag.putDouble("Z", z);
        tag.putFloat("Yaw", yaw);
        tag.putFloat("Pitch", pitch);
        tag.putBoolean("Pinned", pinned);
        return tag;
    }

    public static Waypoint load(CompoundTag tag)
    {
        Waypoint wp = new Waypoint();
        wp.name = tag.getString("Name");
        wp.dimension = tag.getString("Dimension");
        wp.x = tag.getDouble("X");
        wp.y = tag.getDouble("Y");
        wp.z = tag.getDouble("Z");
        wp.yaw = tag.getFloat("Yaw");
        wp.pitch = tag.getFloat("Pitch");
        wp.pinned = tag.getBoolean("Pinned");
        return wp;
    }

    public static void write(FriendlyByteBuf buf, Waypoint wp)
    {
        buf.writeUtf(wp.name);
        buf.writeUtf(wp.dimension);
        buf.writeDouble(wp.x);
        buf.writeDouble(wp.y);
        buf.writeDouble(wp.z);
        buf.writeFloat(wp.yaw);
        buf.writeFloat(wp.pitch);
        buf.writeBoolean(wp.pinned);
    }

    public static Waypoint read(FriendlyByteBuf buf)
    {
        Waypoint wp = new Waypoint();
        wp.name = buf.readUtf();
        wp.dimension = buf.readUtf();
        wp.x = buf.readDouble();
        wp.y = buf.readDouble();
        wp.z = buf.readDouble();
        wp.yaw = buf.readFloat();
        wp.pitch = buf.readFloat();
        wp.pinned = buf.readBoolean();
        return wp;
    }
}
