package daomephsta.spinneret;

import java.util.Scanner;

import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;

public class SpinneretInteractiveCLI
{
    public static void main(String[] args)
    {
        var spinneretArgs = new SpinneretArguments();
        try (var input = new Scanner(System.in))
        {
            System.out.print("Mod name: ");
            spinneretArgs.modName(input.nextLine());
            prompt(input, "Mod ID", spinneretArgs.suggestModId(), spinneretArgs::modId);
            prompt(input, "Folder Name", spinneretArgs.suggestFolderName(), spinneretArgs::folderName);
        }
    }

    private interface ArgumentConsumer
    {
        public void consume(String argument) throws InvalidArgumentException;
    }

    private static void prompt(Scanner input, String prompt, String defaultValue, ArgumentConsumer consumer)
    {
        try
        {
            if (defaultValue == null || defaultValue.isBlank())
            {
                System.out.printf(prompt + ": ");
                var value = input.nextLine();
                if (value == null || value.isBlank())
                {
                    System.out.println("Invalid value");
                    prompt(input, prompt, defaultValue, consumer);
                }
                else
                    consumer.consume(value);
            }
            else
            {
                System.out.printf("%s (default '%s'): ", prompt, defaultValue);
                var value = input.nextLine();
                if (value == null || value.isBlank())
                    consumer.consume(defaultValue);
                else
                    consumer.consume(value);
            }
        }
        catch (InvalidArgumentException e)
        {
            System.out.println(e.getMessage());
            prompt(input, prompt, defaultValue, consumer);
        }
    }
}