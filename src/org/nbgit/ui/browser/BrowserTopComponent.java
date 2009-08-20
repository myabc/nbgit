/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Jonas Fonseca <fonseca@diku.dk>
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file.
 *
 * This particular file is subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
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
package org.nbgit.ui.browser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.util.ImageUtilities;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.revplot.PlotWalk;
import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.revwalk.RevObject;
import org.spearce.jgit.revwalk.RevSort;

/**
 * Repository browser top component.
 */
public final class BrowserTopComponent extends TopComponent {

    private static final String ICON_PATH = "org/nbgit/resources/icons/gitvcs-icon.png"; // NOI18N
    private static final String PREFERRED_ID = "BrowserTopComponent"; // NOI18N
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public BrowserTopComponent() {
        initComponents();
        setName(_("CTL_BrowserTopComponent")); // NOI18N
        setToolTipText(_("HINT_BrowserTopComponent")); // NOI18N
        setIcon(ImageUtilities.loadImage(ICON_PATH, true));
        setupComponents();
    }

    private void setupComponents() {
        graphScrollPane.setViewportView(commitGraphPane);
        commitGraphPane.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent event) {
                ListSelectionModel model = (ListSelectionModel) event.getSource();
                for (int i = event.getFirstIndex(); i <= event.getLastIndex(); i++) {
                    if (model.isSelectedIndex(i)) {
                        selected(i);
                    }
                }
            }

            private String formatIdent(PersonIdent ident) {
                return ident.getName() + " <" + ident.getEmailAddress() + "> " + // NOI18N
                        dateFormat.format(ident.getWhen()) + " " + // NOI18N
                        ident.getTimeZone().getDisplayName();
            }

            private void selected(int i) {
                RevObject obj = commitGraphPane.getCommitList().get(i);
                idField.setText(obj.name());
                if (obj instanceof RevCommit) {
                    StringBuilder str = new StringBuilder();
                    RevCommit commit = (RevCommit) obj;
                    str.append(_("Author", formatIdent(commit.getAuthorIdent()))). // NOI18N
                            append("\n"); // NOI18N
                    str.append(_("Committer", formatIdent(commit.getCommitterIdent()))). // NOI18N
                            append("\n"); // NOI18N
                    for (RevCommit parent : commit.getParents()) {
                        str.append(_("Parent", parent.name(), parent.getShortMessage())). // NOI18N
                                append("\n"); // NOI18N
                    }
                    str.append("\n"); // NOI18N
                    str.append(commit.getFullMessage());
                    textArea.setText(str.toString());
                }
            }
        });
    }

    public void show(final Repository repo, final String revision) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                PlotWalk walk = null;
                try {
                    ObjectId start = repo.resolve(revision);
                    walk = new PlotWalk(repo);
                    walk.sort(RevSort.BOUNDARY, true);
                    walk.markStart(walk.parseCommit(start));
                    commitGraphPane.getCommitList().source(walk);
                    commitGraphPane.getCommitList().fillTo(Integer.MAX_VALUE);
                } catch (Throwable error) {
                    Exceptions.printStackTrace(error);
                    textArea.setText(error.getMessage());
                } finally {
                    if (walk != null) {
                        walk.dispose();
                    }
                }
            }
        });
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }

    private static String _(String id, Object... args) {
        return NbBundle.getMessage(BrowserTopComponent.class, id, args);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        browserSplitPane.setDividerLocation(200);
        browserSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        browserSplitPane.setOneTouchExpandable(true);

        graphScrollPane.setViewportView(commitGraphPane);

        browserSplitPane.setTopComponent(graphScrollPane);

        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.setFocusable(false);
        toolBar.setRequestFocusEnabled(false);
        toolBar.setVerifyInputWhenFocusTarget(false);

        org.openide.awt.Mnemonics.setLocalizedText(idLabel, org.openide.util.NbBundle.getMessage(BrowserTopComponent.class, "BrowserTopComponent.idLabel.text")); // NOI18N
        toolBar.add(idLabel);

        idField.setEditable(false);
        idField.setText(org.openide.util.NbBundle.getMessage(BrowserTopComponent.class, "BrowserTopComponent.idField.text")); // NOI18N
        idField.setToolTipText(org.openide.util.NbBundle.getMessage(BrowserTopComponent.class, "BrowserTopComponent.idField.toolTipText")); // NOI18N
        idField.setPreferredSize(new java.awt.Dimension(120, 24));
        toolBar.add(idField);

        textArea.setColumns(20);
        textArea.setEditable(false);
        textArea.setRows(5);
        textScrollPane.setViewportView(textArea);

        org.jdesktop.layout.GroupLayout textPanelLayout = new org.jdesktop.layout.GroupLayout(textPanel);
        textPanel.setLayout(textPanelLayout);
        textPanelLayout.setHorizontalGroup(
            textPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(textScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)
            .add(toolBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)
        );
        textPanelLayout.setVerticalGroup(
            textPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(textPanelLayout.createSequentialGroup()
                .add(toolBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(textScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 63, Short.MAX_VALUE))
        );

        browserSplitPane.setRightComponent(textPanel);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(browserSplitPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 451, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(browserSplitPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private final javax.swing.JSplitPane browserSplitPane = new javax.swing.JSplitPane();
    private final org.spearce.jgit.awtui.CommitGraphPane commitGraphPane = new org.spearce.jgit.awtui.CommitGraphPane();
    private final javax.swing.JScrollPane graphScrollPane = new javax.swing.JScrollPane();
    private final javax.swing.JTextField idField = new javax.swing.JTextField();
    private final javax.swing.JLabel idLabel = new javax.swing.JLabel();
    private final javax.swing.JTextArea textArea = new javax.swing.JTextArea();
    private final javax.swing.JPanel textPanel = new javax.swing.JPanel();
    private final javax.swing.JScrollPane textScrollPane = new javax.swing.JScrollPane();
    private final javax.swing.JToolBar toolBar = new javax.swing.JToolBar();
    // End of variables declaration//GEN-END:variables
}
