package es.sim.gui;

import org.jetbrains.annotations.*;

import javax.swing.*;
import java.awt.*;

public abstract class Screen extends JPanel {
    @Nullable
    Screen parent;

    protected Screen(@Nullable Screen parent) {
        super();
        this.parent = parent;
    }

    @Override
    protected final void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        render(g2d);
        g2d.dispose();
    }

    public abstract void render(Graphics2D g2d);

    public void onClose() {}
}
