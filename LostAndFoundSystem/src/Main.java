import javax.swing.*;

/**
 * Lost and Found Management System - GUI entry point.
 * Launches the Swing-based interface (MainFrame) backed by the same
 * DataStore / MatchingEngine classes used throughout the project.
 */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to default look and feel
        }
        DataStore store = new DataStore();
        SwingUtilities.invokeLater(() -> new MainFrame(store).setVisible(true));
    }
}
