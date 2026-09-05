package es.sim.logging;

import ch.qos.logback.classic.*;
import ch.qos.logback.classic.spi.*;
import ch.qos.logback.core.pattern.color.*;

public class LevelColorMessageConverter extends ForegroundCompositeConverterBase<ILoggingEvent> {

    @Override
    protected String getForegroundColorCode(ILoggingEvent event) {
        return switch (event.getLevel().toInt()) {
            case Level.ERROR_INT -> ANSIConstants.RED_FG;
            case Level.WARN_INT -> ANSIConstants.YELLOW_FG;
            default -> ANSIConstants.DEFAULT_FG;
        };
    }

    @Override
    public String transform(ILoggingEvent event, String in) {
        int level = event.getLevel().toInt();
        if (level == Level.WARN_INT || level == Level.ERROR_INT) {
            return super.transform(event, in);
        }
        return in; // leave INFO/DEBUG/TRACE messages uncolored
    }
}