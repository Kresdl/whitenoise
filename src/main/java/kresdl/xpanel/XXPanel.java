package kresdl.xpanel;

import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import kresdl.geometry.Vec2;
import kresdl.utilities.FilterMouse;

/**
 * Utility panel with conveniance methods for logic integration and render
 * management.
 */
@SuppressWarnings("serial")
public abstract class XXPanel extends VolatileXPanel {

    private final FilterMouse mouse;

    /**
     * Constructs an XXPanel with given width,height and transparency type.
     *
     * @param w width
     * @param h height
     */
    public XXPanel(int w, int h) {
        super(w, h);
        mouse = new FilterMouse(this);
    }

    /**
     * Register callback on mouse click.
     *
     * @param button mouse button, 0-3
     * @param f callback
     */
    public void onMouseClick(int button, Consumer<MouseEvent> f) {
        mouse.onClick(button, get(f));
    }

    /**
     * Register callback on mouse press.
     *
     * @param button mousebutton, 0-3
     * @param f callback
     */
    public void onMousePress(int button, Consumer<MouseEvent> f) {
        mouse.onPress(button, get(f));
    }

    /**
     * Register callback on mouse move.
     *
     * @param f callback
     */
    public void onMouseMove(Consumer<MouseEvent> f) {
        mouse.onMove(get(f));
    }

    /**
     * Register callback on mouse drag.
     *
     * @param button mousebutton, 0-3
     * @param f callback
     */
    public void onMouseDrag(int button, Consumer<MouseEvent> f) {
        mouse.onDrag(button, get(f));
    }

    /**
     * Perform logic and redraw on separate thread, then schedule a repaint.
     *
     * @param logic logic to be evaluated before redraw
     */
    public void redraw(Runnable logic) {
        get(logic).run();
    }

    /**
     * Perform logic and redraw on separate thread, then schedule a repaint.
     *
     * @param <T> input type for logic
     * @param logic logic to be evaluated before redraw
     * @param value input for logic
     */
    public <T> void redraw(Consumer<T> logic, T value) {
        get(logic, value).run();
    }

    /**
     * Get mouse movement since last call to this method.
     *
     * @return mouse movement
     */
    public Vec2 getMouseMovement() {
        return mouse.getMovement();
    }

    private Consumer<MouseEvent> get(Consumer<MouseEvent> logic) {
        return e -> {
            if (lock()) {
                new Thread() {
                    @Override
                    public void run() {
                        logic.accept(e);
                        redraw();
                        unlock();
                    }
                }.start();
            }
        };
    }

    private Runnable get(Runnable logic) {
        return () -> {
            if (lock()) {
                new Thread() {
                    @Override
                    public void run() {
                        logic.run();
                        redraw();
                        unlock();
                    }
                }.start();
            }
        };
    }

    private <T> Runnable get(Consumer<T> logic, T value) {
        return () -> {
            if (lock()) {
                new Thread() {
                    @Override
                    public void run() {
                        logic.accept(value);
                        redraw();
                        unlock();
                    }
                }.start();
            }
        };
    }
}
