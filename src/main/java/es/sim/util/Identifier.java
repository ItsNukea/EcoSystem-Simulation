package es.sim.util;

import es.sim.exceptions.*;

import java.util.*;
import java.util.regex.*;

import static es.sim.Main.LOGGER;

/**An Identifier is a unique name given to a texture, entity, or tile that differentiates its type from other types.<br>
 * It is valid if and only if {@link Identifier#validNameOrThrow(String, String)} does not throw an {@link InvalidIdentifierException}
 */
public class Identifier {
    public static final String DEFAULT_NAMESPACE = "ess";
    private final String namespace;
    private final String path;

    public Identifier(String namespace, String path) {
        validNameOrThrow(namespace, path);
        this.namespace = namespace;
        this.path = path;
    }

    public Identifier(String path) {
        this(DEFAULT_NAMESPACE, path);
    }

    public static Identifier of(String namespace, String path) {
        return new Identifier(namespace, path);
    }
    
    public static Identifier of(String id) {
        int index = id.indexOf(':');
        int lastIndex = id.lastIndexOf(':');
        if(index == -1 || index != lastIndex) {
            throw new InvalidIdentifierException("Identifier does not contain exactly one char ':'");
        }
        String namespace = id.substring(0, index);
        String path = id.substring(index + 1);
        return of(namespace, path);
    }

    public static Identifier withDefaultNamespace(String path) {
        return new Identifier(path);
    }

    public String asString() {
        return namespace + ":" + path;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPath() {
        return path;
    }

    /**Checks whether the supplied {@code Identifier} is valid and throws an {@link  InvalidIdentifierException} if it's not.<br>
     *An {@code Identifier} should have
     */
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

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Identifier id)) return false;

        return Objects.equals(this.namespace, id.namespace) && Objects.equals(this.path, id.path) && this.hashCode() == id.hashCode();
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
