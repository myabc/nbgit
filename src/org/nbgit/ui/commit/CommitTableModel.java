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

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javax.swing.table.AbstractTableModel;
import org.nbgit.GitModuleConfig;
import org.nbgit.StatusInfo;
import org.nbgit.ui.GitFileNode;
import org.nbgit.util.GitUtils;
import org.openide.util.NbBundle;

/**
 * Table model for the Commit dialog table.
 *
 * @author Maros Sandor
 */
public class CommitTableModel extends AbstractTableModel {

    public static final String COLUMN_NAME_NAME = "name"; // NOI18N
    public static final String COLUMN_NAME_STATUS = "status"; // NOI18N
    public static final String COLUMN_NAME_ACTION = "action"; // NOI18N
    public static final String COLUMN_NAME_PATH = "path"; // NOI18N
    public static final String COLUMN_NAME_BRANCH = "branch"; // NOI18N

    private class RootFile {
        String repositoryPath;
        String rootLocalPath;
    }
    //private Set<SVNUrl> repositoryRoots;
    private RootFile rootFile;
    /**
     * Defines labels for Versioning view table columns.
     */
    private static final Map<String, String[]> columnLabels = new HashMap<String, String[]>(4);
    {
        ResourceBundle loc = NbBundle.getBundle(CommitTableModel.class);
        columnLabels.put(COLUMN_NAME_NAME, new String[]{
                    loc.getString("CTL_CommitTable_Column_File"), // NOI18N
                    loc.getString("CTL_CommitTable_Column_File")
                }); // NOI18N
        columnLabels.put(COLUMN_NAME_BRANCH, new String[]{
                    loc.getString("CTL_CommitTable_Column_Branch"), // NOI18N
                    loc.getString("CTL_CommitTable_Column_Branch")
                }); // NOI18N
        columnLabels.put(COLUMN_NAME_STATUS, new String[]{
                    loc.getString("CTL_CommitTable_Column_Status"), // NOI18N
                    loc.getString("CTL_CommitTable_Column_Status")
                }); // NOI18N
        columnLabels.put(COLUMN_NAME_ACTION, new String[]{
                    loc.getString("CTL_CommitTable_Column_Action"), // NOI18N
                    loc.getString("CTL_CommitTable_Column_Action")
                }); // NOI18N
        columnLabels.put(COLUMN_NAME_PATH, new String[]{
                    loc.getString("CTL_CommitTable_Column_Folder"), // NOI18N
                    loc.getString("CTL_CommitTable_Column_Folder")
                }); // NOI18N
    }
    private CommitOptions[] commitOptions;
    private GitFileNode[] nodes;
    private String[] columns;

    /**
     * Create stable with name, status, action and path columns
     * and empty nodes {@link #setNodes model}.
     */
    public CommitTableModel(String[] columns) {
        setColumns(columns);
        setNodes(new GitFileNode[0]);
    }

    void setNodes(GitFileNode[] nodes) {
        this.nodes = nodes;
        defaultCommitOptions();
        fireTableDataChanged();
    }

    void setColumns(String[] cols) {
        if (Arrays.equals(cols, columns)) {
            return;
        }
        columns = cols;
        fireTableStructureChanged();
    }

    /**
     * @return Map&lt;GitFileNode, CommitOptions>
     */
    public Map<GitFileNode, CommitOptions> getCommitFiles() {
        Map<GitFileNode, CommitOptions> ret = new HashMap<GitFileNode, CommitOptions>(nodes.length);
        for (int i = 0; i < nodes.length; i++) {
            ret.put(nodes[i], commitOptions[i]);
        }
        return ret;
    }

    @Override
    public String getColumnName(int column) {
        return columnLabels.get(columns[column])[0];
    }

    public int getColumnCount() {
        return columns.length;
    }

    public int getRowCount() {
        return nodes.length;
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        String col = columns[columnIndex];
        if (col.equals(COLUMN_NAME_ACTION)) {
            return CommitOptions.class;
        }
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        String col = columns[columnIndex];
        return col.equals(COLUMN_NAME_ACTION);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        GitFileNode node;
        String col = columns[columnIndex];
        if (col.equals(COLUMN_NAME_NAME)) {
            return nodes[rowIndex].getName();
        } else if (col.equals(COLUMN_NAME_STATUS)) {
            node = nodes[rowIndex];
            StatusInfo finfo = node.getInformation();
            //TODO what should we do with this?
            //finfo.getEntry(node.getFile());  // HACK returned value is not interesting, point is side effect, it loads ISVNStatus structure
            return finfo.getStatusText();
        } else if (col.equals(COLUMN_NAME_ACTION)) {
            return commitOptions[rowIndex];
        } else if (col.equals(COLUMN_NAME_PATH)) {
            String shortPath = null;
            // XXX this is a mess
            if (rootFile != null) {
                // must convert from native separators to slashes
                String relativePath = nodes[rowIndex].getFile().getAbsolutePath().substring(rootFile.rootLocalPath.length());
                shortPath = rootFile.repositoryPath + relativePath.replace(File.separatorChar, '/');
            } else {
                shortPath = GitUtils.getRelativePath(nodes[rowIndex].getFile());
                if (shortPath == null) {
                    shortPath = org.openide.util.NbBundle.getMessage(CommitTableModel.class, "CTL_CommitForm_NotInRepository");
                }
            }
            return shortPath;
        }
        throw new IllegalArgumentException("Column index out of range: " + columnIndex); // NOI18N
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        String col = columns[columnIndex];
        if (col.equals(COLUMN_NAME_ACTION)) {
            commitOptions[rowIndex] = (CommitOptions) aValue;
            fireTableCellUpdated(rowIndex, columnIndex);
        } else {
            throw new IllegalArgumentException("Column index out of range: " + columnIndex);
        }
    }

    private void defaultCommitOptions() {
        boolean excludeNew = System.getProperty("netbeans.git.excludeNewFiles") != null; // NOI18N
        commitOptions = new CommitOptions[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            GitFileNode node = nodes[i];
            File file = node.getFile();
            if (GitModuleConfig.getDefault().isExcludedFromCommit(file.getAbsolutePath())) {
                commitOptions[i] = CommitOptions.EXCLUDE;
            } else {
                switch (node.getInformation().getStatus()) {
                    case StatusInfo.STATUS_VERSIONED_DELETEDLOCALLY:
                    case StatusInfo.STATUS_VERSIONED_REMOVEDLOCALLY:
                        commitOptions[i] = CommitOptions.COMMIT_REMOVE;
                        break;
                    default:
                        commitOptions[i] = CommitOptions.COMMIT;
                }
            }
        }
    }

    public GitFileNode getNode(int row) {
        return nodes[row];
    }

    public CommitOptions getOptions(int row) {
        return commitOptions[row];
    }

    void setRootFile(String repositoryPath, String rootLocalPath) {
        rootFile = new RootFile();
        rootFile.repositoryPath = repositoryPath;
        rootFile.rootLocalPath = rootLocalPath;
    }
}
