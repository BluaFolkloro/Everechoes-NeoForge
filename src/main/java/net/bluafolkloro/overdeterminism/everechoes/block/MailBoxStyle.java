package net.bluafolkloro.overdeterminism.everechoes.block;

import net.minecraft.util.StringRepresentable;

/**
 * 封装一组受限且稳定的业务状态。
 */
public enum MailBoxStyle implements StringRepresentable {
    /**
     * 定义 {@code POST} 常量。
     */
    POST("post"),
    /**
     * 定义 {@code DESK} 常量。
     */
    DESK("desk"),
    /**
     * 定义 {@code WALL} 常量。
     */
    WALL("wall"),
    /**
     * 定义 {@code HANGING} 常量。
     */
    HANGING("hanging"),
    /**
     * 定义 {@code HANGING_RAISED} 常量。
     */
    HANGING_RAISED("hanging_raised"),
    /**
     * 定义 {@code HANGING_CHAINED} 常量。
     */
    HANGING_CHAINED("hanging_chained");

    /**
     * 定义 {@code CODEC} 常量。
     */
    public static final StringRepresentable.EnumCodec<MailBoxStyle> CODEC = StringRepresentable.fromEnum(MailBoxStyle::values);

    /**
     * 保存 {@code name} 对应的状态或依赖。
     */
    private final String name;

    /**
     * 创建并初始化当前对象。
     */
    MailBoxStyle(String name) {
        this.name = name;
    }

    /**
     * 获取或解析 {@code getSerializedName} 对应的业务操作。
     */
    @Override
    public String getSerializedName() {
        return this.name;
    }
}
