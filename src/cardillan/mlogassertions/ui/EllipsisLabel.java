package cardillan.mlogassertions.ui;

import arc.graphics.Color;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.GlyphLayout;
import arc.scene.ui.Label;
import arc.util.Align;

public class EllipsisLabel extends Label {
    public static final int maxStringLength = 256;

    private String originalText;
    private int maxLines = 1;
    private boolean updating;

    public EllipsisLabel(CharSequence text) {
        super(text);
        originalText = text == null ? "" : text.toString();
        setWrap(true);
    }

    public EllipsisLabel maxLines(int maxLines) {
        if (maxLines < 1) throw new IllegalArgumentException("maxLines must be positive");
        this.maxLines = maxLines;
        invalidate();
        return this;
    }

    @Override
    public void setText(CharSequence text) {
        originalText = text == null ? "" : text.length() > maxStringLength ? text.subSequence(0, maxStringLength).toString() : text.toString();

        if (!updating) {
            super.setText(originalText);
        } else {
            super.setText(text);
        }
    }

    @Override
    public void layout() {
        if (updating || getWidth() <= 0 || originalText.isEmpty()) {
            super.layout();
            return;
        }

        Font font = getStyle().font;
        float maxHeight = font.getCapHeight() + (maxLines - 1) * font.getLineHeight();

        GlyphLayout layout = new GlyphLayout();

        if (originalText.length() < maxStringLength) {
            layout.setText(font, originalText, Color.white, getWidth(), Align.left, true);

            if (layout.height <= maxHeight) {
                super.layout();
                return;
            }
        }

        int low = 0;
        int high = originalText.length();

        while (low < high) {
            int mid = (low + high + 1) / 2;

            String candidate = originalText.substring(0, mid).stripTrailing() + "...";
            layout.setText(font, candidate, Color.white, getWidth(), Align.left, true);

            if (layout.height <= maxHeight) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }

        updating = true;
        super.setText(originalText.substring(0, low).stripTrailing() + "...");
        updating = false;

        super.layout();
    }
}
