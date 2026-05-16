package socialnetwork.front.seirs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Pre-launch dialog: lets the user choose population size and initial speed
 * before the simulation window opens.
 */
public final class LaunchDialog extends JDialog {

    private static final int DEFAULT_POP   = 1200;
    private static final int MIN_POP       = 50;
    private static final int MAX_POP       = 5000;
    private static final int DEFAULT_SPEED = 50;  // slider midpoint = 1× speed

    private final JSpinner    popSpinner;
    private final JSlider     speedSlider;
    private final JLabel      speedLabel;
    private boolean confirmed = false;

    public LaunchDialog(Frame owner) {
        super(owner, "Configurer la simulation", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        // Population spinner
        SpinnerNumberModel model = new SpinnerNumberModel(DEFAULT_POP, MIN_POP, MAX_POP, 50);
        popSpinner = new JSpinner(model);
        popSpinner.setPreferredSize(new Dimension(90, 26));

        // Speed slider (0–100, maps to 0.01×–5.0×)
        speedSlider = new JSlider(0, 100, DEFAULT_SPEED);
        speedSlider.setMajorTickSpacing(25);
        speedSlider.setMinorTickSpacing(5);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(false);
        speedLabel = new JLabel(speedText(DEFAULT_SPEED), SwingConstants.RIGHT);
        speedLabel.setPreferredSize(new Dimension(70, 22));
        speedSlider.addChangeListener(e ->
            speedLabel.setText(speedText(speedSlider.getValue())));

        // Layout
        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(new EmptyBorder(16, 20, 8, 20));
        GridBagConstraints lc = new GridBagConstraints();
        lc.anchor = GridBagConstraints.EAST;
        lc.insets = new Insets(4, 0, 4, 8);
        GridBagConstraints vc = new GridBagConstraints();
        vc.gridwidth = GridBagConstraints.REMAINDER;
        vc.fill = GridBagConstraints.HORIZONTAL;
        vc.insets = new Insets(4, 0, 4, 0);

        content.add(new JLabel("Nombre de personnes :"), lc);
        content.add(popSpinner, vc);

        content.add(new JLabel("Vitesse initiale :"), lc);
        JPanel speedRow = new JPanel(new BorderLayout(6, 0));
        speedRow.add(speedSlider, BorderLayout.CENTER);
        speedRow.add(speedLabel,  BorderLayout.EAST);
        vc.gridwidth = GridBagConstraints.REMAINDER;
        content.add(speedRow, vc);

        // Note about rumor system
        JLabel note = new JLabel(
            "<html><small>Les scénarios de crise se gèrent depuis la barre latérale.</small></html>");
        note.setBorder(new EmptyBorder(4, 0, 0, 0));
        GridBagConstraints nc = new GridBagConstraints();
        nc.gridwidth = GridBagConstraints.REMAINDER;
        nc.insets    = new Insets(6, 0, 6, 0);
        content.add(note, nc);

        // Buttons
        JButton launch = new JButton("Lancer");
        JButton cancel = new JButton("Annuler");
        launch.setPreferredSize(new Dimension(90, 28));
        cancel.setPreferredSize(new Dimension(90, 28));
        launch.addActionListener(e -> { confirmed = true; dispose(); });
        cancel.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setBorder(new EmptyBorder(0, 0, 8, 0));
        buttons.add(cancel);
        buttons.add(launch);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(content, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(launch);
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isConfirmed()    { return confirmed; }
    public int     getPopulation()  { return (Integer) popSpinner.getValue(); }
    public double  getInitialSpeed() { return sliderToSpeed(speedSlider.getValue()); }

    /** Maps slider [0..100] to speed [0.0001..5.0] on a log-like scale. */
    public static double sliderToSpeed(int v) {
        if (v <= 50) {
            return 0.0001 + (1.0 - 0.0001) * (v / 50.0);
        } else {
            return 1.0 + (5.0 - 1.0) * ((v - 50) / 50.0);
        }
    }

    /** Maps speed [0.0001..5.0] back to slider [0..100]. */
    public static int speedToSlider(double speed) {
        if (speed <= 1.0) {
            return (int) Math.round((speed - 0.0001) / (1.0 - 0.0001) * 50.0);
        } else {
            return (int) Math.round(50.0 + (speed - 1.0) / (5.0 - 1.0) * 50.0);
        }
    }

    private static String speedText(int v) {
        double s = sliderToSpeed(v);
        if (s < 0.01) return String.format("%.2f%%", s * 100);
        if (s < 1.0)  return String.format("%.0f%%", s * 100);
        return String.format("%.1fx", s);
    }
}
