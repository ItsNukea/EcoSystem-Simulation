package es.sim.stats.collectors;

import es.sim.game.entities.*;
import es.sim.stats.*;
import org.xguzm.pathfinding.grid.finders.*;

import java.util.*;

public class DeathCauseStatCollector<T extends Entity> implements StatCollector {
    private static final ArrayList<DeathCauseStatCollector<?>> allCollectors = new ArrayList<>();

    public EnumMap<DeathCause, Long> amounts = new EnumMap<>(DeathCause.class);
    public Class<T> entityType;

    public DeathCauseStatCollector(Class<T> entityType) {
        this.entityType = entityType;
        for(DeathCause cause : DeathCause.values()) {
            amounts.put(cause, 0L);
        }

        allCollectors.add(this);
    }

    public void increase(DeathCause cause) {
        amounts.put(cause, amounts.get(cause) + 1L);
    }

    public static <T extends Entity> DeathCauseStatCollector<T> getFromClass(Class<T> entityType) {
        for(DeathCauseStatCollector<?> collector : allCollectors) {
            if(collector.entityType.equals(entityType)) {
                //We already checked this, but in a way the compiler doesn't like it.
                //We need to perform a sacrifice so the compiler gods won't screw us over
                @SuppressWarnings("unchecked")
                DeathCauseStatCollector<T> casted = (DeathCauseStatCollector<T>) collector;
                return casted;
            }
        }
        return null;
    }
}
