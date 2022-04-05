package com.tombrus.githubPackageDeleter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

public class GithubPackageDeleter {
    public static final  GithubPackageDeleter INSTANCE             = new GithubPackageDeleter();
    private static final String               STORE_TOKEN          = "storeToken";
    private static final String               GITHUB_TOKEN         = "githubToken";
    private static final String               USER_OR_ORGANIZATION = "userOrOrganization";

    public static void main(String[] args) {
    }

    //==========================================================================
    private JPanel      mainPanel;
    private JTabbedPane tabPanel;
    private JTextField  githubTokenField;
    private JCheckBox   storeCheckBox;
    private JTextField  userOrOrganizationField;
    public  JLabel      settingsInfoLabel;
    private JTree       packageTree;
    public  JLabel      rateLimitLabel;
    private JButton     refreshButton;
    private JButton     expandAllButton;
    private JButton     collapseAllButton;
    private JButton     selectObsoleteButton;

    //==========================================================================
    private final JFrame           frame;
    private final DefaultTreeModel packageTreeModel;
    private final Preferences      prefs = Preferences.userRoot().node(GithubPackageDeleter.class.getSimpleName());
    private       boolean          settingsHaveChanged;

    private GithubPackageDeleter() {
        packageTreeModel = new DefaultTreeModel(null);
        packageTree      = new JTree(packageTreeModel);

        $$$setupUI$$$();

        boolean storeToken         = prefs.getBoolean(STORE_TOKEN, false);
        String  githubToken        = U.decrypt(prefs.get(GITHUB_TOKEN, U.encrypt("")));
        String  userOrOrganization = prefs.get(USER_OR_ORGANIZATION, "");

        TokenStore.setToken(githubToken);
        storeCheckBox.setSelected(storeToken);
        githubTokenField.setText(githubToken);
        userOrOrganizationField.setText(userOrOrganization);

        frame = new JFrame("GitHub Package Deleter");
        frame.setMinimumSize(new Dimension(300, 300));
        frame.setContentPane(mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addComponentListener(new ComponentAdapter() {
            private final TimedUpdater updater = new TimedUpdater(() -> saveBoundsToPrefs(frame));

            @Override
            public void componentResized(ComponentEvent e) {
                updater.update();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                updater.update();
            }
        });
        setBoundsFromPrefs(frame);
        moveOnAnyScreen(frame);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> saveBoundsToPrefs(frame)));

