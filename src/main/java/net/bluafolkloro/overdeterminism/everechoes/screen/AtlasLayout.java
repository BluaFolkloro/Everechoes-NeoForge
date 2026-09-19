package net.bluafolkloro.overdeterminism.everechoes.screen;

/**
 * Single source of atlas GUI geometry. Local to the leather origin.
 * Screen pixels = (leftPos, topPos) + local. Window width is always
 * leather + map + gap + sidebar — never inflated independently.
 */
final class AtlasLayout {
    static final int GRID_COLS = 25;
    static final int GRID_ROWS = 19;
    static final int LEATHER = 8;
    static final int SPINE = 4;
    static final int PAPER_IN = 5;
    static final int PAD_H = 6;
    static final int PAD_V = 4;
    static final int BUTTON_H = 16;
    static final int COMPASS_BTN = 14;
    static final int CELL_MIN = 6;
    static final int CELL_MAX = 24;
    static final int LEGEND_ROWS = 7;
    static final int LEGEND_ROW_GAP = 4;

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
    final Rect quill;
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
                    cjk * 16 + 12,
                    cjk * 4 + ascii * 10,
                    cjk * 4 + 22,
                    cjk * 4 + 22,
                    cjk * 2 + 18,
                    12 + 6 + cjk * 4,
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
            Rect footer, Rect quill, Rect summary, Rect apply, Rect undo, Rect close
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
        this.quill = quill;
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
        boolean compact = screenW < 480 || screenH < 270;
        int headerRow = lineH + PAD_V * 2;
        int headerH = headerRow * 2 + 1;
        int footerH = BUTTON_H + 8;
        int rowH = lineH + (compact ? 1 : LEGEND_ROW_GAP);
        int compassBox = COMPASS_BTN * 3 + 4;
        int statusH = lineH + PAD_V * 2;
        int legendTwoH = 4 * rowH;
        int sectionGap = compact ? 3 : 7;
        int sidebarMinH = 6 + compassBox + sectionGap + legendTwoH + sectionGap + statusH + 6;

        int maxW = Math.min(screenW - 8, Math.round(screenW * 0.92f));
        int maxH = Math.min(screenH - 8, Math.round(screenH * (compact ? 0.97f : 0.92f)));
        int targetW = Math.round(screenW * 0.80f);
        maxW = Math.max(260, maxW);
        maxH = Math.max(180, Math.min(maxH, screenH - 8));

        int applyW = Math.max(72, need.apply);
        int undoW = Math.max(72, need.undo);
        int closeW = Math.max(48, need.close);
        int sidebarW = Math.max(148, Math.max(need.status + 16, 16 + need.legendItem * 2));
        int cell = CELL_MIN;
        int mapPxW = cell * GRID_COLS;
        int mapPxH = cell * GRID_ROWS;
        int mapPaperW = mapPxW + PAPER_IN * 2;
        int mapPaperH = mapPxH + PAPER_IN * 2;
        int imageW = 0;
        int imageH = 0;
        for (int candidate = CELL_MIN; candidate <= CELL_MAX; candidate++) {
            int candidateMapW = candidate * GRID_COLS + PAPER_IN * 2;
            int candidateMapH = candidate * GRID_ROWS + PAPER_IN * 2;
            int candidateW = LEATHER * 2 + candidateMapW + SPINE + sidebarW;
            int candidateH = LEATHER * 2 + headerH + candidateMapH + footerH;
            if (candidateW > maxW || candidateH > maxH) {
                break;
            }
            cell = candidate;
            imageW = candidateW;
            imageH = candidateH;
            if (candidateW >= targetW) {
                break;
            }
        }
        mapPxW = cell * GRID_COLS;
        mapPxH = cell * GRID_ROWS;
        mapPaperW = mapPxW + PAPER_IN * 2;
        mapPaperH = Math.max(mapPxH + PAPER_IN * 2, sidebarMinH);
        imageW = LEATHER * 2 + mapPaperW + SPINE + sidebarW;
        imageH = LEATHER * 2 + headerH + mapPaperH + footerH;

        Rect window = new Rect(0, 0, imageW, imageH);
        Rect header = new Rect(LEATHER, LEATHER, imageW - LEATHER * 2, headerH);
        int leftPairW = mapPaperW;
        int c0 = leftPairW / 2;
        int c1 = leftPairW - c0;
        int r1 = header.y;
        int r2 = header.y + headerRow + 1;
        Rect icon = new Rect(header.x + PAD_H, r1 + (headerRow - 18) / 2, 18, 18);
        Rect title = new Rect(icon.right() + 4, r1, Math.max(8, c0 - (icon.right() + 4 - header.x) - PAD_H), headerRow);
        Rect dimension = new Rect(header.x + c0 + PAD_H, r1, c1 - PAD_H * 2, headerRow);
        Rect stamp = new Rect(header.right() - PAD_H - 40, r1 + (headerRow - 16) / 2, 40, 16);
        Rect revision = new Rect(header.x + leftPairW + PAD_H, r1, Math.max(8, stamp.x - 6 - (header.x + leftPairW + PAD_H)), headerRow);
        Rect district = new Rect(header.x + PAD_H, r2, c0 - PAD_H * 2, headerRow);
        Rect chunk = new Rect(header.x + c0 + PAD_H, r2, c1 - PAD_H * 2, headerRow);
        Rect count = new Rect(header.x + leftPairW + PAD_H, r2, header.w - leftPairW - PAD_H * 2, headerRow);

        int mapX = LEATHER;
        int mapY = LEATHER + headerH;
        Rect map = new Rect(mapX, mapY, mapPaperW, mapPaperH);
        Rect grid = new Rect(map.x + PAPER_IN, map.y + (mapPaperH - mapPxH) / 2, mapPxW, mapPxH);
        Rect sidebar = new Rect(map.right() + SPINE, map.y, sidebarW, mapPaperH);

        Rect navTitle = new Rect(sidebar.x, sidebar.y, 0, 0);
        Rect compass = new Rect(sidebar.x + (sidebar.w - compassBox) / 2, sidebar.y + 7, compassBox, compassBox);
        Rect status = new Rect(sidebar.x + 7, sidebar.bottom() - statusH - 7, sidebar.w - 14, statusH);
        Rect statusTitle = new Rect(sidebar.x, status.y, 0, 0);
        Rect legendTitle = new Rect(sidebar.x, compass.bottom() + sectionGap, 0, 0);
        Rect legend = new Rect(sidebar.x + 8, compass.bottom() + sectionGap, sidebar.w - 16,
                Math.max(rowH, status.y - sectionGap - (compass.bottom() + sectionGap)));

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
        Rect quill = new Rect(footer.x + PAD_H, footer.y + (footer.h - 16) / 2, 16, 16);
        Rect summary = new Rect(quill.right() + 4, footer.y, Math.max(8, apply.x - 8 - (quill.right() + 4)), footer.h);

        return new AtlasLayout(
                imageW, imageH, cell, lineH, header.x + c0, header.x + leftPairW,
                true, compact,
                window, header, icon, title, dimension, revision, stamp,
                district, chunk, count, map, grid, sidebar,
                navTitle, compass, legendTitle, legend, statusTitle, status,
                footer, quill, summary, apply, undo, close
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
