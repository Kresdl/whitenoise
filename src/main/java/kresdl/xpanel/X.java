package kresdl.xpanel;

/**
 * Interface defining a lockable panel with double buffer support
 */
public interface X {

    /**
     * Try to acquire a lock on this object.
     *
     * @return true if success, false if object is already locked.
     */
    public boolean lock();

    /**
     * Unlock.
     */
    public void unlock();

    /**
     * Is locked?
     *
     * @return lock state
     */
    public boolean isLocked();

    /**
     * Swap buffers.
     */
    public void swapBuffers();

    /**
     * Draw to back buffer.
     */
    public void drawImage();

    /**
     * Redraw. Convenience method for draw image, swap buffers and repaint.
     */
    public void redraw();
}
