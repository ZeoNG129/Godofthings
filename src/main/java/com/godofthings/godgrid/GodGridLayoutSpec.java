package com.godofthings.godgrid;

/**
 * 单台机器的界面几何 —— v5.30.0 起，18 台机器的面板尺寸与槽位排布<b>各按自己的 UI 原画</b>，
 * 不再共用一套 {@link GodGridLayout} 常量。
 *
 * <p><b>为什么改成 per-machine</b>：重绘资源包里 18 张概念原画有三种不同宽高比
 * （1376×768 = 1.79、1200×896 = 1.34、1024×1024 = 1.0），槽位行数/列数也各不相同
 * （熔炉上排 17 下排 15、温床左右各 4×3、秘柜多排……）。原先「一套 362×242 服务全部机器」
 * 的做法必然对不上任何一张原画。</p>
 *
 * <p><b>这份 spec 是三处共用的唯一真相</b>：</p>
 * <ol>
 *   <li>菜单 {@link AbstractGodGridMenu} —— 按它 addSlot，所以点击区域永远等于这里的坐标；</li>
 *   <li>界面 {@link AbstractGodGridScreen} —— 按它画槽框/箭头/页签，并把底图按面板尺寸 blit；</li>
 *   <li>底图 blit 的裁剪尺寸 —— 贴图按 spec 的面板宽高裁左上角那一块。</li>
 * </ol>
 *
 * <p>坐标全部是<b>面板内相对坐标</b>（左上角为 0,0），与 {@code leftPos/topPos} 无关。</p>
 */
public final class GodGridLayoutSpec
{
    /** 注册名（与 godofthings:xxx 方块 id 对应，便于日志排查） */
    public final String id;

    // ---- 面板 ----
    public final int imageW;
    public final int imageH;

    // ---- 输入区：一行或多行，cols 列，起点 (inX, inY)，行列间距 pitchX / pitchY ----
    public final int inCols;
    public final int inRows;
    public final int inX;
    public final int inY;

    // ---- 输出区 ----
    public final int outCols;
    public final int outRows;
    public final int outX;
    public final int outY;

    /** 槽位边长（多数机器 18；小尺寸原画可用 16） */
    public final int slot;
    /** 列距 */
    public final int pitchX;
    /** 行距 */
    public final int pitchY;

    // ---- 加速槽 ----
    public final int accelX;
    public final int accelY;
    public final boolean hasAccel;

    // ---- 玩家物品栏 ----
    public final int invX;
    public final int invY;
    public final int hotbarOffset;
    /** 物品栏格距（原画若是紧排网格，可小于 18） */
    public final int invPitch;

    // ---- 进度箭头（只画装饰，不占槽） ----
    public final int arrowX;
    public final int arrowY;

    // ---- 底图 ----
    /** 贴图文件内的实际像素宽（blit 的 texW） */
    public final int texW;
    /** 贴图文件内的实际像素高（blit 的 texH） */
    public final int texH;

    private GodGridLayoutSpec(Builder b)
    {
        this.id = b.id;
        this.imageW = b.imageW;
        this.imageH = b.imageH;
        this.inCols = b.inCols;
        this.inRows = b.inRows;
        this.inX = b.inX;
        this.inY = b.inY;
        this.outCols = b.outCols;
        this.outRows = b.outRows;
        this.outX = b.outX;
        this.outY = b.outY;
        this.slot = b.slot;
        this.pitchX = b.pitchX;
        this.pitchY = b.pitchY;
        this.accelX = b.accelX;
        this.accelY = b.accelY;
        this.hasAccel = b.hasAccel;
        this.invX = b.invX;
        this.invY = b.invY;
        this.hotbarOffset = b.hotbarOffset;
        this.invPitch = b.invPitch;
        this.arrowX = b.arrowX;
        this.arrowY = b.arrowY;
        this.texW = b.texW;
        this.texH = b.texH;
    }

    public int inputCount()
    {
        return this.inCols * this.inRows;
    }

    public int outputCount()
    {
        return this.outCols * this.outRows;
    }

    public int machineSlots()
    {
        return inputCount() + outputCount();
    }

    /** 第 i 个输入槽的 X */
    public int inputX(int i)
    {
        return this.inX + (i % this.inCols) * this.pitchX;
    }

    /** 第 i 个输入槽的 Y */
    public int inputY(int i)
    {
        return this.inY + (i / this.inCols) * this.pitchY;
    }

    /** 第 j 个输出槽的 X */
    public int outputX(int j)
    {
        return this.outX + (j % this.outCols) * this.pitchX;
    }

    /** 第 j 个输出槽的 Y */
    public int outputY(int j)
    {
        return this.outY + (j / this.outCols) * this.pitchY;
    }

    /** 玩家背包第 (row, col) 格的 X */
    public int invSlotX(int col)
    {
        return this.invX + col * this.invPitch;
    }

    /** 玩家背包第 row 行的 Y */
    public int invSlotY(int row)
    {
        return this.invY + row * this.invPitch;
    }

    /** 快捷栏 Y */
    public int hotbarY()
    {
        return this.invY + this.hotbarOffset;
    }

    // ---------------- builder ----------------

    public static Builder builder(String id, int imageW, int imageH)
    {
        return new Builder(id, imageW, imageH);
    }

    /** 链式构造器：只写与默认值不同的项。 */
    public static final class Builder
    {
        private final String id;
        private final int imageW;
        private final int imageH;
        private int inCols = 0;
        private int inRows = 1;
        private int inX = 0;
        private int inY = 0;
        private int outCols = 0;
        private int outRows = 1;
        private int outX = 0;
        private int outY = 0;
        private int slot = 18;
        private int pitchX = 18;
        private int pitchY = 18;
        private int accelX = 0;
        private int accelY = 0;
        private boolean hasAccel = true;
        private int invX = 0;
        private int invY = 0;
        private int hotbarOffset = 58;
        private int invPitch = 18;
        private int arrowX = 0;
        private int arrowY = 0;
        private int texW = 512;
        private int texH = 512;

        private Builder(String id, int imageW, int imageH)
        {
            this.id = id;
            this.imageW = imageW;
            this.imageH = imageH;
        }

        public Builder inputs(int cols, int rows, int x, int y)
        {
            this.inCols = cols;
            this.inRows = rows;
            this.inX = x;
            this.inY = y;
            return this;
        }

        public Builder outputs(int cols, int rows, int x, int y)
        {
            this.outCols = cols;
            this.outRows = rows;
            this.outX = x;
            this.outY = y;
            return this;
        }

        public Builder slot(int slot, int pitchX, int pitchY)
        {
            this.slot = slot;
            this.pitchX = pitchX;
            this.pitchY = pitchY;
            return this;
        }

        public Builder accel(int x, int y)
        {
            this.accelX = x;
            this.accelY = y;
            this.hasAccel = true;
            return this;
        }

        public Builder noAccel()
        {
            this.hasAccel = false;
            return this;
        }

        public Builder inventory(int x, int y, int hotbarOffset, int pitch)
        {
            this.invX = x;
            this.invY = y;
            this.hotbarOffset = hotbarOffset;
            this.invPitch = pitch;
            return this;
        }

        public Builder arrow(int x, int y)
        {
            this.arrowX = x;
            this.arrowY = y;
            return this;
        }

        public Builder texture(int texW, int texH)
        {
            this.texW = texW;
            this.texH = texH;
            return this;
        }

        public GodGridLayoutSpec build()
        {
            return new GodGridLayoutSpec(this);
        }
    }
}
