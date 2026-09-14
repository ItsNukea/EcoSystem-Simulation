package es.sim.game.board;

import es.sim.game.entities.Entity;

import java.util.Optional;

///A class that represents a single square on the {@link Board}. Each cell can hold nothing, or an entity.
public class Cell {
    public Optional<Entity> holder = Optional.empty();

    public Cell() {}

    public void setContents(Entity entity) {
        if(entity == null) {
            holder = Optional.empty();
        } else {
            holder = Optional.of(entity);
        }
    }
}
