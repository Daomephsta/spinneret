package daomephsta.spinneret;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;

import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;

public class ArgumentTests
{
    private Spinneret spinneret = new Spinneret(Paths.get("."));

    @Test
    public void minimumModIDLength()
    {
        var error = assertThrows(InvalidArgumentException.class,
            () -> spinneret.createArguments().modId(""));
        assertTrue(error.problems.contains(
            "Minimum mod ID length is 1 character"));
    }

    @Test
    public void maximumModIDLength()
    {
        StringBuilder tooLong = new StringBuilder(72);
        for (int i = 0; i < 72; i++)
        {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            tooLong.append((char) ('a' + random.nextInt('z' - 'a' + 1)));
        }
        var error = assertThrows(InvalidArgumentException.class,
            () -> spinneret.createArguments().modId(tooLong.toString()));
        assertTrue(error.problems.contains(
            "Maximum mod ID length is 64 characters"));
    }

    @Test
    public void invalidModIDStartCharacter()
    {
        var error = assertThrows(InvalidArgumentException.class,
            () -> spinneret.createArguments().modId("42istheanswer"));
        assertTrue(error.problems.contains("Start of mod ID must be a-z"));
    }

    @Test
    public void invalidModIDCharacter()
    {
        var error = assertThrows(InvalidArgumentException.class,
            () -> spinneret.createArguments().modId("tèst1"));
        assertTrue(error.problems.contains("Invalid character at index 1. "
            + "Non-start mod ID characters must be a-z, 0-9, -, or _"));
    }
}
