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
package org.nbgit.ui.commit;

import java.awt.Component;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import org.nbgit.StatusInfo;
import org.nbgit.ui.GitFileNode;
import org.nbgit.util.GitUtils;
import org.nbgit.util.HtmlFormatter;
import org.netbeans.modules.versioning.util.FilePathCellRenderer;
import org.netbeans.modules.versioning.util.TableSorter;
import org.openide.util.NbBundle;

/**
 * {@link #getComponent Table} that displays nodes in the commit dialog.
 *
 * @author Maros Sandor
 */
public class CommitTable implements AncestorListener, TableModelListener {

    public static String[] COMMIT_COLUMNS = new String[]{
        CommitTableModel.COLUMN_NAME_NAME,
        CommitTableModel.COLUMN_NAME_STATUS,
        CommitTableModel.COLUMN_NAME_ACTION,
        CommitTableModel.COLUMN_NAME_PATH
    };
    public static String[] IMPORT_COLUMNS = new String[]{
        CommitTableModel.COLUMN_NAME_NAME,
        CommitTableModel.COLUMN_NAME_ACTION,
        CommitTableModel.COLUMN_NAME_PATH
    };
    private CommitTableModel tableModel;
    private JTable table;
    private JComponent component;
    private TableSorter sorter;
    private String[] columns;
    private String[] sortByColumns;

    public CommitTable(JLabel label, String[] columns, String[] sortByColumns) {
        init(label, columns, null);
        this.sortByColumns = sortByColumns;
        setSortingStatus();
    }

    public CommitTable(JLabel label, String[] columns, TableSorter sorter) {
        init(label, columns, sorter);
    }

