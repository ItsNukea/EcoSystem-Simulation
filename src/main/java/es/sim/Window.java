package es.sim;

import es.sim.gui.*;

import javax.swing.*;
import java.awt.*;

public class Window extends JFrame {

    public Window() {
        super("Eco System Simulator");
    }

    public void init() {
        //set the correct size first
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = ge.getDefaultScreenDevice();
        Rectangle bounds = device.getDefaultConfiguration().getBounds();
        this.setSize(new Dimension(bounds.width, bounds.height));
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void showScreen(Screen screen) {
        this.setContentPane(screen);
        this.revalidate();
        this.repaint();
    }

    public void setFullscreen() {
        GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
    }

    public static void setFullscreenWindow(Window window) {
        window.setFullscreen();
    }
}
