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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import org.netbeans.modules.git.ui.diff.Setup;
import org.netbeans.modules.git.util.GitCommand;
import org.netbeans.modules.git.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.netbeans.modules.versioning.spi.VersioningSupport;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * Main entry point for Git functionality, use getInstance() to get the Git object.
 * 
 * @author alexbcoles
 * @author Maros Sandor
 */
public class Git {
    
    public static final String GIT_OUTPUT_TAB_TITLE = org.openide.util.NbBundle.getMessage(Git.class, "CTL_Mercurial_DisplayName"); // NOI18N
    public static final String CHANGESET_STR = "changeset:"; // NOI18N
    
    //public static final int GIT_FETCH_20_REVISIONS = 20;
    //public static final int GIT_FETCH_50_REVISIONS = 50;
    //public static final int GIT_FETCH_ALL_REVISIONS = -1;
    //public static final int GIT_NUMBER_FETCH_OPTIONS = 3;
    //public static final int GIT_NUMBER_TO_FETCH_DEFAULT = 7;
 
    static final String PROP_ANNOTATIONS_CHANGED = "annotationsChanged"; // NOI18N
    static final String PROP_VERSIONED_FILES_CHANGED = "versionedFilesChanged"; // NOI18N
    public static final String PROP_CHANGESET_CHANGED = "changesetChanged"; // NOI18N
    
    public static final Logger LOG = Logger.getLogger("org.netbeans.modules.git"); // NOI18N
    
        private static final int STATUS_DIFFABLE =
            FileInformation.STATUS_VERSIONED_UPTODATE |
            FileInformation.STATUS_VERSIONED_MODIFIEDLOCALLY |
            FileInformation.STATUS_VERSIONED_MODIFIEDINREPOSITORY |
            FileInformation.STATUS_VERSIONED_CONFLICT |
            FileInformation.STATUS_VERSIONED_MERGE |
            FileInformation.STATUS_VERSIONED_REMOVEDINREPOSITORY |
            FileInformation.STATUS_VERSIONED_MODIFIEDINREPOSITORY |
            FileInformation.STATUS_VERSIONED_MODIFIEDINREPOSITORY;
    
    private static final String GIT_SUPPORTED_VERSION_153 = "1.5.3.7"; // NOI18N
    private static Git instance;
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    
    public static synchronized Git getInstance() {
        if (instance == null) {
            instance = new Git();
            instance.init();
        }
        return instance;
    }
    
    private GitAnnotator gitAnnotator;
    private GitInterceptor gitInterceptor;
    private FileStatusCache fileStatusCache;
    private HashMap<String, RequestProcessor> processorsToUrl;
    private boolean goodVersion;
    private String version;
    private String runVersion;
    private boolean checkedVersion;
    
    private Git() {
    }
    
    private void init() {
        checkedVersion = false;
        setDefaultPath();
        fileStatusCache = new FileStatusCache();
        gitAnnotator = new GitAnnotator();
        gitInterceptor = new GitInterceptor();
        checkVersion(); // Performs Git check, but postpones user query until menu activation.
    }

    private void setDefaultPath() {
        // Set default executable location for Git on Mac OS X
        if (System.getProperty("os.name").equals("Mac OS X")) { // NOI18N
            String defaultPath = GitModuleConfig.getDefault().getExecutableBinaryPath();
            if (defaultPath == null || defaultPath.length() == 0) {
                 String[] pathNames =   {"/Library/Frameworks/Python.framework/Versions/Current/bin", // NOI18N
                                               "/usr/bin", "/usr/local/bin","/opt/local/bin/", "/sw/bin"}; // NOI18N
               for (int i = 0; i < pathNames.length; i++) {
                   if (GitModuleConfig.getDefault().isExecPathValid(pathNames[i])) {
                       GitModuleConfig.getDefault().setExecutableBinaryPath(pathNames[i]); // NOI18N
                       break;
                   }
               }
            }
        }
        
    }
    
    private void checkVersion() {
        version = GitCommand.getGitVersion();
        LOG.log(Level.FINE, "version: {0}", version); // NOI18N
        if (version != null) {
            goodVersion = version.startsWith(GIT_SUPPORTED_VERSION_153);
            if (!goodVersion) {
                Preferences prefs = GitModuleConfig.getDefault().getPreferences();
                runVersion = prefs.get(GitModuleConfig.PROP_RUN_VERSION, null);
                if (runVersion != null && runVersion.equals(version)) {
                    goodVersion = true;
                }
            }
        } else {
            goodVersion = false;
        }
    }
  
