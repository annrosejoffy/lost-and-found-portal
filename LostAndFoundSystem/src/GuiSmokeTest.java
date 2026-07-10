import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

/** Not part of the shipped app: a throwaway harness to screenshot each tab for QA. */
public class GuiSmokeTest {
    public static void main(String[] args) throws Exception {
        DataStore store = new DataStore();
        MainFrame[] frameHolder = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            MainFrame f = new MainFrame(store);
            f.setVisible(true);
            frameHolder[0] = f;
        });
        Thread.sleep(1000);
        Robot robot = new Robot();
        MainFrame frame = frameHolder[0];

        // Find the JTabbedPane
        JTabbedPane[] tabsHolder = new JTabbedPane[1];
        SwingUtilities.invokeAndWait(() -> {
            for (Component c : frame.getContentPane().getComponents()) {
                if (c instanceof JTabbedPane) tabsHolder[0] = (JTabbedPane) c;
            }
        });
        JTabbedPane tabs = tabsHolder[0];
        int count = tabs.getTabCount();
        for (int i = 0; i < count; i++) {
            final int idx = i;
            SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(idx));
            Thread.sleep(400);
            Rectangle bounds = frame.getBounds();
            BufferedImage img = robot.createScreenCapture(bounds);
            ImageIO.write(img, "png", new File("/tmp/tab_" + idx + ".png"));
            System.out.println("Captured tab " + idx + ": " + tabs.getTitleAt(idx));
        }
        System.out.println("SMOKE_TEST_OK");
        System.exit(0);
    }
}
