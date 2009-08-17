/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 * Portions Copyright 2008 Alexander Coles (Ikonoklastik Productions).
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.nbgit.ui.diff;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import org.netbeans.api.diff.DiffController;
import org.netbeans.api.diff.StreamSource;
import org.nbgit.StatusInfo;
import org.nbgit.StatusCache;
import org.nbgit.Git;
import org.nbgit.GitProgressSupport;
import org.nbgit.task.StatusTask;
import org.nbgit.ui.commit.CommitAction;
import org.nbgit.ui.status.StatusAction;
import org.nbgit.ui.update.UpdateAction;
import org.nbgit.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.netbeans.modules.versioning.util.DelegatingUndoRedo;
import org.netbeans.modules.versioning.util.NoContentPanel;
import org.openide.ErrorManager;
import org.openide.LifecycleManager;
import org.openide.awt.UndoRedo;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.Lookups;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author Maros Sandor
 */
class MultiDiffPanel extends javax.swing.JPanel implements ActionListener, DiffSetupSource, PropertyChangeListener {

    /**
     * Array of DIFF setups that we show in the DIFF view. Contents of this array is changed if
     * the user switches DIFF types.
     */
    private Setup[] setups;
    private final DelegatingUndoRedo delegatingUndoRedo = new DelegatingUndoRedo();
    /**
     * Context in which to DIFF.
     */
    private final VCSContext context;
    private int displayStatuses;
    /**
     * Display name of the context of this diff.
     */
    private final String contextName;
    private int currentType;
    private int currentIndex = -1;
    private int currentModelIndex = -1;
    private RequestProcessor.Task prepareTask;
    private DiffPrepareTask dpt;
    private AbstractAction nextAction;
    private AbstractAction prevAction;
    /**
     * null for view that are not
     */
    private RequestProcessor.Task refreshTask;
    private JComponent diffView;
    private DiffFileTable fileTable;
    private boolean dividerSet;
    private GitProgressSupport executeStatusSupport;

    /**
     * Creates diff panel and immediatelly starts loading...
     */
    public MultiDiffPanel(VCSContext context, int initialType, String contextName) {
        this.context = context;
        this.contextName = contextName;
        currentType = initialType;
        initComponents();
        setupComponents();
        refreshSetups();
        refreshComponents();
        refreshTask = org.netbeans.modules.versioning.util.Utils.createTask(new RefreshViewTask());
    }

    /**
     * Construct diff component showing just one file.
     * It hides All, Local, Remote toggles and file chooser combo.
     */
    public MultiDiffPanel(File file, String rev1, String rev2) {
        context = null;
        contextName = file.getName();
        initComponents();
        setupComponents();
        fileTable.getComponent().setVisible(false);
        commitButton.setVisible(false);

        // mimics refreshSetups()
        setups = new Setup[]{
                    new Setup(file, rev1, rev2)
                };
        setDiffIndex(0, 0);
        dpt = new DiffPrepareTask(setups);
        prepareTask = RequestProcessor.getDefault().post(dpt);
    }
    private boolean fileTableSetSelectedIndexContext;

    public void setSelectedIndex(int viewIndex) {
        if (fileTableSetSelectedIndexContext) {
            return;
        }
        setDiffIndex(viewIndex, 0);
    }

    UndoRedo getUndoRedo() {
        return delegatingUndoRedo;
    }

    private void cancelBackgroundTasks() {
        if (prepareTask != null) {
            prepareTask.cancel();
        }
        if (executeStatusSupport != null) {
            executeStatusSupport.cancel();
        }
    }

    /**
     * Called by the enclosing TopComponent to interrupt the fetching task.
     */
    void componentClosed() {
        setups = null;
        cancelBackgroundTasks();
    }

    void requestActive() {
        if (diffView != null) {
            diffView.requestFocusInWindow();
        }
    }

