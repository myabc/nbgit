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
package org.nbgit.ui.update;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import org.nbgit.Git;
import org.nbgit.GitProgressSupport;
import org.nbgit.OutputLogger;
import org.nbgit.util.GitCommand;
import org.nbgit.util.GitUtils;
import org.nbgit.ui.ContextAction;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * Update action for Git:
 * git update - update or merge working directory
 *
 * @author John Rice
 */
public class UpdateAction extends ContextAction {

    public UpdateAction(String name, VCSContext context) {
        super(name, context);
    }

    public void performAction(ActionEvent e) {
        update(context);
    }

    public static void update(final VCSContext ctx) {
        final File root = GitUtils.getRootFile(ctx);
        if (root == null) {
            return;
        }
        String repository = root.getAbsolutePath();
        String rev = null;

        final Update update = new Update(root);
        if (!update.showDialog()) {
            return;
        }
        rev = update.getSelectionRevision();
        final boolean doForcedUpdate = update.isForcedUpdateRequested();
        final String revStr = rev;

        RequestProcessor rp = Git.getInstance().getRequestProcessor(repository);
        GitProgressSupport support = new GitProgressSupport() {

            public void perform() {
                boolean bNoUpdates = true;
                OutputLogger logger = getLogger();

                logger.outputInRed(
                        NbBundle.getMessage(UpdateAction.class,
                        "MSG_UPDATE_TITLE")); // NOI18N
                logger.outputInRed(
                        NbBundle.getMessage(UpdateAction.class,
                        "MSG_UPDATE_TITLE_SEP")); // NOI18N
                logger.output(
                        NbBundle.getMessage(UpdateAction.class,
                        "MSG_UPDATE_INFO_SEP", revStr, root.getAbsolutePath())); // NOI18N
                List<String> list = GitCommand.doUpdateAll(root, doForcedUpdate, revStr);

                if (list != null && !list.isEmpty()) {
                    bNoUpdates = GitCommand.isNoUpdates(list.get(0));
                    //logger.clearOutput();
                    logger.output(list);
                    logger.output(""); // NOI18N
                }
                // refresh filesystem to take account of changes
                FileObject rootObj = FileUtil.toFileObject(root);
                try {
                    rootObj.getFileSystem().refresh(true);
                } catch (Exception ex) {
                }

                // Force Status Refresh from this dir and below
                if (!bNoUpdates) {
                    GitUtils.forceStatusRefreshProject(ctx);
                }
                logger.outputInRed(
                        NbBundle.getMessage(UpdateAction.class,
                        "MSG_UPDATE_DONE")); // NOI18N
                logger.output(""); // NOI18N
            }
        };
        support.start(rp, repository, org.openide.util.NbBundle.getMessage(UpdateAction.class, "MSG_Update_Progress")); // NOI18N
    }
}
