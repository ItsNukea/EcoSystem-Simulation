package es.sim.game;

import java.awt.*;

/// Represents a direction
public enum Direction {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    private final int xComponent, yComponent;

    Direction(int xComponent, int yComponent) {
        this.xComponent = xComponent;
        this.yComponent = yComponent;
    }

    public static Direction fromCoordSet(int moveX, int moveY) {
        for (Direction dir : values()) {
            if (dir.xComponent == moveX && dir.yComponent == moveY) {
                return dir;
            }
        }
        throw new IllegalArgumentException("Invalid move: (" + moveX + ", " + moveY + ")");
    }
    
    public static Direction inversed(Direction direction) {
        return Direction.fromCoordSet(-direction.xComponent(), -direction.yComponent());
    }

    public int xComponent() {
        return xComponent;
    }

    public int yComponent() {
        return yComponent;
    }
}