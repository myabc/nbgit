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
package org.nbgit;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Set;
import org.netbeans.modules.versioning.spi.VCSAnnotator;
import org.netbeans.modules.versioning.spi.VCSInterceptor;
import org.netbeans.modules.versioning.spi.VersioningSystem;
import org.netbeans.spi.queries.CollocationQueryImplementation;
import org.openide.util.NbBundle;

/**
 * Provides the Git VersioningSystem module.
 */
public class GitVCS extends VersioningSystem implements PropertyChangeListener, CollocationQueryImplementation {

    final private GitInterceptor gitInterceptor = new GitInterceptor();
    final private GitAnnotator gitAnnotator = new GitAnnotator();

    public GitVCS() {
        putProperty(PROP_DISPLAY_NAME, NbBundle.getMessage(GitVCS.class, "CTL_Git_DisplayName")); // NOI18N
        putProperty(PROP_MENU_LABEL, NbBundle.getMessage(GitVCS.class, "CTL_Git_MainMenu")); // NOI18N

        Git.getInstance().addPropertyChangeListener(this);
        Git.getInstance().getStatusCache().addPropertyChangeListener(this);
    }

    public boolean areCollocated(File a, File b) {
        File fra = getTopmostManagedAncestor(a);
        File frb = getTopmostManagedAncestor(b);

        return fra != null && fra.equals(frb);
    }

    public File findRoot(File file) {
        return getTopmostManagedAncestor(file);
    }

    /**
     * Tests whether the file is managed by this versioning system. If it is,
     * the method should return the topmost
     * ancestor of the file that is still versioned.
     *
     * @param file a file
     * @return File the file itself or one of its ancestors or null if the
     *  supplied file is NOT managed by this versioning system
     */
    @Override
    public File getTopmostManagedAncestor(File file) {
        return Git.getInstance().getTopmostManagedParent(file);
    }

    /**
     * Coloring label, modifying icons, providing action on file
     */
    @Override
    public VCSAnnotator getVCSAnnotator() {
        return gitAnnotator;
    }

    /**
     * Handle file system events such as delete, create, remove etc.
     */
    @Override
    public VCSInterceptor getVCSInterceptor() {
        return gitInterceptor;
    }

    @Override
    public void getOriginalFile(File workingCopy, File originalFile) {
        Git.getInstance().getOriginalFile(workingCopy, originalFile);
    }

    @SuppressWarnings("unchecked") // Property Change event.getNewValue returning Object
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(StatusCache.PROP_FILE_STATUS_CHANGED)) {
            StatusCache.ChangedEvent changedEvent = (StatusCache.ChangedEvent) event.getNewValue();
            fireStatusChanged(changedEvent.getFile());
        } else if (event.getPropertyName().equals(Git.PROP_ANNOTATIONS_CHANGED)) {
            fireAnnotationsChanged((Set<File>) event.getNewValue());
        } else if (event.getPropertyName().equals(Git.PROP_VERSIONED_FILES_CHANGED)) {
            fireVersionedFilesChanged();
        }
    }
}