        DocumentListener settingsListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                settingsChanged();
            }

            public void removeUpdate(DocumentEvent e) {
                settingsChanged();
            }

            public void insertUpdate(DocumentEvent e) {
                settingsChanged();
            }
        };
        githubTokenField.getDocument().addDocumentListener(settingsListener);
        userOrOrganizationField.getDocument().addDocumentListener(settingsListener);
        storeCheckBox.addActionListener(e -> settingsChanged());
        packageTree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent e) {
                TreePath path = e.getPath();
                if (!packageTree.hasBeenExpanded(path)) {
                    ((Details) path.getLastPathComponent()).firstExpansion();
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent e) {
            }
        });
        packageTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clickInTree(e);
            }
        });
        refreshButton.addActionListener(e -> refreshPackageTree());
        expandAllButton.addActionListener(e -> expandAll());
        collapseAllButton.addActionListener(e -> collapseAll());
        selectObsoleteButton.addActionListener(e -> selectObsolete());

        SwingUtilities.invokeLater(() -> {
            frame.setVisible(true);
            refreshPackageTree();
        });
    }

    private void clickInTree(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            TreePath path = packageTree.getClosestPathForLocation(e.getX(), e.getY());
            if (!packageTree.isPathSelected(path)) {
                packageTree.setSelectionPath(path);
            }
            Set<VersionDetails> versionsToDelete = selectedVersions();
            if (!versionsToDelete.isEmpty()) {
                JPopupMenu popup = new JPopupMenu();
                Action a = new AbstractAction("Delete " + versionsToDelete.size() + " versions") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String msg = "Are you sure you want to delete ";
                        msg += versionsToDelete.size() == 1 ? "this package" : "all " + versionsToDelete.size() + " packages";
                        msg += "?\nThis can not be undone!";
                        if (JOptionPane.showConfirmDialog(frame, msg) == JOptionPane.OK_OPTION) {
                            new VersionDeleter(selectedVersions()).delete();
                        }
                    }
                };
                popup.add(a);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private Set<VersionDetails> selectedVersions() {
        TreePath[] sel = packageTree.getSelectionPaths();
        return sel == null ? Set.of() : Arrays.stream(sel)
                .map(TreePath::getLastPathComponent)
                .flatMap(n -> n instanceof PackageDetails ? ((PackageDetails) n).versionStream() : n instanceof VersionDetails ? Stream.of(n) : null)
                .map(v -> (VersionDetails) v)
                .collect(Collectors.toSet());
    }

    public DefaultTreeModel getPackageTreeModel() {
        return packageTreeModel;
    }

    private void settingsChanged() {
        settingsHaveChanged = true;
        String  githubToken        = githubTokenField.getText();
        String  userOrOrganization = userOrOrganizationField.getText();
        boolean storeToken         = storeCheckBox.isSelected();

        TokenStore.setToken(githubToken);
        if (storeToken && !prefs.getBoolean(STORE_TOKEN, false)) {
            JOptionPane.showMessageDialog(frame, "Be aware that this is insecure. Your token will be stored on this computer unencrypted.");
        }

        prefs.put(GITHUB_TOKEN, U.encrypt(storeToken ? githubToken : ""));
        prefs.put(USER_OR_ORGANIZATION, userOrOrganization);
        prefs.putBoolean(STORE_TOKEN, storeToken);

        refreshPackageTree();
    }

    private void refreshPackageTree() {
        if (userOrOrganizationField.getText().isBlank()) {
            settingsInfoLabel.setText("enter an organisation to get the owned packages");
        } else {
            UserOrOrganizationDetails root = new UserOrOrganizationDetails(userOrOrganizationField.getText());
            packageTreeModel.setRoot(root);
            if (githubTokenField.getText().isBlank()) {
                settingsInfoLabel.setText("enter a valid token to get the packages");
            } else {
                settingsInfoLabel.setText("connecting...");
                root.startDownload();
            }
        }
    }

    public void expandAll() {
        getPackageDetailsStream().forEach(c -> packageTree.expandPath(new TreePath(c.getPath())));
    }

    public void collapseAll() {
        getPackageDetailsStream().forEach(c -> packageTree.collapsePath(new TreePath(c.getPath())));
    }

    public void selectObsolete() {
        TreePath[] selectionPaths = getPackageDetailsStream()
                .filter(p -> 1 < p.getChildCount() && p.childrenStream().allMatch(cc -> (cc instanceof VersionDetails) && ((VersionDetails) cc).name.endsWith("-SNAPSHOT")))
                .flatMap(p -> p.versionStream()
                        .collect(Collectors.groupingBy(v -> v.name.replaceAll("-[0-9]*_[0-9]*-SNAPSHOT", "")))
                        .values()
                        .stream()
                        .flatMap(l -> {
                            VersionDetails max = l.stream().max(Comparator.comparing(v -> v.name)).orElseThrow();
                            return l.stream().filter(e -> e != max);
                        }))
                .map(c -> new TreePath(c.getPath()))
                .toList()
                .toArray(new TreePath[0]);
        packageTree.setSelectionPaths(selectionPaths);
    }

    private Stream<PackageDetails> getPackageDetailsStream() {
        Object rawRoot = packageTreeModel.getRoot();
        if (rawRoot instanceof UserOrOrganizationDetails root) {
            return Collections.list(root.children())
                    .stream()
                    .filter(c -> c instanceof PackageDetails)
                    .map(c -> (PackageDetails) c);
        } else {
            return Stream.of();
        }
    }

    public void showDeleterPaneIfNoSettingsChange() {
        if (!settingsHaveChanged) {
            tabPanel.setSelectedIndex(1);
        }
    }

    private void saveBoundsToPrefs(JFrame frame) {
        Rectangle bounds = frame.getBounds();
        prefs.putInt("x", bounds.x);
        prefs.putInt("y", bounds.y);
        prefs.putInt("w", bounds.width);
        prefs.putInt("h", bounds.height);
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    private void setBoundsFromPrefs(JFrame frame) {
        if (prefs.get("exists", null) == null) {
            prefs.put("exists", "exists");
            frame.pack();
            moveToCenter(frame);
        } else {
            int x = prefs.getInt("x", 0);
            int y = prefs.getInt("y", 0);
            int w = prefs.getInt("w", 1000);
            int h = prefs.getInt("h", 700);
            frame.setBounds(x, y, w, h);
        }
    }

    private static void moveOnAnyScreen(JFrame frame) {
        Rectangle frameBounds = frame.getBounds();
        for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            Rectangle screenBounds = gd.getDefaultConfiguration().getBounds();
            Rectangle sect         = screenBounds.intersection(frameBounds);
            if (sect.equals(frameBounds)) {
                return;
            }
        }
        moveToCenter(frame);
    }

    private static void moveToCenter(JFrame frame) {
        Rectangle frameBounds = frame.getBounds();
        for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            Rectangle screenBounds = gd.getDefaultConfiguration().getBounds();
            Rectangle sect         = screenBounds.intersection(frameBounds);
            if (!sect.isEmpty()) {
                moveToCenter(frame, screenBounds);
                return;
            }
        }
        moveToCenter(frame, GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0].getDefaultConfiguration().getBounds());
    }

    private static void moveToCenter(JFrame frame, Rectangle screenBounds) {
        Rectangle frameBounds = frame.getBounds();
        frameBounds.width  = Math.min(frameBounds.width, screenBounds.width);
        frameBounds.height = Math.min(frameBounds.height, screenBounds.height);
        frameBounds.x      = (screenBounds.width - frameBounds.width) / 2;
        frameBounds.y      = (screenBounds.height - frameBounds.height) / 2;
        frame.setBounds(frameBounds);
    }


    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setMinimumSize(new Dimension(100, 100));
        tabPanel = new JTabbedPane();
        tabPanel.setPreferredSize(new Dimension(1000, 700));
        GridBagConstraints gbc;
        gbc         = new GridBagConstraints();
        gbc.gridx   = 0;
        gbc.gridy   = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill    = GridBagConstraints.BOTH;
        gbc.insets  = new Insets(2, 5, 2, 5);
        mainPanel.add(tabPanel, gbc);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        tabPanel.addTab("Settings", panel1);
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("GitHub Token");
        panel1.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        settingsInfoLabel = new JLabel();
        settingsInfoLabel.setText("...");
        panel1.add(settingsInfoLabel, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        githubTokenField = new JPasswordField();
        panel1.add(githubTokenField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        storeCheckBox = new JCheckBox();
        storeCheckBox.setText("store");
        panel1.add(storeCheckBox, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("User or Organization");
        panel1.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        userOrOrganizationField = new JTextField();
        panel1.add(userOrOrganizationField, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabPanel.addTab("Deleter", panel2);
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setHorizontalScrollBarPolicy(32);
        scrollPane1.setVerticalScrollBarPolicy(22);
        panel2.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        packageTree.putClientProperty("JTree.lineStyle", "Angled");
        scrollPane1.setViewportView(packageTree);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        refreshButton = new JButton();
        refreshButton.setText("Refresh");
        panel3.add(refreshButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        expandAllButton = new JButton();
        expandAllButton.setText("Expand All");
        panel3.add(expandAllButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        collapseAllButton = new JButton();
        collapseAllButton.setText("Collapse All");
        panel3.add(collapseAllButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        selectObsoleteButton = new JButton();
        selectObsoleteButton.setText("Select Obsolete");
        panel3.add(selectObsoleteButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rateLimitLabel = new JLabel();
        rateLimitLabel.setHorizontalAlignment(4);
        rateLimitLabel.setMinimumSize(new Dimension(45, 12));
        rateLimitLabel.setText("...");
        rateLimitLabel.setToolTipText("API calls remaining");
        gbc        = new GridBagConstraints();
        gbc.gridx  = 0;
        gbc.gridy  = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(0, 0, 15, 15);
        mainPanel.add(rateLimitLabel, gbc);
        label1.setLabelFor(githubTokenField);
        label2.setLabelFor(userOrOrganizationField);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    private void createUIComponents() {
    }
}
