package es.sim;

import es.sim.gui.*;

import javax.imageio.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;

public class Window extends JFrame {
    private boolean isFullScreen = false;

    public Window() {
        super("Eco System Simulator");
    }

    public void init() {
        //set the correct size first
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = ge.getDefaultScreenDevice();
        Rectangle bounds = device.getDefaultConfiguration().getBounds();

        this.setSize(new Dimension(bounds.width, bounds.height));
        this.setFullScreen();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        try(InputStream in = Main.class.getResourceAsStream("/textures/ess/appicon.png")) {
            assert in != null;
            BufferedImage image = ImageIO.read(in);
            this.setIconImage(image);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                getScreen().keyPressed(e);
            }

            @Override
            public void keyTyped(KeyEvent e) {
                getScreen().keyTyped(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                getScreen().keyReleased(e);
            }
        });

        MouseAdapter ma =  new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                getScreen().mouseClicked(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                getScreen().mousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                getScreen().mouseReleased(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                getScreen().mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                getScreen().mouseExited(e);
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                getScreen().mouseWheelMoved(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                getScreen().mouseDragged(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                getScreen().mouseMoved(e);
            }
        };
        this.addMouseListener(ma);
        this.addMouseMotionListener(ma);
        this.addMouseWheelListener(ma);
    }

    public void showScreen(Screen screen) {
        this.setContentPane(screen);
        this.revalidate();
        this.repaint();
        this.requestFocusInWindow();
    }

    ///Returns the current shown screen
    public Screen getScreen() {
        return (Screen) getContentPane();
    }

    public void toggleFullScreen() {
        if(isFullScreen) {
            unFullScreen();
        } else {
            setFullScreen();
        }
    }

    public void setFullScreen() {
        GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
        isFullScreen = true;
    }

    public void unFullScreen() {
        GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(null);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        revalidate();

        this.repaint();
        isFullScreen = false;
    }

    public static void setFullscreenWindow(Window window) {
        window.setFullScreen();
    }
}
