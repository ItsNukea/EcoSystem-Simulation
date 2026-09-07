package es.sim.game.board;

import es.sim.game.entities.Entity;

import java.util.Optional;

public class Cell {
    public final int x;
    public final int y;
    public Optional<Entity> holder = Optional.empty();

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setContents(Entity entity) {
        if(entity == null) {
            holder = Optional.empty();
        } else {
            holder = Optional.of(entity);
        }
    }
}
