package daomephsta.spinneret;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;
import daomephsta.spinneret.template.Template;
import daomephsta.spinneret.versioning.MinecraftVersion;

public class SpinneretInteractiveCLI
{
    public static void main(String[] args)
    {
        var spinneretArgs = new SpinneretArguments();
        try (var input = new Scanner(System.in))
        {
            prompt(input, "Template", "spinneret-java", spinneretArgs::template);
            prompt(input, "Minecraft version", Spinneret.minecraftVersions().getLatest().raw,
                spinneretArgs::minecraftVersion);
            spinneretArgs.selectTemplate(
                (mcVersion, templates) -> noMatchingTemplate(input, mcVersion, templates));

            prompt(input, "Mod name", null, spinneretArgs::modName);
            prompt(input, "Mod ID", spinneretArgs.suggestModId(), spinneretArgs::modId);
            prompt(input, "Folder name", spinneretArgs.suggestFolderName(), spinneretArgs::folderName);
            prompt(input, "Mod version", "0.0.1", spinneretArgs::modVersion);

            Spinneret.spin(spinneretArgs);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static Template noMatchingTemplate(Scanner input,
        MinecraftVersion minecraftVersion,
        List<Template> templates)
    {
        System.out.printf("No template for %s. Attempt to use latest template? (y/n) ", minecraftVersion);
        String response = input.nextLine().toLowerCase();
        if (response.startsWith("y"))
            return templates.stream().reduce((a, b) -> a.isLater(b) ? a : b).get();
        else if (response.startsWith("n"))
        {
            System.out.println("Template required, aborting");
            System.exit(0);
        }
        else
            return noMatchingTemplate(input, minecraftVersion, templates);
        throw new IllegalStateException("Unreachable");
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