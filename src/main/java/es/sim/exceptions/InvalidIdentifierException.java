package es.sim.exceptions;

import es.sim.util.Identifier;

/// An {@link Exception} that is thrown whenever an invalid {@link Identifier} is supplied to any {@link Identifier}
/// constructor.
public class InvalidIdentifierException extends RuntimeException {
    public InvalidIdentifierException(String message) {
        super(message);
    }
}
