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

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.Action;
import org.netbeans.modules.git.FileInformation;
import org.netbeans.modules.git.FileStatusCache;
import org.netbeans.modules.git.Git;
import org.netbeans.modules.git.GitException;
import org.netbeans.modules.git.GitProgressSupport;
import org.netbeans.modules.git.OutputLogger;
import org.netbeans.modules.git.ui.actions.ContextAction;
import org.netbeans.modules.git.util.GitCommand;
import org.netbeans.modules.git.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * Reverts local changes.
 *
 * @author Padraig O'Briain
 */
public class RevertModificationsAction extends ContextAction {
    
    private final VCSContext context;
 
    public RevertModificationsAction(String name, VCSContext context) {        
        this.context =  context;
        putValue(Action.NAME, name);
    }

    public void performAction(ActionEvent e) {
        revert(context);
    }

    public static void revert(final VCSContext ctx) {
        final File[] files = ctx.getRootFiles().toArray(new File[ctx.getRootFiles().size()]);
        final File repository  = GitUtils.getRootFile(ctx);
        if (repository == null) return;
        String rev = null;

        final RevertModifications revertModifications = new RevertModifications(repository, files);
        if (!revertModifications.showDialog()) {
            return;
        }
        rev = revertModifications.getSelectionRevision();
        final String revStr = rev;
        final boolean doBackup = revertModifications.isBackupRequested();

        RequestProcessor rp = Git.getInstance().getRequestProcessor(repository);
        GitProgressSupport support = new GitProgressSupport() {
            public void perform() {
                performRevert(repository, revStr, files, doBackup, this.getLogger());
            }
        };
        support.start(rp, repository.getAbsolutePath(), org.openide.util.NbBundle.getMessage(UpdateAction.class, "MSG_Revert_Progress")); // NOI18N

        return;
    }

    public static void performRevert(File repository, String revStr, File file, boolean doBackup, OutputLogger logger) {
        List<File> revertFiles = new ArrayList<File>();
        revertFiles.add(file);        

        performRevert(repository, revStr, revertFiles, doBackup, logger);
    }
    
    public static void performRevert(File repository, String revStr, File[] files, boolean doBackup, OutputLogger logger) {
        List<File> revertFiles = new ArrayList<File>();
        for (File file : files) {
            revertFiles.add(file);
        }
        performRevert(repository, revStr, revertFiles, doBackup, logger);
    }
    
    public static void performRevert(File repository, String revStr, List<File> revertFiles, boolean doBackup, OutputLogger logger) {
        try{
            logger.outputInRed(
                    NbBundle.getMessage(RevertModificationsAction.class,
                    "MSG_REVERT_TITLE")); // NOI18N
            logger.outputInRed(
                    NbBundle.getMessage(RevertModificationsAction.class,
                    "MSG_REVERT_TITLE_SEP")); // NOI18N
            
            // revStr == null => no -r REV in git revert command
            // No revisions to revert too
            if (revStr != null && NbBundle.getMessage(RevertModificationsAction.class,
                    "MSG_Revision_Default").startsWith(revStr)) {
                logger.output(
                        NbBundle.getMessage(RevertModificationsAction.class,
                        "MSG_REVERT_NOTHING")); // NOI18N
                logger.outputInRed(
                        NbBundle.getMessage(RevertModificationsAction.class,
                        "MSG_REVERT_DONE")); // NOI18N
                logger.outputInRed(""); // NOI18N
                return;
            }
            
            logger.output(
                    NbBundle.getMessage(RevertModificationsAction.class,
                    "MSG_REVERT_REVISION_STR", revStr)); // NOI18N
            for (File file : revertFiles) {
                logger.output(file.getAbsolutePath());
            }
            logger.output(""); // NOI18N

            GitCommand.doCheckout(repository, revertFiles, revStr, doBackup, logger);
            FileStatusCache cache = Git.getInstance().getFileStatusCache();
            File[] conflictFiles = cache.listFiles(revertFiles.toArray(new File[0]), FileInformation.STATUS_VERSIONED_CONFLICT);
            if (conflictFiles.length != 0) {
                ConflictResolvedAction.conflictResolved(repository, conflictFiles);
            }
        } catch (GitException ex) {
            NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(ex);
            DialogDisplayer.getDefault().notifyLater(e);
        }

        if (revStr == null) {
            for (File file : revertFiles) {
                GitUtils.forceStatusRefresh(file);
            }
        } else {
            GitUtils.forceStatusRefresh(revertFiles.get(0));
        }

        // refresh filesystem to take account of changes
        FileObject rootObj = FileUtil.toFileObject(repository);
        try {
            rootObj.getFileSystem().refresh(true);
        } catch (java.lang.Exception exc) {
        }
        logger.outputInRed(
                NbBundle.getMessage(RevertModificationsAction.class,
                "MSG_REVERT_DONE")); // NOI18N
        logger.outputInRed(""); // NOI18N
 
    }

    public boolean isEnabled() {
        Set<File> ctxFiles = context != null? context.getRootFiles(): null;
        if(GitUtils.getRootFile(context) == null || ctxFiles == null || ctxFiles.size() == 0) 
            return false;
        return true; 
    }
}