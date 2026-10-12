package net.bluafolkloro.overdeterminism.everechoes.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 封装相关行为的自动化回归测试。
 */
class MailBoxModelTest {
    /**
     * 定义 {@code EPSILON} 常量。
     */
    private static final double EPSILON = 0.05;
    /**
     * 定义 {@code TEXTURE} 常量。
     */
    private static final String TEXTURE = "everechoes:block/mail_box";
    /**
     * 定义 {@code BLOCK_MODELS} 常量。
     */
    private static final String[] BLOCK_MODELS = {
            "mail_box.json",
            "mail_box_desk.json",
            "mail_box_wall.json",
            "mail_box_hanging.json",
            "mail_box_hanging_raised.json"
    };

    // MailBoxBlock static VoxelShape init calls Block.box; do not class-load it in JUnit.

    /**
     * 执行 {@code blockModelsUseUnifiedMailBoxTexture} 对应的业务操作。
     */
    @Test
    void blockModelsUseUnifiedMailBoxTexture() {
        for (String fileName : BLOCK_MODELS) {
            Path path = assets("models/block/" + fileName);
            String raw = read(path);
            JsonObject model = json(raw);
            JsonObject textures = model.getAsJsonObject("textures");
            assertEquals(TEXTURE, textures.get("2").getAsString(), fileName + " textures.2");
            assertEquals(TEXTURE, textures.get("particle").getAsString(), fileName + " textures.particle");
            assertFalse(textures.has("texture"), fileName + " leftover textures.texture");
            assertFalse(raw.contains("block/texture"), fileName + " leftover block/texture");
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                assertEquals(TEXTURE, entry.getValue().getAsString(), fileName + " textures." + entry.getKey());
            }
        }
        Path png = assets("textures/block/mail_box.png");
        assertTrue(Files.isRegularFile(png), png.toString());
    }

    /**
     * 执行 {@code postModelUsesOriginalDoubleBlockHeight} 对应的业务操作。
     */
    @Test
    void postModelUsesOriginalDoubleBlockHeight() {
        JsonObject model = json(read(assets("models/block/mail_box.json")));
        JsonArray elements = model.getAsJsonArray("elements");
        double peak = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < elements.size(); index++) {
            double maxY = rotatedMaxY(elements.get(index).getAsJsonObject());
            assertTrue(
                    maxY <= 32.0 + EPSILON,
                    "mail_box.json element " + index + " rotated max Y " + maxY
            );
            peak = Math.max(peak, maxY);
        }
        assertTrue(peak > 24.0, "mail_box.json should extend into the upper block: " + peak);
        assertTrue(peak <= 32.0 + EPSILON, "mail_box.json rotated peak " + peak);
    }

    /**
     * 执行 {@code blockModelRotationsUseVanillaLegalAngles} 对应的业务操作。
     */
    @Test
    void blockModelRotationsUseVanillaLegalAngles() {
        Set<Double> allowed = Set.of(-45.0, -22.5, 0.0, 22.5, 45.0);
        for (String fileName : BLOCK_MODELS) {
            JsonArray elements = json(read(assets("models/block/" + fileName))).getAsJsonArray("elements");
            for (int index = 0; index < elements.size(); index++) {
                JsonObject element = elements.get(index).getAsJsonObject();
                if (!element.has("rotation")) {
                    continue;
                }
                double angle = element.getAsJsonObject("rotation").get("angle").getAsDouble();
                assertTrue(
                        allowed.contains(angle),
                        fileName + " element " + index + " rotation " + angle + " is not 0/±22.5/±45"
                );
            }
        }
    }

    /**
     * 执行 {@code deskAndWallModelsDoNotKeepThePostPillar} 对应的业务操作。
     */
    @Test
    void deskAndWallModelsDoNotKeepThePostPillar() {
        JsonObject post = json(read(assets("models/block/mail_box.json")));
        assertTrue(hasPillar(post), "post model missing original [6,0,6]–[10,16,10] pillar");
        assertTrue(hasOriginalPostBody(post), "post model missing original body at Y 16–24");

        JsonObject desk = json(read(assets("models/block/mail_box_desk.json")));
        assertFalse(hasPillar(desk), "desk model still has the post pillar");
        JsonObject wall = json(read(assets("models/block/mail_box_wall.json")));
        assertFalse(hasPillar(wall), "wall model still has the post pillar");
    }

    /**
     * 执行 {@code blockstateComposesSixStylesWithAnInternalHorizontalChain} 对应的业务操作。
     */
    @Test
    void blockstateComposesSixStylesWithAnInternalHorizontalChain() {
        String raw = read(assets("blockstates/mail_box.json"));
        var multipart = json(raw).getAsJsonArray("multipart");
        assertEquals(26, multipart.size(), "mail_box multipart entry count");

        String[] styles = {"post", "desk", "wall", "hanging", "hanging_raised", "hanging_chained"};
        String[] facings = {"north", "south", "west", "east"};
        Map<String, String> models = Map.of(
                "post", "everechoes:block/mail_box",
                "desk", "everechoes:block/mail_box_desk",
                "wall", "everechoes:block/mail_box_wall",
                "hanging", "everechoes:block/mail_box_hanging",
                "hanging_raised", "everechoes:block/mail_box_hanging_raised",
                "hanging_chained", "everechoes:block/mail_box_hanging_raised"
        );
        Map<String, Integer> yaw = new HashMap<>();
        yaw.put("north", 0);
        yaw.put("south", 180);
        yaw.put("west", 270);
        yaw.put("east", 90);

        Set<String> seen = new HashSet<>();
        Set<String> chainAxes = new HashSet<>();
        for (JsonElement partElement : multipart) {
            JsonObject part = partElement.getAsJsonObject();
            JsonObject when = part.getAsJsonObject("when");
            JsonObject spec = part.getAsJsonObject("apply");
            if (when.has("chain_axis")) {
                assertEquals("hanging_chained", when.get("style").getAsString());
                assertEquals("lower", when.get("half").getAsString());
                assertEquals("everechoes:block/mail_box_internal_chain", spec.get("model").getAsString());
                chainAxes.add(when.get("chain_axis").getAsString());
                continue;
            }

            String style = when.get("style").getAsString();
            String facing = when.get("facing").getAsString();
            assertEquals("lower", when.get("half").getAsString());
            assertEquals(models.get(style), spec.get("model").getAsString(), style + "/" + facing);
            int y = spec.has("y") ? spec.get("y").getAsInt() : 0;
            assertEquals(yaw.get(facing), y, style + "/" + facing + " y");
            seen.add(style + "/" + facing);
        }

        Set<String> expected = new HashSet<>();
        for (String style : styles) {
            for (String facing : facings) {
                expected.add(style + "/" + facing);
            }
        }
        assertEquals(expected, seen);
        assertEquals(Set.of("x", "z"), chainAxes);

        JsonObject chainModel = json(read(assets("models/block/mail_box_internal_chain.json")));
        assertEquals("minecraft:block/chain", chainModel.get("parent").getAsString());
        assertEquals("cutout", chainModel.get("render_type").getAsString());
    }

    /**
     * 执行 {@code lootTableDropsOnlyFromLowerHalf} 对应的业务操作。
     */
    @Test
    void lootTableDropsOnlyFromLowerHalf() {
        String raw = read(data("loot_table/blocks/mail_box.json"));
        JsonObject loot = json(raw);
        assertTrue(raw.contains("block_state_property"), "mail_box loot lacks lower-half condition");
        assertTrue(raw.contains("\"half\": \"lower\""), "mail_box loot does not select the lower half");
        boolean drops = false;
        for (JsonElement poolEl : loot.getAsJsonArray("pools")) {
            for (JsonElement entryEl : poolEl.getAsJsonObject().getAsJsonArray("entries")) {
                JsonObject entry = entryEl.getAsJsonObject();
                if ("everechoes:mail_box".equals(entry.get("name").getAsString())) {
                    drops = true;
                }
            }
        }
        assertTrue(drops, "mail_box loot does not drop everechoes:mail_box");
    }

    /**
     * 执行 {@code langNamesMailbox} 对应的业务操作。
     */
    @Test
    void langNamesMailbox() {
        JsonObject zh = json(read(assets("lang/zh_cn.json")));
        JsonObject en = json(read(assets("lang/en_us.json")));
        assertEquals("邮箱", zh.get("block.everechoes.mail_box").getAsString());
        assertEquals("Mailbox", en.get("block.everechoes.mail_box").getAsString());
    }

    /**
     * 执行 {@code itemModelParentsThePostBlockModel} 对应的业务操作。
     */
    @Test
    void itemModelParentsThePostBlockModel() {
        JsonObject item = json(read(assets("models/item/mail_box.json")));
        assertEquals("everechoes:block/mail_box", item.get("parent").getAsString());
    }

    /**
     * 执行 {@code javaSourcesUseDoublePostAndSpecialSupportsWithoutPostalNetwork} 对应的业务操作。
     */
    @Test
    void javaSourcesUseDoublePostAndSpecialSupportsWithoutPostalNetwork() {
        String block = read(javaSrc("block/MailBoxBlock.java"));
        assertTrue(block.contains("DoubleBlockHalf"), "MailBoxBlock lacks double-block support");
        assertTrue(block.contains("DOUBLE_BLOCK_HALF"), "MailBoxBlock lacks the half property");
        assertTrue(block.contains("VoxelShape getShape("), "MailBoxBlock does not override getShape");
        assertTrue(block.contains("VoxelShape getCollisionShape("), "MailBoxBlock does not override getCollisionShape");
        assertTrue(
                block.contains("Block.box(6, 0, 6, 10, 16, 10)"),
                "POST lower shape is missing"
        );
        assertTrue(block.contains("Shapes.empty()"), "occlusion/visual empty shapes missing");
        assertTrue(block.contains("MailBoxStyle.WALL"), "placement WALL branch missing");
        assertTrue(block.contains("isSecondaryUseActive()"), "placement DESK sneak check missing");
        assertTrue(block.contains("MailBoxStyle.DESK"), "placement DESK branch missing");
        assertTrue(block.contains("MailBoxStyle.POST"), "placement POST branch missing");
        assertTrue(block.contains("if (style == MailBoxStyle.POST) {\n            return true;"),
                "placed POST still depends on its lower support");
        assertTrue(block.contains("Direction.DOWN"), "placement DOWN reject missing");
        assertTrue(block.contains("MailBoxStyle.HANGING"), "placement HANGING branch missing");
        assertTrue(block.contains("MailBoxStyle.HANGING_RAISED"), "raised horizontal-chain branch missing");
        assertTrue(block.contains("MailBoxStyle.HANGING_CHAINED"), "internal horizontal-chain branch missing");
        assertTrue(block.contains("isHorizontalEndRod"), "horizontal end-rod hanging support missing");
        assertTrue(block.contains("Blocks.END_ROD"), "end-rod support missing");
        int hangingBranchStart = block.indexOf("if (clickedFace == Direction.DOWN)");
        int wallBranchStart = block.indexOf("} else if (clickedFace.getAxis().isHorizontal())", hangingBranchStart);
        String hangingPlacement = block.substring(hangingBranchStart, wallBranchStart);
        assertFalse(hangingPlacement.contains("isSecondaryUseActive"), "hanging placement still requires sneaking");

        int supportMethodStart = block.indexOf("private static boolean isRaisedHangingSupport");
        int endRodMethodStart = block.indexOf("private static boolean isHorizontalEndRod", supportMethodStart);
        String raisedSupportMethod = block.substring(supportMethodStart, endRodMethodStart);
        assertTrue(raisedSupportMethod.contains("isHorizontalChain(state)"), "vanilla horizontal chain support missing");
        assertTrue(raisedSupportMethod.contains("MailBoxStyle.HANGING_CHAINED"),
                "chain-integrated mailbox is not a direct hanging support");
        assertTrue(block.contains("ItemInteractionResult useItemOn"), "chain insertion interaction missing");
        assertTrue(block.contains("stack.is(Items.CHAIN)"), "chain item conversion missing");
        assertTrue(block.contains("drops.add(new ItemStack(Items.CHAIN))"), "internal chain refund missing");
        assertTrue(block.contains("style == MailBoxStyle.DESK"), "support-independent DESK branch missing");
        assertTrue(block.contains("Blocks.CHAIN"), "chain support missing");

        assertTrue(block.contains("onRemove"), "MailBoxBlock does not override onRemove");
        assertTrue(block.contains("markEndpointOffline"), "MailBoxBlock does not mark the endpoint offline");

        String entity = read(javaSrc("block/entity/MailBoxBlockEntity.java"));
        assertFalse(entity.contains("MenuProvider"), "MailBoxBlockEntity implements MenuProvider");
        assertTrue(entity.contains("registerEndpoint"), "MailBoxBlockEntity does not register the endpoint");
        assertFalse(entity.contains("bindEndpoint"), "MailBoxBlockEntity claims a postal code");
        assertTrue(entity.contains("\"mailboxId\""), "MailBoxBlockEntity does not persist mailboxId");
        assertTrue(
                entity.contains("putUUID(\"mailboxId\"") || entity.contains("putUUID(MAILBOX_ID_TAG"),
                "MailBoxBlockEntity save skips mailboxId"
        );

        String containers = read(javaSrc("block/ContainerBlocks.java"));
        assertTrue(containers.contains("\"mail_box\""), "ContainerBlocks does not register mail_box");

        String blockEntities = read(javaSrc("block/entity/ModBlockEntities.java"));
        assertTrue(blockEntities.contains("\"mail_box\""), "ModBlockEntities does not register mail_box");
    }

    /**
     * 执行 {@code mailBoxStyleSerializedNamesCoverSixInternalInstallations} 对应的业务操作。
     */
    @Test
    void mailBoxStyleSerializedNamesCoverSixInternalInstallations() {
        assertEquals("post", MailBoxStyle.POST.getSerializedName());
        assertEquals("desk", MailBoxStyle.DESK.getSerializedName());
        assertEquals("wall", MailBoxStyle.WALL.getSerializedName());
        assertEquals("hanging", MailBoxStyle.HANGING.getSerializedName());
        assertEquals("hanging_raised", MailBoxStyle.HANGING_RAISED.getSerializedName());
        assertEquals("hanging_chained", MailBoxStyle.HANGING_CHAINED.getSerializedName());
        assertEquals(6, MailBoxStyle.values().length);
    }

    /**
     * 判断 {@code hasPillar} 对应的业务操作。
     */
    private static boolean hasPillar(JsonObject model) {
        for (JsonElement elementEl : model.getAsJsonArray("elements")) {
            JsonObject element = elementEl.getAsJsonObject();
            if (approxVec(element.getAsJsonArray("from"), 6, 0, 6)
                    && approxVec(element.getAsJsonArray("to"), 10, 16, 10)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断 {@code hasOriginalPostBody} 对应的业务操作。
     */
    private static boolean hasOriginalPostBody(JsonObject model) {
        for (JsonElement elementEl : model.getAsJsonArray("elements")) {
            JsonObject element = elementEl.getAsJsonObject();
            if (approxVec(element.getAsJsonArray("from"), 2, 16, 2)
                    && approxVec(element.getAsJsonArray("to"), 14, 24, 14)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 执行 {@code approxVec} 对应的业务操作。
     */
    private static boolean approxVec(JsonArray vec, double x, double y, double z) {
        return Math.abs(vec.get(0).getAsDouble() - x) <= EPSILON
                && Math.abs(vec.get(1).getAsDouble() - y) <= EPSILON
                && Math.abs(vec.get(2).getAsDouble() - z) <= EPSILON;
    }

    // Vanilla FaceBakery: translate by -origin, JOML rotate around axis, translate back.
    /**
     * 执行 {@code rotatedMaxY} 对应的业务操作。
     */
    private static double rotatedMaxY(JsonObject element) {
        JsonArray from = element.getAsJsonArray("from");
        JsonArray to = element.getAsJsonArray("to");
        double minX = from.get(0).getAsDouble();
        double minY = from.get(1).getAsDouble();
        double minZ = from.get(2).getAsDouble();
        double maxX = to.get(0).getAsDouble();
        double maxY = to.get(1).getAsDouble();
        double maxZ = to.get(2).getAsDouble();

        double ox = 0;
        double oy = 0;
        double oz = 0;
        double angle = 0;
        String axis = "y";
        if (element.has("rotation")) {
            JsonObject rotation = element.getAsJsonObject("rotation");
            JsonArray origin = rotation.getAsJsonArray("origin");
            ox = origin.get(0).getAsDouble();
            oy = origin.get(1).getAsDouble();
            oz = origin.get(2).getAsDouble();
            angle = rotation.get("angle").getAsDouble();
            axis = rotation.get("axis").getAsString();
        }

        double peak = Double.NEGATIVE_INFINITY;
        for (double x : new double[] {minX, maxX}) {
            for (double y : new double[] {minY, maxY}) {
                for (double z : new double[] {minZ, maxZ}) {
                    peak = Math.max(peak, rotateCornerY(x, y, z, ox, oy, oz, axis, angle));
                }
            }
        }
        return peak;
    }

    /**
     * 执行 {@code rotateCornerY} 对应的业务操作。
     */
    private static double rotateCornerY(
            double x,
            double y,
            double z,
            double ox,
            double oy,
            double oz,
            String axis,
            double degrees
    ) {
        double dx = x - ox;
        double dy = y - oy;
        double dz = z - oz;
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return switch (axis) {
            case "x" -> oy + dy * cos - dz * sin;
            case "z" -> oy + dx * sin + dy * cos;
            default -> oy + dy;
        };
    }

    /**
     * 执行 {@code parseVariantKey} 对应的业务操作。
     */
    private static Map<String, String> parseVariantKey(String key) {
        Map<String, String> props = new HashMap<>();
        for (String part : key.split(",")) {
            String[] pair = part.trim().split("=", 2);
            assertEquals(2, pair.length, "variant key " + key);
            props.put(pair[0].trim(), pair[1].trim());
        }
        return props;
    }

    /**
     * 执行 {@code json} 对应的业务操作。
     */
    private static JsonObject json(String raw) {
        return JsonParser.parseString(raw).getAsJsonObject();
    }

    /**
     * 获取或解析 {@code read} 对应的业务操作。
     */
    private static String read(Path path) {
        try {
            assertTrue(Files.isRegularFile(path), "missing " + path);
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AssertionError("cannot read " + path, e);
        }
    }

    /**
     * 执行 {@code assets} 对应的业务操作。
     */
    private static Path assets(String relative) {
        return repoRoot().resolve("src/main/resources/assets/everechoes").resolve(relative);
    }

    /**
     * 执行 {@code data} 对应的业务操作。
     */
    private static Path data(String relative) {
        return repoRoot().resolve("src/main/resources/data/everechoes").resolve(relative);
    }

    /**
     * 执行 {@code javaSrc} 对应的业务操作。
     */
    private static Path javaSrc(String relative) {
        return repoRoot()
                .resolve("src/main/java/net/bluafolkloro/overdeterminism/everechoes")
                .resolve(relative);
    }

    /**
     * 执行 {@code repoRoot} 对应的业务操作。
     */
    private static Path repoRoot() {
        Path dir = Path.of("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.isDirectory(dir.resolve("src/main/resources/assets/everechoes"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new AssertionError("cannot locate repo root from " + Path.of("").toAbsolutePath());
    }
}
