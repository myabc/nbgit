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
package org.nbgit.util.exclude;

import java.io.File;
import java.util.HashMap;
import org.nbgit.Git;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.queries.SharabilityQuery;
import org.openide.filesystems.FileUtil;
import org.spearce.jgit.lib.Repository;

/**
 * Provides support for .gitignore files.
 *
 * To keep the querying interface fast, a cache of patterns are maintained.
 */
public class Excludes {

    private Excludes() {
    }

    private static final HashMap<Repository, ExcludeCache> cacheMap
            = new HashMap<Repository, ExcludeCache>();

    /**
     * Check if a file should can be shared between projects as defined by
     * NetBeans SharabilityQuery interface.
     *
     * @param file to check
     * @return true, if the file can be shared.
     */
    public static boolean isSharable(File file) {
        return SharabilityQuery.getSharability(file) != SharabilityQuery.NOT_SHARABLE;
    }

    /**
     * Checks to see if a file is ignored.
     *
     * @param file to check
     * @return true if the file is ignored
     */
    public static boolean isIgnored(File file) {
        return isIgnored(file, true);
    }

    public static boolean isIgnored(File file, boolean checkSharability) {
        if (file == null) {
            return false;
        }

        File topFile = Git.getInstance().getTopmostManagedParent(file);
        // We assume that the toplevel directory should not be ignored.
        if (topFile == null || topFile.equals(file)) {
            return false;        // We assume that the Project should not be ignored.
        }
        if (file.isDirectory()) {
            ProjectManager projectManager = ProjectManager.getDefault();
            if (projectManager.isProject(FileUtil.toFileObject(file))) {
                return false;
            }
        }

        Repository repo = Git.getInstance().getRepository(topFile);
        ExcludeCache cache = getCache(repo);
        if (cache.isExcluded(file))
            return true;

        if (file.getName().equals(".gitignore") ||
            file.getName().equals(".gitattributes") ||
            file.getName().equals(".gitmodules"))
            return false;

        if (checkSharability && !isSharable(file)) {
            return true;
        }
        return false;
    }


    private static ExcludeCache getCache(Repository repo) {
        ExcludeCache cache = cacheMap.get(repo);
        if (cache == null) {
            cache = ExcludeCache.create(repo);
            cacheMap.put(repo, cache);
        }
        return cache;
    }
}
