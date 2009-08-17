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

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import org.openide.ErrorManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.NodeAction;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.Lookups;

/**
 * Visible in the Search History Diff view.
 *
 * @author Maros Sandor
 */
class RevisionNode extends AbstractNode {

    static final String COLUMN_NAME_NAME = "name"; // NOI18N
    static final String COLUMN_NAME_DATE = "date"; // NOI18N
    static final String COLUMN_NAME_USERNAME = "username"; // NOI18N
    static final String COLUMN_NAME_MESSAGE = "message"; // NOI18N
    private RepositoryRevision.Event event;
    private RepositoryRevision container;
    private String path;

    public RevisionNode(RepositoryRevision container, SearchHistoryPanel master) {
        super(new RevisionNodeChildren(container, master), Lookups.fixed(master, container));
        this.container = container;
        this.event = null;
        this.path = null;
        setName(container.getRevision() +
                NbBundle.getMessage(RevisionNode.class, "LBL_NumberOfChangedPaths", 0/*container.getLog().getChangedPaths().length*/));
        initProperties();
    }

    public RevisionNode(RepositoryRevision.Event revision, SearchHistoryPanel master) {
        super(Children.LEAF, Lookups.fixed(master, revision));
        this.path = revision.getChangedPath().getPath();
        this.event = revision;
        setName(revision.getName());
        initProperties();
    }

    RepositoryRevision.Event getRevision() {
        return event;
    }

    RepositoryRevision getContainer() {
        return container;
    }

    RepositoryRevision.Event getEvent() {
        return event;
    }

    @Override
    public String getShortDescription() {
        return path;
    }

    @Override
    public Action[] getActions(boolean context) {
        if (context) {
            return null;
        // TODO: reuse action code from SummaryView
        }
        if (event == null) {
            return new Action[]{
                        SystemAction.get(RevertModificationsAction.class)
                    };
        } else {
            return new Action[]{
                        new RollbackAction(),
                        SystemAction.get(RevertModificationsAction.class)
                    };
        }
    }

    private void initProperties() {
        Sheet sheet = Sheet.createDefault();
        Sheet.Set ps = Sheet.createPropertiesSet();

        ps.put(new DateProperty());
        ps.put(new UsernameProperty());
        ps.put(new MessageProperty());

        sheet.put(ps);
        setSheet(sheet);
    }

    private abstract class CommitNodeProperty<T> extends PropertySupport.ReadOnly<T> {

        protected CommitNodeProperty(String name, Class<T> type, String displayName, String shortDescription) {
            super(name, type, displayName, shortDescription);
        }

        @Override
        public String toString() {
            try {
                return getValue().toString();
            } catch (Exception e) {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
                return e.getLocalizedMessage();
            }
        }

        @Override
        public PropertyEditor getPropertyEditor() {
            try {
                return new RevisionPropertyEditor((String) getValue());
            } catch (Exception e) {
                return super.getPropertyEditor();
            }
        }
    }

    private class UsernameProperty extends CommitNodeProperty {

        @SuppressWarnings("unchecked")
        public UsernameProperty() {
            super(COLUMN_NAME_USERNAME, String.class, COLUMN_NAME_USERNAME, COLUMN_NAME_USERNAME);
        }

        public Object getValue() throws IllegalAccessException, InvocationTargetException {
            if (event == null) {
                return container.getAuthor();
            } else {
                return "";
            }
        }
    }

    private class DateProperty extends CommitNodeProperty {

        @SuppressWarnings("unchecked")
        public DateProperty() {
            super(COLUMN_NAME_DATE, String.class, COLUMN_NAME_DATE, COLUMN_NAME_DATE);
        }

        public Object getValue() throws IllegalAccessException, InvocationTargetException {
            if (event == null) {
                return DateFormat.getDateTimeInstance().format(container.getDate());
            } else {
                return "";
            }
        }
    }

    private class MessageProperty extends CommitNodeProperty {

        @SuppressWarnings("unchecked")
        public MessageProperty() {
            super(COLUMN_NAME_MESSAGE, String.class, COLUMN_NAME_MESSAGE, COLUMN_NAME_MESSAGE);
        }

        public Object getValue() throws IllegalAccessException, InvocationTargetException {
            if (event == null) {
                return container.getMessage();
            } else {
                return "";
            }
        }
    }

    private class RollbackAction extends AbstractAction {

        public RollbackAction() {
            putValue(Action.NAME, NbBundle.getMessage(RevisionNode.class, "CTL_Action_RollbackTo", // NOI18N
                    event.getLogInfoHeader().getRevision()));
        }

        public void actionPerformed(ActionEvent e) {
            SummaryView.rollback(event);
        }
    }

    private static class RevertModificationsAction extends NodeAction {

        protected void performAction(Node[] activatedNodes) {
            Set<RepositoryRevision.Event> events = new HashSet<RepositoryRevision.Event>();
            Set<RepositoryRevision> revisions = new HashSet<RepositoryRevision>();
            for (Node n : activatedNodes) {
                RevisionNode node = (RevisionNode) n;
                if (node.event != null) {
                    events.add(node.event);
                } else {
                    revisions.add(node.container);
                }
            }
            SearchHistoryPanel master = activatedNodes[0].getLookup().lookup(SearchHistoryPanel.class);
            SummaryView.revert(master, revisions.toArray(new RepositoryRevision[revisions.size()]), events.toArray(new RepositoryRevision.Event[events.size()]));
        }

        protected boolean enable(Node[] activatedNodes) {
            return true;
        }

        public String getName() {
            return NbBundle.getMessage(RevisionNode.class, "CTL_Action_RollbackChange"); // NOI18N
        }

        public HelpCtx getHelpCtx() {
            return new HelpCtx(RevertModificationsAction.class);
        }
    }

    private static class RevisionPropertyEditor extends PropertyEditorSupport {

        private static final JLabel renderer = new JLabel();


        static {
            renderer.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        }

        public RevisionPropertyEditor(String value) {
            setValue(value);
        }

        @Override
        public void paintValue(Graphics gfx, Rectangle box) {
            renderer.setForeground(gfx.getColor());
            renderer.setText((String) getValue());
            renderer.setBounds(box);
            renderer.paint(gfx);
        }

        @Override
        public boolean isPaintable() {
            return true;
        }
    }
}
