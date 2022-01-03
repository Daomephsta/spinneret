package daomephsta.spinneret.swing;

import static java.util.stream.Collectors.toList;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import daomephsta.spinneret.Spinneret;
import daomephsta.spinneret.SpinneretArguments;
import daomephsta.spinneret.SpinneretArguments.InvalidArgumentException;
import daomephsta.spinneret.swing.WizardPager.WizardPage;
import daomephsta.spinneret.versioning.MinecraftVersion;

public class SpinneretGui extends JFrame
{
    private static final String
        TEMPLATE_CARD = "template",
        LOADING_CARD = "loading";
    private final SpinneretArguments spinneretArgs = new SpinneretArguments();
    private final JButton
        prev = new JButton("< Prev"),
        next = new JButton("Next >");
    private final WizardPager cards = new WizardPager(this, spinneretArgs, TEMPLATE_CARD, "test");

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

        var buttons = new JPanel();
        buttons.add(prev, BorderLayout.WEST);
        prev.setEnabled(false);
        prev.addActionListener(e ->
        {
            cards.previous();
            prev.setEnabled(cards.hasPrevious());
            next.setEnabled(cards.hasNext());
        });
        buttons.add(next, BorderLayout.EAST);
        next.setEnabled(false);
        next.addActionListener(e ->
        {
            cards.next();
            prev.setEnabled(cards.hasPrevious());
            next.setEnabled(cards.hasNext());
        });
        add(buttons, BorderLayout.SOUTH);

        var loading = new WizardPage<>();
        loading.add(new JLabel("Loading"));
        cards.add(LOADING_CARD, loading);
        CompletableFuture.supplyAsync(TemplateSelectionPage::new)
            .thenAccept(template ->
            {
                cards.add(TEMPLATE_CARD, template);
                cards.showPage(TEMPLATE_CARD);
                next.setEnabled(true);
            });
        var test = new WizardPage<>();
        test.add(new JLabel("TEST"));
        cards.add("test", test);
        cards.attach(this, BorderLayout.CENTER);
        pack();
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
            super(new GridLayout(2, 2), GridBagConstraints::new);
            label("Template:", (label, constraints) ->
            {
                label.setHorizontalAlignment(SwingConstants.RIGHT);
                constraints.gridy = 0;
            });
            this.template = combo((combo, constraints) ->
            {
                combo.setEditable(true);
                for (var alias : Spinneret.configuration().getTemplateAliases())
                    combo.addItem(alias);
                constraints.gridx = 0;
                constraints.gridy = 1;
            });
            label("Minecraft version:", (label, constraints) ->
            {
                label.setHorizontalAlignment(SwingConstants.RIGHT);
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
                return null;
            });
        }
    }
}
