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

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import org.nbgit.GitModuleConfig;
import org.netbeans.modules.versioning.util.ListenersSupport;
import org.netbeans.modules.versioning.util.StringSelector;
import org.netbeans.modules.versioning.util.Utils;
import org.netbeans.modules.versioning.util.VersioningListener;
import org.openide.util.NbBundle;

/**
 *
 * @author  pk97937
 */
public class CommitPanel extends javax.swing.JPanel implements PreferenceChangeListener, TableModelListener {

    static final Object EVENT_SETTINGS_CHANGED = new Object();
    private CommitTable commitTable;

    /** Creates new form CommitPanel */
    public CommitPanel() {
        initComponents();
    }

    void setCommitTable(CommitTable commitTable) {
        this.commitTable = commitTable;
    }

    void setErrorLabel(String htmlErrorLabel) {
        jLabel2.setText(htmlErrorLabel);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        GitModuleConfig.getDefault().getPreferences().addPreferenceChangeListener(this);
        commitTable.getTableModel().addTableModelListener(this);
        listenerSupport.fireVersioningEvent(EVENT_SETTINGS_CHANGED);

        recentLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        recentLink.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                onBrowseRecentMessages();
            }
        });

        List<String> messages = Utils.getStringList(GitModuleConfig.getDefault().getPreferences(), CommitAction.RECENT_COMMIT_MESSAGES);
        if (messages.size() > 0) {
            messageTextArea.setText(messages.get(0));
        }

        Object signOffProp = getClientProperty(CommitAction.SIGN_OFF_MESSAGE);
        if (signOffProp != null && signOffProp instanceof String) {
            String signOff = (String) signOffProp;
            String message = messageTextArea.getText();
            if (!message.contains(signOff)) {
                if (!message.endsWith("\n"))
                    messageTextArea.append("\n");
                messageTextArea.append("\n" + signOff);
            }
        }

        messageTextArea.selectAll();
    }

    @Override
    public void removeNotify() {
        commitTable.getTableModel().removeTableModelListener(this);
        GitModuleConfig.getDefault().getPreferences().removePreferenceChangeListener(this);
        super.removeNotify();
    }

    private void onBrowseRecentMessages() {
        String message = StringSelector.select(NbBundle.getMessage(CommitPanel.class, "CTL_CommitForm_RecentTitle"), // NOI18N
                NbBundle.getMessage(CommitPanel.class, "CTL_CommitForm_RecentPrompt"), // NOI18N
                Utils.getStringList(GitModuleConfig.getDefault().getPreferences(), CommitAction.RECENT_COMMIT_MESSAGES));
        if (message != null) {
            messageTextArea.replaceSelection(message);
        }
    }

    public void preferenceChange(PreferenceChangeEvent evt) {
        if (evt.getKey().startsWith(GitModuleConfig.PROP_COMMIT_EXCLUSIONS)) {
            commitTable.dataChanged();
            listenerSupport.fireVersioningEvent(EVENT_SETTINGS_CHANGED);
        }
    }

    public void tableChanged(TableModelEvent e) {
        listenerSupport.fireVersioningEvent(EVENT_SETTINGS_CHANGED);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setMinimumSize(new java.awt.Dimension(400, 300));
        setPreferredSize(new java.awt.Dimension(650, 400));

        jLabel1.setLabelFor(messageTextArea);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(CommitPanel.class, "CTL_CommitForm_Message")); // NOI18N

        messageTextArea.setColumns(30);
        messageTextArea.setLineWrap(true);
        messageTextArea.setRows(6);
        messageTextArea.setTabSize(4);
        messageTextArea.setWrapStyleWord(true);
        messageTextArea.setMinimumSize(new java.awt.Dimension(100, 18));
        jScrollPane1.setViewportView(messageTextArea);
        messageTextArea.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(CommitPanel.class, "ACSN_CommitForm_Message")); // NOI18N
        messageTextArea.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CommitPanel.class, "ACSD_CommitForm_Message")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(filesLabel, org.openide.util.NbBundle.getMessage(CommitPanel.class, "CTL_CommitForm_FilesToCommit")); // NOI18N

        filesPanel.setPreferredSize(new java.awt.Dimension(240, 108));

        org.jdesktop.layout.GroupLayout filesPanelLayout = new org.jdesktop.layout.GroupLayout(filesPanel);
        filesPanel.setLayout(filesPanelLayout);
        filesPanelLayout.setHorizontalGroup(
            filesPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 610, Short.MAX_VALUE)
        );
        filesPanelLayout.setVerticalGroup(
            filesPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 173, Short.MAX_VALUE)
        );

        recentLink.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/nbgit/resources/icons/recent_messages.png"))); // NOI18N
        recentLink.setToolTipText(org.openide.util.NbBundle.getMessage(CommitPanel.class, "CTL_CommitForm_RecentMessages")); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, filesPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 610, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 610, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 482, Short.MAX_VALUE)
                        .add(recentLink))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, filesLabel)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jLabel2))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(recentLink))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(15, 15, 15)
                .add(filesLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(filesPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 173, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel2)
                .addContainerGap())
        );

        getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(CommitPanel.class, "ACSN_CommitDialog")); // NOI18N
        getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CommitPanel.class, "ACSD_CommitDialog")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    ListenersSupport listenerSupport = new ListenersSupport(this);
    public void addVersioningListener(VersioningListener listener) {
        listenerSupport.addListener(listener);
    }

    public void removeVersioningListener(VersioningListener listener) {
        listenerSupport.removeListener(listener);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    final javax.swing.JLabel filesLabel = new javax.swing.JLabel();
    final javax.swing.JPanel filesPanel = new javax.swing.JPanel();
    final javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
    final javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
    final javax.swing.JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
    final javax.swing.JTextArea messageTextArea = new javax.swing.JTextArea();
    final javax.swing.JLabel recentLink = new javax.swing.JLabel();
    // End of variables declaration//GEN-END:variables

}
