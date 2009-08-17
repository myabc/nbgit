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
import org.nbgit.StatusInfo;
import org.nbgit.StatusCache;
import org.nbgit.Git;
import org.nbgit.GitProgressSupport;
import org.nbgit.ui.ContextAction;
import org.nbgit.util.GitCommand;
import org.nbgit.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * Allow files who have Conlfict Status to be manually resolved.
 *
 * @author Petr Kuzel
 */
public class ConflictResolvedAction extends ContextAction {

    public ConflictResolvedAction(String name, VCSContext context) {
        super(name, context);
    }

    public void performAction(ActionEvent e) {
        resolved(context);
    }

    public static void resolved(VCSContext ctx) {
        StatusCache cache = Git.getInstance().getStatusCache();
        File[] files = cache.listFiles(ctx, StatusInfo.STATUS_VERSIONED_CONFLICT);
        final File root = GitUtils.getRootFile(ctx);
        if (root == null || files == null || files.length == 0) {
            return;
        }
        conflictResolved(root, files);

        return;
    }

    @Override
    public boolean isEnabled() {
        StatusCache cache = Git.getInstance().getStatusCache();

        if (cache.listFiles(context, StatusInfo.STATUS_VERSIONED_CONFLICT).length != 0) {
            return true;
        }
        return false;
    }

    public static void conflictResolved(File repository, final File[] files) {
        if (repository == null || files == null || files.length == 0) {
            return;
        }
        RequestProcessor rp = Git.getInstance().getRequestProcessor(repository);
        GitProgressSupport support = new GitProgressSupport() {

            public void perform() {
                for (int i = 0; i < files.length; i++) {
                    if (isCanceled()) {
                        return;
                    }
                    File file = files[i];
                    ConflictResolvedAction.perform(file);
                }
            }
        };
        support.start(rp, repository.getAbsolutePath(), NbBundle.getMessage(ConflictResolvedAction.class, "MSG_ConflictResolved_Progress")); // NOI18N
    }

    private static void perform(File file) {
        if (file == null) {
            return;
        }
        StatusCache cache = Git.getInstance().getStatusCache();

        GitCommand.deleteConflictFile(file.getAbsolutePath());
        cache.refresh(file, StatusCache.REPOSITORY_STATUS_UNKNOWN);
    }

    public static void resolved(File file) {
        perform(file);
    }
}
