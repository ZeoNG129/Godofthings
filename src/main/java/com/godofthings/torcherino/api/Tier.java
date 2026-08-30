package com.godofthings.torcherino.api;

/**
 * 加速火把等级：最大速度 / XZ 范围 / Y 范围。
 * 移植自 Torcherino（MIT License，Copyright (c) 2021 NinjaPhenix）。
 */
public record Tier(int maxSpeed, int xzRange, int yRange) {
}
