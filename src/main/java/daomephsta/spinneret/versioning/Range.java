package daomephsta.spinneret.versioning;

import java.util.function.Function;
import java.util.function.Predicate;

import com.google.gson.JsonParseException;

public class Range<T extends Comparable<T>>
{
    public final T min, max;
    private final Predicate<T> minBound, maxBound;
    private String raw;

    public Range(T min, T max, Predicate<T> minBound, Predicate<T> maxBound)
    {
        this.min = min;
        this.max = max;
        this.minBound = minBound;
        this.maxBound = maxBound;
    }

    public interface RangeBuilder<T extends Comparable<T>>
    {
        public Range<T> createRange(T max, Predicate<T> maxBoundPredicate);

        public default Range<T> open(T max)
        {
            return createRange(max, t -> max.compareTo(t) > 0);
        }

        public default Range<T> closed(T max)
        {
            return createRange(max, t -> max.compareTo(t) >= 0);
        }
    }

    public static <T extends Comparable<T>> RangeBuilder<T> open(T min)
    {
        return (max, maxBound) -> new Range<>(min, max, t -> min.compareTo(t) < 0, maxBound);
    }

    public static <T extends Comparable<T>> RangeBuilder<T> closed(T min)
    {
        return (max, maxBound) -> new Range<>(min, max, t -> min.compareTo(t) <= 0, maxBound);
    }

    public static <T extends Comparable<T>> Range<T> degenerate(T element)
    {
        return new Range<>(element, element, t -> t == element, t -> t == element);
    }

    public static <T extends Comparable<T>> Range<T> parse(Function<String, T> boundParser, String range)
    {
        var rangeParts = range.split(",");
        Range<T> parsed = switch (rangeParts.length)
        {
        case 1 ->
            Range.degenerate(boundParser.apply(rangeParts[0]));
        case 2 ->
        {
            String min = rangeParts[0];
            RangeBuilder<T> rangeBuilder = switch (min.charAt(0))
            {
            case '[' -> Range.closed(boundParser.apply(min.substring(1)));
            case '(' -> Range.open(boundParser.apply(min.substring(1)));
            default -> throw new JsonParseException("Invalid range format " + range);
            };

            String max = rangeParts[1];
            yield switch (max.charAt(max.length() - 1))
            {
                case ']' -> rangeBuilder.closed(boundParser.apply(max.substring(0, max.length() - 1)));
                case ')' -> rangeBuilder.open(boundParser.apply(max.substring(0, max.length() - 1)));
                default -> throw new JsonParseException("Invalid range format " + range);
            };
        }
        default ->
            throw new IllegalArgumentException("Unexpected value: " + rangeParts.length);
        };
        parsed.raw = range;
        return parsed;
    }

    public boolean contains(T t)
    {
        return minBound.test(t) && maxBound.test(t);
    }

    @Override
    public String toString()
    {
        if (raw != null)
            return raw;
        return super.toString();
    }
}
