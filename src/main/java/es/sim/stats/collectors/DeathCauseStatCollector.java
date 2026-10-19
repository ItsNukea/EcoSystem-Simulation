package es.sim.stats.collectors;

import es.sim.game.entities.*;
import es.sim.stats.*;
import tools.jackson.databind.node.*;

import java.util.*;

public class DeathCauseStatCollector<T extends Entity> implements StatCollector {
    public EnumMap<DeathCause, Long> data = new EnumMap<>(DeathCause.class);
    public Class<T> entityType;

    public DeathCauseStatCollector(Class<T> entityType) {
        this.entityType = entityType;
        for(DeathCause cause : DeathCause.values()) {
            data.put(cause, 0L);
        }
    }

    public void increase(DeathCause cause) {
        data.put(cause, data.get(cause) + 1L);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(entityType.getSimpleName()).append(" deaths:");
        for (Map.Entry<DeathCause, Long> entry : data.entrySet()) {
            sb.append("\n  ").append(entry.getKey()).append(": ").append(entry.getValue());
        }
        return sb.toString();
    }

    @Override
    public void save(ObjectNode root) {
        root.put("entityType", entityType.getName());

        ObjectNode amounts = root.putObject("data");
        this.data.forEach((cause, amount) -> {
            amounts.put(cause.name(), amount);
        });
    }

    @Override
    public String getName() {
        return "death_causes/" + entityType.getSimpleName();
    }
}