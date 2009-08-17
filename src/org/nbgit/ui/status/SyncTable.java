package org.nbgit.ui.status;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import org.nbgit.StatusInfo;
import org.nbgit.StatusCache;
import org.nbgit.Git;
import org.nbgit.GitAnnotator;
import org.nbgit.GitModuleConfig;
import org.nbgit.util.GitUtils;
import org.nbgit.ui.commit.CommitAction;
import org.nbgit.ui.commit.ExcludeFromCommitAction;
import org.nbgit.ui.diff.DiffAction;
import org.nbgit.ui.log.SearchHistoryAction;
import org.nbgit.ui.update.RevertModificationsAction;
import org.nbgit.util.HtmlFormatter;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.netbeans.modules.versioning.util.FilePathCellRenderer;
import org.netbeans.modules.versioning.util.TableSorter;
import org.openide.awt.Mnemonics;
import org.openide.awt.MouseUtils;
import org.openide.explorer.view.NodeTableModel;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport.ReadOnly;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

/**
 * Controls the {@link #getComponent() table} that displays nodes
 * in the Versioning view. The table is  {@link #setTableModel populated)
 * from VersioningPanel.
 *
 * @author Maros Sandor
 */
class SyncTable implements MouseListener, ListSelectionListener, AncestorListener {

    private NodeTableModel tableModel;
    private JTable table;
    private JScrollPane component;
    private SyncFileNode[] nodes = new SyncFileNode[0];
    private String[] tableColumns;
    private TableSorter sorter;
    /**
     * Defines labels for Versioning view table columns.
     */
    private static final Map<String, String[]> columnLabels = new HashMap<String, String[]>(4);


    {
        ResourceBundle loc = NbBundle.getBundle(SyncTable.class);
        columnLabels.put(SyncFileNode.COLUMN_NAME_BRANCH, new String[]{
                    loc.getString("CTL_VersioningView_Column_Branch_Title"), // NOI18N
                    loc.getString("CTL_VersioningView_Column_Branch_Desc")
                }); // NOI18N
        columnLabels.put(SyncFileNode.COLUMN_NAME_NAME, new String[]{
                    loc.getString("CTL_VersioningView_Column_File_Title"), // NOI18N
                    loc.getString("CTL_VersioningView_Column_File_Desc")
                }); // NOI18N
        columnLabels.put(SyncFileNode.COLUMN_NAME_STATUS, new String[]{
                    loc.getString("CTL_VersioningView_Column_Status_Title"), // NOI18N
                    loc.getString("CTL_VersioningView_Column_Status_Desc")
                }); // NOI18N
        columnLabels.put(SyncFileNode.COLUMN_NAME_PATH, new String[]{
                    loc.getString("CTL_VersioningView_Column_Path_Title"), // NOI18N
                    loc.getString("CTL_VersioningView_Column_Path_Desc")
                }); // NOI18N
    }
    private static final Comparator NodeComparator = new Comparator() {

        public int compare(Object o1, Object o2) {
            Node.Property p1 = (Node.Property) o1;
            Node.Property p2 = (Node.Property) o2;
            String sk1 = (String) p1.getValue("sortkey"); // NOI18N
            if (sk1 != null) {
                String sk2 = (String) p2.getValue("sortkey"); // NOI18N
                return sk1.compareToIgnoreCase(sk2);
            } else {
                try {
                    String s1 = (String) p1.getValue();
                    String s2 = (String) p2.getValue();
                    return s1.compareToIgnoreCase(s2);
                } catch (Exception e) {
                    Git.LOG.log(Level.INFO, null, e);
                    return 0;
                }
            }
        }
    };

