package net.bluafolkloro.overdeterminism.everechoes.block;

import net.minecraft.util.StringRepresentable;

public enum MailBoxStyle implements StringRepresentable {
    POST("post"),
    DESK("desk"),
    WALL("wall"),
    HANGING("hanging"),
    HANGING_RAISED("hanging_raised"),
    HANGING_CHAINED("hanging_chained");

    public static final StringRepresentable.EnumCodec<MailBoxStyle> CODEC = StringRepresentable.fromEnum(MailBoxStyle::values);

    private final String name;

    MailBoxStyle(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
