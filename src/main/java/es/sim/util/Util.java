package es.sim.util;

public class Util {
    public static long toNanos(long millis) {
        return millis * 1000000L;
    }

    public static long toMillis(long nanos) {
        return nanos / 1000000L;
    }


    /// This method transforms a number in the linear range {@code [0, 1]} into a value in the
    /// logarithmic range {@code [min, max]}<br>
    /// The resulting distribution is centered around the geometric mean of {@code min} and {@code max}
    /// Note that values closer to {@code min} or {@code max} have a smaller probability of getting returned, while
    /// values near the geometric center have a larger probability of occuring
    /// @param randomNumber A number in the linear range {@code [0, 1]}
    /// @param min The minimum value of the ouput logarithmic range
    /// @param max The maximum value of the range logarithmic range
    /// @param centerStrength How much the result will skew to the center
    public static double linearToLogarithmicDistribution(double randomNumber, double min, double max, double centerStrength) {
        if(min <= 0) {
            throw new ArithmeticException("The minimum value in the output logarithmic range is smaller than 0! Value was " + min);
        }
        if(max <= min) {
            throw new ArithmeticException("The maximum value from the output range is smaller than the minimum value! Max was " + max + ", Min was " + min);
        }
        if(centerStrength <= 0) {
            throw new ArithmeticException("The center strength is smaller than 0! Value was " + centerStrength);
        }
        if(randomNumber < 0 || randomNumber > 1) {
            throw new ArithmeticException("The random number supplied is not in the range [0, 1]! Value was " + randomNumber);
        }

        double center = Math.sqrt(min * max);   //Geometric center of the output range

        double normalizedOffset = 2.0 * randomNumber - 1.0;
        double exponent = Math.copySign(
                Math.pow(Math.abs(normalizedOffset), centerStrength),
                normalizedOffset
        );

        return center * Math.pow(max / center, exponent);
    }
}