    public SyncTable() {
        tableModel = new NodeTableModel();
        sorter = new TableSorter(tableModel);
        sorter.setColumnComparator(Node.Property.class, NodeComparator);
        table = new JTable(sorter);
        sorter.setTableHeader(table.getTableHeader());
        table.setRowHeight(table.getRowHeight() * 6 / 5);
        component = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        component.getViewport().setBackground(table.getBackground());
        Color borderColor = UIManager.getColor("scrollpane_border"); // NOI18N
        if (borderColor == null) {
            borderColor = UIManager.getColor("controlShadow"); // NOI18N
        }
        component.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, borderColor));
        table.addMouseListener(this);
        table.setDefaultRenderer(Node.Property.class, new SyncTableCellRenderer());
        table.getSelectionModel().addListSelectionListener(this);
        table.addAncestorListener(this);
        table.getAccessibleContext().setAccessibleName(NbBundle.getMessage(SyncTable.class, "ACSN_VersioningTable")); // NOI18N
        table.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(SyncTable.class, "ACSD_VersioningTable")); // NOI18N
        setColumns(new String[]{
                    SyncFileNode.COLUMN_NAME_NAME,
                    SyncFileNode.COLUMN_NAME_STATUS,
                    SyncFileNode.COLUMN_NAME_PATH
                });
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F10, KeyEvent.SHIFT_DOWN_MASK), "org.openide.actions.PopupAction"); // NOI18N
        table.getActionMap().put("org.openide.actions.PopupAction", new AbstractAction() { // NOI18N

            public void actionPerformed(ActionEvent e) {
                showPopup(org.netbeans.modules.versioning.util.Utils.getPositionForPopup(table));
            }
        });
    }

    void setDefaultColumnSizes() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                int width = table.getWidth();
                if (tableColumns.length == 3) {
                    for (int i = 0; i < tableColumns.length; i++) {
                        if (SyncFileNode.COLUMN_NAME_PATH.equals(tableColumns[i])) {
                            table.getColumnModel().getColumn(i).setPreferredWidth(width * 60 / 100);
                        } else {
                            table.getColumnModel().getColumn(i).setPreferredWidth(width * 20 / 100);
                        }
                    }
                } else if (tableColumns.length == 4) {
                    for (int i = 0; i < tableColumns.length; i++) {
                        if (SyncFileNode.COLUMN_NAME_PATH.equals(tableColumns[i])) {
                            table.getColumnModel().getColumn(i).setPreferredWidth(width * 55 / 100);
                        } else if (SyncFileNode.COLUMN_NAME_BRANCH.equals(tableColumns[i])) {
                            table.getColumnModel().getColumn(i).setPreferredWidth(width * 20 / 100);
                        } else {
                            table.getColumnModel().getColumn(i).setPreferredWidth(width * 15 / 100);
                        }
                    }
                }
            }
        });
    }

    public void ancestorAdded(AncestorEvent event) {
        setDefaultColumnSizes();
    }

    public void ancestorMoved(AncestorEvent event) {
    }

    public void ancestorRemoved(AncestorEvent event) {
    }

    public SyncFileNode[] getDisplayedNodes() {
        int n = sorter.getRowCount();
        SyncFileNode[] ret = new SyncFileNode[n];
        for (int i = 0; i < n; i++) {
            ret[i] = nodes[sorter.modelIndex(i)];
        }
        return ret;
    }

    public JComponent getComponent() {
        return component;
    }

    /**
     * Sets visible columns in the Versioning table.
     *
     * @param columns array of column names, they must be one of SyncFileNode.COLUMN_NAME_XXXXX constants.
     */
    final void setColumns(String[] columns) {
        if (Arrays.equals(columns, tableColumns)) {
            return;
        }
        setModelProperties(columns);
        tableColumns = columns;
        for (int i = 0; i < tableColumns.length; i++) {
            sorter.setColumnComparator(i, null);
            sorter.setSortingStatus(i, TableSorter.NOT_SORTED);
            if (SyncFileNode.COLUMN_NAME_STATUS.equals(tableColumns[i])) {
                sorter.setSortingStatus(i, TableSorter.ASCENDING);
                break;
            }
        }
        setDefaultColumnSizes();
    }

    private void setModelProperties(String[] columns) {
        Node.Property[] properties = new Node.Property[columns.length];
        for (int i = 0; i < columns.length; i++) {
            String column = columns[i];
            String[] labels = columnLabels.get(column);
            properties[i] = new ColumnDescriptor(column, String.class, labels[0], labels[1]);
        }
        tableModel.setProperties(properties);
    }

    void setTableModel(SyncFileNode[] nodes) {
        this.nodes = nodes;
        tableModel.setNodes(nodes);
    }

    void focus() {
        table.requestFocus();
    }

    private static class ColumnDescriptor extends ReadOnly {

        @SuppressWarnings("unchecked")
        public ColumnDescriptor(String name, Class type, String displayName, String shortDescription) {
            super(name, type, displayName, shortDescription);
        }

        public Object getValue() throws IllegalAccessException, InvocationTargetException {
            return null;
        }
    }

    private void showPopup(final MouseEvent e) {
        int row = table.rowAtPoint(e.getPoint());
        if (row != -1) {
            boolean makeRowSelected = true;
            int[] selectedrows = table.getSelectedRows();

            for (int i = 0; i < selectedrows.length; i++) {
                if (row == selectedrows[i]) {
                    makeRowSelected = false;
                    break;
                }
            }
            if (makeRowSelected) {
                table.getSelectionModel().setSelectionInterval(row, row);
            }
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                // invoke later so the selection on the table will be set first
                JPopupMenu menu = getPopup();
                menu.show(table, e.getX(), e.getY());
            }
        });
    }

    private void showPopup(Point p) {
        JPopupMenu menu = getPopup();
        menu.show(table, p.x, p.y);
    }

    /**
     * Constructs contextual Menu: File Node
    <pre>
    Open
    -------------------
    Diff                 (default action)
    Update
    Commit...
    --------------------
    Conflict Resolved    (on conflicting file)
    --------------------
    Blame
    Show History...
    --------------------
    Revert Modifications  (Revert Delete)(Delete)
    Exclude from Commit   (Include in Commit)
    Ignore                (Unignore)
    </pre>
     */
    private JPopupMenu getPopup() {

        JPopupMenu menu = new JPopupMenu();
        JMenuItem item;
        VCSContext context = GitUtils.getCurrentContext(null);
        ResourceBundle loc = NbBundle.getBundle(Git.class);

        item = menu.add(new OpenInEditorAction());
        Mnemonics.setLocalizedText(item, item.getText());
        menu.add(new JSeparator());
        item = menu.add(new DiffAction(loc.getString("CTL_PopupMenuItem_Diff"), context)); // NOI18N
        Mnemonics.setLocalizedText(item, item.getText());
        item = menu.add(new CommitAction(loc.getString("CTL_PopupMenuItem_Commit"), context)); // NOI18N
        Mnemonics.setLocalizedText(item, item.getText());

        /*
        menu.add(new JSeparator());

        item = menu.add(new ConflictResolvedAction(loc.getString("CTL_PopupMenuItem_MarkResolved"), context)); // NOI18N
        Mnemonics.setLocalizedText(item, item.getText());
        menu.add(new JSeparator());
         */

        /*
        AnnotateAction tempA = new AnnotateAction(loc.getString("CTL_PopupMenuItem_ShowAnnotations"), context); // NOI18N
        if (tempA.visible(null)) {
        tempA = new AnnotateAction(loc.getString("CTL_PopupMenuItem_HideAnnotations"), context); // NOI18N
        }
        item = menu.add(tempA);
        Mnemonics.setLocalizedText(item, item.getText());
         */
        menu.add(new JSeparator());

        boolean allLocallyDeleted = true;
        StatusCache cache = Git.getInstance().getStatusCache();
        Set<File> files = GitUtils.getCurrentContext(null).getRootFiles();

        for (File file : files) {
            StatusInfo info = cache.getStatus(file);
            if (info.getStatus() != StatusInfo.STATUS_VERSIONED_DELETEDLOCALLY && info.getStatus() != StatusInfo.STATUS_VERSIONED_REMOVEDLOCALLY) {
                allLocallyDeleted = false;
            }
        }
        if (allLocallyDeleted) {
            item = menu.add(new RevertModificationsAction(loc.getString("CTL_PopupMenuItem_RevertDelete"), context));
        } else {
            item = menu.add(new RevertModificationsAction(loc.getString("CTL_PopupMenuItem_GetClean"), context));
        }
        Mnemonics.setLocalizedText(item, item.getText());

        ExcludeFromCommitAction exclude = new ExcludeFromCommitAction(loc.getString("CTL_PopupMenuItem_IncludeInCommit"), context); // NOI18N
        if (exclude.getActionStatus(null) != ExcludeFromCommitAction.INCLUDING) {
            exclude = new ExcludeFromCommitAction(loc.getString("CTL_PopupMenuItem_ExcludeFromCommit"), context); // NOI18N
        }
        item = menu.add(exclude);
        Mnemonics.setLocalizedText(item, item.getText());

        /*
        item = menu.add(new SystemActionBridge(SystemAction.get(SearchHistoryAction.class), actionString("CTL_PopupMenuItem_SearchHistory"))); // NOI18N
        Mnemonics.setLocalizedText(item, item.getText());

        menu.add(new JSeparator());

        //        item = menu.add(new SystemActionBridge(SystemAction.get(ResolveConflictsAction.class), actionString("CTL_PopupMenuItem_ResolveConflicts"))); // NOI18N
        //        Mnemonics.setLocalizedText(item, item.getText());
        /*
        Action ignoreAction = new SystemActionBridge(SystemAction.get(IgnoreAction.class),
        ((IgnoreAction)SystemAction.get(IgnoreAction.class)).getActionStatus(files) == IgnoreAction.UNIGNORING ?
        actionString("CTL_PopupMenuItem_Unignore") : // NOI18N
        actionString("CTL_PopupMenuItem_Ignore")); // NOI18N
        item = menu.add(ignoreAction);
        Mnemonics.setLocalizedText(item, item.getText());
         */

        return menu;
    }

    /**
     * Workaround.
     * I18N Test Wizard searches for keys in syncview package Bundle.properties
     */
    private String actionString(String key) {
        ResourceBundle actionsLoc = NbBundle.getBundle(GitAnnotator.class);
        return actionsLoc.getString(key);
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && MouseUtils.isDoubleClick(e)) {
            int row = table.rowAtPoint(e.getPoint());
            if (row == -1) {
                return;
            }
            row = sorter.modelIndex(row);
            Action action = nodes[row].getPreferredAction();
            if (action == null || !action.isEnabled()) {
                action = new OpenInEditorAction();
            }
            if (action.isEnabled()) {
                action.actionPerformed(new ActionEvent(this, 0, ""));
            }
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        List<SyncFileNode> selectedNodes = new ArrayList<SyncFileNode>();
        ListSelectionModel selectionModel = table.getSelectionModel();
        final TopComponent tc = (TopComponent) SwingUtilities.getAncestorOfClass(TopComponent.class, table);
        if (tc == null) {
            return; // table is no longer in component hierarchy
        }
        int min = selectionModel.getMinSelectionIndex();
        if (min != -1) {
            int max = selectionModel.getMaxSelectionIndex();
            for (int i = min; i <= max; i++) {
                if (selectionModel.isSelectedIndex(i)) {
                    int idx = sorter.modelIndex(i);
                    selectedNodes.add(nodes[idx]);
                }
            }
        }
        // this method may be called outside of AWT if a node fires change events from some other thread, see #79174
        final Node[] nodes = selectedNodes.toArray(new Node[selectedNodes.size()]);
        if (SwingUtilities.isEventDispatchThread()) {
            tc.setActivatedNodes(nodes);
        } else {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    tc.setActivatedNodes(nodes);
                }
            });
        }
    }

    private class SyncTableCellRenderer extends DefaultTableCellRenderer {

        private FilePathCellRenderer pathRenderer = new FilePathCellRenderer();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component renderer;
            int modelColumnIndex = table.convertColumnIndexToModel(column);
            if (modelColumnIndex == 0) {
                SyncFileNode node = nodes[sorter.modelIndex(row)];
                if (!isSelected) {
                    value = "<html>" + node.getHtmlDisplayName();
                }
                if (GitModuleConfig.getDefault().isExcludedFromCommit(node.getFile().getAbsolutePath())) {
                    String nodeName = node.getDisplayName();
                    if (isSelected) {
                        value = "<html><s>" + nodeName + "</s></html>";
                    } else {
                        value = "<html><s>" + HtmlFormatter.getInstance().annotateNameHtml(nodeName, node.getFileInformation(), null) + "</s>";
                    }
                }
            }
            if (modelColumnIndex == 2) {
                renderer = pathRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            } else {
                renderer = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
            if (renderer instanceof JComponent) {
                String path = nodes[sorter.modelIndex(row)].getFile().getAbsolutePath();
                ((JComponent) renderer).setToolTipText(path);
            }
            return renderer;
        }
    }
}
