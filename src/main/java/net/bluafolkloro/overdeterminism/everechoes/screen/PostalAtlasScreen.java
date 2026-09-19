package net.bluafolkloro.overdeterminism.everechoes.screen;

import net.bluafolkloro.overdeterminism.everechoes.menu.PostalAtlasMenu;
import net.bluafolkloro.overdeterminism.everechoes.network.PostalAtlasRequestPayload;
import net.bluafolkloro.overdeterminism.everechoes.postal.DistrictCoverage;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalAtlasLimits;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalAtlasSnapshot;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalChunk;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class PostalAtlasScreen extends AbstractContainerScreen<PostalAtlasMenu> {
    private static final ResourceLocation LEATHER = AtlasSpriteButton.tex("panel_leather");
    private static final ResourceLocation PAPER = AtlasSpriteButton.tex("panel_paper");
    private static final ResourceLocation ENVELOPE = AtlasSpriteButton.tex("icon_envelope");
    private static final ResourceLocation POSTMARK = AtlasSpriteButton.tex("icon_postmark");
    private static final ResourceLocation QUILL = AtlasSpriteButton.tex("icon_quill");
    private static final ResourceLocation CELL_ADD = AtlasSpriteButton.tex("cell_add");
    private static final ResourceLocation CELL_REMOVE = AtlasSpriteButton.tex("cell_remove");
    private static final ResourceLocation CELL_FOREIGN = AtlasSpriteButton.tex("cell_foreign");
    private static final ResourceLocation CELL_HERE = AtlasSpriteButton.tex("cell_here");
    private static final ResourceLocation ICON_HUB = AtlasSpriteButton.tex("icon_hub");
    private static final ResourceLocation ICON_COLLECTION = AtlasSpriteButton.tex("icon_collection");

    private static final int INK = 0x2A1C10;
    private static final int INK_LABEL = 0x3F2A14;
    private static final int EMPTY = 0xFFE3D5A3;
    private static final int SAVED = 0xFFC4A35A;
    private static final int ADD = 0xFFE8D48A;
    private static final int REMOVE = 0xFFA06058;
    private static final int GRID = 0xFFD6C797;
    private static final int INDEX = 0xFFAA945B;
    private static final int HOVER = 0xFF5A3A20;
    private static final int STAMP = 0xFF80052C;
    private static final int PAPER_LINE = 0xFFD1C084;
    private static final int PAN_SHIFT_X = PostalAtlasLimits.DEFAULT_WINDOW_WIDTH / 2;
    private static final int PAN_SHIFT_Z = PostalAtlasLimits.DEFAULT_WINDOW_HEIGHT / 2;

    private final Set<Long> proposed = new LinkedHashSet<>();
    private int appliedEpoch = -1;
    private AtlasLayout layout;
    private AtlasSpriteButton applyButton;
    private AtlasSpriteButton revertButton;
    @Nullable
    private Component headerHover;

    public PostalAtlasScreen(PostalAtlasMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 280;
        this.imageHeight = 210;
        this.titleLabelY = 10000;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        layout = AtlasLayout.compute(this.width, this.height, this.font.lineHeight, textNeed());
        this.imageWidth = layout.imageW;
        this.imageHeight = layout.imageH;
        super.init();
        syncProposedFromMenu();
        AtlasLayout.Rect box = layout.compass;
        int s = AtlasLayout.COMPASS_BTN;
        addRenderableWidget(AtlasSpriteButton.compass(
                leftPos + box.x() + s, topPos + box.y(), "n",
                Component.translatable("gui.everechoes.atlas.pan.north"),
                button -> pan(0, -PAN_SHIFT_Z)
        ));
        addRenderableWidget(AtlasSpriteButton.compass(
                leftPos + box.x(), topPos + box.y() + s, "w",
                Component.translatable("gui.everechoes.atlas.pan.west"),
                button -> pan(-PAN_SHIFT_X, 0)
        ));
        addRenderableWidget(AtlasSpriteButton.icon(
                leftPos + box.x() + s, topPos + box.y() + s, AtlasSpriteButton.tex("compass_center"),
                Component.translatable("gui.everechoes.atlas.pan.home"),
                button -> home()
        ));
        addRenderableWidget(AtlasSpriteButton.compass(
                leftPos + box.x() + s * 2, topPos + box.y() + s, "e",
                Component.translatable("gui.everechoes.atlas.pan.east"),
                button -> pan(PAN_SHIFT_X, 0)
        ));
        addRenderableWidget(AtlasSpriteButton.compass(
                leftPos + box.x() + s, topPos + box.y() + s * 2, "s",
                Component.translatable("gui.everechoes.atlas.pan.south"),
                button -> pan(0, PAN_SHIFT_Z)
        ));
        applyButton = AtlasSpriteButton.nine(
                leftPos + layout.apply.x(), topPos + layout.apply.y(),
                layout.apply.w(), layout.apply.h(),
                Component.translatable("gui.everechoes.atlas.submit"),
                "btn_primary",
                button -> submit()
        );
        revertButton = AtlasSpriteButton.nine(
                leftPos + layout.undo.x(), topPos + layout.undo.y(),
                layout.undo.w(), layout.undo.h(),
                Component.translatable("gui.everechoes.atlas.revert"),
                "btn_secondary",
                button -> revert()
        );
        addRenderableWidget(applyButton);
        addRenderableWidget(revertButton);
        addRenderableWidget(AtlasSpriteButton.nine(
                leftPos + layout.close.x(), topPos + layout.close.y(),
                layout.close.w(), layout.close.h(),
                Component.translatable("gui.everechoes.atlas.close"),
                "btn_ghost",
                button -> onClose()
        ));
        refreshActionState();
    }

    @Override
    protected void containerTick() {
        if (menu.coverageEpoch() != appliedEpoch) {
            syncProposedFromMenu();
        }
        refreshActionState();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        headerHover = headerHoverAt(mouseX - leftPos, mouseY - topPos);
        super.render(graphics, mouseX, mouseY, partialTick);
        Component hover = headerHover != null ? headerHover : hoverText(mouseX, mouseY);
        if (hover != null) {
            List<FormattedCharSequence> lines = font.split(hover, Math.max(80, layout.sidebar.w() - 4));
            int tooltipX = mouseX < leftPos + layout.grid.right() ? leftPos + layout.sidebar.x() : mouseX;
            graphics.renderTooltip(font, lines, tooltipX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && toggleCell(mouseX, mouseY)) {
            refreshActionState();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos;
        int y0 = topPos;
        AtlasNine.nine(graphics, LEATHER, x0, y0, imageWidth, imageHeight, 256, 8);
        AtlasNine.nine(
                graphics,
                PAPER,
                x0 + AtlasLayout.LEATHER,
                y0 + AtlasLayout.LEATHER,
                imageWidth - AtlasLayout.LEATHER * 2,
                imageHeight - AtlasLayout.LEATHER * 2,
                256,
                4
        );
        blitBrassCorners(graphics);
        AtlasNine.blit(graphics, ENVELOPE, leftPos + layout.icon.x(), topPos + layout.icon.y(), 18, 18, 18);
        AtlasNine.blit(graphics, POSTMARK, leftPos + layout.stamp.x(), topPos + layout.stamp.y(), 40, 16, 160, 64);
        AtlasNine.blit(graphics, QUILL, leftPos + layout.quill.x(), topPos + layout.quill.y(), 16, 16, 16);
        hairline(graphics, layout.map);
        hairline(graphics, layout.sidebar);
        hairline(graphics, inset(layout.sidebar, 3));
        hairline(graphics, layout.status);
        vline(graphics, layout.col0, layout.header.y() + 3, layout.header.h() - 6);
        vline(graphics, layout.col1, layout.header.y() + 3, layout.header.h() - 6);
        headerRule(graphics, layout.district.y() - 1);
        ruleWithDiamonds(graphics, layout.header.x() + 4, layout.header.bottom() - 1, layout.header.w() - 8);
        ruleWithDiamonds(graphics, layout.sidebar.x() + 7, layout.legend.y() - 3, layout.sidebar.w() - 14);
        ruleWithDiamonds(graphics, layout.sidebar.x() + 7, layout.status.y() - 4, layout.sidebar.w() - 14);
        renderGrid(graphics, mouseX, mouseY);
        renderLegend(graphics);
        renderAllText(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    private void renderAllText(GuiGraphics graphics) {
        PostalAtlasSnapshot snapshot = menu.snapshot();
        int ox = leftPos;
        int oy = topPos;
        AtlasText.draw(graphics, font, Component.translatable("gui.everechoes.atlas.title").withStyle(ChatFormatting.BOLD), layout.title, ox, oy, AtlasText.Align.CENTER, INK_LABEL);
        AtlasText.draw(graphics, font, dimensionName(snapshot.dimension()), layout.dimension, ox, oy, AtlasText.Align.CENTER, INK);
        AtlasText.draw(graphics, font, Component.translatable("gui.everechoes.atlas.rev", snapshot.revision()), layout.revision, ox, oy, AtlasText.Align.CENTER, INK);
        AtlasText.draw(graphics, font, districtLabel(snapshot), layout.district, ox, oy, AtlasText.Align.CENTER, INK_LABEL, true);
        AtlasText.draw(graphics, font, chunkLabel(), layout.chunk, ox, oy, AtlasText.Align.CENTER, INK);
        AtlasText.draw(graphics, font, Component.translatable("gui.everechoes.atlas.count", proposed.size(), snapshot.maxChunks()), layout.count, ox, oy, AtlasText.Align.CENTER, INK);
        AtlasText.draw(graphics, font, statusLine(), layout.status, ox, oy, AtlasText.Align.CENTER, statusColor());
        AtlasText.draw(graphics, font, footerSummary(), layout.summary, ox, oy, AtlasText.Align.LEFT, INK);
    }

    private void fillLocal(GuiGraphics graphics, AtlasLayout.Rect rect, int color) {
        graphics.fill(leftPos + rect.x(), topPos + rect.y(), leftPos + rect.right(), topPos + rect.bottom(), color);
    }

    private void blitBrassCorners(GuiGraphics graphics) {
        int gold = 0xFFC4A35A;
        int hi = 0xFFF8DD72;
        cornerL(graphics, leftPos + 2, topPos + 2, 1, 1, gold, hi);
        cornerL(graphics, leftPos + imageWidth - 3, topPos + 2, -1, 1, gold, hi);
        cornerL(graphics, leftPos + 2, topPos + imageHeight - 3, 1, -1, gold, hi);
        cornerL(graphics, leftPos + imageWidth - 3, topPos + imageHeight - 3, -1, -1, gold, hi);
    }

    private void cornerL(GuiGraphics graphics, int x, int y, int dx, int dy, int gold, int hi) {
        bar(graphics, x, y, x + dx * 6, y + dy, gold);
        bar(graphics, x, y, x + dx, y + dy * 6, gold);
        bar(graphics, x, y, x + dx, y + dy, hi);
    }

    private void bar(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, color);
    }

    private void vline(GuiGraphics graphics, int x, int y, int h) {
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 1, topPos + y + h, PAPER_LINE);
    }

    private void ruleWithDiamonds(GuiGraphics graphics, int x, int y, int w) {
        int x0 = leftPos + x + 4;
        int y0 = topPos + y;
        graphics.fill(x0, y0, x0 + w - 8, y0 + 1, PAPER_LINE);
        diamond(graphics, leftPos + x + 2, y0);
        diamond(graphics, leftPos + x + w - 3, y0);
    }

    private void headerRule(GuiGraphics graphics, int y) {
        int x0 = leftPos + layout.header.x() + 3;
        int x1 = leftPos + layout.header.right() - 3;
        int sy = topPos + y;
        graphics.fill(x0, sy, x1, sy + 1, PAPER_LINE);
        diamond(graphics, leftPos + layout.header.x() + 2, sy);
        diamond(graphics, leftPos + layout.col0, sy);
        diamond(graphics, leftPos + layout.col1, sy);
        diamond(graphics, leftPos + layout.header.right() - 3, sy);
    }

    private static AtlasLayout.Rect inset(AtlasLayout.Rect rect, int amount) {
        return new AtlasLayout.Rect(rect.x() + amount, rect.y() + amount,
                Math.max(1, rect.w() - amount * 2), Math.max(1, rect.h() - amount * 2));
    }

    private void diamond(GuiGraphics graphics, int cx, int cy) {
        graphics.fill(cx, cy - 1, cx + 1, cy + 2, PAPER_LINE);
        graphics.fill(cx - 1, cy, cx + 2, cy + 1, PAPER_LINE);
    }

    private void hairline(GuiGraphics graphics, AtlasLayout.Rect rect) {
        int x = leftPos + rect.x();
        int y = topPos + rect.y();
        int x2 = leftPos + rect.right();
        int y2 = topPos + rect.bottom();
        graphics.fill(x, y, x2, y + 1, PAPER_LINE);
        graphics.fill(x, y2 - 1, x2, y2, PAPER_LINE);
        graphics.fill(x, y, x + 1, y2, PAPER_LINE);
        graphics.fill(x2 - 1, y, x2, y2, PAPER_LINE);
    }

    private void renderGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        PostalAtlasSnapshot snapshot = menu.snapshot();
        int width = Math.min(snapshot.width(), PostalAtlasLimits.MAX_WINDOW_SIDE);
        int height = Math.min(snapshot.height(), PostalAtlasLimits.MAX_WINDOW_SIDE);
        int gridX = leftPos + layout.grid.x();
        int gridY = topPos + layout.grid.y();
        int cell = layout.cell;
        long here = herePacked();
        Set<Long> saved = snapshot.savedCoveragePacked();
        long hoverPacked = cellAt(mouseX, mouseY);
        for (int cz = 0; cz < height; cz++) {
            for (int cx = 0; cx < width; cx++) {
                long packed = ChunkPos.asLong(snapshot.originX() + cx, snapshot.originZ() + cz);
                int x = gridX + cx * cell;
                int y = gridY + cz * cell;
                graphics.fill(x, y, x + cell, y + cell, cellColor(saved, packed));
            }
        }
        for (int i = 0; i <= width; i++) {
            int x = gridX + i * cell;
            int color = i % 5 == 0 ? INDEX : GRID;
            graphics.fill(x, gridY, x + 1, gridY + height * cell + 1, color);
        }
        for (int i = 0; i <= height; i++) {
            int y = gridY + i * cell;
            int color = i % 5 == 0 ? INDEX : GRID;
            graphics.fill(gridX, y, gridX + width * cell + 1, y + 1, color);
        }
        for (int cz = 0; cz < height; cz++) {
            for (int cx = 0; cx < width; cx++) {
                long packed = ChunkPos.asLong(snapshot.originX() + cx, snapshot.originZ() + cz);
                int x = gridX + cx * cell;
                int y = gridY + cz * cell;
                boolean inProposed = proposed.contains(packed);
                boolean inSaved = saved.contains(packed);
                if (inProposed && !inSaved) {
                    AtlasNine.blit(graphics, CELL_ADD, x, y, cell, cell, 32);
                } else if (!inProposed && inSaved) {
                    AtlasNine.blit(graphics, CELL_REMOVE, x, y, cell, cell, 32);
                }
                if (snapshot.foreignPacked().contains(packed)) {
                    AtlasNine.blit(graphics, CELL_FOREIGN, x, y, cell, cell, 32);
                }
                if (packed == hoverPacked) {
                    graphics.fill(x, y, x + cell, y + 1, HOVER);
                    graphics.fill(x, y + cell - 1, x + cell, y + cell, HOVER);
                    graphics.fill(x, y, x + 1, y + cell, HOVER);
                    graphics.fill(x + cell - 1, y, x + cell, y + cell, HOVER);
                }
                if (snapshot.hubPacked().contains(packed)) {
                    AtlasNine.blit(graphics, ICON_HUB, x, y, cell, cell, 32);
                } else if (snapshot.collectionPacked().contains(packed)) {
                    AtlasNine.blit(graphics, ICON_COLLECTION, x, y, cell, cell, 32);
                }
                if (packed == here) {
                    AtlasNine.blit(graphics, CELL_HERE, x, y, cell, cell, 32);
                }
            }
        }
    }

    private int cellColor(Set<Long> saved, long packed) {
        boolean inProposed = proposed.contains(packed);
        boolean inSaved = saved.contains(packed);
        if (inProposed && !inSaved) {
            return ADD;
        }
        if (!inProposed && inSaved) {
            return REMOVE;
        }
        if (inProposed || inSaved) {
            return SAVED;
        }
        return EMPTY;
    }

    private void renderLegend(GuiGraphics graphics) {
        int rowH = layout.lineH + (layout.compact ? 1 : AtlasLayout.LEGEND_ROW_GAP);
        int x = leftPos + layout.legend.x();
        int y = topPos + layout.legend.y();
        String[] keys = {
                "gui.everechoes.atlas.legend.coverage",
                "gui.everechoes.atlas.legend.add",
                "gui.everechoes.atlas.legend.remove",
                "gui.everechoes.atlas.legend.foreign",
                "gui.everechoes.atlas.legend.hub",
                "gui.everechoes.atlas.legend.collection",
                "gui.everechoes.atlas.legend.here"
        };
        int[] fills = {SAVED, ADD, REMOVE, SAVED, SAVED, SAVED, SAVED};
        ResourceLocation[] overlays = {null, CELL_ADD, CELL_REMOVE, CELL_FOREIGN, ICON_HUB, ICON_COLLECTION, CELL_HERE};
        int colW = layout.legendTwoCol ? layout.legend.w() / 2 : layout.legend.w();
        for (int i = 0; i < keys.length; i++) {
            int col = layout.legendTwoCol ? (i < 4 ? 0 : 1) : 0;
            int row = layout.legendTwoCol ? (i < 4 ? i : i - 4) : i;
            legendRow(graphics, x + col * colW, y + row * rowH, fills[i], overlays[i], keys[i]);
        }
    }

    private void legendRow(GuiGraphics graphics, int x, int y, int fill, @Nullable ResourceLocation overlay, String key) {
        int rowH = layout.lineH + (layout.compact ? 1 : AtlasLayout.LEGEND_ROW_GAP);
        int iconSize = 12;
        int iconY = y + (rowH - iconSize) / 2;
        int textY = y + (rowH - font.lineHeight) / 2;
        graphics.fill(x, iconY, x + iconSize, iconY + iconSize, fill);
        if (overlay != null) {
            AtlasNine.blit(graphics, overlay, x, iconY, iconSize, iconSize, 32);
        }
        graphics.drawString(font, Component.translatable(key), x + 16, textY, INK, false);
    }

    private Component districtLabel(PostalAtlasSnapshot snapshot) {
        if (!snapshot.domainCode().isEmpty() && !snapshot.districtCode().isEmpty()) {
            String code = net.bluafolkloro.overdeterminism.everechoes.postal.PostalCodes.formatOutward(
                    snapshot.domainCode(), snapshot.districtCode());
            return Component.translatable("gui.everechoes.atlas.district", code);
        }
        if (!snapshot.districtCode().isEmpty()) {
            return Component.translatable("gui.everechoes.atlas.district", snapshot.districtCode());
        }
        return Component.translatable("gui.everechoes.atlas.untitled_district");
    }

    private AtlasLayout.TextNeed textNeed() {
        return new AtlasLayout.TextNeed(
                font.width(Component.translatable("gui.everechoes.atlas.chunk", "+99", "+99")),
                font.width(Component.translatable("gui.everechoes.atlas.count", 4096, 4096)),
                statusTextWidth(),
                font.width(Component.translatable("gui.everechoes.atlas.footer_delta", 0, 0)),
                font.width(Component.translatable("gui.everechoes.atlas.submit")) + 20,
                font.width(Component.translatable("gui.everechoes.atlas.revert")) + 20,
                font.width(Component.translatable("gui.everechoes.atlas.close")) + 16,
                12 + 6 + font.width(Component.translatable("gui.everechoes.atlas.legend.here")),
                font.width(Component.translatable("gui.everechoes.atlas.title")),
                font.width(Component.translatable("gui.everechoes.atlas.rev", 99)),
                font.width(Component.translatable("gui.everechoes.dimension.overworld"))
        );
    }

    private int statusTextWidth() {
        Component[] values = {
                Component.translatable("gui.everechoes.atlas.unchanged"),
                Component.translatable("gui.everechoes.atlas.pending", 4096, 4096),
                Component.translatable("message.everechoes.coverage.empty"),
                Component.translatable("message.everechoes.coverage.too_large"),
                Component.translatable("message.everechoes.coverage.excludes_node"),
                Component.translatable("message.everechoes.coverage.out_of_bounds"),
                Component.translatable("message.everechoes.coverage.disconnected")
        };
        int widest = 0;
        for (Component value : values) {
            widest = Math.max(widest, font.width(value));
        }
        return widest;
    }

    private Component chunkLabel() {
        return Component.translatable(
                "gui.everechoes.atlas.chunk",
                signed(menu.pos().getX() >> 4),
                signed(menu.pos().getZ() >> 4)
        );
    }

    private static String signed(int value) {
        return (value >= 0 ? "+" : "") + value;
    }

    private Component dimensionName(ResourceLocation dimension) {
        String key = "gui.everechoes.dimension." + dimension.getPath();
        return Component.translatableWithFallback(key, dimension.getPath());
    }

    private Component statusLine() {
        if (menu.lastReasonKey() != null) {
            return Component.translatable(menu.lastReasonKey());
        }
        String preview = previewReasonKey();
        if (preview != null && dirty()) {
            return Component.translatable(preview);
        }
        if (!dirty()) {
            return Component.translatable("gui.everechoes.atlas.unchanged");
        }
        return Component.translatable("gui.everechoes.atlas.pending", addedCount(), removedCount());
    }

    private Component footerSummary() {
        String key = layout.compact ? "gui.everechoes.atlas.footer_delta_compact" : "gui.everechoes.atlas.footer_delta";
        return Component.translatable(key, addedCount(), removedCount());
    }

    private int addedCount() {
        int added = 0;
        Set<Long> saved = menu.snapshot().savedCoveragePacked();
        for (long packed : proposed) {
            if (!saved.contains(packed)) {
                added++;
            }
        }
        return added;
    }

    private int removedCount() {
        int removed = 0;
        Set<Long> saved = menu.snapshot().savedCoveragePacked();
        for (long packed : saved) {
            if (!proposed.contains(packed)) {
                removed++;
            }
        }
        return removed;
    }

    private int statusColor() {
        return menu.lastReasonKey() != null ? STAMP : INK;
    }

    @Nullable
    private String previewReasonKey() {
        if (proposed.isEmpty()) {
            return "message.everechoes.coverage.empty";
        }
        if (proposed.size() > menu.snapshot().maxChunks()) {
            return "message.everechoes.coverage.too_large";
        }
        for (long node : menu.snapshot().nodePacked()) {
            if (!proposed.contains(node)) {
                return "message.everechoes.coverage.excludes_node";
            }
        }
        Set<PostalChunk> chunks = new LinkedHashSet<>();
        for (long packed : proposed) {
            PostalChunk chunk = PostalChunk.unpack(menu.snapshot().dimension(), packed);
            if (!PostalAtlasLimits.isLegalChunk(chunk.x(), chunk.z())) {
                return "message.everechoes.coverage.out_of_bounds";
            }
            chunks.add(chunk);
        }
        if (!DistrictCoverage.isValidShape(chunks)) {
            return "message.everechoes.coverage.disconnected";
        }
        return null;
    }

    @Nullable
    private Component headerHoverAt(int lx, int ly) {
        PostalAtlasSnapshot snapshot = menu.snapshot();
        Component[] texts = {
                Component.translatable("gui.everechoes.atlas.title"),
                dimensionName(snapshot.dimension()),
                Component.translatable("gui.everechoes.atlas.rev", snapshot.revision()),
                districtLabel(snapshot),
                chunkLabel(),
                Component.translatable("gui.everechoes.atlas.count", proposed.size(), snapshot.maxChunks())
        };
        AtlasLayout.Rect[] areas = {layout.title, layout.dimension, layout.revision, layout.district, layout.chunk, layout.count};
        for (int i = 0; i < areas.length; i++) {
            if (areas[i].contains(lx, ly) && AtlasText.truncated(font, texts[i], areas[i])) {
                return texts[i];
            }
        }
        return null;
    }

    @Nullable
    private Component hoverText(int mouseX, int mouseY) {
        long packed = cellAt(mouseX, mouseY);
        if (packed == Long.MIN_VALUE) {
            return null;
        }
        PostalAtlasSnapshot snapshot = menu.snapshot();
        int chunkX = ChunkPos.getX(packed);
        int chunkZ = ChunkPos.getZ(packed);
        int relX = chunkX - (menu.pos().getX() >> 4);
        int relZ = chunkZ - (menu.pos().getZ() >> 4);
        return Component.translatable(
                "gui.everechoes.atlas.hover.cell",
                dimensionName(snapshot.dimension()),
                String.valueOf(chunkX),
                String.valueOf(chunkZ),
                signed(relX),
                signed(relZ)
        ).append("\n").append(cellStateLabel(snapshot, packed));
    }

    private Component cellStateLabel(PostalAtlasSnapshot snapshot, long packed) {
        long here = herePacked();
        boolean hub = snapshot.hubPacked().contains(packed);
        boolean collection = snapshot.collectionPacked().contains(packed);
        if (packed == here && hub) {
            return Component.translatable("gui.everechoes.atlas.hover.here_hub");
        }
        if (packed == here && collection) {
            return Component.translatable("gui.everechoes.atlas.hover.here_collection");
        }
        if (packed == here) {
            return Component.translatable("gui.everechoes.atlas.hover.here");
        }
        if (hub) {
            return Component.translatable("gui.everechoes.atlas.hover.hub");
        }
        if (collection) {
            return Component.translatable("gui.everechoes.atlas.hover.collection");
        }
        boolean inProposed = proposed.contains(packed);
        boolean inSaved = snapshot.savedCoveragePacked().contains(packed);
        boolean foreign = snapshot.foreignPacked().contains(packed);
        if (inProposed && !inSaved) {
            return Component.translatable("gui.everechoes.atlas.hover.add");
        }
        if (!inProposed && inSaved) {
            return Component.translatable("gui.everechoes.atlas.hover.remove");
        }
        if (foreign && inSaved) {
            return Component.translatable("gui.everechoes.atlas.hover.overlap");
        }
        if (inSaved) {
            return Component.translatable("gui.everechoes.atlas.legend.coverage");
        }
        return Component.translatable("gui.everechoes.atlas.legend.empty");
    }

    private long cellAt(double mouseX, double mouseY) {
        PostalAtlasSnapshot snapshot = menu.snapshot();
        int width = Math.min(snapshot.width(), PostalAtlasLimits.MAX_WINDOW_SIDE);
        int height = Math.min(snapshot.height(), PostalAtlasLimits.MAX_WINDOW_SIDE);
        int gx = (int) mouseX - (leftPos + layout.grid.x());
        int gz = (int) mouseY - (topPos + layout.grid.y());
        if (gx < 0 || gz < 0) {
            return Long.MIN_VALUE;
        }
        int cx = gx / layout.cell;
        int cz = gz / layout.cell;
        if (cx >= width || cz >= height) {
            return Long.MIN_VALUE;
        }
        return ChunkPos.asLong(snapshot.originX() + cx, snapshot.originZ() + cz);
    }

    private boolean toggleCell(double mouseX, double mouseY) {
        long packed = cellAt(mouseX, mouseY);
        if (packed == Long.MIN_VALUE) {
            return false;
        }
        PostalAtlasSnapshot snapshot = menu.snapshot();
        int chunkX = ChunkPos.getX(packed);
        int chunkZ = ChunkPos.getZ(packed);
        if (!PostalAtlasLimits.isLegalChunk(chunkX, chunkZ)) {
            return true;
        }
        if (proposed.contains(packed)) {
            if (snapshot.nodePacked().contains(packed)) {
                return true;
            }
            proposed.remove(packed);
            return true;
        }
        proposed.add(packed);
        return true;
    }

    private void pan(int dx, int dz) {
        PostalAtlasSnapshot snapshot = menu.snapshot();
        PacketDistributor.sendToServer(PostalAtlasRequestPayload.pan(
                snapshot.originX() + dx, snapshot.originZ() + dz, snapshot.width(), snapshot.height()));
    }

    private void home() {
        PostalAtlasSnapshot snapshot = menu.snapshot();
        int originX = (menu.pos().getX() >> 4) - snapshot.width() / 2;
        int originZ = (menu.pos().getZ() >> 4) - snapshot.height() / 2;
        PacketDistributor.sendToServer(PostalAtlasRequestPayload.pan(originX, originZ, snapshot.width(), snapshot.height()));
    }

    private void submit() {
        if (!dirty()) {
            return;
        }
        PostalAtlasSnapshot snapshot = menu.snapshot();
        PacketDistributor.sendToServer(PostalAtlasRequestPayload.submit(
                snapshot.revision(),
                snapshot.dimension(),
                proposed
        ));
    }

    private void revert() {
        proposed.clear();
        proposed.addAll(menu.snapshot().savedCoveragePacked());
        refreshActionState();
    }

    private void syncProposedFromMenu() {
        proposed.clear();
        proposed.addAll(menu.snapshot().savedCoveragePacked());
        appliedEpoch = menu.coverageEpoch();
        refreshActionState();
    }

    private void refreshActionState() {
        boolean dirty = dirty();
        if (applyButton != null) {
            applyButton.active = dirty;
        }
        if (revertButton != null) {
            revertButton.active = dirty;
        }
    }

    private boolean dirty() {
        return !proposed.equals(menu.snapshot().savedCoveragePacked());
    }

    private long herePacked() {
        return ChunkPos.asLong(menu.pos().getX() >> 4, menu.pos().getZ() >> 4);
    }
}
