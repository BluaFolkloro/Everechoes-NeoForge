package net.bluafolkloro.overdeterminism.everechoes.screen;

import net.bluafolkloro.overdeterminism.everechoes.menu.PostalAtlasMenu;
import net.bluafolkloro.overdeterminism.everechoes.network.PostalAtlasRequestPayload;
import net.bluafolkloro.overdeterminism.everechoes.postal.DistrictCoverage;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalAtlasLimits;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalAtlasSnapshot;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalChunk;
import net.bluafolkloro.overdeterminism.everechoes.postal.PostalCodes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class PostalAtlasScreen extends AbstractContainerScreen<PostalAtlasMenu> {
    private static final int PANEL = 0xFF2B2118;
    private static final int PAPER = 0xFFF5E6C8;
    private static final int INK = 0x3F2A14;
    private static final int UNEXPLORED = 0xFF3A3024;
    private static final int EXPLORED = 0xFFE3D5A3;
    private static final int SAVED = 0xFFC4A35A;
    private static final int ADD = 0xFFE8D48A;
    private static final int REMOVE = 0xFF80052C;
    private static final int FOREIGN = 0xFF3A6E70;
    private static final int HERE = 0xFFF5E6C8;
    private static final int GRID_LEFT = 8;
    private static final int GRID_TOP = 28;
    private static final int CELL = 7;
    private static final int PAN_SHIFT = PostalAtlasLimits.WINDOW_SIZE / 2;

    private final Set<Long> proposed = new LinkedHashSet<>();
    private int appliedEpoch = -1;

    public PostalAtlasScreen(PostalAtlasMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 256;
        this.imageHeight = 220;
        this.titleLabelY = 10000;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        syncProposedFromMenu();
        int panX = leftPos + 172;
        int panY = topPos + 28;
        addRenderableWidget(Button.builder(Component.translatable("gui.everechoes.atlas.pan_north"), button -> pan(0, -PAN_SHIFT))
                .bounds(panX + 24, panY, 20, 16)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.everechoes.atlas.pan_west"), button -> pan(-PAN_SHIFT, 0))
                .bounds(panX, panY + 18, 20, 16)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.everechoes.atlas.pan_east"), button -> pan(PAN_SHIFT, 0))
                .bounds(panX + 48, panY + 18, 20, 16)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.everechoes.atlas.pan_south"), button -> pan(0, PAN_SHIFT))
                .bounds(panX + 24, panY + 36, 20, 16)
                .build());

        int buttonY = topPos + 194;
        addRenderableWidget(Button.builder(Component.translatable("gui.everechoes.atlas.submit"), button -> submit())
                .bounds(leftPos + 8, buttonY, 76, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.everechoes.atlas.revert"), button -> revert())
                .bounds(leftPos + 90, buttonY, 76, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.everechoes.atlas.close"), button -> onClose())
                .bounds(leftPos + 172, buttonY, 76, 20)
                .build());
    }

    @Override
    protected void containerTick() {
        if (menu.coverageEpoch() != appliedEpoch) {
            syncProposedFromMenu();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && toggleCell(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, PAPER);
        renderGrid(graphics);
        renderLegend(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        PostalAtlasSnapshot snapshot = menu.snapshot();
        graphics.drawString(font, headerLine(snapshot), 8, 6, INK, false);
        graphics.drawString(
                font,
                Component.translatable("gui.everechoes.atlas.count", proposed.size(), snapshot.maxChunks()),
                8,
                16,
                INK,
                false
        );
        graphics.drawString(
                font,
                Component.translatable("gui.everechoes.atlas.revision", snapshot.revision()),
                140,
                16,
                INK,
                false
        );
        Component status = statusLine();
        if (status != null) {
            graphics.drawString(font, status, 8, 180, INK, false);
        }
    }

    private void renderGrid(GuiGraphics graphics) {
        PostalAtlasSnapshot snapshot = menu.snapshot();
        int width = Math.min(snapshot.width(), PostalAtlasLimits.WINDOW_SIZE);
        int height = Math.min(snapshot.height(), PostalAtlasLimits.WINDOW_SIZE);
        int gridX = leftPos + GRID_LEFT;
        int gridY = topPos + GRID_TOP;
        long here = ChunkPos.asLong(menu.pos().getX() >> 4, menu.pos().getZ() >> 4);
        Set<Long> saved = snapshot.savedCoveragePacked();
        for (int cz = 0; cz < height; cz++) {
            for (int cx = 0; cx < width; cx++) {
                long packed = ChunkPos.asLong(snapshot.originX() + cx, snapshot.originZ() + cz);
                int x = gridX + cx * CELL;
                int y = gridY + cz * CELL;
                graphics.fill(x, y, x + CELL - 1, y + CELL - 1, cellColor(snapshot, saved, packed));
                boolean inProposed = proposed.contains(packed);
                boolean inSaved = saved.contains(packed);
                if (inProposed && !inSaved) {
                    graphics.fill(x + 1, y + 1, x + 2, y + 2, SAVED);
                    graphics.fill(x + CELL - 3, y + CELL - 3, x + CELL - 2, y + CELL - 2, SAVED);
                } else if (!inProposed && inSaved) {
                    graphics.fill(x + CELL - 3, y + 1, x + CELL - 2, y + 2, REMOVE);
                    graphics.fill(x + 1, y + CELL - 3, x + 2, y + CELL - 2, REMOVE);
                }
                if (snapshot.foreignPacked().contains(packed)) {
                    graphics.fill(x, y, x + CELL - 1, y + 1, FOREIGN);
                    graphics.fill(x, y + CELL - 2, x + CELL - 1, y + CELL - 1, FOREIGN);
                    graphics.fill(x, y, x + 1, y + CELL - 1, FOREIGN);
                    graphics.fill(x + CELL - 2, y, x + CELL - 1, y + CELL - 1, FOREIGN);
                }
                if (snapshot.hubPacked().contains(packed)) {
                    graphics.fill(x + 2, y + 3, x + CELL - 3, y + 4, INK | 0xFF000000);
                    graphics.fill(x + 3, y + 2, x + 4, y + CELL - 3, INK | 0xFF000000);
                } else if (snapshot.collectionPacked().contains(packed)) {
                    graphics.fill(x + 2, y + 2, x + CELL - 3, y + CELL - 3, INK | 0xFF000000);
                }
                if (packed == here) {
                    graphics.fill(x + 1, y + 1, x + CELL - 2, y + 2, HERE);
                    graphics.fill(x + 1, y + CELL - 3, x + CELL - 2, y + CELL - 2, HERE);
                }
            }
        }
    }

    private int cellColor(PostalAtlasSnapshot snapshot, Set<Long> saved, long packed) {
        boolean inProposed = proposed.contains(packed);
        boolean inSaved = saved.contains(packed);
        if (!snapshot.exploredPacked().contains(packed) && !inProposed && !inSaved) {
            return UNEXPLORED;
        }
        if (inProposed && !inSaved) {
            return ADD;
        }
        if (!inProposed && inSaved) {
            return REMOVE;
        }
        if (inProposed || inSaved) {
            return SAVED;
        }
        return EXPLORED;
    }

    private void renderLegend(GuiGraphics graphics) {
        int x = leftPos + 168;
        int y = topPos + 88;
        legendRow(graphics, x, y, UNEXPLORED, "gui.everechoes.atlas.legend.unexplored");
        legendRow(graphics, x, y + 10, EXPLORED, "gui.everechoes.atlas.legend.explored");
        legendRow(graphics, x, y + 20, SAVED, "gui.everechoes.atlas.legend.coverage");
        legendRow(graphics, x, y + 30, ADD, "gui.everechoes.atlas.legend.add");
        legendRow(graphics, x, y + 40, REMOVE, "gui.everechoes.atlas.legend.remove");
        legendRow(graphics, x, y + 50, FOREIGN, "gui.everechoes.atlas.legend.foreign");
        legendRow(graphics, x, y + 60, INK | 0xFF000000, "gui.everechoes.atlas.legend.hub");
        legendRow(graphics, x, y + 70, INK | 0xFF000000, "gui.everechoes.atlas.legend.collection");
    }

    private void legendRow(GuiGraphics graphics, int x, int y, int color, String key) {
        graphics.fill(x, y, x + 6, y + 6, color);
        graphics.drawString(font, Component.translatable(key), x + 9, y - 1, INK, false);
    }

    private Component headerLine(PostalAtlasSnapshot snapshot) {
        String district = !snapshot.domainCode().isEmpty() && !snapshot.districtCode().isEmpty()
                ? PostalCodes.formatOutward(snapshot.domainCode(), snapshot.districtCode())
                : snapshot.districtId().toString().substring(0, 8);
        return Component.literal(snapshot.dimension().getPath() + "  ·  " + district);
    }

    @Nullable
    private Component statusLine() {
        String preview = previewReasonKey();
        if (preview != null) {
            return Component.translatable(preview);
        }
        if (menu.lastReasonKey() != null) {
            return Component.translatable(menu.lastReasonKey());
        }
        return null;
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
            chunks.add(PostalChunk.unpack(menu.snapshot().dimension(), packed));
        }
        if (!DistrictCoverage.isValidShape(chunks)) {
            return "message.everechoes.coverage.disconnected";
        }
        return null;
    }

    private boolean toggleCell(double mouseX, double mouseY) {
        PostalAtlasSnapshot snapshot = menu.snapshot();
        int width = Math.min(snapshot.width(), PostalAtlasLimits.WINDOW_SIZE);
        int height = Math.min(snapshot.height(), PostalAtlasLimits.WINDOW_SIZE);
        int gx = (int) mouseX - (leftPos + GRID_LEFT);
        int gz = (int) mouseY - (topPos + GRID_TOP);
        if (gx < 0 || gz < 0) {
            return false;
        }
        int cx = gx / CELL;
        int cz = gz / CELL;
        if (cx >= width || cz >= height) {
            return false;
        }
        long packed = ChunkPos.asLong(snapshot.originX() + cx, snapshot.originZ() + cz);
        if (proposed.contains(packed)) {
            if (snapshot.nodePacked().contains(packed)) {
                return true;
            }
            proposed.remove(packed);
            return true;
        }
        if (!snapshot.exploredPacked().contains(packed)) {
            return true;
        }
        proposed.add(packed);
        return true;
    }

    private void pan(int dx, int dz) {
        PostalAtlasSnapshot snapshot = menu.snapshot();
        PacketDistributor.sendToServer(PostalAtlasRequestPayload.pan(snapshot.originX() + dx, snapshot.originZ() + dz));
    }

    private void submit() {
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
    }

    private void syncProposedFromMenu() {
        proposed.clear();
        proposed.addAll(menu.snapshot().savedCoveragePacked());
        appliedEpoch = menu.coverageEpoch();
    }
}
