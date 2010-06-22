package org.nbgit.ui.status;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.nbgit.Git;
import org.nbgit.GitModuleConfig;
import org.nbgit.GitProgressSupport;
import org.nbgit.StatusInfo;
import org.nbgit.StatusCache;
import org.nbgit.task.StatusTask;
import org.nbgit.ui.GitFileNode;
import org.nbgit.ui.commit.CommitAction;
import org.nbgit.ui.diff.DiffAction;
import org.nbgit.ui.diff.Setup;
import org.nbgit.ui.update.UpdateAction;
import org.nbgit.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.netbeans.modules.versioning.util.NoContentPanel;
import org.openide.LifecycleManager;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

/**
 * The main class of the Synchronize view, shows and acts on set of file roots.
 *
 * @author Maros Sandor
 */
class VersioningPanel extends JPanel implements ExplorerManager.Provider, PreferenceChangeListener, PropertyChangeListener, ActionListener {

    private ExplorerManager explorerManager;
    private final GitVersioningTopComponent parentTopComponent;
    private final Git git;
    private VCSContext context;
    private int displayStatuses;
    private String branchInfo;
    private SyncTable syncTable;
    private RequestProcessor.Task refreshViewTask;
    private Thread refreshViewThread;
    private GitProgressSupport gitProgressSupport;
    private static final RequestProcessor rp = new RequestProcessor("GitView", 1, true);  // NOI18N
    private final NoContentPanel noContentComponent = new NoContentPanel();

    /**
     * Creates a new Synchronize Panel managed by the given versioning system.
     *
     * @param parent enclosing top component
     */
    public VersioningPanel(GitVersioningTopComponent parent) {
        this.parentTopComponent = parent;
        this.git = Git.getInstance();
        refreshViewTask = rp.create(new RefreshViewTask());
        explorerManager = new ExplorerManager();
        displayStatuses = StatusInfo.STATUS_LOCAL_CHANGE;
        noContentComponent.setLabel(NbBundle.getMessage(VersioningPanel.class, "MSG_No_Changes_All")); // NOI18N
        syncTable = new SyncTable();

        initComponents();
        setVersioningComponent(syncTable.getComponent());
        reScheduleRefresh(0);

        // XXX click it in form editor, probbaly requires  Mattisse >=v2
        jPanel2.setFloatable(false);
        jPanel2.putClientProperty("JToolBar.isRollover", Boolean.TRUE);  // NOI18N
        jPanel2.setLayout(new ToolbarLayout());
        setButtonEnabled(false);
    }

    private void setButtonEnabled(boolean enable) {
        btnCommit.setEnabled(enable);
        btnDiff.setEnabled(enable);
        btnRefresh.setEnabled(enable);
        btnUpdate.setEnabled(enable);
    }

