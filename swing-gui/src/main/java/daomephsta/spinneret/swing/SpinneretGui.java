package daomephsta.spinneret.swing;

import static java.util.stream.Collectors.toList;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import daomephsta.spinneret.ArgumentSuggestions;
import daomephsta.spinneret.Spinneret;
import daomephsta.spinneret.SpinneretArguments;
import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;
import daomephsta.spinneret.swing.WizardPager.WizardPage;
import daomephsta.spinneret.versioning.MinecraftVersion;

public class SpinneretGui extends JFrame
{
    private static final String
        TEMPLATE_CARD = "template",
        MOD_INFO_CARD = "mod-info",
        LOADING_CARD = "loading";
    private final SpinneretArguments spinneretArgs = new SpinneretArguments();
    private final JButton
        prev = new JButton("< Prev"),
        next = new JButton("Next >");
    private final WizardPager pager = new WizardPager(this, spinneretArgs, TEMPLATE_CARD, MOD_INFO_CARD, "dummy");

    public SpinneretGui()
    {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImages(Stream.of("16", "32", "48", "256")
            .map(size ->
            {
                URL iconResource = getClass().getResource("/icon-" + size + ".png");
                return Toolkit.getDefaultToolkit().createImage(iconResource);
            })
            .collect(toList()));
        setTitle("Spinneret");

        var buttons = new JPanel();
        buttons.add(prev, BorderLayout.WEST);
        prev.setEnabled(false);
        prev.addActionListener(e ->
        {
            pager.previous();
            prev.setEnabled(pager.hasPrevious());
            next.setEnabled(pager.hasNext());
        });
        buttons.add(next, BorderLayout.EAST);
        next.setEnabled(false);
        next.addActionListener(e ->
        {
            pager.next();
            prev.setEnabled(pager.hasPrevious());
            next.setEnabled(pager.hasNext());
        });
        add(buttons, BorderLayout.SOUTH);

        var loading = new WizardPage<>();
        loading.add(new JLabel("Loading"));
        pager.add(LOADING_CARD, loading);
        CompletableFuture.supplyAsync(TemplateSelectionPage::new)
            .thenAccept(template ->
            {
                pager.add(TEMPLATE_CARD, template);
                pager.showPage(TEMPLATE_CARD);
                next.setEnabled(true);
            });
        pager.add(MOD_INFO_CARD, new ModInfoPage());
        pager.add("dummy", new WizardPage<>());
        pager.attach(this, BorderLayout.CENTER);
        pack();
    }

    @Override
    public Insets getInsets()
    {
        Insets insets = super.getInsets();
        insets.left += 5;
        insets.right += 5;
        return insets;
    }

    public static void main(String[] args)
    {
        EventQueue.invokeLater(() ->
        {
            var window = new SpinneretGui();
            window.setVisible(true);
        });
    }

    private static class TemplateSelectionPage extends WizardPage<GridBagConstraints>
    {
        private JComboBox<String> template;
        private JComboBox<MinecraftVersion> minecraftVersion;

        public TemplateSelectionPage()
        {
            super(new GridBagLayout(), GridBagConstraints::new);
            label("Template: ", (label, constraints) ->
            {
                constraints.anchor = GridBagConstraints.EAST;
                constraints.gridx = 0;
                constraints.gridy = 0;
            });
            this.template = combo((combo, constraints) ->
            {
                combo.setEditable(true);
                for (var alias : Spinneret.configuration().getTemplateAliases())
                    combo.addItem(alias);
                constraints.anchor = GridBagConstraints.WEST;
                constraints.gridx = 1;
                constraints.gridy = 0;
            });
            label("Minecraft version: ", (label, constraints) ->
            {
                constraints.anchor = GridBagConstraints.EAST;
                constraints.gridx = 0;
                constraints.gridy = 1;
            });
            this.minecraftVersion = combo((combo, constraints) ->
            {
                MinecraftVersion mc114 = Spinneret.minecraftVersions().get("1.14");
                for (var version : Spinneret.minecraftVersions().getDescending())
                {
                    // TODO make minimum configurable
                    if (version.compareTo(mc114) >= 0)
                        combo.addItem(version);
                }
                constraints.anchor = GridBagConstraints.WEST;
                constraints.gridx = 1;
                constraints.gridy = 1;
            });
        }

        @Override
        public void apply(SpinneretArguments spinneretArgs) throws InvalidArgumentException
        {
            spinneretArgs.template((String) template.getSelectedItem());
            spinneretArgs.minecraftVersion((MinecraftVersion) minecraftVersion.getSelectedItem());
            spinneretArgs.selectTemplateVariant((version, templateVariants) ->
            {
                int result = JOptionPane.showConfirmDialog(this,
                    "No template variant for " + version.raw + ". Attempt to use the latest template?",
                    "No matching template variant", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.NO_OPTION)
                {
                    System.exit(0);
                    throw new IllegalStateException("Unreachable");
                }
                else if (result == JOptionPane.YES_OPTION)
                    return templateVariants.stream().reduce((a, b) -> a.isLater(b) ? a : b).get();
                else
                    throw new IllegalStateException("Unexpected result " + result);
            });
        }
    }

