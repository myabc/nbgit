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
package org.netbeans.modules.git.util;

import java.io.File;
import java.util.Set;
import org.netbeans.modules.versioning.spi.VCSContext;

/**
 * 
 * @author alexbcoles
 */
public class GitRepositoryContextCache {
    
    private static boolean hasHistory;
    private static String pushDefault;
    private static String pullDefault;
    private static File root;
    
    private static VCSContext rootCtx;
    private static Set<File> historyCtxRootFiles;
    private static Set<File> pushCtxRootFiles;
    private static Set<File> pullCtxRootFiles;

    public static boolean hasHistory(VCSContext ctx) {
        Set<File> files;
        if (ctx == null) {
            return false;
        }
        files = ctx.getRootFiles();

        if (files.equals(historyCtxRootFiles)) {
            return hasHistory;
        } else {
            root = getRoot(ctx);
            hasHistory = GitCommand.hasHistory(root);
            historyCtxRootFiles = ctx.getRootFiles();
            return hasHistory;
        }

    }
        
    public static void setHasHistory(VCSContext ctx) {
        historyCtxRootFiles = ctx.getRootFiles();
        hasHistory = true;
    }

    public static void resetPullDefault() {
        pullCtxRootFiles = null;
    }

    public static String getPullDefault(VCSContext ctx) {
        Set<File> files;
        if (ctx == null) {
            return null;
        }
        files = ctx.getRootFiles();

        if (files.equals(pullCtxRootFiles)) {
            return pullDefault;
        } else {
            root = getRoot(ctx);
            pullDefault = GitCommand.getPullDefault(root);
            pullCtxRootFiles = ctx.getRootFiles();
            return pullDefault;
        }
    }
    
    public static void resetPushDefault() {
        pushCtxRootFiles = null;
    }

    public static String getPushDefault(VCSContext ctx) {
        Set<File> files;
        if (ctx == null) {
            return null;
        }
        files = ctx.getRootFiles();

        if (files.equals(pushCtxRootFiles)) {
            return pushDefault;
        } else {
            root = getRoot(ctx);
            pushDefault = GitCommand.getPushDefault(root);
            pushCtxRootFiles = ctx.getRootFiles();
            return pushDefault;
        }
    }
    
    private static File getRoot(VCSContext ctx) {
        if (ctx == rootCtx && root != null) {
            return root;
        } else {
            root = GitCommand.getRootFile(ctx);
            rootCtx = ctx;
            return root;
        }
    }
    
}