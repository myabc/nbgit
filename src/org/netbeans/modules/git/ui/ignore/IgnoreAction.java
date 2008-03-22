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
package org.netbeans.modules.git.ui.ignore;


import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.Action;
import org.netbeans.api.queries.SharabilityQuery;
import org.netbeans.modules.git.FileInformation;
import org.netbeans.modules.git.FileStatusCache;
import org.netbeans.modules.git.Git;
import org.netbeans.modules.git.GitProgressSupport;
import org.netbeans.modules.git.OutputLogger;
import org.netbeans.modules.git.ui.actions.ContextAction;
import org.netbeans.modules.git.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;


/**
 * Adds/removes files to repository .hgignore.
 *
 * @author Maros Sandor
 */
public class IgnoreAction extends ContextAction {
    
    private final VCSContext context;
    private int mActionStatus;
    public static final int UNDEFINED  = 0;
    public static final int IGNORING   = 1;
    public static final int UNIGNORING = 2;
    
    public IgnoreAction(String name, VCSContext context) {
        this.context = context;
        putValue(Action.NAME, name);
    }

    protected int getFileEnabledStatus() {
        return FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY | FileInformation.STATUS_NOTVERSIONED_EXCLUDED;
    }

    protected int getDirectoryEnabledStatus() {
        return FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY | FileInformation.STATUS_NOTVERSIONED_EXCLUDED;
    }
   
    public int getActionStatus(File [] files) {
        int actionStatus = -1;
        if (files.length == 0) return UNDEFINED; 
        FileStatusCache cache = Git.getInstance().getFileStatusCache();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().equals(".hg") || // NOI18N
                    files[i].isDirectory() ||
                    SharabilityQuery.getSharability(files[i])== SharabilityQuery.NOT_SHARABLE) { 
                actionStatus = UNDEFINED;
                break;
            }
            FileInformation info = cache.getStatus(files[i]);
            if (info.getStatus() == FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY) {
                if (actionStatus == UNIGNORING) {
                    actionStatus = UNDEFINED;
                    break;
                }
                actionStatus = IGNORING;
            } else if (info.getStatus() == FileInformation.STATUS_NOTVERSIONED_EXCLUDED) {
                if (actionStatus == IGNORING) {
                    actionStatus = UNDEFINED;
                    break;
                }
                actionStatus = UNIGNORING;
            } else {
                actionStatus = UNDEFINED;
                break;
            }
        }
        return actionStatus == -1 ? UNDEFINED : actionStatus;
    }
    
    public boolean isEnabled() {
        Set<File> ctxFiles = context != null? context.getRootFiles(): null;
        final File repository = GitUtils.getRootFile(context);
        if(repository == null || ctxFiles == null || ctxFiles.size() == 0) 
            return false;
        return true; 
    }

    public void performAction(ActionEvent e) {
        final File repository = GitUtils.getRootFile(context);
        if(repository == null) return;        
        Set<File> ctxFiles = context != null? context.getRootFiles(): null;
        if(ctxFiles == null || ctxFiles.size() == 0) return;        
        final File[] files = ctxFiles.toArray(new File[context.getRootFiles().size()]);

        RequestProcessor rp = Git.getInstance().getRequestProcessor(repository.getAbsolutePath());
        GitProgressSupport support = new GitProgressSupport() {
            public void perform() {
                OutputLogger logger = getLogger();
                try {
                    mActionStatus = getActionStatus(files);
                    if (mActionStatus == UNDEFINED) {
                        logger.outputInRed(
                                NbBundle.getMessage(IgnoreAction.class, "MSG_IGNORE_TITLE")); // NOI18N
                        logger.outputInRed(
                                NbBundle.getMessage(IgnoreAction.class, "MSG_IGNORE_TITLE_SEP")); // NOI18N
                        logger.output(
                                NbBundle.getMessage(IgnoreAction.class, "MSG_IGNORE_ONLY_LOCALLY_NEW")); // NOI18N
                        logger.outputInRed(
                                NbBundle.getMessage(IgnoreAction.class, "MSG_IGNORE_DONE")); // NOI18N
                        logger.output(""); // NOI18N
                        return;
                    }
        
                    if (mActionStatus == IGNORING) {
                        GitUtils.addIgnored(repository, files);
                        logger.outputInRed(
                                NbBundle.getMessage(IgnoreAction.class,
                                "MSG_IGNORE_TITLE")); // NOI18N
                        logger.outputInRed(
                                NbBundle.getMessage(IgnoreAction.class,
                                "MSG_IGNORE_TITLE_SEP")); // NOI18N
                        logger.output(
                                NbBundle.getMessage(IgnoreAction.class,
                                "MSG_IGNORE_INIT_SEP", repository.getName())); // NOI18N                          
                    } else {
                        GitUtils.removeIgnored(repository, files);
                        logger.outputInRed(
                                NbBundle.getMessage(IgnoreAction.class,
                                "MSG_UNIGNORE_TITLE")); // NOI18N
                        logger.outputInRed(
                                NbBundle.getMessage(IgnoreAction.class,
                                "MSG_UNIGNORE_TITLE_SEP")); // NOI18N
                        logger.output(
                                NbBundle.getMessage(IgnoreAction.class,
                                "MSG_UNIGNORE_INIT_SEP", repository.getName())); // NOI18N
                    }
                } catch (IOException ex) {
                   Git.LOG.log(Level.FINE, "IgnoreAction(): File {0} - {1}", // NOI18N
                        new Object[] {repository.getAbsolutePath(), ex.toString()});
                }
                // refresh files manually
                for (File file : files) {
                    Git.getInstance().getFileStatusCache().refresh(file, FileStatusCache.REPOSITORY_STATUS_UNKNOWN);
                    logger.output("\t" + file.getAbsolutePath()); // NOI18N
                }
                if (mActionStatus == IGNORING) {
                    logger.outputInRed(
                            NbBundle.getMessage(IgnoreAction.class,
                            "MSG_IGNORE_DONE")); // NOI18N
                } else {
                    logger.outputInRed(
                            NbBundle.getMessage(IgnoreAction.class,
                            "MSG_UNIGNORE_DONE")); // NOI18N
                }
                logger.output(""); // NOI18N
            }
        };
        support.start(rp, repository.getAbsolutePath(), org.openide.util.NbBundle.getMessage(IgnoreAction.class, "LBL_Ignore_Progress")); // NOI18N
    }
}