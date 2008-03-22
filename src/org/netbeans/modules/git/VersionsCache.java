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
package org.netbeans.modules.git;

import java.io.File;
import java.io.IOException;
import org.netbeans.modules.git.ui.diff.Setup;
import org.netbeans.modules.git.util.GitCommand;

/**
 * File revisions cache. It can access pristine files.
 *
 * XXX and what exactly is cached here?!
 * 
 * @author Petr Kuzel
 */
public class VersionsCache {
    
    private static VersionsCache instance;

    /** Creates a new instance of VersionsCache */
    private VersionsCache() {
    }

    public static synchronized VersionsCache getInstance() {
        if (instance == null) {
            instance = new VersionsCache();
        }
        return instance;
    }
    
    /**
     * Loads the file in specified revision.
     *
     * @return null if the file does not exist in given revision
     */
    public File getFileRevision(File base, String revision) throws IOException {
        if (revision.equals("-1")) {
            return null; // NOI18N

        }
        File repository = Git.getInstance().getTopmostManagedParent(base);
        if (Setup.REVISION_BASE.equals(revision)) {
            try {
                File tempFile = File.createTempFile("tmp", "-" + base.getName()); //NOI18N

                GitCommand.doCat(repository, base, tempFile, null);
                if (tempFile.length() == 0) {
                    return null;
                }
                return tempFile;
            } catch (GitException e) {
                IOException ioe = new IOException();
                ioe.initCause(e);
                throw ioe;
            }
        } else if (Setup.REVISION_CURRENT.equals(revision)) {
            return base;
        } else {
            try {
                File tempFile = File.createTempFile("tmp", "-" + base.getName()); //NOI18N

                GitCommand.doCat(repository, base, tempFile, revision, null);
                if (tempFile.length() == 0) {
                    return null;
                }
                return tempFile;
            } catch (GitException e) {
                IOException ioe = new IOException();
                ioe.initCause(e);
                throw ioe;
            }
        }
    }
    
}