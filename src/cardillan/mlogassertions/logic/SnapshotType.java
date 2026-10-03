package cardillan.mlogassertions.logic;

import arc.scene.style.TextureRegionDrawable;
import mindustry.gen.Icon;

public enum SnapshotType {
    isolated    (Icon.logic,    (char)59406),
    connected   (Icon.sitemap,  (char)61672),
    recording   (Icon.layers,   (char)59455),
    global      (Icon.planet,   (char)59443),
    ;

    public final TextureRegionDrawable icon;
    public final char charIcon;

    SnapshotType(TextureRegionDrawable icon, char charIcon) {
        this.icon = icon;
        this.charIcon = charIcon;
    }

    public static final SnapshotType[] all = values();
}