    public void checkVersionNotify() {
        if (version != null && !goodVersion) {
            if (runVersion == null || !runVersion.equals(version)) {
                Preferences prefs = GitModuleConfig.getDefault().getPreferences();
                                NotifyDescriptor descriptor = new NotifyDescriptor.Confirmation(NbBundle.getMessage(Git.class, "MSG_VERSION_CONFIRM_QUERY", version)); // NOI18N
                descriptor.setTitle(NbBundle.getMessage(Git.class, "MSG_VERSION_CONFIRM")); // NOI18N
                descriptor.setMessageType(JOptionPane.WARNING_MESSAGE);
                descriptor.setOptionType(NotifyDescriptor.YES_NO_OPTION);

                Object res = DialogDisplayer.getDefault().notify(descriptor);
                OutputLogger logger = getLogger(Git.GIT_OUTPUT_TAB_TITLE);
                if (res == NotifyDescriptor.YES_OPTION) {
                    goodVersion = true;
                    prefs.put(GitModuleConfig.PROP_RUN_VERSION, version);
                    logger.outputInRed(NbBundle.getMessage(Git.class, "MSG_USING_VERSION_MSG", version)); // NOI18N);
                } else {
                    prefs.remove(GitModuleConfig.PROP_RUN_VERSION);
                    logger.outputInRed(NbBundle.getMessage(Git.class, "MSG_NOT_USING_VERSION_MSG", version)); // NOI18N);
                }
                logger.closeLog();
            } else {
                goodVersion = true;
            }
            
        } else if (version == null) {
            Preferences prefs = GitModuleConfig.getDefault().getPreferences();
            prefs.remove(GitModuleConfig.PROP_RUN_VERSION);
            OutputLogger logger = getLogger(Git.GIT_OUTPUT_TAB_TITLE);
            logger.outputInRed(NbBundle.getMessage(Git.class, "MSG_VERSION_NONE_OUTPUT_MSG")); // NOI18N);
            GitUtils.warningDialog(Git.class, "MSG_VERSION_NONE_TITLE", "MSG_VERSION_NONE_MSG");// NOI18N
            logger.closeLog();
        }
    }
    
    public GitAnnotator getGitAnnotator() {
        return gitAnnotator;
    }
    
    GitInterceptor getGitInterceptor() {
        return gitInterceptor;
    }
    
    /**
     * Gets the File Status Cache for the Git repository.
     * 
     * @return FileStatusCache for the repository
     */
    public FileStatusCache getFileStatusCache() {
        return fileStatusCache;
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
        if (GitUtils.isPartOfMercurialMetadata(file)) {
            for (;file != null; file = file.getParentFile()) {
                if (isAdministrative(file)) {
                    file = file.getParentFile();
                    break;
                }
            }
        }
        File topmost = null;
        for (;file != null; file = file.getParentFile()) {
            if (org.netbeans.modules.versioning.util.Utils.isScanForbidden(file)) break;
            if (new File(file, ".git").canWrite()){ // NOI18N
                topmost =  file;
                break;
            }
        }
        return topmost;
    }
    
    public GitFileNode[] getNodes(VCSContext context, int includeStatus) {
        File[] files = fileStatusCache.listFiles(context, includeStatus);
        GitFileNode[] nodes = new GitFileNode[files.length];
        for (int i = 0; i < files.length; i++) {
            nodes[i] = new GitFileNode(files[i]);
        }
        return nodes;
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
            foMime = "content/unknown"; // NOI18N
        } else {
            foMime = fo.getMIMEType();
            if ("content/unknown".equals(foMime)) { // NOI18N
                foMime = "text/plain"; // NOI18N
            }
        }
        if ((fileStatusCache.getStatus(file).getStatus() & FileInformation.STATUS_VERSIONED) == 0) {
            return GitUtils.isFileContentBinary(file) ? "application/octet-stream" : foMime; // NOI18N
        } else {
            return foMime;
        }
    }
    
    public boolean isGoodVersion() {
        return goodVersion;
    }
    
    public boolean isGoodVersionAndNotify() {
        if (checkedVersion == false) {
            checkVersionNotify();
            checkedVersion = true;
        }
        return goodVersion;
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
         FileInformation info = fileStatusCache.getStatus(workingCopy);
         LOG.log(Level.FINE, "getOriginalFile: {0} {1}", new Object[] {workingCopy, info}); // NOI18N
         if ((info.getStatus() & STATUS_DIFFABLE) == 0) return;
         
         // We can get status returned as UptoDate instead of LocallyNew
         // because refreshing of status after creation has been scheduled
         // but may not have happened yet.
         try {
            File original = VersionsCache.getInstance().getFileRevision(workingCopy, Setup.REVISION_BASE);
            if (original == null) return;
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
        if(processorsToUrl == null) {
            processorsToUrl = new HashMap<String, RequestProcessor>();
        }

        String key;
        if(url != null) {
            key = url;
        } else {
            key = "ANY_URL"; // NOI18N
        }

        RequestProcessor rp = processorsToUrl.get(key);
        if(rp == null) {
            rp = new RequestProcessor("Mercurial - " + key, 1, true); // NOI18N
            processorsToUrl.put(key, rp);
        }
        return rp;
    }
    
    public void clearRequestProcessor(String url) {
        if(processorsToUrl != null & url != null) {
             processorsToUrl.remove(url);
        }
    }

    /**
     *
     * @param repositoryRoot String of Mercurial repository so that logger writes to correct output tab. Can be null
     * in which case the logger will not print anything
     * @return OutputLogger logger to write to
     */
    public OutputLogger getLogger(String repositoryRoot) {
        return OutputLogger.getLogger(repositoryRoot);
    }
    
}