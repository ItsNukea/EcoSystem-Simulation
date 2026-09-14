package es.sim.game;

public enum Direction {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    private int moveX, moveY;

    Direction(int moveX, int moveY) {
        this.moveX = moveX;
        this.moveY = moveY;
    }

    public static Direction fromCoordSet(int moveX, int moveY) {
        for (Direction dir : values()) {
            if (dir.moveX == moveX && dir.moveY == moveY) {
                return dir;
            }
        }
        throw new IllegalArgumentException("Invalid move: (" + moveX + ", " + moveY + ")");
    }

    public int xComponent() {
        return moveX;
    }

    public int yComponent() {
        return moveY;
    }
}
