package es.sim;

import es.sim.gui.*;
import es.sim.texture.*;
import es.sim.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

///This class represents the main window of the program.
public class Window extends JFrame {
    private boolean isFullScreen = false;

    public Window() {
        super("Eco System Simulator");
    }

    ///This method initializes the window as a normal maximized window (title bar, close button and taskbar stay
    ///available, so it can always be closed, even if the program freezes), and adds a window icon.
    ///Fullscreen is optional: press F11 or use the fullscreen button
    public void init() {
        //set the correct size first
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = ge.getDefaultScreenDevice();
        Rectangle bounds = device.getDefaultConfiguration().getBounds();
        Texture iconImage = new Texture(Identifier.withDefaultNamespace("appicon"));

        this.setSize(new Dimension(bounds.width, bounds.height));
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setIconImage(iconImage.asImage());


        KeyAdapter ka = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                //F11 switches between the normal window and fullscreen, and is not passed on to the current screen
                if (e.getKeyCode() == KeyEvent.VK_F11) {
                    toggleFullScreen();
                    return;
                }
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
        };
        this.addKeyListener(ka);

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

        FocusListener fl = new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {}

            @Override
            public void focusLost(FocusEvent e) {}
        };
        this.addFocusListener(fl);
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

    ///Switches between the normal window and fullscreen. Bound to the F11 key and to the fullscreen button,
    ///see {@link #applyFullScreen(boolean)}
    public void toggleFullScreen() {
        applyFullScreen(!isFullScreen);
    }

    ///Makes the window fullscreen, see {@link #applyFullScreen(boolean)}
    public void setFullScreen() {
        applyFullScreen(true);
    }

    ///Returns the window to its normal maximized state, with title bar, see {@link #applyFullScreen(boolean)}
    public void unFullScreen() {
        applyFullScreen(false);
    }

    ///"Fullscreen" is a borderless window covering the screen, deliberately NOT Java's exclusive fullscreen mode.
    ///Exclusive mode cannot be left when the program freezes, which left the user stuck. As an ordinary window,
    ///Alt+Tab, the Windows key, Ctrl+Shift+Esc (Task Manager) and the "not responding" dialog keep working
    ///@param enable {@code true} to go fullscreen, {@code false} to go back to the normal window
    private void applyFullScreen(boolean enable) {
        if (isFullScreen == enable) return;

        //Decorations can only be changed while the window is not displayable
        dispose();
        setUndecorated(enable);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
        toFront();
        requestFocus();

        isFullScreen = enable;
    }

    public static void setFullscreenWindow(Window window) {
        window.setFullScreen();
    }
}
