package net.bluafolkloro.overdeterminism.everechoes.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasLayoutTest {
    @Test
    void defaultScalePrioritizesTheMapAndUsesTheAvailableHeight() {
        AtlasLayout.TextNeed need = AtlasLayout.TextNeed.estimate(9);
        for (int[] screen : new int[][]{{640, 360}, {960, 540}}) {
            AtlasLayout layout = AtlasLayout.compute(screen[0], screen[1], 9, need);
            int bodyWidth = layout.map.w() + AtlasLayout.SPINE + layout.sidebar.w();
            double mapShare = layout.map.w() / (double) bodyWidth;
            double heightShare = layout.imageH / (double) screen[1];
            assertTrue(mapShare >= 0.65, screen[0] + "x" + screen[1] + " map share " + mapShare);
            assertTrue(heightShare >= 0.84, screen[0] + "x" + screen[1] + " height share " + heightShare);
            assertTrue(layout.legend.w() / 2 >= need.legendItem(), screen[0] + "x" + screen[1] + " legend column width");
            int legendRowH = layout.lineH + AtlasLayout.LEGEND_ROW_GAP;
            assertTrue(layout.legend.y() + legendRowH * 4 <= layout.status.y(), screen[0] + "x" + screen[1] + " rendered legend height");
        }
    }

    @Test
    void widgetsStayInsideWindowOnTypicalScales() {
        AtlasLayout.TextNeed need = AtlasLayout.TextNeed.estimate(9);
        int[][] screens = {{960, 540}, {640, 360}, {480, 270}, {400, 240}, {1280, 720}};
        for (int[] screen : screens) {
            AtlasLayout layout = AtlasLayout.compute(screen[0], screen[1], 9, need);
            String tag = screen[0] + "x" + screen[1] + " " + layout.imageW + "x" + layout.imageH + " cell=" + layout.cell;
            assertTrue(layout.imageW <= screen[0] - 8, tag + " width " + layout.imageW + "/" + screen[0]);
            assertTrue(layout.imageH <= screen[1] - 4, tag + " height " + layout.imageH + "/" + screen[1]);
            assertTrue(layout.window.containsRect(layout.header), tag + " header");
            assertTrue(layout.window.containsRect(layout.map), tag + " map");
            assertTrue(layout.window.containsRect(layout.sidebar), tag + " sidebar");
            assertTrue(layout.window.containsRect(layout.footer), tag + " footer");
            assertTrue(layout.header.containsRect(layout.icon), tag + " icon");
            assertTrue(layout.header.containsRect(layout.stamp), tag + " stamp");
            assertTrue(layout.header.containsRect(layout.title), tag + " title");
            assertTrue(layout.header.containsRect(layout.dimension), tag + " dim");
            assertTrue(layout.header.containsRect(layout.revision), tag + " rev");
            assertTrue(layout.header.containsRect(layout.district), tag + " district");
            assertTrue(layout.header.containsRect(layout.chunk), tag + " chunk");
            assertTrue(layout.header.containsRect(layout.count), tag + " count");
            assertTrue(
                    layout.title.h() - layout.district.h() == AtlasLayout.HEADER_FIRST_ROW_EXTRA,
                    tag + " first header row breathing room"
            );
            assertTrue(layout.title.x() == layout.district.x(), tag + " title/district left axis");
            assertTrue(layout.title.w() == layout.district.w(), tag + " title/district center axis");
            assertTrue(layout.sidebar.containsRect(layout.compass), tag + " compass");
            assertTrue(layout.sidebar.containsRect(layout.legend), tag + " legend");
            assertTrue(layout.sidebar.containsRect(layout.status), tag + " status");
            assertTrue(layout.footer.containsRect(layout.apply), tag + " apply");
            assertTrue(layout.footer.containsRect(layout.undo), tag + " undo");
            assertTrue(layout.footer.containsRect(layout.close), tag + " close");
            assertFalse(layout.apply.overlaps(layout.legend), tag + " apply/legend");
            assertFalse(layout.apply.overlaps(layout.sidebar), tag + " apply/sidebar");
            assertFalse(layout.title.overlaps(layout.dimension), tag + " title/dim");
            assertFalse(layout.dimension.overlaps(layout.revision), tag + " dim/rev");
            assertTrue(layout.apply.x() >= AtlasLayout.LEATHER, tag + " apply left");
            assertTrue(layout.close.right() <= layout.imageW - AtlasLayout.LEATHER, tag + " close right");
            assertTrue(layout.legend.bottom() <= layout.status.y(), tag + " legend vs status");
            assertTrue(layout.grid.w() == layout.cell * AtlasLayout.GRID_COLS, tag + " grid width");
            assertTrue(layout.grid.h() == layout.cell * AtlasLayout.GRID_ROWS, tag + " grid height");
            assertTrue(layout.grid.w() > layout.grid.h(), tag + " landscape map");
            assertTrue(layout.col1 - layout.header.x() == layout.map.w(), tag + " header pair follows map");
            assertTrue(layout.status.w() >= need.status(), tag + " status fits one line");
            assertTrue(
                    layout.imageW == AtlasLayout.LEATHER * 2 + layout.map.w() + AtlasLayout.SPINE + layout.sidebar.w(),
                    tag + " window follows columns"
            );
            int inner = layout.map.w() + AtlasLayout.SPINE + layout.sidebar.w();
            double mapShare = layout.map.w() / (double) inner;
            double sideShare = layout.sidebar.w() / (double) inner;
            assertTrue(mapShare >= 0.50 && mapShare <= 0.81, tag + " map share " + mapShare);
            assertTrue(sideShare >= 0.19 && sideShare <= 0.50, tag + " side share " + sideShare);
            assertTrue(layout.legendTwoCol, tag + " two-col legend");
            assertFalse(layout.map.right() + AtlasLayout.SPINE < layout.sidebar.x() - 1, tag + " gap");
            assertTrue(layout.sidebar.right() == layout.imageW - AtlasLayout.LEATHER, tag + " sidebar flush");
        }
    }
}
