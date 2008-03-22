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
package org.netbeans.modules.git.ui.clone;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.git.Git;
import org.netbeans.modules.git.GitProgressSupport;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;


/**
 *
 * @author Tomas Stupka
 */
public class CloneCompleted implements ActionListener {

    private final File workingFolder;
    private final boolean openProject;

    private CloneCompletedPanel panel;
    private Dialog dialog;
    private Project projectToBeOpened;

    public CloneCompleted(File workingFolder) {
        this.openProject = true;
        this.workingFolder = workingFolder;
    }

    public void scanForProjects(GitProgressSupport support) {

        List<Project> clonedProjects = new LinkedList<Project>();
        File normalizedWorkingFolder = FileUtil.normalizeFile(workingFolder);
        FileObject fo = FileUtil.toFileObject(normalizedWorkingFolder);
        if (fo != null) {
            clonedProjects = ProjectUtilities.scanForProjects(fo);
        }

        panel = new CloneCompletedPanel();
        panel.openButton.addActionListener(this);
        panel.createButton.addActionListener(this);
        panel.closeButton.addActionListener(this);
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.againCheckBox.setVisible(openProject == false);
        String title = NbBundle.getMessage(CloneAction.class, "BK3008"); // NOI18N
        DialogDescriptor descriptor = new DialogDescriptor(panel, title);
        descriptor.setModal(true);

        // move buttons from dialog to descriptor
        panel.remove(panel.openButton);
        panel.remove(panel.createButton);
        panel.remove(panel.closeButton);

        Object[] options = null;
        if (clonedProjects.size() > 1) {
            String msg = NbBundle.getMessage(CloneAction.class, "BK3009", new Integer(clonedProjects.size()));   // NOI18N
            panel.jLabel1.setText(msg);
            options = new Object[]{panel.openButton, panel.closeButton};
        } else if (clonedProjects.size() == 1) {
            Project project = (Project) clonedProjects.iterator().next();
            projectToBeOpened = project;
            ProjectInformation projectInformation = ProjectUtils.getInformation(project);
            String projectName = projectInformation.getDisplayName();
            String msg = NbBundle.getMessage(CloneAction.class, "BK3011", projectName);                              // NOI18N
            panel.jLabel1.setText(msg);
            panel.openButton.setText(NbBundle.getMessage(CloneAction.class, "BK3012"));                              // NOI18N
            options = new Object[]{panel.openButton, panel.closeButton};
        } else {
            String msg = NbBundle.getMessage(CloneAction.class, "BK3010");                                           // NOI18N
            panel.jLabel1.setText(msg);
            options = new Object[]{panel.createButton, panel.closeButton};
        }

        descriptor.setMessageType(DialogDescriptor.INFORMATION_MESSAGE);
        descriptor.setOptions(options);
        descriptor.setClosingOptions(options);
        descriptor.setHelpCtx(new HelpCtx(CloneCompletedPanel.class));
        dialog = DialogDisplayer.getDefault().createDialog(descriptor);
        dialog.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(CloneAction.class, "ACSD_CloneCompleted_Dialog")); // NOI18N
        if (support != null && support.isCanceled()) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                dialog.setVisible(true);
            }
        });
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        dialog.setVisible(false);
        if (panel.openButton.equals(src)) {
            // show project chooser
            if (projectToBeOpened == null) {
                JFileChooser chooser = ProjectChooser.projectChooser();
                chooser.setCurrentDirectory(workingFolder);
                chooser.setMultiSelectionEnabled(true);
                chooser.showOpenDialog(dialog);
                File[] projectDirs = chooser.getSelectedFiles();
                for (int i = 0; i < projectDirs.length; i++) {
                    File projectDir = projectDirs[i];
                    FileObject projectFolder = FileUtil.toFileObject(projectDir);
                    if (projectFolder != null) {
                        try {
                            if(projectFolder != null){
                                Project p = ProjectManager.getDefault().findProject(projectFolder);
                                openProject(p);
                            }
                        } catch (IOException e1) {
                            Throwable cause = new Throwable(NbBundle.getMessage(CloneAction.class, "BK1014", projectFolder));
                            e1.initCause(cause);
                            Git.LOG.log(Level.INFO, null, e1);
                        }
                    }
                }
            } else {
                openProject(projectToBeOpened);
            }
        } else if (panel.createButton.equals(src)) {
            ProjectUtilities.newProjectWizard(workingFolder);
        }
    }

    private void openProject(Project p) {
        if(p == null) return;
        
        Project[] projects = new Project[]{p};
        OpenProjects.getDefault().open(projects, false);

        // set as main project and expand
        OpenProjects.getDefault().setMainProject(p);        
        ProjectUtilities.selectAndExpandProject(p);
    }
}