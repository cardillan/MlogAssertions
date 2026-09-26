package cardillan.mlogassertions.logic;

import arc.scene.style.TextureRegionDrawable;
import mindustry.gen.Icon;

public enum SnapshotType {
    isolated    (Icon.logic),
    connected   (Icon.sitemap),
    global      (Icon.planet),
    ;

    public final TextureRegionDrawable icon;

    SnapshotType(TextureRegionDrawable icon) {
        this.icon = icon;
    }

    public static final SnapshotType[] all = values();
}