    private void init(JLabel label, String[] columns, TableSorter sorter) {
        tableModel = new CommitTableModel(columns);
        tableModel.addTableModelListener(this);
        if (sorter == null) {
            sorter = new TableSorter(tableModel);
        }
        this.sorter = sorter;
        table = new JTable(this.sorter);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(String.class, new CommitStringsCellRenderer());
        table.setDefaultEditor(CommitOptions.class, new CommitOptionsCellEditor());
        table.getTableHeader().setReorderingAllowed(true);
        this.sorter.setTableHeader(table.getTableHeader());
        table.setRowHeight(table.getRowHeight() * 6 / 5);
        table.addAncestorListener(this);
        component = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        label.setLabelFor(table);
        table.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(CommitTable.class, "ACSD_CommitTable")); // NOI18N
        setColumns(columns);
    }

    public void ancestorAdded(AncestorEvent event) {
        setDefaultColumnSizes();
    }

    /**
     * Sets sizes of Commit table columns, kind of hardcoded.
     */
    private void setDefaultColumnSizes() {
        int width = table.getWidth();
        TableColumnModel columnModel = table.getColumnModel();
        if (columns == null || columnModel == null) {
            return; // unsure when this methed will be called (component realization)
        }
        if (columnModel.getColumnCount() != columns.length) {
            return;
        }
        if (columns.length == 3) {
            for (int i = 0; i < columns.length; i++) {
                String col = columns[i];
                sorter.setColumnComparator(i, null);
                if (col.equals(CommitTableModel.COLUMN_NAME_NAME)) {
                    sorter.setColumnComparator(i, new FileNameComparator());
                    columnModel.getColumn(i).setPreferredWidth(width * 30 / 100);
                } else if (col.equals(CommitTableModel.COLUMN_NAME_ACTION)) {
                    columnModel.getColumn(i).setPreferredWidth(width * 15 / 100);
                } else {
                    columnModel.getColumn(i).setPreferredWidth(width * 40 / 100);
                }
            }
        } else if (columns.length == 4) {
            for (int i = 0; i < columns.length; i++) {
                String col = columns[i];
                sorter.setColumnComparator(i, null);
                if (col.equals(CommitTableModel.COLUMN_NAME_NAME)) {
                    sorter.setColumnComparator(i, new FileNameComparator());
                    columnModel.getColumn(i).setPreferredWidth(width * 25 / 100);
                } else if (col.equals(CommitTableModel.COLUMN_NAME_STATUS)) {
                    sorter.setColumnComparator(i, new StatusComparator());
                    columnModel.getColumn(i).setPreferredWidth(width * 15 / 100);
                } else if (col.equals(CommitTableModel.COLUMN_NAME_ACTION)) {
                    columnModel.getColumn(i).setPreferredWidth(width * 20 / 100);
                } else {
                    columnModel.getColumn(i).setPreferredWidth(width * 40 / 100);
                }
            }
        } else if (columns.length == 5) {
            for (int i = 0; i < columns.length; i++) {
                String col = columns[i];
                sorter.setColumnComparator(i, null);
                if (col.equals(CommitTableModel.COLUMN_NAME_NAME)) {
                    sorter.setColumnComparator(i, new FileNameComparator());
                    columnModel.getColumn(i).setPreferredWidth(width * 25 / 100);
                } else if (col.equals(CommitTableModel.COLUMN_NAME_STATUS)) {
                    sorter.setColumnComparator(i, new StatusComparator());
                    sorter.setSortingStatus(i, TableSorter.ASCENDING);
                    columnModel.getColumn(i).setPreferredWidth(width * 15 / 100);
                } else if (col.equals(CommitTableModel.COLUMN_NAME_ACTION)) {
                    columnModel.getColumn(i).setPreferredWidth(width * 15 / 100);
                } else {
                    columnModel.getColumn(i).setPreferredWidth(width * 30 / 100);
                }
            }
        }
    }

    private void setSortingStatus() {
        for (int i = 0; i < sortByColumns.length; i++) {
            String sortByColumn = sortByColumns[i];
            for (int j = 0; j < columns.length; j++) {
                String column = columns[j];
                if (column.equals(sortByColumn)) {
                    sorter.setSortingStatus(j, column.equals(sortByColumn) ? TableSorter.ASCENDING : TableSorter.NOT_SORTED);
                    break;
                }
            }
        }
    }

    public TableSorter getSorter() {
        return sorter;
    }

    public void ancestorMoved(AncestorEvent event) {
    }

    public void ancestorRemoved(AncestorEvent event) {
    }

    void setColumns(String[] cols) {
        if (Arrays.equals(columns, cols)) {
            return;
        }
        columns = cols;
        tableModel.setColumns(cols);
        setDefaultColumnSizes();
    }

    public void setNodes(GitFileNode[] nodes) {
        tableModel.setNodes(nodes);
    }

    /**
     *
     * @return Map&lt;GitFileNode, CommitOptions&gt;
     */
    public Map<GitFileNode, CommitOptions> getCommitFiles() {
        return tableModel.getCommitFiles();
    }

    /**
     * @return table in a scrollpane
     */
    public JComponent getComponent() {
        return component;
    }

    void dataChanged() {
        int idx = table.getSelectedRow();
        tableModel.fireTableDataChanged();
        if (idx != -1) {
            table.getSelectionModel().addSelectionInterval(idx, idx);
        }
    }

    TableModel getTableModel() {
        return tableModel;
    }

    public void tableChanged(TableModelEvent e) {
        // change in commit options may alter name rendering (strikethrough)
        table.repaint();
    }

    public void setRootFile(String repositoryPath, String rootLocalPath) {
        tableModel.setRootFile(repositoryPath, rootLocalPath);
    }

    private class CommitOptionsCellEditor extends DefaultCellEditor {

        private final Object[] dirAddOptions = new Object[]{
            CommitOptions.COMMIT,
            CommitOptions.EXCLUDE
        };
        private final Object[] addOptions = new Object[]{
            CommitOptions.COMMIT,
            CommitOptions.EXCLUDE
        };
        private final Object[] commitOptions = new Object[]{
            CommitOptions.COMMIT,
            CommitOptions.EXCLUDE
        };
        private final Object[] removeOptions = new Object[]{
            CommitOptions.COMMIT_REMOVE,
            CommitOptions.EXCLUDE
        };

        public CommitOptionsCellEditor() {
            super(new JComboBox());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            StatusInfo info = tableModel.getNode(sorter.modelIndex(row)).getInformation();
            int fileStatus = info.getStatus();
            JComboBox combo = (JComboBox) editorComponent;
            if (fileStatus == StatusInfo.STATUS_VERSIONED_DELETEDLOCALLY || fileStatus == StatusInfo.STATUS_VERSIONED_REMOVEDLOCALLY) {
                combo.setModel(new DefaultComboBoxModel(removeOptions));
            } else if ((fileStatus & StatusInfo.STATUS_IN_REPOSITORY) == 0) {
                if (info.isDirectory()) {
                    combo.setModel(new DefaultComboBoxModel(dirAddOptions));
                } else {
                    combo.setModel(new DefaultComboBoxModel(addOptions));
                }
            } else {
                combo.setModel(new DefaultComboBoxModel(commitOptions));
            }
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
    }

    private class CommitStringsCellRenderer extends DefaultTableCellRenderer {

        private FilePathCellRenderer pathRenderer = new FilePathCellRenderer();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            int col = table.convertColumnIndexToModel(column);
            if (columns[col].equals(CommitTableModel.COLUMN_NAME_NAME)) {
                TableSorter sorter = (TableSorter) table.getModel();
                CommitTableModel model = (CommitTableModel) sorter.getTableModel();
                GitFileNode node = model.getNode(sorter.modelIndex(row));
                CommitOptions options = model.getOptions(sorter.modelIndex(row));
                if (!isSelected) {
                    value = "<html>" + HtmlFormatter.getInstance().annotateNameHtml( // NOI18N
                            node.getFile().getName(), node.getInformation(), null);
                }
                if (options == CommitOptions.EXCLUDE) {
                    value = "<html><s>" + value + "</s></html>";
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            } else if (columns[col].equals(CommitTableModel.COLUMN_NAME_PATH)) {
                return pathRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            } else {
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        }
    }

    private class StatusComparator extends GitUtils.ByImportanceComparator {

        public int compare(Object o1, Object o2) {
            Integer row1 = (Integer) o1;
            Integer row2 = (Integer) o2;
            return super.compare(tableModel.getNode(row1.intValue()).getInformation(),
                    tableModel.getNode(row2.intValue()).getInformation());
        }
    }

    private class FileNameComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            Integer row1 = (Integer) o1;
            Integer row2 = (Integer) o2;
            return tableModel.getNode(row1.intValue()).getName().compareToIgnoreCase(
                    tableModel.getNode(row2.intValue()).getName());
        }
    }
}