    public void preferenceChange(PreferenceChangeEvent evt) {
        if (evt.getKey().startsWith(GitModuleConfig.PROP_COMMIT_EXCLUSIONS)) {
            repaint();
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (StatusCache.PROP_FILE_STATUS_CHANGED.equals(evt.getPropertyName())) {
            StatusCache.ChangedEvent changedEvent = (StatusCache.ChangedEvent) evt.getNewValue();
            Git.LOG.log(Level.FINE, "Status.propertyChange(): {0} file:  {1}", new Object[]{parentTopComponent.getContentTitle(), changedEvent.getFile()}); // NOI18N
            if (affectsView(evt)) {
                reScheduleRefresh(1000);
            }
            return;
        }
        if (Git.PROP_CHANGESET_CHANGED.equals(evt.getPropertyName())) {
            Object source = evt.getOldValue();
            File root = GitUtils.getRootFile(context);
            Git.LOG.log(Level.FINE, "Git.changesetChanged: source {0} repo {1} ", new Object[]{source, root}); // NOI18N
            if (root != null && root.equals(source)) {
                reScheduleRefresh(1000);
            }
            return;
        }
        if (ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
            TopComponent tc = (TopComponent) SwingUtilities.getAncestorOfClass(TopComponent.class, this);
            if (tc != null) {
                tc.setActivatedNodes((Node[]) evt.getNewValue());
            }
            return;
        }
    }

    /**
     * Sets roots (directories) to display in the view.
     *
     * @param ctx new context if the Versioning panel
     */
    void setContext(VCSContext ctx) {
        if (ctx == null) {
            setButtonEnabled(false);
            return;
        }
        setButtonEnabled(true);
        context = ctx;
        reScheduleRefresh(0);
    }

    public ExplorerManager getExplorerManager() {
        return explorerManager;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        GitModuleConfig.getDefault().getPreferences().addPreferenceChangeListener(this);
        git.getStatusCache().addPropertyChangeListener(this);
        git.addPropertyChangeListener(this);
        explorerManager.addPropertyChangeListener(this);
        reScheduleRefresh(0);   // the view does not listen for changes when it is not visible
    }

    @Override
    public void removeNotify() {
        GitModuleConfig.getDefault().getPreferences().removePreferenceChangeListener(this);
        git.getStatusCache().removePropertyChangeListener(this);
        git.removePropertyChangeListener(this);
        explorerManager.removePropertyChangeListener(this);
        super.removeNotify();
    }

    private void setVersioningComponent(JComponent component) {
        Component[] children = getComponents();
        for (int i = 0; i < children.length; i++) {
            Component child = children[i];
            if (child != jPanel2) {
                if (child == component) {
                    return;
                } else {
                    remove(child);
                    break;
                }
            }
        }
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;

        add(component, gbc);
        revalidate();
        repaint();
    }

    /**
     * Must NOT be run from AWT.
     */
    private void setupModels() {
        if (context == null) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    syncTable.setTableModel(new SyncFileNode[0]);
                    File root = GitUtils.getRootFile(GitUtils.getCurrentContext(null));
                /* #126311: Optimize UI for Large repos
                if (root != null) {
                String[] info = getRepositoryBranchInfo(root);
                String rev = info != null ? info[1] : null;
                String changeset = info != null ? info[2] : null;
                setRepositoryBranchInfo(rev, changeset);
                }*/
                }
            });
            return;
        }
        // XXX attach Cancelable hook
        final ProgressHandle ph = ProgressHandleFactory.createHandle(NbBundle.getMessage(VersioningPanel.class, "MSG_Refreshing_Versioning_View")); // NOI18N
        try {
            refreshViewThread = Thread.currentThread();
            Thread.interrupted();  // clear interupted status
            ph.start();
            final SyncFileNode[] nodes = getNodes(context, displayStatuses);  // takes long

            if (nodes == null) {
                return;
            }
            final String[] tableColumns;
            final String branchTitle;
            File[] files = context.getRootFiles().toArray(new File[context.getRootFiles().size()]);
            if (files == null || files.length == 0) {
                return;

            /* #126311: begin Optimize UI for Large repos */
            }
            File root = git.getTopmostManagedParent(files[0]);
            String[] info = getRepositoryBranchInfo(root);
            if (info != null) {
                branchTitle = NbBundle.getMessage(VersioningPanel.class, "CTL_VersioningView_BranchTitle", info[0]);
            } else {
                branchTitle = NbBundle.getMessage(VersioningPanel.class, "CTL_VersioningView_UnnamedBranchTitle");
            /* #126311: end */
            }
            if (nodes.length > 0) {
                boolean stickyCommon = false;
                for (int i = 1; i < nodes.length; i++) {
                    if (Thread.interrupted()) // TODO set model that displays that fact to user
                    {
                        return;
                    }
                }
                tableColumns = new String[]{SyncFileNode.COLUMN_NAME_NAME, SyncFileNode.COLUMN_NAME_STATUS, SyncFileNode.COLUMN_NAME_PATH};
            } else {
                tableColumns = null;
            /* #126311: Optimize UI for Large repos */
            }
            setRepositoryBranchInfo(info != null ? info[1] : null);
            /* end */
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    /* #126311: Optimize UI for Large repos */
                    parentTopComponent.setBranchTitle(branchTitle);
                    /* end */
                    if (nodes.length > 0) {
                        syncTable.setColumns(tableColumns);
                        setVersioningComponent(syncTable.getComponent());
                    } else {
                        setVersioningComponent(noContentComponent);
                    }
                    syncTable.setTableModel(nodes);
                // finally section, it's enqueued after this request
                }
            });
        } finally {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    ph.finish();
                }
            });
        }
    }

    private void setRepositoryBranchInfo(String rev) {
        String info;
        if (rev != null) {
            info = NbBundle.getMessage(VersioningPanel.class,
                    "CTL_VersioningView_BranchInfo", // NOI18N
                    rev);
        } else {
            info = NbBundle.getMessage(VersioningPanel.class,
                    "CTL_VersioningView_BranchInfoNotCommitted");
        }
        String title = NbBundle.getMessage(VersioningPanel.class, "CTL_VersioningView_StatusTitle", info); // NOI18N
        if (!title.equals(statusLabel.getText())) {
            statusLabel.setText(title);
        }
    }

    private String[] getRepositoryBranchInfo(File root) {
        Repository repo = git.getRepository(root);

        if (repo == null) {
            return null;
        }
        try {
            String branch = repo.getBranch();
            String head = branch != null ? repo.getFullBranch() : Constants.HEAD;
            ObjectId id = repo.resolve(head);
            if (branch == null) {
                branch = Constants.HEAD;
            }
            return new String[] {
                branch,
                id != null ? id.name() : null
            };
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return null;
    }

    private SyncFileNode[] getNodes(VCSContext context, int status) {
        File[] files = git.getStatusCache().listFiles(context, status);
        SyncFileNode[] nodes = new SyncFileNode[files.length];
        int i = 0;

        for (File file : files) {
            if (Thread.interrupted()) {
                return null;
            }
            nodes[i++] = new SyncFileNode(new GitFileNode(file), this);
        }

        return nodes;
    }

    public int getDisplayStatuses() {
        return displayStatuses;
    }

    public String getDisplayBranchInfo() {
        return branchInfo;
    }

    /**
     * Performs the "cvs commit" command on all diplayed roots plus "cvs add" for files that are not yet added. // NOI18N
     */
    private void onCommitAction() {
        //TODO: Status Commit Action
        LifecycleManager.getDefault().saveAll();
        CommitAction.commit(parentTopComponent.getContentTitle(), context);
    }

    /**
     * Performs the "cvs update" command on all diplayed roots. // NOI18N
     */
    private void onUpdateAction() {
        UpdateAction.update(context);
        parentTopComponent.contentRefreshed();
    }

    /**
     * Refreshes statuses of all files in the view. It does
     * that by issuing the "git status -marduiC" command, updating the cache
     * and refreshing file nodes.
     */
    private void onRefreshAction() {
        LifecycleManager.getDefault().saveAll();
        if (context == null || context.getRootFiles().size() == 0) {
            return;
        }
        refreshStatuses();
    }

    /**
     * Programmatically invokes the Refresh action.
     * Connects to repository and gets recent status.
     */
    void performRefreshAction() {
        refreshStatuses();
    }

    /* Async Connects to repository and gets recent status. */
    private void refreshStatuses() {
        if (gitProgressSupport != null) {
            gitProgressSupport.cancel();
            gitProgressSupport = null;
        }

        final String repository = GitUtils.getRootPath(context);
        if (repository == null) {
            return;
        }
        RequestProcessor rp = Git.getInstance().getRequestProcessor(repository);
        gitProgressSupport = new StatusTask(context) {

            @Override
            protected void performAfter() {
                setupModels();
            }
        };

        parentTopComponent.contentRefreshed();
        gitProgressSupport.start(rp, repository, org.openide.util.NbBundle.getMessage(VersioningPanel.class, "LBL_Refresh_Progress")); // NOI18N

    }

    /**
     * Shows Diff panel for all files in the view. The initial type of diff depends on the sync mode: Local, Remote, All.
     * In Local mode, the diff shows CURRENT <-> BASE differences. In Remote mode, it shows BASE<->HEAD differences.
     */
    private void onDiffAction() {
        String title = parentTopComponent.getContentTitle();
        if (displayStatuses == StatusInfo.STATUS_LOCAL_CHANGE) {
            LifecycleManager.getDefault().saveAll();
            DiffAction.diff(context, Setup.DIFFTYPE_LOCAL, title);
        } else if (displayStatuses == StatusInfo.STATUS_REMOTE_CHANGE) {
            DiffAction.diff(context, Setup.DIFFTYPE_REMOTE, title);
        } else {
            LifecycleManager.getDefault().saveAll();
            DiffAction.diff(context, Setup.DIFFTYPE_ALL, title);
        }
    }

    private void onDisplayedStatusChanged() {
        setDisplayStatuses(StatusInfo.STATUS_REMOTE_CHANGE | StatusInfo.STATUS_LOCAL_CHANGE);
        noContentComponent.setLabel(NbBundle.getMessage(VersioningPanel.class, "MSG_No_Changes_All")); // NOI18N
    }

    private void setDisplayStatuses(int displayStatuses) {
        this.displayStatuses = displayStatuses;
        reScheduleRefresh(0);
    }

    private boolean affectsView(PropertyChangeEvent event) {
        StatusCache.ChangedEvent changedEvent = (StatusCache.ChangedEvent) event.getNewValue();
        File file = changedEvent.getFile();
        StatusInfo oldInfo = changedEvent.getOldInfo();
        StatusInfo newInfo = changedEvent.getNewInfo();
        if (oldInfo == null) {
            if ((newInfo.getStatus() & displayStatuses) == 0) {
                return false;
            }
        } else if ((oldInfo.getStatus() & displayStatuses) + (newInfo.getStatus() & displayStatuses) == 0) {
            return false;
        }
        return context == null ? false : context.contains(file);
    }

    /** Reloads data from cache */
    private void reScheduleRefresh(int delayMillis) {
        refreshViewTask.schedule(delayMillis);
    }
    // HACK copy&paste HACK, replace by save/restore of column width/position
    void deserialize() {
        if (syncTable != null) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    syncTable.setDefaultColumnSizes();
                }
            });
        }
    }

    void focus() {
        syncTable.focus();
    }

    /**
     * Cancels both:
     * <ul>
     * <li>cache data fetching
     * <li>background cvs -N update
     * </ul>
     */
    public void cancelRefresh() {
        refreshViewTask.cancel();
    }

    private class RefreshViewTask implements Runnable {

        public void run() {
            setupModels();
        }
    }

    /**
     * Hardcoded toolbar layout. It eliminates need
     * for nested panels their look is hardly maintanable
     * accross several look and feels
     * (e.g. strange layouting panel borders on GTK+).
     *
     * <p>It sets authoritatively component height and takes
     * "prefered" width from components itself. // NOI18N
     *
     */
    private class ToolbarLayout implements LayoutManager {

        /** Expected border height */
        private int TOOLBAR_HEIGHT_ADJUSTMENT = 4;
        private int TOOLBAR_SEPARATOR_MIN_WIDTH = 12;
        /** Cached toolbar height */
        private int toolbarHeight = -1;
        /** Guard for above cache. */
        private Dimension parentSize;
        private Set<JComponent> adjusted = new HashSet<JComponent>();

        public void removeLayoutComponent(Component comp) {
        }

        public void layoutContainer(Container parent) {
            Dimension dim = VersioningPanel.this.getSize();
            Dimension max = parent.getSize();

            int reminder = max.width - minimumLayoutSize(parent).width;

            int components = parent.getComponentCount();
            int horizont = 0;
            for (int i = 0; i < components; i++) {
                JComponent comp = (JComponent) parent.getComponent(i);
                if (comp.isVisible() == false) {
                    continue;
                }
                comp.setLocation(horizont, 0);
                Dimension pref = comp.getPreferredSize();
                int width = pref.width;
                if (comp instanceof JSeparator && ((dim.height - dim.width) <= 0)) {
                    width = Math.max(width, TOOLBAR_SEPARATOR_MIN_WIDTH);
                }
                if (comp instanceof JProgressBar && reminder > 0) {
                    width += reminder;
//                if (comp == getMiniStatus()) {
//                    width = reminder;
//                }

                // in column layout use taller toolbar
                }
                int height = getToolbarHeight(dim) - 1;
                comp.setSize(width, height);  // 1 verySoftBevel compensation
                horizont += width;
            }
        }

        public void addLayoutComponent(String name, Component comp) {
        }

        public Dimension minimumLayoutSize(Container parent) {

            // in column layout use taller toolbar
            Dimension dim = VersioningPanel.this.getSize();
            int height = getToolbarHeight(dim);

            int components = parent.getComponentCount();
            int horizont = 0;
            for (int i = 0; i < components; i++) {
                Component comp = parent.getComponent(i);
                if (comp.isVisible() == false) {
                    continue;
                }
                if (comp instanceof AbstractButton) {
                    adjustToobarButton((AbstractButton) comp);
                } else {
                    adjustToolbarComponentSize((JComponent) comp);
                }
                Dimension pref = comp.getPreferredSize();
                int width = pref.width;
                if (comp instanceof JSeparator && ((dim.height - dim.width) <= 0)) {
                    width = Math.max(width, TOOLBAR_SEPARATOR_MIN_WIDTH);
                }
                horizont += width;
            }

            return new Dimension(horizont, height);
        }

        public Dimension preferredLayoutSize(Container parent) {
            // Eliminates double height toolbar problem
            Dimension dim = VersioningPanel.this.getSize();
            int height = getToolbarHeight(dim);

            return new Dimension(Integer.MAX_VALUE, height);
        }

        /**
         * Computes vertical toolbar components height that can used for layout manager hinting.
         * @return size based on font size and expected border.
         */
        private int getToolbarHeight(Dimension parent) {

            if (parentSize == null || (parentSize.equals(parent) == false)) {
                parentSize = parent;
                toolbarHeight = -1;
            }

            if (toolbarHeight == -1) {
                BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_GRAY);
                Graphics2D g = image.createGraphics();
                UIDefaults def = UIManager.getLookAndFeelDefaults();

                int height = 0;
                String[] fonts = {"Label.font", "Button.font", "ToggleButton.font"};      // NOI18N
                for (int i = 0; i < fonts.length; i++) {
                    Font f = def.getFont(fonts[i]);
                    FontMetrics fm = g.getFontMetrics(f);
                    height = Math.max(height, fm.getHeight());
                }
                toolbarHeight = height + TOOLBAR_HEIGHT_ADJUSTMENT;
                if ((parent.height - parent.width) > 0) {
                    toolbarHeight += TOOLBAR_HEIGHT_ADJUSTMENT;
                }
            }

            return toolbarHeight;
        }

        /** Toolbar controls must be smaller and should be transparent*/
        private void adjustToobarButton(final AbstractButton button) {

            if (adjusted.contains(button)) {
                return;            // workaround for Ocean L&F clutter - toolbars use gradient.
            // To make the gradient visible under buttons the content area must not
            // be filled. To support rollover it must be temporarily filled
            }
            if (button instanceof JToggleButton == false) {
                button.setContentAreaFilled(false);
                button.setMargin(new Insets(0, 3, 0, 3));
                button.setBorderPainted(false);
                button.addMouseListener(new MouseAdapter() {

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        button.setContentAreaFilled(true);
                        button.setBorderPainted(true);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        button.setContentAreaFilled(false);
                        button.setBorderPainted(false);
                    }
                });
            }

            adjustToolbarComponentSize(button);
        }

        private void adjustToolbarComponentSize(JComponent button) {

            if (adjusted.contains(button)) {
                return;            // as we cannot get the button small enough using the margin and border...
            }
            if (button.getBorder() instanceof CompoundBorder) { // from BasicLookAndFeel
                Dimension pref = button.getPreferredSize();

                // XXX #41827 workaround w2k, that adds eclipsis (...) instead of actual text
                if ("Windows".equals(UIManager.getLookAndFeel().getID())) // NOI18N
                {
                    pref.width += 9;
                }
                button.setPreferredSize(pref);
            }

            adjusted.add(button);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jComboBox1 = new javax.swing.JComboBox();
        jPanel2 = new javax.swing.JToolBar();
        jPanel4 = new javax.swing.JPanel();
        statusLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        btnRefresh = new javax.swing.JButton();
        btnDiff = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        btnUpdate = new javax.swing.JButton();
        btnCommit = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        setLayout(new java.awt.GridBagLayout());

        jPanel2.setBorderPainted(false);

        jPanel4.setOpaque(false);
        jPanel2.add(jPanel4);

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/nbgit/ui/status/Bundle"); // NOI18N
        statusLabel.setText(bundle.getString("CTL_Versioning_Status_Table_Title")); // NOI18N
        statusLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        statusLabel.setMaximumSize(new java.awt.Dimension(120, 17));
        statusLabel.setMinimumSize(new java.awt.Dimension(120, 17));
        jPanel2.add(statusLabel);
        statusLabel.getAccessibleContext().setAccessibleName(bundle.getString("CTL_Versioning_Status_Table_Title")); // NOI18N

        jPanel1.setOpaque(false);
        jPanel1.add(jSeparator1);

        jPanel2.add(jPanel1);

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jPanel2.add(jSeparator2);

        btnRefresh.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/nbgit/resources/icons/refresh.png"))); // NOI18N
        btnRefresh.setToolTipText(org.openide.util.NbBundle.getMessage(VersioningPanel.class, "CTL_Synchronize_Action_Refresh_Tooltip")); // NOI18N
        btnRefresh.setMaximumSize(new java.awt.Dimension(28, 28));
        btnRefresh.setMinimumSize(new java.awt.Dimension(28, 28));
        btnRefresh.setPreferredSize(new java.awt.Dimension(22, 25));
        btnRefresh.addActionListener(this);
        jPanel2.add(btnRefresh);
        btnRefresh.getAccessibleContext().setAccessibleName("Refresh Status");

        btnDiff.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/nbgit/resources/icons/diff.png"))); // NOI18N
        btnDiff.setToolTipText(bundle.getString("CTL_Synchronize_Action_Diff_Tooltip")); // NOI18N
        btnDiff.setFocusable(false);
        btnDiff.setPreferredSize(new java.awt.Dimension(22, 25));
        btnDiff.addActionListener(this);
        jPanel2.add(btnDiff);
        btnDiff.getAccessibleContext().setAccessibleName("Diff All");

        jPanel3.setOpaque(false);
        jPanel2.add(jPanel3);

        btnUpdate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/nbgit/resources/icons/update.png"))); // NOI18N
        btnUpdate.setToolTipText(bundle.getString("CTL_Synchronize_Action_Update_Tooltip")); // NOI18N
        btnUpdate.setFocusable(false);
        btnUpdate.setPreferredSize(new java.awt.Dimension(22, 25));
        btnUpdate.addActionListener(this);
        jPanel2.add(btnUpdate);
        btnUpdate.getAccessibleContext().setAccessibleName("Update");

        btnCommit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/nbgit/resources/icons/commit.png"))); // NOI18N
        btnCommit.setToolTipText(bundle.getString("CTL_CommitForm_Action_Commit_Tooltip")); // NOI18N
        btnCommit.setFocusable(false);
        btnCommit.setPreferredSize(new java.awt.Dimension(22, 25));
        btnCommit.addActionListener(this);
        jPanel2.add(btnCommit);
        btnCommit.getAccessibleContext().setAccessibleName("Commit");

        jPanel5.setOpaque(false);
        jPanel2.add(jPanel5);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 0);
        add(jPanel2, gridBagConstraints);
    }

    // Code for dispatching events from components to event handlers.

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        if (evt.getSource() == btnRefresh) {
            VersioningPanel.this.btnRefreshActionPerformed(evt);
        }
        else if (evt.getSource() == btnDiff) {
            VersioningPanel.this.btnDiffActionPerformed(evt);
        }
        else if (evt.getSource() == btnUpdate) {
            VersioningPanel.this.btnUpdateActionPerformed(evt);
        }
        else if (evt.getSource() == btnCommit) {
            VersioningPanel.this.btnCommitActionPerformed(evt);
        }
    }// </editor-fold>//GEN-END:initComponents

private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
    onRefreshAction();
}//GEN-LAST:event_btnRefreshActionPerformed

    private void btnDiffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDiffActionPerformed
        onDiffAction();
    }//GEN-LAST:event_btnDiffActionPerformed

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        onUpdateAction();
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnCommitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCommitActionPerformed
        onCommitAction();
    }//GEN-LAST:event_btnCommitActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCommit;
    private javax.swing.JButton btnDiff;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JToolBar jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JLabel statusLabel;
    // End of variables declaration//GEN-END:variables
}
