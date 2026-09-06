package es.sim.util;

import es.sim.exceptions.*;

import java.util.regex.*;

import static es.sim.Main.LOGGER;

public record Identifier(String namespace, String path) {
    public static final String DEFAULT_NAMESPACE = "ess";

    public Identifier {
        validNameOrThrow(namespace, path);
    }

    public Identifier(String path) {
        this(DEFAULT_NAMESPACE, path);
    }

    public static Identifier of(String namespace, String path) {
        return new Identifier(namespace, path);
    }

    public static Identifier withDefaultNamespace(String path) {
        return new Identifier(path);
    }

    public String asString() {
        return namespace + ":" + path;
    }

    public void validNameOrThrow(String namespace, String path) {
        Pattern validCharacters = Pattern.compile("[^a-z0-9_/-]");
        Matcher nameSpaceMatcher = validCharacters.matcher(namespace);
        Matcher pathMatcher = validCharacters.matcher(path);

        if (nameSpaceMatcher.find() || pathMatcher.find()) {
            InvalidIdentifierException e = new InvalidIdentifierException("Identifier " + namespace + ":" + path + "contains illegal characters! Allowed: a-z, 0-9, /, _, -");
            LOGGER.error("Invalid Identifier supplied", e);
            throw e;
        }
    }
}
