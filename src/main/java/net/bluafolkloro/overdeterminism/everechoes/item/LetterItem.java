package net.bluafolkloro.overdeterminism.everechoes.item;

import net.bluafolkloro.overdeterminism.everechoes.letter.LetterContents;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterData;
import net.bluafolkloro.overdeterminism.everechoes.letter.LetterState;
import net.bluafolkloro.overdeterminism.everechoes.menu.LetterMenu;
import net.bluafolkloro.overdeterminism.everechoes.postal.Waybill;
import net.bluafolkloro.overdeterminism.everechoes.postal.Waybills;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class LetterItem extends Item {
    private final LetterState expectedState;

    public LetterItem(Properties properties, LetterState expectedState) {
        super(properties);
        this.expectedState = expectedState;
    }

    public LetterState expectedState() {
        return expectedState;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (LetterContents.get(stack) == null && expectedState != LetterState.DRAFT) {
            return InteractionResultHolder.fail(stack);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            LetterData data = LetterContents.ensureInitialized(stack, serverPlayer);
            if (data == null) {
                return InteractionResultHolder.fail(stack);
            }
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, inventory, menuPlayer) -> new LetterMenu(containerId, inventory, hand, data),
                            Component.translatable(screenTitleKey(data.state()))
                    ),
                    buffer -> LetterMenu.writeOpeningData(buffer, hand, data)
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public Component getName(ItemStack stack) {
        LetterData data = LetterContents.get(stack);
        if (data != null && !data.isSealed() && !data.title().isBlank()) {
            return Component.literal(data.title());
        }

        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        LetterData data = LetterContents.get(stack);
        if (data == null) {
            return;
        }

        tooltipComponents.add(Component.translatable(
                "item.everechoes.letter.tooltip.state",
                Component.translatable(stateKey(data.state()))
        ).withStyle(ChatFormatting.GRAY));

        if (!data.isSealed() && !data.title().isBlank()) {
            tooltipComponents.add(Component.translatable(
                    "item.everechoes.letter.tooltip.title",
                    data.title()
            ).withStyle(ChatFormatting.GRAY));
        }

        data.recipientAddress().ifPresentOrElse(
                address -> tooltipComponents.add(Component.translatable(
                        "item.everechoes.letter.tooltip.recipient",
                        LetterContents.formatAddress(address, context.level())
                ).withStyle(ChatFormatting.GRAY)),
                () -> tooltipComponents.add(Component.translatable("item.everechoes.letter.tooltip.no_recipient")
                        .withStyle(ChatFormatting.DARK_GRAY))
        );

        Waybill waybill = Waybills.get(stack);
        if (waybill != null) {
            if (waybill.state() == Waybill.CustodyState.AWAITING_CARRIER) {
                tooltipComponents.add(Component.translatable("item.everechoes.letter.tooltip.awaiting_carrier")
                        .withStyle(ChatFormatting.DARK_AQUA));
            } else {
                tooltipComponents.add(Component.translatable("item.everechoes.letter.tooltip.in_transit")
                        .withStyle(ChatFormatting.DARK_AQUA));
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        LetterData data = LetterContents.get(stack);
        return (data != null && data.isSealed()) || super.isFoil(stack);
    }

    private static String stateKey(LetterState state) {
        return switch (state) {
            case DRAFT -> "item.everechoes.letter.state.draft";
            case SEALED -> "item.everechoes.letter.state.sealed";
            case OPENED -> "item.everechoes.letter.state.opened";
        };
    }

    private static String screenTitleKey(LetterState state) {
        return switch (state) {
            case DRAFT -> "gui.everechoes.letter.screen.edit";
            case SEALED -> "gui.everechoes.letter.screen.sealed";
            case OPENED -> "gui.everechoes.letter.screen.read";
        };
    }
}