    private static class ModInfoPage extends WizardPage<GridBagConstraints>
    {
        private final  JTextField
            modName,
            modId,
            rootPackage,
            folderName,
            modVersion;
        private final JTextArea description;
        private final JTextField authors;

        public ModInfoPage()
        {
            super(new GridBagLayout(), GridBagConstraints::new);
            label("Mod name: ", (label, constraints) ->
            {
                constraints.anchor = GridBagConstraints.EAST;
                constraints.gridx = 0;
                constraints.gridy = 0;
            });
            this.modName = textField((text, constraints) ->
            {
                text.setColumns(20);
                constraints.anchor = GridBagConstraints.WEST;
                constraints.gridx = 1;
                constraints.gridy = 0;
            });
            label("Mod ID: ", (label, constraints) ->
            {
                constraints.anchor = GridBagConstraints.EAST;
                constraints.gridx = 0;
                constraints.gridy = 1;
            });
            this.modId = textField((text, constraints) ->
            {
                text.setColumns(20);
                onFocusChanged(text, () ->
                {
                    if (text.getText().isEmpty())
                        text.setText(ArgumentSuggestions.modId(modName.getText()));
                });
                constraints.anchor = GridBagConstraints.WEST;
                constraints.gridx = 1;
                constraints.gridy = 1;
            });
            label("Description: ", (label, constraints) ->
            {
                constraints.anchor = GridBagConstraints.EAST;
                constraints.gridx = 0;
                constraints.gridy = 2;
            });
            this.description = textArea((text, constraints) ->
            {
                text.setColumns(20);
                constraints.anchor = GridBagConstraints.WEST;
                constraints.gridx = 1;
                constraints.gridy = 2;
            });
            label("Authors: ", (label, constraints) ->
            {
                constraints.anchor = GridBagConstraints.EAST;
                constraints.gridx = 0;
                constraints.gridy = 3;
            });
            this.authors = textField((text, constraints) ->
            {
                text.setColumns(20);
                constraints.gridx = 1;
                constraints.gridy = 3;
            });
            label("Root package: ", (label, constraints) ->
            {
                constraints.anchor = GridBagConstraints.EAST;
                constraints.gridx = 0;
                constraints.gridy = 4;
            });
            this.rootPackage = textField((text, constraints) ->
            {
                text.setColumns(20);
                onFocusChanged(text, () ->
                {
                    if (!modId.getText().isBlank() && !authors.getText().isBlank() && text.getText().isBlank())
                        text.setText(ArgumentSuggestions.rootPackageName(modId.getText(), getAuthors()));
                });
                constraints.gridx = 1;
                constraints.gridy = 4;
            });
            label("Folder name: ", (label, constraints) ->
            {
                constraints.anchor = GridBagConstraints.EAST;
                constraints.gridx = 0;
                constraints.gridy = 5;
            });
            this.folderName = textField((text, constraints) ->
            {
                text.setColumns(20);
                onFocusChanged(text, () ->
                {
                    if (!modName.getText().isBlank() && text.getText().isBlank())
                        text.setText(ArgumentSuggestions.folderName(modName.getText()));
                });
                constraints.gridx = 1;
                constraints.gridy = 5;
            });
            label("Mod version: ", (label, constraints) ->
            {
                constraints.anchor = GridBagConstraints.EAST;
                constraints.gridx = 0;
                constraints.gridy = 6;
            });
            this.modVersion = textField((text, constraints) ->
            {
                text.setColumns(20);
                text.setText("0.0.1");
                constraints.gridx = 1;
                constraints.gridy = 6;
            });
        }

        private static void onFocusChanged(JTextField textField, Runnable listener)
        {
            textField.addFocusListener(new FocusListener()
            {
                @Override
                public void focusLost(FocusEvent e)
                {
                    listener.run();
                }

                @Override
                public void focusGained(FocusEvent e)
                {
                    listener.run();
                }
            });
        }

        @Override
        public void apply(SpinneretArguments spinneretArgs) throws InvalidArgumentException
        {
            List<String> problems = new ArrayList<>();
            if (modName.getText().isBlank())
                problems.add("Missing mod name");
            if (modId.getText().isBlank())
                problems.add("Missing mod ID");
            if (description.getText().isBlank())
                problems.add("Missing description");
            if (authors.getText().isBlank())
                problems.add("Missing authors");
            if (rootPackage.getText().isBlank())
                problems.add("Missing root package name");
            if (folderName.getText().isBlank())
                problems.add("Missing folder name");
            if (modVersion.getText().isBlank())
                problems.add("Missing mod version");
            if (!problems.isEmpty())
                throw new InvalidArgumentException("Missing required information", problems);
            spinneretArgs.modName(modName.getText())
                .modId(modId.getText())
                .description(description.getText());
            for (String author : getAuthors())
                spinneretArgs.addAuthor(author);
            spinneretArgs.rootPackageName(rootPackage.getText())
                .folderName(folderName.getText())
                .modVersion(modVersion.getText());
        }

        private List<String> getAuthors()
        {
            return Arrays.stream(authors.getText().split(","))
                .map(String::strip)
                .toList();
        }
    }
}
