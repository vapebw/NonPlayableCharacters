package dev.custom.npcs.infrastructure;

import cn.nukkit.entity.data.Skin;
import cn.nukkit.utils.SerializedImage;
import dev.custom.npcs.api.NpcVisualProfile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

public final class NpcSkinCodec {
    private NpcSkinCodec() {
    }

    public static NpcVisualProfile fromSkin(Skin skin, String skinId) {
        String geometryName = extractGeometryName(skin.getSkinResourcePatch());
        String encoded = encode(skin.getSkinData().data);
        return new NpcVisualProfile(
                skinId,
                geometryName,
                nullSafe(skin.getGeometryData()),
                encoded,
                nullSafe(skin.getSkinResourcePatch())
        );
    }

    public static NpcVisualProfile fromFile(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Invalid skin file: " + file.getPath());
        }
        Skin skin = new Skin();
        skin.setSkinData(image);
        skin.generateSkinId(file.getName());
        String geometryName = "geometry.humanoid.custom";
        String patch = geometryPatch(geometryName);
        return new NpcVisualProfile(
                "file:" + file.getName(),
                geometryName,
                "",
                encode(skin.getSkinData().data),
                patch
        );
    }

    public static Skin decode(NpcVisualProfile visual) {
        Skin skin = new Skin();
        if (!visual.skinData().isEmpty()) {
            skin.setSkinData(new SerializedImage(0, 0, Base64.getDecoder().decode(visual.skinData())));
        }
        if (!visual.skinId().isEmpty()) {
            skin.setSkinId(visual.skinId());
        }
        if (!visual.geometryData().isEmpty()) {
            skin.setGeometryData(visual.geometryData());
        }
        String patch = visual.skinResourcePatch().isEmpty() ? geometryPatch(visual.geometryName()) : visual.skinResourcePatch();
        if (!patch.isEmpty()) {
            skin.setSkinResourcePatch(patch);
        }
        return skin;
    }

    public static String geometryPatch(String geometryName) {
        if (geometryName == null || geometryName.isEmpty()) {
            return "";
        }
        return "{\"geometry\":{\"default\":\"" + geometryName + "\"}}";
    }

    private static String encode(byte[] value) {
        return value == null || value.length == 0 ? "" : Base64.getEncoder().encodeToString(value);
    }

    private static String extractGeometryName(String patch) {
        if (patch == null || patch.isEmpty()) {
            return "";
        }
        int marker = patch.indexOf("\"default\":\"");
        if (marker < 0) {
            return "";
        }
        int start = marker + "\"default\":\"".length();
        int end = patch.indexOf('"', start);
        return end < 0 ? "" : patch.substring(start, end);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
