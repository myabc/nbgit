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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.nbgit.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.netbeans.modules.versioning.spi.VersioningSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.RequestProcessor;
import org.spearce.jgit.lib.Repository;

/**
 * Main entry point for Git functionality, use getInstance() to get the Git object.
 *
 * @author alexbcoles
 * @author Maros Sandor
 */
public class Git {

    public static final String GIT_OUTPUT_TAB_TITLE = org.openide.util.NbBundle.getMessage(Git.class, "CTL_Git_DisplayName"); // NOI18N
    public static final String PROP_ANNOTATIONS_CHANGED = "annotationsChanged"; // NOI18N
    public static final String PROP_VERSIONED_FILES_CHANGED = "versionedFilesChanged"; // NOI18N
    public static final String PROP_CHANGESET_CHANGED = "changesetChanged"; // NOI18N
    public static final Logger LOG = Logger.getLogger("org.nbgit"); // NOI18N
    private static final int STATUS_DIFFABLE =
            StatusInfo.STATUS_VERSIONED_UPTODATE |
            StatusInfo.STATUS_VERSIONED_MODIFIEDLOCALLY |
            StatusInfo.STATUS_VERSIONED_MODIFIEDINREPOSITORY |
            StatusInfo.STATUS_VERSIONED_CONFLICT |
            StatusInfo.STATUS_VERSIONED_MERGE |
            StatusInfo.STATUS_VERSIONED_REMOVEDINREPOSITORY |
            StatusInfo.STATUS_VERSIONED_MODIFIEDINREPOSITORY |
            StatusInfo.STATUS_VERSIONED_MODIFIEDINREPOSITORY;
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private final StatusCache statusCache = new StatusCache(this);
    private HashMap<String, RequestProcessor> processorsToUrl;
    private final Map<File, Repository> repos = new HashMap<File, Repository>();
    private static Git instance;

    private Git() {
    }

    public static synchronized Git getInstance() {
        if (instance == null) {
            instance = new Git();
        }
        return instance;
    }

    public Repository getRepository(File root) {
        Repository repo = repos.get(root);

        if (repo == null) {
            final File gitDir = new File(root, GitRepository.GIT_DIR);
            try {
                repo = new Repository(gitDir);
                repos.put(root, repo);
            } catch (IOException ex) {
            }
        }

        return repo;
    }

    /**
     * Gets the Status Cache for the Git repository.
     *
     * @return StatusCache for the repository
     */
    public StatusCache getStatusCache() {
        return statusCache;
    }

    /**
     * Tests the <tt>.git</tt> directory itself.
     *
     * @param file
     * @return
     */
    public boolean isAdministrative(File file) {
        String name = file.getName();
        return isAdministrative(name) && file.isDirectory();
    }

    public boolean isAdministrative(String fileName) {
        return fileName.equals(".git"); // NOI18N
    }

    /**
     * Tests whether a file or directory should receive the STATUS_NOTVERSIONED_NOTMANAGED status.
     * All files and folders that have a parent with CVS/Repository file are considered versioned.
     *
     * @param file a file or directory
     * @return false if the file should receive the STATUS_NOTVERSIONED_NOTMANAGED status, true otherwise
     */
    public boolean isManaged(File file) {
        return VersioningSupport.getOwner(file) instanceof GitVCS && !GitUtils.isPartOfGitMetadata(file);
    }

    public File getTopmostManagedParent(File file) {
        if (GitUtils.isPartOfGitMetadata(file)) {
            for (; file != null; file = file.getParentFile()) {
                if (isAdministrative(file)) {
                    file = file.getParentFile();
                    break;
                }
            }
        }
        File topmost = null;
        for (; file != null; file = file.getParentFile()) {
            if (org.netbeans.modules.versioning.util.Utils.isScanForbidden(file)) {
                break;
            }
            if (new File(file, ".git").canWrite()) { // NOI18N
                topmost = file;
                break;
            }
        }
        return topmost;
    }

    /**
     * Uses content analysis to return the mime type for files.
     *
     * @param file file to examine
     * @return String mime type of the file (or best guess)
     */
    public String getMimeType(File file) {
        FileObject fo = FileUtil.toFileObject(file);
        String foMime;
        if (fo == null) {
            foMime = "content/unknown";
        } else {
            foMime = fo.getMIMEType();
            if ("content/unknown".equals(foMime)) // NOI18N
            {
                foMime = "text/plain";
            }
        }
        if ((statusCache.getStatus(file).getStatus() & StatusInfo.STATUS_VERSIONED) == 0) {
            return GitUtils.isFileContentBinary(file) ? "application/octet-stream" : foMime;
        } else {
            return foMime;
        }
    }

    public void versionedFilesChanged() {
        support.firePropertyChange(PROP_VERSIONED_FILES_CHANGED, null, null);
    }

    public void refreshAllAnnotations() {
        support.firePropertyChange(PROP_ANNOTATIONS_CHANGED, null, null);
    }

    public void changesetChanged(File repository) {
        support.firePropertyChange(PROP_CHANGESET_CHANGED, repository, null);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    public void getOriginalFile(File workingCopy, File originalFile) {
        StatusInfo info = statusCache.getStatus(workingCopy);
        LOG.log(Level.FINE, "getOriginalFile: {0} {1}", new Object[]{workingCopy, info}); // NOI18N
        if ((info.getStatus() & STATUS_DIFFABLE) == 0) {
            return;        // We can get status returned as UptoDate instead of LocallyNew
        // because refreshing of status after creation has been scheduled
        // but may not have happened yet.
        }
        try {
            File original = GitUtils.getFileRevision(workingCopy, GitRepository.REVISION_BASE);
            if (original == null) {
                return;
            }
            org.netbeans.modules.versioning.util.Utils.copyStreamsCloseAll(new FileOutputStream(originalFile), new FileInputStream(original));
            original.delete();
        } catch (IOException e) {
            Logger.getLogger(Git.class.getName()).log(Level.INFO, "Unable to get original file", e); // NOI18N
        }

    }

    /**
     * Serializes all Git requests (moves them out of AWT).
     */
    public RequestProcessor getRequestProcessor() {
        return getRequestProcessor((String) null);
    }

    /**
     * Serializes all Git requests (moves them out of AWT).
     */
    public RequestProcessor getRequestProcessor(File file) {
        return getRequestProcessor(file.getAbsolutePath());
    }

    public RequestProcessor getRequestProcessor(String url) {
        if (processorsToUrl == null) {
            processorsToUrl = new HashMap<String, RequestProcessor>();
        }
        String key;
        if (url != null) {
            key = url;
        } else {
            key = "ANY_URL";
        }
        RequestProcessor rp = processorsToUrl.get(key);
        if (rp == null) {
            rp = new RequestProcessor("Git - " + key, 1, true); // NOI18N
            processorsToUrl.put(key, rp);
        }
        return rp;
    }

    public void clearRequestProcessor(String url) {
        if (processorsToUrl != null & url != null) {
            processorsToUrl.remove(url);
        }
    }
}
