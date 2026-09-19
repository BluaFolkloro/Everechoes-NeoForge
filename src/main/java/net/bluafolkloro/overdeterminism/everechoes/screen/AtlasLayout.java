package net.bluafolkloro.overdeterminism.everechoes.screen;

/**
 * Single source of atlas GUI geometry. Local to the leather origin.
 * Screen pixels = (leftPos, topPos) + local. Window width is always
 * leather + map + gap + sidebar — never inflated independently.
 */
final class AtlasLayout {
    static final int GRID_N = 21;
    static final int LEATHER = 6;
    static final int SPINE = 4;
    static final int PAPER_IN = 4;
    static final int PAD_H = 6;
    static final int PAD_V = 3;
    static final int BUTTON_H = 16;
    static final int COMPASS_BTN = 12;
    static final int CELL_MIN = 6;
    static final int CELL_MAX = 16;
    static final int LEGEND_ROWS = 7;

    final int imageW;
    final int imageH;
    final int cell;
    final int lineH;
    final int col0;
    final int col1;
    final boolean legendTwoCol;
    final boolean compact;
    final Rect window;
    final Rect header;
    final Rect icon;
    final Rect title;
    final Rect dimension;
    final Rect revision;
    final Rect stamp;
    final Rect district;
    final Rect chunk;
    final Rect count;
    final Rect map;
    final Rect grid;
    final Rect sidebar;
    final Rect navTitle;
    final Rect compass;
    final Rect legendTitle;
    final Rect legend;
    final Rect statusTitle;
    final Rect status;
    final Rect footer;
    final Rect summary;
    final Rect apply;
    final Rect undo;
    final Rect close;

    record TextNeed(
            int chunk,
            int count,
            int status,
            int summary,
            int apply,
            int undo,
            int close,
            int legendItem,
            int title,
            int rev,
            int dim
    ) {
        static TextNeed estimate(int lineH) {
            int cjk = Math.max(7, lineH - 1);
            int ascii = 6;
            return new TextNeed(
                    cjk * 2 + ascii * 18,
                    cjk * 2 + ascii * 14,
                    cjk * 6 + 8,
                    cjk * 4 + ascii * 10,
                    cjk * 4 + 22,
                    cjk * 4 + 22,
                    cjk * 2 + 18,
                    8 + 6 + cjk * 4,
                    cjk * 5 + 8,
                    cjk * 2 + ascii * 4,
                    cjk * 3 + 8
            );
        }
    }

    private AtlasLayout(
            int imageW, int imageH, int cell, int lineH, int col0, int col1,
            boolean legendTwoCol, boolean compact,
            Rect window, Rect header, Rect icon, Rect title, Rect dimension, Rect revision, Rect stamp,
            Rect district, Rect chunk, Rect count, Rect map, Rect grid, Rect sidebar,
            Rect navTitle, Rect compass, Rect legendTitle, Rect legend, Rect statusTitle, Rect status,
            Rect footer, Rect summary, Rect apply, Rect undo, Rect close
    ) {
        this.imageW = imageW;
        this.imageH = imageH;
        this.cell = cell;
        this.lineH = lineH;
        this.col0 = col0;
        this.col1 = col1;
        this.legendTwoCol = legendTwoCol;
        this.compact = compact;
        this.window = window;
        this.header = header;
        this.icon = icon;
        this.title = title;
        this.dimension = dimension;
        this.revision = revision;
        this.stamp = stamp;
        this.district = district;
        this.chunk = chunk;
        this.count = count;
        this.map = map;
        this.grid = grid;
        this.sidebar = sidebar;
        this.navTitle = navTitle;
        this.compass = compass;
        this.legendTitle = legendTitle;
        this.legend = legend;
        this.statusTitle = statusTitle;
        this.status = status;
        this.footer = footer;
        this.summary = summary;
        this.apply = apply;
        this.undo = undo;
        this.close = close;
    }

    static AtlasLayout compute(int screenW, int screenH, int lineHeight) {
        return compute(screenW, screenH, lineHeight, TextNeed.estimate(lineHeight));
    }