    private void setupComponents() {
        fileTable = new DiffFileTable(this);
        splitPane.setTopComponent(fileTable.getComponent());
        splitPane.setBottomComponent(new NoContentPanel(NbBundle.getMessage(MultiDiffPanel.class, "MSG_DiffPanel_NoContent")));
        commitButton.addActionListener(this);

        commitButton.setToolTipText(NbBundle.getMessage(MultiDiffPanel.class, "MSG_CommitDiff_Tooltip", contextName));
        updateButton.setToolTipText(NbBundle.getMessage(MultiDiffPanel.class, "MSG_UpdateDiff_Tooltip", contextName));
        ButtonGroup grp = new ButtonGroup();

        nextAction = new AbstractAction(null, new javax.swing.ImageIcon(getClass().getResource("/org/nbgit/resources/icons/diff-next.png"))) {  // NOI18N

            {
                putValue(Action.SHORT_DESCRIPTION, java.util.ResourceBundle.getBundle("org/nbgit/ui/diff/Bundle").
                        getString("CTL_DiffPanel_Next_Tooltip"));
            }

            public void actionPerformed(ActionEvent e) {
                onNextButton();
            }
        };
        prevAction = new AbstractAction(null, new javax.swing.ImageIcon(getClass().getResource("/org/nbgit/resources/icons/diff-prev.png"))) { // NOI18N

            {
                putValue(Action.SHORT_DESCRIPTION, java.util.ResourceBundle.getBundle("org/nbgit/ui/diff/Bundle").
                        getString("CTL_DiffPanel_Prev_Tooltip"));
            }

            public void actionPerformed(ActionEvent e) {
                onPrevButton();
            }
        };
        nextButton.setAction(nextAction);
        prevButton.setAction(prevAction);
    }

    private void refreshComponents() {
        DiffController view = setups != null && currentModelIndex != -1 ? setups[currentModelIndex].getView() : null;
        int currentDifferenceIndex = view != null ? view.getDifferenceIndex() : -1;
        if (view != null) {
            nextAction.setEnabled(currentIndex < setups.length - 1 || currentDifferenceIndex < view.getDifferenceCount() - 1);
        } else {
            nextAction.setEnabled(false);
        }
        prevAction.setEnabled(currentIndex > 0 || currentDifferenceIndex > 0);
        dividerSet = false;
        updateSplitLocation();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (refreshTask != null) {
            Git.getInstance().getStatusCache().addPropertyChangeListener(this);
        }
        JComponent parent = (JComponent) getParent();
        parent.getActionMap().put("jumpNext", nextAction);  // NOI18N
        parent.getActionMap().put("jumpPrev", prevAction); // NOI18N
    }

