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
package org.netbeans.modules.git.ui.diff;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.netbeans.modules.git.Git;
import org.netbeans.modules.git.GitException;
import org.netbeans.modules.git.GitModuleConfig;
import org.netbeans.modules.git.GitProgressSupport;
import org.netbeans.modules.git.OutputLogger;
import org.netbeans.modules.git.ui.actions.ContextAction;
import org.netbeans.modules.git.util.GitCommand;
import org.netbeans.modules.git.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.netbeans.modules.versioning.util.AccessibleJFileChooser;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * ImportDiff action for Git: 
 * git apply (?) TODO: verify this is git apply
 * 
 * @author Padraig O'Briain
 */
public class ApplyDiffAction extends ContextAction {
    
    private final VCSContext context;

    public ApplyDiffAction(String name, VCSContext context) {
        this.context = context;
        putValue(Action.NAME, name);
    }
    
    public void performAction(ActionEvent e) {
        importDiff(context);
    }
    
    @Override
    public boolean isEnabled() {
        return GitUtils.getRootFile(context) != null;
    } 

    private static void importDiff(VCSContext ctx) {
        final File root = GitUtils.getRootFile(ctx);
        JFileChooser fileChooser = new AccessibleJFileChooser(NbBundle.getMessage(ApplyDiffAction.class, "ACSD_ImportBrowseFolder"), null);   // NO I18N
        fileChooser.setDialogTitle(NbBundle.getMessage(ApplyDiffAction.class, "ImportBrowse_title"));                                            // NO I18N
        fileChooser.setMultiSelectionEnabled(false);
        FileFilter[] old = fileChooser.getChoosableFileFilters();
        for (int i = 0; i < old.length; i++) {
            FileFilter fileFilter = old[i];
            fileChooser.removeChoosableFileFilter(fileFilter);

        }
       fileChooser.setCurrentDirectory(new File(GitModuleConfig.getDefault().getImportFolder()));

        if (fileChooser.showDialog(null, NbBundle.getMessage(ApplyDiffAction.class, "OK_Button")) == JFileChooser.APPROVE_OPTION) { // NO I18N
            final File patchFile = fileChooser.getSelectedFile();

            GitModuleConfig.getDefault().setImportFolder(patchFile.getParent());
            if (patchFile != null) {
                RequestProcessor rp = Git.getInstance().getRequestProcessor(root.getAbsolutePath());
                GitProgressSupport support = new GitProgressSupport() {
                    public void perform() {
                        OutputLogger logger = getLogger();
                        performImport(root, patchFile, logger);
                    }
                };
                support.start(rp, root.getAbsolutePath(), org.openide.util.NbBundle.getMessage(ApplyDiffAction.class, "LBL_ImportDiff_Progress")); // NOI18N
            }
        }
    }

    private static void performImport(File repository, File patchFile, OutputLogger logger) {
    try {
        logger.outputInRed(
                NbBundle.getMessage(ApplyDiffAction.class,
                "MSG_IMPORT_TITLE")); // NOI18N
        logger.outputInRed(
                NbBundle.getMessage(ApplyDiffAction.class,
                "MSG_IMPORT_TITLE_SEP")); // NOI18N

        List<String> list = GitCommand.doImport(repository, patchFile, logger);
        Git.getInstance().changesetChanged(repository);
        logger.output(list); // NOI18N

        } catch (GitException ex) {
            NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(ex);
            DialogDisplayer.getDefault().notifyLater(e);
        } finally {
            logger.outputInRed(NbBundle.getMessage(ApplyDiffAction.class, "MSG_IMPORT_DONE")); // NOI18N
            logger.output(""); // NOI18N
        }
    }
}