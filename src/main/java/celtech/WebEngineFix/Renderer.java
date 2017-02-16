package celtech.WebEngineFix;

import javafx.scene.control.Control;

import com.sun.webkit.graphics.WCGraphicsContext;

public abstract class Renderer {
    private static Renderer instance;

    public static void setRenderer(Renderer renderer) {
        instance = renderer;
    }

    public static Renderer getRenderer() {
        return instance;
    }

    protected abstract void render(Control c, WCGraphicsContext g);
}
