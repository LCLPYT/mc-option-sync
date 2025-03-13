package work.lclpnet.opt_sync.gui;

import org.slf4j.Logger;
import work.lclpnet.opt_sync.lib.ConflictHandler;
import work.lclpnet.opt_sync.lib.Constants;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

public class ConflictsGui implements ConflictHandler {

    private final Translator translator;
    private final Logger logger;
    private boolean shouldOverwrite = false;

    public ConflictsGui(Translator translator, Logger logger) {
        this.translator = translator;
        this.logger = logger;
    }

    @Override
    public boolean shouldOverwrite() {
        try {
            return awaitResponse();
        } catch (Exception e) {
            logger.error("Failed to get user response via the gui (selecting keep)", e);
            return false;
        }
    }

    public boolean awaitResponse() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }

        String title = translator.translate("mc-option-sync.conflict.detected") + " - " + Constants.MOD_ID;

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
                  <h1>%s</h1>
                  %s
                </html>""".formatted(
                translator.translate("mc-option-sync.conflict"),
                translator.translate("mc-option-sync.conflict.detail", Constants.MOD_ID).replaceAll("\n", "<br>")
        ));

        text.setHorizontalAlignment(SwingConstants.CENTER);
        text.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        content.add(text);

        var btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        var overwriteBtn = new JButton(translator.translate("mc-option-sync.conflict.overwrite"));
        var keepBtn = new JButton(translator.translate("mc-option-sync.conflict.keep"));

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

    public interface Translator {
        String translate(String key, Object... args);
    }
}
