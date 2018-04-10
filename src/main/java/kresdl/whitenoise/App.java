package kresdl.whitenoise;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import kresdl.utilities.Mouse;

@SuppressWarnings("serial")
public class App extends JFrame {

    public static final int SCROLLWIDTH = 1200,
            SCROLLHEIGHT = 800,
            NOISE_PRE = 129,
            OUT_PRE = 256,
            BEZIERCURVEPOINTS = 16,
            PARALLELISM = 4;

    public static final Color BGCOLOR = new Color(0.7f, 0.7f, 0.6f);

    public static JScrollPane createScrollPane(Main main) {
        JScrollPane scroll = new JScrollPane(main);
        scroll.setPreferredSize(new Dimension(SCROLLWIDTH, SCROLLHEIGHT));

        scroll.getHorizontalScrollBar().addAdjustmentListener(e -> {
            main.updateSize();
        });

        scroll.getVerticalScrollBar().addAdjustmentListener(e -> {
            main.updateSize();
        });

        Mouse mouse = main.getMouse();
        mouse.onDrag(0, e -> {
            JScrollBar h = scroll.getHorizontalScrollBar();
            JScrollBar v = scroll.getVerticalScrollBar();
            Point pt = mouse.getMovement();
            int x = h.getValue() - pt.x;
            int y = v.getValue() - pt.y;
            h.setValue(x);
            v.setValue(y);
            main.updateSize();
        });
        return scroll;
    }

    public static JMenuBar createMenuBar(Main main) {
        JMenuBar bar = new JMenuBar();
        JMenuItem load = new JMenuItem(main.new LoadTree());
        JMenuItem clear = new JMenuItem(main.new Clear());
        JMenuItem about = new JMenuItem(main.new About());
        load.setAccelerator(KeyStroke.getKeyStroke("control R"));
        clear.setAccelerator(KeyStroke.getKeyStroke("control D"));
        JMenu n = new JMenu("File");
        n.add(load);
        n.add(clear);
        n.add(about);
        bar.add(n);
        return bar;
    }

    App() {
        super();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                App app = new App();
                Main main = new Main();
                app.setJMenuBar(createMenuBar(main));
                app.add(createScrollPane(main));
                app.setTitle("White Noise");
                app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                app.pack();
                app.setLocationRelativeTo(null);
                app.setVisible(true);
            } catch (RuntimeException e) {
                e.printStackTrace();
                System.exit(1);
            }
        });
    }
}
