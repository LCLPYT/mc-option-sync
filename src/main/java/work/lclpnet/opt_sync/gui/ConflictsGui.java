package work.lclpnet.opt_sync.gui;

import org.slf4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

public class ConflictsGui {

    private final Logger logger;
    private boolean shouldOverwrite = false;

    public ConflictsGui(Logger logger) {
        this.logger = logger;
    }

    public boolean awaitResponse() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }

        // TODO translate
        String title = "Sync conflicts detected - mc-option-sync";

        System.setProperty("apple.awt.application.appearance", "system");
        System.setProperty("apple.awt.application.name", title);

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        var latch = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> openWindow(latch, title));

        try {
            latch.await();
            return shouldOverwrite;
        } catch (InterruptedException e) {
            return false;
        }
    }

    private void openWindow(CountDownLatch latch, String title) {
        // use similar logic as fabric-loader FabricMainWindow as that will work as well
        var frame = new JFrame();
        frame.setVisible(false);
        frame.setTitle(title);

        try {
            Image image = readIcon();
            frame.setIconImage(image);

            Taskbar.getTaskbar().setIconImage(image);
        } catch (IOException e) {
            logger.error("Failed to load icon", e);
        } catch (UnsupportedOperationException ignored) {}

        frame.setMinimumSize(new Dimension(500, 250));
        frame.setPreferredSize(new Dimension(550, 250));
        frame.setLocationByPlatform(true);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        addContent(frame);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                latch.countDown();
            }
        });

        frame.pack();
        frame.setVisible(true);
        frame.requestFocus();
    }

    private BufferedImage readIcon() throws IOException {
        InputStream in = ConflictsGui.class.getResourceAsStream("/assets/mc-option-sync/icon.png");

        if (in == null) {
            throw new IllegalStateException("Icon not found");
        }

        try (in) {
            return ImageIO.read(in);
        }
    }

    private void addContent(JFrame frame) {
        frame.setLayout(new GridBagLayout());

        var content = new JPanel();
        content.setLayout(new GridLayout(0, 1, 10, 0));

        var text = new JLabel("""
                <html>
                  <h1>Sync Conflict</h1>
                  mc-option-sync found different versions of files to sync.<br>
                  Would you like to overwrite the options of the current Minecraft instance?
                </html>""");

        text.setHorizontalAlignment(SwingConstants.CENTER);
        text.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        content.add(text);

        var btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        var overwriteBtn = new JButton("Overwrite");
        var keepBtn = new JButton("Keep");

        overwriteBtn.setBackground(new Color(0x994343));
        overwriteBtn.setOpaque(true);
        overwriteBtn.setBorderPainted(false);

        btnPanel.add(keepBtn);
        btnPanel.add(overwriteBtn);
        content.add(btnPanel);

        frame.add(content);

        keepBtn.addActionListener(e -> {
            shouldOverwrite = false;
            frame.dispose();
        });

        overwriteBtn.addActionListener(e -> {
            shouldOverwrite = true;
            frame.dispose();
        });
    }
}
