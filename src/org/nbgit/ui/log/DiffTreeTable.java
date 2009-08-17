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
package org.nbgit.ui.log;

import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeCellRenderer;
import org.openide.ErrorManager;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.TreeTableView;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;

/**
 * Treetable to show results of Search History action.
 *
 * @author Maros Sandor
 */
class DiffTreeTable extends TreeTableView {

    private RevisionsRootNode rootNode;
    private List results;
    private final SearchHistoryPanel master;

    public DiffTreeTable(SearchHistoryPanel master) {
        this.master = master;
        treeTable.setShowHorizontalLines(true);
        treeTable.setShowVerticalLines(false);
        setRootVisible(false);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        setupColumns();

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        renderer.setLeafIcon(null);
        tree.setCellRenderer(renderer);
    }

    @SuppressWarnings("unchecked")
    private void setupColumns() {
        Node.Property[] columns = new Node.Property[4];
        ResourceBundle loc = NbBundle.getBundle(DiffTreeTable.class);
        columns[0] = new ColumnDescriptor(RevisionNode.COLUMN_NAME_NAME, String.class, "", "");  // NOI18N
        columns[0].setValue("TreeColumnTTV", Boolean.TRUE); // NOI18N
        columns[1] = new ColumnDescriptor(RevisionNode.COLUMN_NAME_DATE, String.class, loc.getString("LBL_DiffTree_Column_Time"), loc.getString("LBL_DiffTree_Column_Time_Desc"));
        columns[2] = new ColumnDescriptor(RevisionNode.COLUMN_NAME_USERNAME, String.class, loc.getString("LBL_DiffTree_Column_Username"), loc.getString("LBL_DiffTree_Column_Username_Desc"));
        columns[3] = new ColumnDescriptor(RevisionNode.COLUMN_NAME_MESSAGE, String.class, loc.getString("LBL_DiffTree_Column_Message"), loc.getString("LBL_DiffTree_Column_Message_Desc"));
        setProperties(columns);
    }

    private void setDefaultColumnSizes() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                int width = getWidth();
                treeTable.getColumnModel().getColumn(0).setPreferredWidth(width * 25 / 100);
                treeTable.getColumnModel().getColumn(1).setPreferredWidth(width * 15 / 100);
                treeTable.getColumnModel().getColumn(2).setPreferredWidth(width * 10 / 100);
                treeTable.getColumnModel().getColumn(3).setPreferredWidth(width * 50 / 100);
            }
        });
    }

    void setSelection(int idx) {
        treeTable.getSelectionModel().setValueIsAdjusting(false);
        treeTable.scrollRectToVisible(treeTable.getCellRect(idx, 1, true));
        treeTable.getSelectionModel().setSelectionInterval(idx, idx);
    }

    void setSelection(RepositoryRevision container) {
        RevisionNode node = (RevisionNode) getNode(rootNode, container);
        if (node == null) {
            return;
        }
        ExplorerManager em = ExplorerManager.find(this);
        try {
            em.setSelectedNodes(new Node[]{node});
        } catch (PropertyVetoException e) {
            ErrorManager.getDefault().notify(e);
        }
    }

    void setSelection(RepositoryRevision.Event revision) {
        RevisionNode node = (RevisionNode) getNode(rootNode, revision);
        if (node == null) {
            return;
        }
        ExplorerManager em = ExplorerManager.find(this);
        try {
            em.setSelectedNodes(new Node[]{node});
        } catch (PropertyVetoException e) {
            ErrorManager.getDefault().notify(e);
        }
    }

    private Node getNode(Node node, Object obj) {
        Object object = node.getLookup().lookup(obj.getClass());
        if (obj.equals(object)) {
            return node;
        }
        Enumeration children = node.getChildren().nodes();
        while (children.hasMoreElements()) {
            Node child = (Node) children.nextElement();
            Node result = getNode(child, obj);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public int[] getSelection() {
        return treeTable.getSelectedRows();
    }

    public int getRowCount() {
        return treeTable.getRowCount();
    }

    private static class ColumnDescriptor<T> extends PropertySupport.ReadOnly<T> {

        public ColumnDescriptor(String name, Class<T> type, String displayName, String shortDescription) {
            super(name, type, displayName, shortDescription);
        }

        public T getValue() throws IllegalAccessException, InvocationTargetException {
            return null;
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        ExplorerManager em = ExplorerManager.find(this);
        em.setRootContext(rootNode);
        setDefaultColumnSizes();
    }

    public void setResults(List results) {
        this.results = results;
        rootNode = new RevisionsRootNode();
        ExplorerManager em = ExplorerManager.find(this);
        if (em != null) {
            em.setRootContext(rootNode);
        }
    }

    private class RevisionsRootNode extends AbstractNode {

        public RevisionsRootNode() {
            super(new RevisionsRootNodeChildren(), Lookups.singleton(results));
        }

        @Override
        public String getName() {
            return "revision"; // NOI18N
        }

        @Override
        public String getDisplayName() {
            return NbBundle.getMessage(DiffTreeTable.class, "LBL_DiffTree_Column_Name"); // NOI18N
        }

        @Override
        public String getShortDescription() {
            return NbBundle.getMessage(DiffTreeTable.class, "LBL_DiffTree_Column_Name_Desc"); // NOI18N
        }
    }

    private class RevisionsRootNodeChildren extends Children.Keys {

        public RevisionsRootNodeChildren() {
        }

        @Override
        protected void addNotify() {
            refreshKeys();
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void removeNotify() {
            setKeys(Collections.EMPTY_SET);
        }

        @SuppressWarnings("unchecked")
        private void refreshKeys() {
            setKeys(results);
        }

        protected Node[] createNodes(Object key) {
            RevisionNode node;
            if (key instanceof RepositoryRevision) {
                node = new RevisionNode((RepositoryRevision) key, master);
            } else // key instanceof RepositoryRevision.Event
            {
                node = new RevisionNode(((RepositoryRevision.Event) key), master);
            }
            return new Node[]{node};
        }
    }
}
