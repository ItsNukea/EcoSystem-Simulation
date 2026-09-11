package es.sim;

import es.sim.gui.*;

import javax.imageio.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;

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
        try(InputStream in = Main.class.getResourceAsStream("/textures/ess/appicon.png")) {
            assert in != null;
            BufferedImage image = ImageIO.read(in);
            this.setIconImage(image);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void showScreen(Screen screen) {
        this.setContentPane(screen);
        this.revalidate();
        this.repaint();
    }

    ///Returns the current shown screen
    public Screen getScreen() {
        return (Screen) getContentPane();
    }

    public void setFullscreen() {
        GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
    }

    public static void setFullscreenWindow(Window window) {
        window.setFullscreen();
    }
}
