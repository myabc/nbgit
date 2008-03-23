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
package org.netbeans.modules.git.ui.update;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.git.util.GitCommand;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author  Padraig O'Briain
 */
public class UpdatePanel extends javax.swing.JPanel {

    private File                            repository;
    private RequestProcessor.Task           refreshViewTask;
    private static final RequestProcessor   rp = new RequestProcessor("MercurialUpdate", 1);  // NOI18N
    private Thread                          refreshViewThread;

    private static final int HG_REVERT_TARGET_LIMIT = 100;

    /** Creates new form ReverModificationsPanel */
     public UpdatePanel(File repo) {
        repository = repo;
        refreshViewTask = rp.create(new RefreshViewTask());
        initComponents();
        refreshViewTask.schedule(0);
    }

    public String getSelectedRevision() {
        String revStr = (String) revisionsComboBox.getSelectedItem();
        if(revStr != null){
            if (revStr.equals(NbBundle.getMessage(RevertModificationsPanel.class, "MSG_Revision_Default")) || // NOI18N
                revStr.equals(NbBundle.getMessage(RevertModificationsPanel.class, "MSG_Fetching_Revisions"))) { // NOI18N
                revStr = null;
            } else {
                revStr = revStr.substring(0, revStr.indexOf(" ")); // NOI18N
            }
        }
        return revStr;
    }

    public boolean isForcedUpdateRequested() {
        return forcedUpdateChxBox.isSelected();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        revisionsLabel = new javax.swing.JLabel();
        revisionsComboBox = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        forcedUpdateChxBox = new javax.swing.JCheckBox();

        revisionsLabel.setLabelFor(revisionsComboBox);
        org.openide.awt.Mnemonics.setLocalizedText(revisionsLabel, org.openide.util.NbBundle.getMessage(UpdatePanel.class, "UpdatePanel.revisionsLabel.text")); // NOI18N

        jLabel1.setFont(new java.awt.Font("Dialog", 1, 11));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(UpdatePanel.class, "UpdatePanel.infoLabel.text")); // NOI18N

        jLabel2.setForeground(new java.awt.Color(153, 153, 153));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(UpdatePanel.class, "UpdatePanel.infoLabel2.text")); // NOI18N

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(UpdatePanel.class, "UpdatePanel.jPanel1.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(forcedUpdateChxBox, org.openide.util.NbBundle.getMessage(UpdatePanel.class, "UpdatePanel.forcedUpdateChxBox.text")); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(forcedUpdateChxBox)
                .addContainerGap(120, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(forcedUpdateChxBox)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jLabel2)
                    .add(jLabel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 501, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(36, 36, 36)
                        .add(revisionsLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(revisionsComboBox, 0, 311, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(jLabel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(4, 4, 4)
                .add(jLabel2)
                .add(29, 29, 29)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(revisionsLabel)
                    .add(revisionsComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    

    /**
     * Must NOT be run from AWT.
     */
    private void setupModels() {
        // XXX attach Cancelable hook
        final ProgressHandle ph = ProgressHandleFactory.createHandle(NbBundle.getMessage(RevertModificationsPanel.class, "MSG_Refreshing_Update_Versions")); // NOI18N
        try {
            Set<String>  initialRevsSet = new LinkedHashSet<String>();
            initialRevsSet.add(NbBundle.getMessage(RevertModificationsPanel.class, "MSG_Fetching_Revisions")); // NOI18N
            ComboBoxModel targetsModel = new DefaultComboBoxModel(new Vector<String>(initialRevsSet));
            revisionsComboBox.setModel(targetsModel);
            refreshViewThread = Thread.currentThread();
            Thread.interrupted();  // clear interupted status
            ph.start();

            refreshRevisions();
        } finally {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    ph.finish();
                    refreshViewThread = null;
                }
            });
        }
    }

    private void refreshRevisions() {
        java.util.List<String> targetRevsList = GitCommand.getRevisions(repository, HG_REVERT_TARGET_LIMIT);

        Set<String>  targetRevsSet = new LinkedHashSet<String>();

        int size;
        if( targetRevsList == null){
            size = 0;
            targetRevsSet.add(NbBundle.getMessage(RevertModificationsPanel.class, "MSG_Revision_Default")); // NOI18N
        }else{
            size = targetRevsList.size();
            int i = 0 ;
            while(i < size){
                targetRevsSet.add(targetRevsList.get(i));
                i++;
            }
        }
        ComboBoxModel targetsModel = new DefaultComboBoxModel(new Vector<String>(targetRevsSet));
        revisionsComboBox.setModel(targetsModel);

        if (targetRevsSet.size() > 0 ) {
            revisionsComboBox.setSelectedIndex(0);
        }
    }

    private class RefreshViewTask implements Runnable {
        public void run() {
            setupModels();
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox forcedUpdateChxBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JComboBox revisionsComboBox;
    private javax.swing.JLabel revisionsLabel;
    // End of variables declaration//GEN-END:variables
    
}