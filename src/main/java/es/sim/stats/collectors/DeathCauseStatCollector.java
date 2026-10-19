package es.sim.stats.collectors;

import es.sim.game.entities.*;
import es.sim.stats.*;
import org.xguzm.pathfinding.grid.finders.*;

import java.util.*;

public class DeathCauseStatCollector<T extends Entity> implements StatCollector {

    public EnumMap<DeathCause, Long> amounts = new EnumMap<>(DeathCause.class);
    public Class<T> entityType;

    public DeathCauseStatCollector(Class<T> entityType) {
        this.entityType = entityType;
        for(DeathCause cause : DeathCause.values()) {
            amounts.put(cause, 0L);
        }
    }

    public void increase(DeathCause cause) {
        amounts.put(cause, amounts.get(cause) + 1L);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(entityType.getSimpleName()).append(" deaths:");
        for (Map.Entry<DeathCause, Long> entry : amounts.entrySet()) {
            sb.append("\n  ").append(entry.getKey()).append(": ").append(entry.getValue());
        }
        return sb.toString();
    }
}
