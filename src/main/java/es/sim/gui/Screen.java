package es.sim.gui;

import es.sim.*;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public abstract class Screen extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    @Nullable
    private final Screen parent;
    protected final boolean shouldCloseOnEsc = true;

    protected Screen(@Nullable Screen parent) {
        super();
        this.parent = parent;
        setFocusable(true);
    }

    @Override
    protected final void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        render(g2d);
        g2d.dispose();
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
    }

    protected void drawCenteredString(Graphics2D graphics, String str, int centerX, int centerY) {
        FontMetrics metrics = graphics.getFontMetrics();
        int strWidth = metrics.stringWidth(str);
        int strHeight = metrics.getAscent() + metrics.getDescent();

        graphics.drawString(str, centerX - strWidth / 2, centerY + strHeight / 2);
    }

    public abstract void render(Graphics2D g2d);

    public void onClose() {
        Main.getWindow().showScreen(parent);
    }

    //Keyboard and Mouse Listener methods
    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_ESCAPE && shouldCloseOnEsc) {
            onClose();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {}

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {}
}
