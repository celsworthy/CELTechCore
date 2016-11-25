package celtech.WebEngineFix;

import com.sun.webkit.ThemeClient;
import com.sun.webkit.graphics.RenderTheme;
import com.sun.webkit.graphics.ScrollBarTheme;

public final class ThemeClientImpl extends ThemeClient {
    private final Accessor accessor;

    public ThemeClientImpl(Accessor accessor) {
        this.accessor = accessor;
    }

    @Override protected RenderTheme createRenderTheme() {
        return new RenderThemeImpl(accessor);
    }

    @Override protected ScrollBarTheme createScrollBarTheme() {
        return new ScrollBarThemeImpl(accessor);
    }
}
