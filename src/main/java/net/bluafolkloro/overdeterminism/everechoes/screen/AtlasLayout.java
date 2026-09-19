package net.bluafolkloro.overdeterminism.everechoes.screen;

/**
 * Single source of atlas GUI geometry. All values are local to the window
 * origin (0,0 = top-left of the leather). Screen pixels = (leftPos, topPos) + local.
 */
final class AtlasLayout {
    static final int GRID_N = 21;
    static final int LEATHER = 5;
    static final int SPINE = 3;
    static final int PAPER_IN = 3;
    static final int PAD_H = 5;
    static final int PAD_V = 3;
    static final int BUTTON_H = 14;
    static final int APPLY_W = 80;
    static final int UNDO_W = 80;
    static final int CLOSE_W = 52;
    static final int COMPASS_BTN = 10;
    static final int ICON = 12;
    static final int CELL_MIN = 6;
    static final int CELL_MAX = 8;
    static final int SIDEBAR_MIN = 108;
    static final int LEGEND_ROWS = 7;

    final int imageW;
    final int imageH;
    final int cell;
    final int lineH;
    final int col0;
    final int col1;
    final boolean legendTwoCol;
    final Rect window;
    final Rect header;
    final Rect title;
    final Rect dimension;
    final Rect revision;
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

    private AtlasLayout(
            int imageW, int imageH, int cell, int lineH, int col0, int col1, boolean legendTwoCol,
            Rect window, Rect header, Rect title, Rect dimension, Rect revision,
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
        this.window = window;
        this.header = header;
        this.title = title;
        this.dimension = dimension;
        this.revision = revision;
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
        int lineH = Math.max(8, lineHeight);
        int headerRow = lineH + PAD_V * 2;
        int headerH = headerRow * 2 + 2;
        int footerH = BUTTON_H + 6;
        int titleH = lineH + 2;
        int rowH = lineH + 1;
        int compassBox = COMPASS_BTN * 3;
        int statusH = lineH + PAD_V * 2;
        int legendOneCol = LEGEND_ROWS * rowH;
        int sidebarNeededOne = titleH + compassBox + 3 + titleH + legendOneCol + 3 + titleH + statusH;
        int sidebarNeededTwo = titleH + compassBox + 3 + titleH + 4 * rowH + 3 + titleH + statusH;

        int maxW = Math.min(screenW - 16, (int) (screenW * 0.82));
        int maxH = Math.min(screenH - 16, (int) (screenH * 0.86));
        maxW = Math.max(220, maxW);
        maxH = Math.max(170, maxH);

        int applyW = APPLY_W;
        int undoW = UNDO_W;
        int closeW = CLOSE_W;
        int footerBtns = applyW + undoW + closeW + 8;
        int summaryMin = 84;

        int cell = CELL_MAX;
        boolean twoCol = false;
        int mapPx = 0;
        int mapPaper = 0;
        int sidebarW = SIDEBAR_MIN;
        int imageW = 0;
        int imageH = 0;
        while (true) {
            mapPx = cell * GRID_N;
            mapPaper = mapPx + PAPER_IN * 2;
            twoCol = mapPaper < sidebarNeededOne;
            sidebarW = Math.max(SIDEBAR_MIN, Math.min(124, Math.round(mapPaper * 0.25f / 0.72f)));
            imageW = LEATHER + mapPaper + SPINE + sidebarW + LEATHER;
            imageH = LEATHER + headerH + mapPaper + footerH + LEATHER;
            int footerNeed = LEATHER * 2 + summaryMin + 8 + footerBtns;
            if (imageW < footerNeed) {
                imageW = footerNeed;
            }
            if (imageW <= maxW && imageH <= maxH) {
                break;
            }
            if (imageW > maxW && (applyW > 60 || summaryMin > 64)) {
                if (applyW > 60) {
                    applyW = Math.max(60, applyW - 4);
                    undoW = Math.max(60, undoW - 4);
                    closeW = Math.max(40, closeW - 4);
                    footerBtns = applyW + undoW + closeW + 8;
                }
                if (summaryMin > 64) {
                    summaryMin = Math.max(64, summaryMin - 8);
                }
                continue;
            }
            if (cell > CELL_MIN) {
                cell--;
                applyW = APPLY_W;
                undoW = UNDO_W;
                closeW = CLOSE_W;
                footerBtns = applyW + undoW + closeW + 8;
                continue;
            }
            break;
        }

        Rect window = new Rect(0, 0, imageW, imageH);
        Rect header = new Rect(LEATHER, LEATHER, imageW - LEATHER * 2, headerH);
        int inner = header.w;
        int c0 = inner * 40 / 100;
        int c1 = inner * 32 / 100;
        int r1 = header.y;
        int r2 = header.y + headerRow + 2;
        Rect title = new Rect(header.x + PAD_H, r1, c0 - PAD_H * 2, headerRow);
        Rect dimension = new Rect(header.x + c0 + PAD_H, r1, c1 - PAD_H * 2, headerRow);
        Rect revision = new Rect(header.x + c0 + c1 + PAD_H, r1, inner - c0 - c1 - PAD_H * 2, headerRow);
        Rect district = new Rect(header.x + PAD_H, r2, c0 - PAD_H * 2, headerRow);
        Rect chunk = new Rect(header.x + c0 + PAD_H, r2, c1 - PAD_H * 2, headerRow);
        Rect count = new Rect(header.x + c0 + c1 + PAD_H, r2, inner - c0 - c1 - PAD_H * 2, headerRow);

        Rect map = new Rect(LEATHER, LEATHER + headerH, mapPaper, mapPaper);
        Rect grid = new Rect(map.x + PAPER_IN, map.y + PAPER_IN, mapPx, mapPx);
        Rect sidebar = new Rect(map.right() + SPINE, map.y, sidebarW, mapPaper);

        Rect navTitle = new Rect(sidebar.x + 4, sidebar.y + 2, sidebar.w - 8, titleH);
        Rect compass = new Rect(sidebar.x + (sidebar.w - compassBox) / 2, navTitle.bottom() + 1, compassBox, compassBox);
        Rect status = new Rect(sidebar.x + 4, sidebar.bottom() - statusH - 2, sidebar.w - 8, statusH);
        Rect statusTitle = new Rect(sidebar.x + 4, status.y - titleH, sidebar.w - 8, titleH);
        Rect legendTitle = new Rect(sidebar.x + 4, compass.bottom() + 2, sidebar.w - 8, titleH);
        int legendBottom = statusTitle.y - 2;
        Rect legend = new Rect(sidebar.x + 4, legendTitle.bottom() + 1, sidebar.w - 8, Math.max(rowH, legendBottom - (legendTitle.bottom() + 1)));

        Rect footer = new Rect(LEATHER, imageH - LEATHER - footerH, imageW - LEATHER * 2, footerH);
        int by = footer.y + (footer.h - BUTTON_H) / 2;
        Rect close = new Rect(footer.right() - closeW - 3, by, closeW, BUTTON_H);
        Rect undo = new Rect(close.x - 3 - undoW, by, undoW, BUTTON_H);
        Rect apply = new Rect(undo.x - 3 - applyW, by, applyW, BUTTON_H);
        Rect summary = new Rect(footer.x + PAD_H, footer.y, Math.max(0, apply.x - 6 - (footer.x + PAD_H)), footer.h);

        return new AtlasLayout(
                imageW, imageH, cell, lineH, header.x + c0, header.x + c0 + c1, twoCol,
                window, header, title, dimension, revision, district, chunk, count,
                map, grid, sidebar, navTitle, compass, legendTitle, legend, statusTitle, status,
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