    static AtlasLayout compute(int screenW, int screenH, int lineHeight, TextNeed need) {
        int lineH = Math.max(8, lineHeight);
        int headerRow = lineH + PAD_V * 2;
        int headerH = headerRow * 2 + 4;
        int footerH = BUTTON_H + 8;
        int titleH = lineH + 2;
        int rowH = lineH + 2;
        int compassBox = COMPASS_BTN * 3 + 4;
        int statusH = lineH + PAD_V * 2 + 4;
        int legendTwoH = 4 * rowH;
        int sidebarMinH = titleH + compassBox + 6 + titleH + legendTwoH + 6 + titleH + statusH;

        int maxW = Math.min(screenW - 16, Math.round(screenW * 0.74f));
        int maxH = Math.min(screenH - 16, Math.round(screenH * 0.82f));
        int targetW = Math.round(screenW * 0.70f);
        maxW = Math.max(260, maxW);
        maxH = Math.max(180, Math.min(maxH, screenH - 8));

        int applyW = Math.max(72, need.apply);
        int undoW = Math.max(72, need.undo);
        int closeW = Math.max(48, need.close);
        int sidebarW = Math.max(132, 16 + need.legendItem * 2);
        boolean compact = screenW < 380;

        int cell = 8;
        int mapPx = cell * GRID_N;
        int mapPaper = mapPx + PAPER_IN * 2;
        int imageW = 0;
        int imageH = 0;
        int headerNeed = 24 + need.title + 12 + Math.max(need.chunk, need.dim) + 12
                + Math.max(need.count, need.rev) + 48;
        int footerNeed = 12 + (compact ? 36 : need.summary) + 10 + applyW + undoW + closeW + 10;
        for (int step = 0; step < 16; step++) {
            mapPx = cell * GRID_N;
            mapPaper = Math.max(mapPx + PAPER_IN * 2, sidebarMinH);
            imageW = LEATHER + mapPaper + SPINE + sidebarW + LEATHER;
            imageH = LEATHER + headerH + mapPaper + footerH + LEATHER;
            int innerW = imageW - LEATHER * 2;
            boolean tooNarrow = innerW < headerNeed || innerW < footerNeed;
            boolean canGrow = cell < CELL_MAX && imageH + GRID_N <= maxH && imageW + GRID_N <= maxW;
            if (tooNarrow && canGrow) {
                cell++;
                continue;
            }
            if (!tooNarrow && imageW < targetW && canGrow) {
                cell++;
                continue;
            }
            if ((imageW > maxW || imageH > maxH) && cell > 8) {
                cell--;
                continue;
            }
            break;
        }
        mapPx = cell * GRID_N;
        mapPaper = Math.max(mapPx + PAPER_IN * 2, sidebarMinH);
        int maxMap = maxH - LEATHER * 2 - headerH - footerH;
        if (mapPaper > maxMap && maxMap >= 80) {
            cell = Math.max(CELL_MIN, (maxMap - PAPER_IN * 2) / GRID_N);
            mapPx = cell * GRID_N;
            mapPaper = Math.min(maxMap, mapPx + PAPER_IN * 2);
        }
        imageW = LEATHER + mapPaper + SPINE + sidebarW + LEATHER;
        imageH = LEATHER + headerH + mapPaper + footerH + LEATHER;
        int innerNeed = Math.max(headerNeed, footerNeed);
        int innerNow = imageW - LEATHER * 2;
        if (innerNow < innerNeed) {
            imageW += innerNeed - innerNow;
        }
        if (imageW > screenW - 8) {
            imageW = Math.max(240, screenW - 8);
        }
        if (imageH > screenH - 8) {
            imageH = Math.max(160, screenH - 8);
        }

        Rect window = new Rect(0, 0, imageW, imageH);
        Rect header = new Rect(LEATHER, LEATHER, imageW - LEATHER * 2, headerH);
        int inner = header.w;
        int rightW = Math.max(need.count, need.rev) + 52;
        int midW = Math.max(need.chunk, need.dim) + 16;
        int leftW = inner - rightW - midW;
        if (leftW < need.title + 28) {
            int extra = need.title + 28 - leftW;
            if (midW - extra > need.chunk + 8) {
                midW -= extra;
                leftW += extra;
            }
        }
        int c0 = leftW;
        int c1 = midW;
        int r1 = header.y;
        int r2 = header.y + headerRow + 2;
        Rect icon = new Rect(header.x + PAD_H, r1 + (headerRow - 16) / 2, 16, 16);
        Rect title = new Rect(icon.right() + 4, r1, Math.max(8, c0 - (icon.right() + 4 - header.x) - PAD_H), headerRow);
        Rect dimension = new Rect(header.x + c0 + PAD_H, r1, c1 - PAD_H * 2, headerRow);
        Rect stamp = new Rect(header.right() - PAD_H - 40, r1 + (headerRow - 16) / 2, 40, 16);
        Rect revision = new Rect(header.x + c0 + c1 + PAD_H, r1, Math.max(8, stamp.x - 6 - (header.x + c0 + c1 + PAD_H)), headerRow);
        Rect district = new Rect(header.x + PAD_H, r2, c0 - PAD_H * 2, headerRow);
        Rect chunk = new Rect(header.x + c0 + PAD_H, r2, c1 - PAD_H * 2, headerRow);
        Rect count = new Rect(header.x + c0 + c1 + PAD_H, r2, inner - c0 - c1 - PAD_H * 2, headerRow);

        int mapX = LEATHER;
        int mapY = LEATHER + headerH;
        int gridSize = cell * GRID_N;
        int mapW = imageW - LEATHER * 2 - SPINE - sidebarW;
        Rect map = new Rect(mapX, mapY, mapW, mapPaper);
        Rect grid = new Rect(map.x + Math.max(PAPER_IN, (mapW - gridSize) / 2), map.y + (mapPaper - gridSize) / 2, gridSize, gridSize);
        Rect sidebar = new Rect(map.right() + SPINE, map.y, sidebarW, mapPaper);

        Rect navTitle = new Rect(sidebar.x + 6, sidebar.y + 4, sidebar.w - 12, titleH);
        Rect compass = new Rect(sidebar.x + (sidebar.w - compassBox) / 2, navTitle.bottom() + 2, compassBox, compassBox);
        Rect status = new Rect(sidebar.x + 6, sidebar.bottom() - statusH - 4, sidebar.w - 12, statusH);
        Rect statusTitle = new Rect(sidebar.x + 6, status.y - titleH - 2, sidebar.w - 12, titleH);
        Rect legendTitle = new Rect(sidebar.x + 6, compass.bottom() + 4, sidebar.w - 12, titleH);
        Rect legend = new Rect(sidebar.x + 6, legendTitle.bottom() + 2, sidebar.w - 12, Math.max(rowH, statusTitle.y - 4 - (legendTitle.bottom() + 2)));

        Rect footer = new Rect(LEATHER, imageH - LEATHER - footerH, imageW - LEATHER * 2, footerH);
        while (applyW + undoW + closeW + 24 + (compact ? 28 : 64) > footer.w && applyW > 52) {
            applyW = Math.max(52, applyW - 2);
            undoW = Math.max(52, undoW - 2);
            closeW = Math.max(36, closeW - 2);
        }
        int by = footer.y + (footer.h - BUTTON_H) / 2;
        Rect close = new Rect(footer.right() - closeW - 4, by, closeW, BUTTON_H);
        Rect undo = new Rect(close.x - 4 - undoW, by, undoW, BUTTON_H);
        Rect apply = new Rect(undo.x - 4 - applyW, by, applyW, BUTTON_H);
        Rect summary = new Rect(footer.x + PAD_H + 12, footer.y, Math.max(8, apply.x - 8 - (footer.x + PAD_H + 12)), footer.h);

        return new AtlasLayout(
                imageW, imageH, cell, lineH, header.x + c0, header.x + c0 + c1,
                true, compact,
                window, header, icon, title, dimension, revision, stamp,
                district, chunk, count, map, grid, sidebar,
                navTitle, compass, legendTitle, legend, statusTitle, status,
                footer, summary, apply, undo, close
        );
    }

    record Rect(int x, int y, int w, int h) {
        int right() {
            return x + w;
        }

        int bottom() {
            return y + h;
        }

        int textY(int lineHeight) {
            return y + (h - lineHeight) / 2;
        }

        boolean contains(int px, int py) {
            return px >= x && py >= y && px < x + w && py < y + h;
        }

        boolean overlaps(Rect other) {
            return x < other.right() && other.x < right() && y < other.bottom() && other.y < bottom();
        }

        boolean containsRect(Rect other) {
            return other.x >= x && other.y >= y && other.right() <= right() && other.bottom() <= bottom();
        }
    }
}
