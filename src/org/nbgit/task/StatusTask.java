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
package org.nbgit.task;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import org.nbgit.Git;
import org.nbgit.GitProgressSupport;
import org.nbgit.StatusCache;
import org.nbgit.StatusInfo;
import org.nbgit.util.GitCommand;
import org.nbgit.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;

/**
 * Connects to repository and gets recent status.
 */
public class StatusTask extends GitProgressSupport {

    private final VCSContext context;

    public StatusTask(VCSContext context) {
        this.context = context;
    }

    @Override
    final protected void perform() {
        if (context == null || context.getRootFiles().size() == 0) {
            return;
        }
        File repository = GitUtils.getRootFile(context);
        if (repository == null) {
            return;
        }
        StatusCache cache = Git.getInstance().getStatusCache();
        cache.refreshCached(context);

        for (File root : context.getRootFiles()) {
            if (isCanceled()) {
                return;
            }
            if (root.isDirectory()) {
                Map<File, StatusInfo> interestingFiles;
                interestingFiles = GitCommand.getInterestingStatus(repository, root);
                if (!interestingFiles.isEmpty()) {
                    Collection<File> files = interestingFiles.keySet();

                    Map<File, Map<File, StatusInfo>> interestingDirs =
                            GitUtils.getInterestingDirs(interestingFiles, files);

                    for (File file : files) {
                        if (isCanceled()) {
                            return;
                        }
                        StatusInfo fi = interestingFiles.get(file);

                        cache.refreshFileStatus(file, fi,
                                interestingDirs.get(file.isDirectory() ? file : file.getParentFile()));
                    }
                }
            } else {
                cache.refresh(root, StatusCache.REPOSITORY_STATUS_UNKNOWN);
            }
        }
        performAfter();
    }

    protected void performAfter() {
    }
}
