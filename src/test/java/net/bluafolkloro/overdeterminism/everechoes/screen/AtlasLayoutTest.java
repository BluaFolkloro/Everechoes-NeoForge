package net.bluafolkloro.overdeterminism.everechoes.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasLayoutTest {
    @Test
    void widgetsStayInsideWindowOnTypicalScales() {
        int[][] screens = {{960, 540}, {640, 360}, {480, 270}, {400, 240}, {1280, 720}, {320, 240}};
        for (int[] screen : screens) {
            AtlasLayout layout = AtlasLayout.compute(screen[0], screen[1], 9);
            String tag = screen[0] + "x" + screen[1];
            assertTrue(layout.imageW <= (int) (screen[0] * 0.82) + 1, tag + " width");
            assertTrue(layout.imageH <= (int) (screen[1] * 0.86) + 1, tag + " height");
            assertTrue(layout.window.containsRect(layout.header), tag + " header");
            assertTrue(layout.window.containsRect(layout.map), tag + " map");
            assertTrue(layout.window.containsRect(layout.sidebar), tag + " sidebar");
            assertTrue(layout.window.containsRect(layout.footer), tag + " footer");
            assertTrue(layout.header.containsRect(layout.title), tag + " title");
            assertTrue(layout.header.containsRect(layout.dimension), tag + " dim");
            assertTrue(layout.header.containsRect(layout.revision), tag + " rev");
            assertTrue(layout.header.containsRect(layout.district), tag + " district");
            assertTrue(layout.header.containsRect(layout.chunk), tag + " chunk");
            assertTrue(layout.header.containsRect(layout.count), tag + " count");
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
            assertTrue(layout.legend.bottom() <= layout.statusTitle.y(), tag + " legend vs status");
            assertTrue(layout.grid.w() == layout.grid.h(), tag + " square map");
        }
    }
}
