package es.sim.io;

import tools.jackson.databind.node.*;

public interface Savable {
    void save(ObjectNode root);
}