    private void updateSplitLocation() {
        if (dividerSet) {
            return;
        }
        JComponent parent = (JComponent) getParent();
        Dimension dim = parent == null ? new Dimension() : parent.getSize();
        if (dim.width <= 0 || dim.height <= 0) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    updateSplitLocation();
                }
            });
        }
        dividerSet = true;
        JTable jt = fileTable.getTable();
        int optimalLocation = jt.getPreferredSize().height + jt.getTableHeader().getPreferredSize().height;
        if (optimalLocation > dim.height / 3) {
            optimalLocation = dim.height / 3;
        }
        if (optimalLocation <= jt.getTableHeader().getPreferredSize().height) {
            optimalLocation = jt.getTableHeader().getPreferredSize().height * 3;
        }
        splitPane.setDividerLocation(optimalLocation);
    }

    @Override
    public void removeNotify() {
        Git.getInstance().getStatusCache().removePropertyChangeListener(this);
        super.removeNotify();
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
        } else {
            if ((oldInfo.getStatus() & displayStatuses) + (newInfo.getStatus() & displayStatuses) == 0) {
                return false;
            }
        }
        return context == null ? false : context.contains(file);
    }

    private void setDiffIndex(int idx, int location) {
        currentIndex = idx;
        DiffController view = null;

        if (currentIndex != -1) {
            currentModelIndex = showingFileTable() ? fileTable.getModelIndex(currentIndex) : 0;
            view = setups[currentModelIndex].getView();

            // enable Select in .. action
            TopComponent tc = (TopComponent) getClientProperty(TopComponent.class);
            if (tc != null) {
                Node node = Node.EMPTY;
                File baseFile = setups[currentModelIndex].getBaseFile();
                if (baseFile != null) {
                    FileObject fo = FileUtil.toFileObject(baseFile);
                    if (fo != null) {
                        node = new AbstractNode(Children.LEAF, Lookups.singleton(fo));
                    }
                }
                tc.setActivatedNodes(new Node[]{node});
            }

            diffView = null;
            boolean focus = false;
            if (view != null) {
                if (showingFileTable()) {
                    fileTableSetSelectedIndexContext = true;
                    fileTable.setSelectedIndex(currentIndex);
                    fileTableSetSelectedIndexContext = false;
                }
                diffView = view.getJComponent();
                diffView.getActionMap().put("jumpNext", nextAction);  // NOI18N
                diffView.getActionMap().put("jumpPrev", prevAction);  // NOI18N
                setBottomComponent();
                if (location == -1) {
                    location = view.getDifferenceCount() - 1;
                }
                if (location >= 0 && location < view.getDifferenceCount()) {
                    view.setLocation(DiffController.DiffPane.Modified, DiffController.LocationType.DifferenceIndex, location);
                }
                Component toc = WindowManager.getDefault().getRegistry().getActivated();
                if (SwingUtilities.isDescendingFrom(this, toc)) {
                    //                focus = true;
                }
            } else {
                diffView = new NoContentPanel(NbBundle.getMessage(MultiDiffPanel.class, "MSG_DiffPanel_NoContent"));
            }
        } else {
            currentModelIndex = -1;
            diffView = new NoContentPanel(NbBundle.getMessage(MultiDiffPanel.class, "MSG_DiffPanel_NoFileSelected"));
            setBottomComponent();
        }

        delegatingUndoRedo.setDiffView(diffView);

        refreshComponents();
//        if (focus) {
//            diffView.requestFocusInWindow();
//        }
    }

    private boolean showingFileTable() {
        return fileTable.getComponent().isVisible();
    }

    private void setBottomComponent() {
        int gg = splitPane.getDividerLocation();
        splitPane.setBottomComponent(diffView);
        splitPane.setDividerLocation(gg);
    }

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == commitButton) {
            onCommitButton();
        }
    }

    private void onRefreshButton() {
        if (context == null || context.getRootFiles().size() == 0) {
            return;
        }

        if (executeStatusSupport != null) {
            executeStatusSupport.cancel();
            executeStatusSupport = null;
        }

        LifecycleManager.getDefault().saveAll();
        RequestProcessor rp = Git.getInstance().getRequestProcessor();
        executeStatusSupport = new StatusTask(context) {

            @Override
            public void performAfter() {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        refreshSetups();
                    }
                });
            }
        };
        String repository = GitUtils.getRootPath(context);
        executeStatusSupport.start(rp, repository, NbBundle.getMessage(MultiDiffPanel.class, "MSG_Refresh_Progress"));
    }

    private void onUpdateButton() {
        UpdateAction.update(context);
    }

    private void onCommitButton() {
        LifecycleManager.getDefault().saveAll();
        CommitAction.commit(contextName, context);
    }

    /** Next that is driven by visibility. It continues to next not yet visible difference. */
    private void onNextButton() {
        if (showingFileTable()) {
            currentIndex = fileTable.getSelectedIndex();
            currentModelIndex = fileTable.getSelectedModelIndex();
        }

        DiffController view = setups[currentModelIndex].getView();
        int currentDifferenceIndex = view != null ? view.getDifferenceIndex() : -1;
        if (view != null) {
            int visibleDiffernce = view.getDifferenceIndex();
            if (visibleDiffernce < view.getDifferenceCount() - 1) {
                currentDifferenceIndex = Math.max(currentDifferenceIndex, visibleDiffernce);
            }
            if (++currentDifferenceIndex >= view.getDifferenceCount()) {
                if (++currentIndex >= setups.length) {
                    currentIndex--;
                } else {
                    setDiffIndex(currentIndex, 0);
                }
            } else {
                view.setLocation(DiffController.DiffPane.Modified, DiffController.LocationType.DifferenceIndex, currentDifferenceIndex);
            }
        } else {
            if (++currentIndex >= setups.length) {
                currentIndex = 0;
            }
            setDiffIndex(currentIndex, 0);
        }
        refreshComponents();
    }

    private void onPrevButton() {
        DiffController view = setups[currentModelIndex].getView();
        if (view != null) {
            int currentDifferenceIndex = view.getDifferenceIndex();
            if (--currentDifferenceIndex < 0) {
                if (--currentIndex < 0) {
                    currentIndex++;
                } else {
                    setDiffIndex(currentIndex, -1);
                }
            } else {
                view.setLocation(DiffController.DiffPane.Modified, DiffController.LocationType.DifferenceIndex, currentDifferenceIndex);
            }
        } else {
            if (--currentIndex < 0) {
                currentIndex = setups.length - 1;
            }
            setDiffIndex(currentIndex, -1);
        }
        refreshComponents();
    }

    /**
     * @return setups, takes into account Local, Remote, All switch
     */
    public Collection<Setup> getSetups() {
        if (setups == null) {
            return Collections.emptySet();
        } else {
            return Arrays.asList(setups);
        }
    }

    public String getSetupDisplayName() {
        return contextName;
    }

    private void refreshSetups() {
        if (dpt != null) {
            prepareTask.cancel();
        }

        File[] files;
        switch (currentType) {
            case Setup.DIFFTYPE_LOCAL:
                displayStatuses = StatusInfo.STATUS_LOCAL_CHANGE;
                break;
            case Setup.DIFFTYPE_REMOTE:
                displayStatuses = StatusInfo.STATUS_REMOTE_CHANGE;
                break;
            case Setup.DIFFTYPE_ALL:
                displayStatuses = StatusInfo.STATUS_LOCAL_CHANGE | StatusInfo.STATUS_REMOTE_CHANGE;
                break;
            default:
                throw new IllegalStateException("Unknown DIFF type:" + currentType); // NOI18N
        }
        files = GitUtils.getModifiedFiles(context, displayStatuses);

        setups = computeSetups(files);
        boolean propertyColumnVisible = false;
        for (Setup setup : setups) {
            if (setup.getPropertyName() != null) {
                propertyColumnVisible = true;
                break;
            }
        }
        fileTable.setColumns(propertyColumnVisible ? new String[]{DiffNode.COLUMN_NAME_NAME, DiffNode.COLUMN_NAME_PROPERTY, DiffNode.COLUMN_NAME_STATUS, DiffNode.COLUMN_NAME_LOCATION} : new String[]{DiffNode.COLUMN_NAME_NAME, DiffNode.COLUMN_NAME_STATUS, DiffNode.COLUMN_NAME_LOCATION});
        fileTable.setTableModel(setupToNodes(setups));

        if (setups.length == 0) {
            String noContentLabel;
            switch (currentType) {
                case Setup.DIFFTYPE_LOCAL:
                    noContentLabel = NbBundle.getMessage(MultiDiffPanel.class, "MSG_DiffPanel_NoLocalChanges");
                    break;
                case Setup.DIFFTYPE_REMOTE:
                    noContentLabel = NbBundle.getMessage(MultiDiffPanel.class, "MSG_DiffPanel_NoRemoteChanges");
                    break;
                case Setup.DIFFTYPE_ALL:
                    noContentLabel = NbBundle.getMessage(MultiDiffPanel.class, "MSG_DiffPanel_NoAllChanges");
                    break;
                default:
                    throw new IllegalStateException("Unknown DIFF type:" + currentType); // NOI18N
            }
            setups = null;
            fileTable.setTableModel(new Node[0]);
            fileTable.getComponent().setEnabled(false);
            fileTable.getComponent().setPreferredSize(null);
            Dimension dim = fileTable.getComponent().getPreferredSize();
            fileTable.getComponent().setPreferredSize(new Dimension(dim.width + 1, dim.height));
            diffView = null;
            diffView = new NoContentPanel(noContentLabel);
            setBottomComponent();
            nextAction.setEnabled(false);
            prevAction.setEnabled(false);
            revalidate();
            repaint();
        } else {
            fileTable.getComponent().setEnabled(true);
            fileTable.getComponent().setPreferredSize(null);
            Dimension dim = fileTable.getComponent().getPreferredSize();
            fileTable.getComponent().setPreferredSize(new Dimension(dim.width + 1, dim.height));
            setDiffIndex(0, 0);
            dpt = new DiffPrepareTask(setups);
            prepareTask = RequestProcessor.getDefault().post(dpt);
        }
    }

    private Setup[] computeSetups(File[] files) {
        List<Setup> newSetups = new ArrayList<Setup>(files.length);
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (!file.isDirectory()) {
                Setup setup = new Setup(file, null, currentType);
                setup.setNode(new DiffNode(setup));
                newSetups.add(setup);
            }
        }
        Collections.sort(newSetups, new SetupsComparator());
        return newSetups.toArray(new Setup[newSetups.size()]);
    }

    private Node[] setupToNodes(Setup[] setups) {
        List<Node> nodes = new ArrayList<Node>(setups.length);
        for (Setup setup : setups) {
            nodes.add(setup.getNode());
        }
        return nodes.toArray(new Node[nodes.size()]);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (DiffController.PROP_DIFFERENCES.equals(evt.getPropertyName())) {
            refreshComponents();
        } else if (StatusCache.PROP_FILE_STATUS_CHANGED.equals(evt.getPropertyName())) {
            if (!affectsView(evt)) {
                return;
            }
            refreshTask.schedule(200);
        }
    }

    private class DiffPrepareTask implements Runnable {

        private final Setup[] prepareSetups;

        public DiffPrepareTask(Setup[] prepareSetups) {
            this.prepareSetups = prepareSetups;
        }

        public void run() {
            for (int i = 0; i < prepareSetups.length; i++) {
                if (prepareSetups != setups) {
                    return;
                }
                try {
                    prepareSetups[i].initSources();  // slow network I/O
                    final int fi = i;
                    StreamSource ss1 = prepareSetups[fi].getFirstSource();
                    StreamSource ss2 = prepareSetups[fi].getSecondSource();
                    final DiffController view = DiffController.create(ss1, ss2);  // possibly executing slow external diff
                    view.addPropertyChangeListener(MultiDiffPanel.this);
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            prepareSetups[fi].setView(view);
                            if (prepareSetups != setups) {
                                return;
                            }
                            if (currentModelIndex == fi) {
                                setDiffIndex(currentIndex, 0);
                            }
                        }
                    });
                } catch (IOException e) {
                    ErrorManager.getDefault().notify(e);
                }
            }
        }
    }

    private static class SetupsComparator implements Comparator<Setup> {

        private GitUtils.ByImportanceComparator delegate = new GitUtils.ByImportanceComparator();
        private StatusCache cache;

        public SetupsComparator() {
            cache = Git.getInstance().getStatusCache();
        }

        public int compare(Setup setup1, Setup setup2) {
            int cmp = delegate.compare(cache.getStatus(setup1.getBaseFile()), cache.getStatus(setup2.getBaseFile()));
            if (cmp == 0) {
                return setup1.getBaseFile().getName().compareToIgnoreCase(setup2.getBaseFile().getName());
            }
            return cmp;
        }
    }

    private class RefreshViewTask implements Runnable {

        public void run() {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    refreshSetups();
                }
            });
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        controlsToolBar = new javax.swing.JToolBar();
        jPanel4 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        nextButton = new javax.swing.JButton();
        prevButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        refreshButton = new javax.swing.JButton();
        updateButton = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        commitButton = new javax.swing.JButton();
        splitPane = new javax.swing.JSplitPane();

        controlsToolBar.setFloatable(false);
        controlsToolBar.setRollover(true);

        jPanel4.setMaximumSize(new java.awt.Dimension(12, 32767));

        jPanel3.setMaximumSize(new java.awt.Dimension(12, 32767));

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 12, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 21, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(20, Short.MAX_VALUE)
                .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        controlsToolBar.add(jPanel4);

        jPanel1.setMaximumSize(new java.awt.Dimension(80, 32767));

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 80, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 21, Short.MAX_VALUE)
        );

        controlsToolBar.add(jPanel1);

        nextButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/nbgit/resources/icons/diff-next.png"))); // NOI18N
        nextButton.setToolTipText(org.openide.util.NbBundle.getMessage(MultiDiffPanel.class, "CTL_DiffPanel_Next_Tooltip")); // NOI18N
        nextButton.setFocusable(false);
        nextButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        nextButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        controlsToolBar.add(nextButton);

        prevButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/nbgit/resources/icons/diff-prev.png"))); // NOI18N
        prevButton.setToolTipText(org.openide.util.NbBundle.getMessage(MultiDiffPanel.class, "CTL_DiffPanel_Prev_Tooltip")); // NOI18N
        prevButton.setFocusable(false);
        prevButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        prevButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        controlsToolBar.add(prevButton);

        jPanel2.setMaximumSize(new java.awt.Dimension(30, 32767));

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 30, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 21, Short.MAX_VALUE)
        );

        controlsToolBar.add(jPanel2);

        refreshButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/nbgit/resources/icons/refresh.png"))); // NOI18N
        refreshButton.setToolTipText(org.openide.util.NbBundle.getMessage(MultiDiffPanel.class, "refreshButton.toolTipText")); // NOI18N
        refreshButton.setFocusable(false);
        refreshButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        refreshButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        refreshButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonActionPerformed(evt);
            }
        });
        controlsToolBar.add(refreshButton);

        updateButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/nbgit/resources/icons/update.png"))); // NOI18N
        updateButton.setFocusable(false);
        updateButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        updateButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        updateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateButtonActionPerformed(evt);
            }
        });
        controlsToolBar.add(updateButton);

        jPanel5.setMaximumSize(new java.awt.Dimension(20, 32767));

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 20, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 21, Short.MAX_VALUE)
        );

        controlsToolBar.add(jPanel5);

        commitButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/nbgit/resources/icons/commit.png"))); // NOI18N
        commitButton.setToolTipText(org.openide.util.NbBundle.getMessage(MultiDiffPanel.class, "MSG_CommitDiff_Tooltip")); // NOI18N
        commitButton.setFocusable(false);
        commitButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        commitButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        controlsToolBar.add(commitButton);

        splitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(controlsToolBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 716, Short.MAX_VALUE)
            .add(splitPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 716, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(controlsToolBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(splitPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 331, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void updateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateButtonActionPerformed
        onUpdateButton();
    }//GEN-LAST:event_updateButtonActionPerformed

    private void refreshButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButtonActionPerformed
        onRefreshButton();
    }//GEN-LAST:event_refreshButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton commitButton;
    private javax.swing.JToolBar controlsToolBar;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JButton nextButton;
    private javax.swing.JButton prevButton;
    private javax.swing.JButton refreshButton;
    private javax.swing.JSplitPane splitPane;
    private javax.swing.JButton updateButton;
    // End of variables declaration//GEN-END:variables
    /** Interprets property blob. */
    static final class Property {

        final byte[] value;

        Property(Object value) {
            this.value = (byte[]) value;
        }

        String getMIME() {
            return "text/plain"; // NOI18N
        }

        Reader toReader() {
            if (GitUtils.isBinary(value)) {
                return new StringReader(NbBundle.getMessage(MultiDiffPanel.class, "LBL_Diff_NoBinaryDiff"));  // hexa-flexa txt? // NOI18N
            } else {
                try {
                    return new InputStreamReader(new ByteArrayInputStream(value), "utf8");  // NOI18N
                } catch (UnsupportedEncodingException ex) {
                    Git.LOG.log(Level.SEVERE, "UnsupportedEncodingException " + ex);
                    return new StringReader("[ERROR: " + ex.getLocalizedMessage() + "]"); // NOI18N
                }
            }
        }
    }
}
