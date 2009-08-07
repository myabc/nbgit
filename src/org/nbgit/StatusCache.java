/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
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

import java.io.IOException;
import org.nbgit.util.GitUtils;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.nbgit.util.GitCommand;
import org.nbgit.util.exclude.Excludes;
import org.netbeans.modules.turbo.CustomProviders;
import org.netbeans.modules.turbo.Turbo;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.netbeans.modules.versioning.spi.VersioningSupport;
import org.netbeans.modules.versioning.util.Utils;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;

/**
 * Central part of status management, deduces and caches statuses of files under version control.
 *
 * @author Maros Sandor
 */
public class StatusCache {

    /**
     * Indicates that status of a file changed and listeners SHOULD check new status
     * values if they are interested in this file.
     * The New value is a ChangedEvent object (old StatusInfo object may be null)
     */
    public static final String PROP_FILE_STATUS_CHANGED = "status.changed"; // NOI18N
    /**
     * A special map saying that no file inside the folder is managed.
     */
    private static final Map<File, StatusInfo> NOT_MANAGED_MAP = new NotManagedMap();
    public static final File REPOSITORY_STATUS_UNKNOWN = null;  // Constant File objects that can be safely reused
    // Files that have a revision number cannot share StatusInfo objects
    private static final StatusInfo FILE_STATUS_EXCLUDED = new StatusInfo(StatusInfo.STATUS_NOTVERSIONED_EXCLUDED, false);
    private static final StatusInfo FILE_STATUS_EXCLUDED_DIRECTORY = new StatusInfo(StatusInfo.STATUS_NOTVERSIONED_EXCLUDED, true);
    private static final StatusInfo FILE_STATUS_UPTODATE_DIRECTORY = new StatusInfo(StatusInfo.STATUS_VERSIONED_UPTODATE, true);
    private static final StatusInfo FILE_STATUS_NOTMANAGED = new StatusInfo(StatusInfo.STATUS_NOTVERSIONED_NOTMANAGED, false);
    private static final StatusInfo FILE_STATUS_NOTMANAGED_DIRECTORY = new StatusInfo(StatusInfo.STATUS_NOTVERSIONED_NOTMANAGED, true);
    private static final StatusInfo FILE_STATUS_UNKNOWN = new StatusInfo(StatusInfo.STATUS_UNKNOWN, false);
    private static final StatusInfo FILE_STATUS_NEWLOCALLY = new StatusInfo(StatusInfo.STATUS_NOTVERSIONED_NEWLOCALLY, false);
    private static final StatusInfo FILE_STATUS_CONFLICT = new StatusInfo(StatusInfo.STATUS_VERSIONED_CONFLICT, false);
    private static final StatusInfo FILE_STATUS_REMOVEDLOCALLY = new StatusInfo(StatusInfo.STATUS_VERSIONED_REMOVEDLOCALLY, false);
    private PropertyChangeSupport listenerSupport = new PropertyChangeSupport(this);
    /**
     * Caches status of files in memory and on disk
     */
    private final Turbo turbo;
    private final String FILE_STATUS_MAP = DiskMapTurboProvider.ATTR_STATUS_MAP;
    private DiskMapTurboProvider cacheProvider;
    private Git git;
    private Set<FileSystem> filesystemsToRefresh;

    StatusCache(Git git) {
        this.git = git;
        cacheProvider = new DiskMapTurboProvider();
        turbo = Turbo.createCustom(new CustomProviders() {

            private final Set providers = Collections.singleton(cacheProvider);

            public Iterator providers() {
                return providers.iterator();
            }
        }, 200, 5000);
    }

    // --- Public interface -------------------------------------------------
    /**
     * Lists <b>modified files</b> and all folders that are known to be inside
     * this folder. There are locally modified files present
     * plus any files that exist in the folder in the remote repository.
     *
     * @param dir folder to list
     * @return
     */
    public File[] listFiles(File dir) {
        Set<File> files = getScannedFiles(dir, null).keySet();
        return files.toArray(new File[files.size()]);
    }

    /**
     * Lists <b>interesting files</b> that are known to be inside given folders.
     * These are locally and remotely modified and ignored files.
     *
     * <p>This method returns both folders and files.
     *
     * @param context context to examine
     * @param includeStatus limit returned files to those having one of supplied statuses
     * @return File [] array of interesting files
     */
    public File[] listFiles(VCSContext context, int includeStatus) {
        Set<File> set = new HashSet<File>();
        Map<File, StatusInfo> allFiles = cacheProvider.getAllModifiedValues();
        if (allFiles == null) {
            Git.LOG.log(Level.FINE, "StatusCache: listFiles(): allFiles == null"); // NOI18N
            return new File[0];
        }

        for (File file : allFiles.keySet()) {
            StatusInfo info = allFiles.get(file);
            if ((info.getStatus() & includeStatus) == 0) {
                continue;
            }
            Set<File> roots = context.getRootFiles();
            for (File root : roots) {
                if (VersioningSupport.isFlat(root)) {
                    if (file.equals(root) || file.getParentFile().equals(root)) {
                        set.add(file);
                        break;
                    }
                } else if (Utils.isAncestorOrEqual(root, file)) {
                    set.add(file);
                    break;
                }
            }
        }
        if (context.getExclusions().size() > 0) {
            for (File excluded : context.getExclusions()) {
                for (Iterator<File> j = set.iterator(); j.hasNext();) {
                    if (Utils.isAncestorOrEqual(excluded, j.next())) {
                        j.remove();
                    }
                }
            }
        }
        return set.toArray(new File[set.size()]);
    }

    /**
     * Lists <b>interesting files</b> that are known to be inside given folders.
     * These are locally and remotely modified and ignored files.
     *
     * <p>Comparing to CVS this method returns both folders and files.
     *
     * @param roots context to examine
     * @param includeStatus limit returned files to those having one of supplied statuses
     * @return File [] array of interesting files
     */
    public File[] listFiles(File[] roots, int includeStatus) {
        Set<File> set = new HashSet<File>();
        Map<File, StatusInfo> allFiles = cacheProvider.getAllModifiedValues();

        for (File file : allFiles.keySet()) {
            StatusInfo info = allFiles.get(file);
            if ((info.getStatus() & includeStatus) == 0) {
                continue;
            }
            for (int j = 0; j < roots.length; j++) {
                File root = roots[j];
                if (VersioningSupport.isFlat(root)) {
                    if (file.getParentFile().equals(root)) {
                        set.add(file);
                        break;
                    }
                } else if (Utils.isAncestorOrEqual(root, file)) {
                    set.add(file);
                    break;
                }
            }
        }
        return set.toArray(new File[set.size()]);
    }

    /**
     * Check if this context has at least one file with the passed in status
     *
     * @param context context to examine
     * @param includeStatus file status to check for
     * @return boolean true if this context contains at least one file with the includeStatus, false otherwise
     */
    public boolean containsFileOfStatus(VCSContext context, int includeStatus) {
        Map<File, StatusInfo> allFiles = cacheProvider.getAllModifiedValues();
        if (allFiles == null) {
            Git.LOG.log(Level.FINE, "containsFileOfStatus(): allFiles == null"); // NOI18N
            return false;
        }

        Set<File> roots = context.getRootFiles();
        Set<File> exclusions = context.getExclusions();
        Set<File> setAllFiles = allFiles.keySet();
        boolean bExclusions = exclusions != null && exclusions.size() > 0;
        boolean bContainsFile = false;

        for (File file : setAllFiles) {
            StatusInfo info = allFiles.get(file);
            if ((info.getStatus() & includeStatus) == 0) {
                continue;
            }
            for (File root : roots) {
                if (VersioningSupport.isFlat(root)) {
                    if (file.equals(root) || file.getParentFile().equals(root)) {
                        bContainsFile = true;
                        break;
                    }
                } else if (Utils.isAncestorOrEqual(root, file)) {
                    bContainsFile = true;
                    break;
                }
            }
            // Check it is not an excluded file
            if (bContainsFile && bExclusions) {
                for (File excluded : exclusions) {
                    if (!Utils.isAncestorOrEqual(excluded, file)) {
                        return true;
                    }
                }
            } else if (bContainsFile) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines the versioning status of a file. This method accesses disk and may block for a long period of time.
     *
     * @param file file to get status for
     * @return StatusInfo structure containing the file status
     * @see StatusInfo
     */
    public StatusInfo getStatus(File file) {
        if (file.isDirectory() && excludeFile(file)) {
            return StatusCache.FILE_STATUS_EXCLUDED_DIRECTORY;
        }
        File dir = file.getParentFile();
        if (dir == null) {
            return StatusCache.FILE_STATUS_NOTMANAGED;
        }
        Map files = getScannedFiles(dir, null);
        if (files == StatusCache.NOT_MANAGED_MAP) {
            return StatusCache.FILE_STATUS_NOTMANAGED;
        }
        StatusInfo fi = (StatusInfo) files.get(file);
        if (fi != null) {
            return fi;
        }
        if (!exists(file)) {
            return StatusCache.FILE_STATUS_UNKNOWN;
        }
        if (file.isDirectory()) {
            return refresh(file, REPOSITORY_STATUS_UNKNOWN);
        } else {
            return new StatusInfo(StatusInfo.STATUS_VERSIONED_UPTODATE, false);
        }
    }

    /**
     * Looks up cached file status.
     *
     * @param file file to check
     * @return give file's status or null if the file's status is not in cache
     */
    @SuppressWarnings("unchecked") // Need to change turbo module to remove warning at source
    StatusInfo getCachedStatus( File file, boolean bCheckSharability) {
        File parent = file.getParentFile();
        if (parent == null) {
            return StatusCache.FILE_STATUS_NOTMANAGED_DIRECTORY;
        }
        Map<File, StatusInfo> files = (Map<File, StatusInfo>) turbo.readEntry(parent, FILE_STATUS_MAP);
        StatusInfo fi = files != null ? files.get(file) : null;
        if (fi != null) {
            return fi;
        }
        if (file.isDirectory()) {
            if (excludeFile(file)) {
                return StatusCache.FILE_STATUS_EXCLUDED_DIRECTORY;
            } else {
                return StatusCache.FILE_STATUS_UPTODATE_DIRECTORY;
            }
        }
        return fi;
    }

    private StatusInfo refresh(File file, File repoStat, boolean forceChangeEvent) {
        Git.LOG.log(Level.FINE, "refresh(): {0}", file); // NOI18N
        File dir = file.getParentFile();
        if (dir == null) {
            return StatusCache.FILE_STATUS_NOTMANAGED;
        }
        Map<File, StatusInfo> files = getScannedFiles(dir, null); // Has side effect of updating the cache
        if (files == StatusCache.NOT_MANAGED_MAP && repoStat == StatusCache.REPOSITORY_STATUS_UNKNOWN) {
            return StatusCache.FILE_STATUS_NOTMANAGED;
        }
        StatusInfo current = files.get(file);

        StatusInfo fi = createFileInformation(file, true);

        if (StatusInfo.equivalent(fi, current)) {
            if (forceChangeEvent) {
                fireFileStatusChanged(file, current, fi);
            }
            return fi;
        }

        // do not include uptodate files into cache, missing directories must be included
        if (current == null && !fi.isDirectory() && fi.getStatus() == StatusInfo.STATUS_VERSIONED_UPTODATE) {
            if (forceChangeEvent) {
                fireFileStatusChanged(file, current, fi);
            }
            return fi;
        }

        file = FileUtil.normalizeFile(file);
        dir = FileUtil.normalizeFile(dir);
        Map<File, StatusInfo> newFiles = new HashMap<File, StatusInfo>(files);
        if (fi.getStatus() == StatusInfo.STATUS_UNKNOWN) {
            newFiles.remove(file);
            turbo.writeEntry(file, FILE_STATUS_MAP, null);  // remove mapping in case of directories
        } else if (fi.getStatus() == StatusInfo.STATUS_VERSIONED_UPTODATE && file.isFile()) {
            newFiles.remove(file);
        } else {
            newFiles.put(file, fi);
        }
        assert newFiles.containsKey(dir) == false;
        turbo.writeEntry(dir, FILE_STATUS_MAP, newFiles.size() == 0 ? null : newFiles);

        if (file.isDirectory() && needRecursiveRefresh(fi, current)) {
            File[] content = listFiles(file); // Has side effect of updating the cache
            for (int i = 0; i < content.length; i++) {
                refresh(content[i], StatusCache.REPOSITORY_STATUS_UNKNOWN);
            }
        }
        fireFileStatusChanged(file, current, fi);
        return fi;
    }

    private StatusInfo createFileInformation(File file, Boolean callStatus) {
        Git.LOG.log(Level.FINE, "createFileInformation(): {0} {1}", new Object[]{file, callStatus}); // NOI18N
        if (file == null) {
            return FILE_STATUS_UNKNOWN;
        }
        if (git.isAdministrative(file)) {
            return FILE_STATUS_EXCLUDED_DIRECTORY; // Excluded
        }
        File rootManagedFolder = git.getTopmostManagedParent(file);
        if (rootManagedFolder == null) {
            return FILE_STATUS_UNKNOWN; // Avoiding returning NOT_MANAGED dir or file
        }
        if (file.isDirectory()) {
            if (Excludes.isIgnored(file)) {
                return FILE_STATUS_EXCLUDED_DIRECTORY;
            } else {
                return FILE_STATUS_UPTODATE_DIRECTORY;
            }
        }
        if (callStatus == false) {
            if (Excludes.isIgnored(file)) {
                return FILE_STATUS_EXCLUDED;
            }
            return null;
        }

        return GitCommand.getSingleStatus(rootManagedFolder, file);
    }

    /**
     * Refreshes the status of the file given the repository status. Repository status is filled
     * in when this method is called while processing server output.
     *
     * <p>Note: it's not necessary if you use Subversion.getClient(), it
     * updates the cache automatically using onNotify(). It's not
     * fully reliable for removed files.
     *
     * @param file
     * @param repositoryStatus
     */
    public StatusInfo refresh(File file, File repositoryStatus) {
        return refresh(file, repositoryStatus, false);
    }

    @SuppressWarnings("unchecked") // Need to change turbo module to remove warning at source
    public Map<File, StatusInfo> getScannedFiles(File dir, Map<File, StatusInfo> interestingFiles) {
        Map<File, StatusInfo> files;

        files = (Map<File, StatusInfo>) turbo.readEntry(dir, FILE_STATUS_MAP);
        if (files != null) {
            return files;
        }
        if (!dir.exists() && interestingFiles == null) {
            return StatusCache.NOT_MANAGED_MAP;
        }
        dir = FileUtil.normalizeFile(dir);
        files = scanFolder(dir, interestingFiles);
        assert files.containsKey(dir) == false;
        turbo.writeEntry(dir, FILE_STATUS_MAP, files);
        if (interestingFiles == null) {
            for (File file : files.keySet()) {
                StatusInfo info = files.get(file);
                if ((info.getStatus() & (StatusInfo.STATUS_LOCAL_CHANGE | StatusInfo.STATUS_NOTVERSIONED_EXCLUDED)) != 0) {
                    fireFileStatusChanged(file, null, info);
                }
            }
        }
        return files;
    }

    public void refreshFileStatus(File file, StatusInfo fi, Map<File, StatusInfo> interestingFiles) {
        refreshFileStatus(file, fi, interestingFiles, false);
    }

    public void refreshFileStatus(File file, StatusInfo fi, Map<File, StatusInfo> interestingFiles, boolean alwaysFireEvent) {
        if (file == null || fi == null) {
            return;
        }
        File dir = file.getParentFile();
        if (dir == null) {
            return;
        }
        Map<File, StatusInfo> files = getScannedFiles(dir, interestingFiles);

        if (files == null || files == StatusCache.NOT_MANAGED_MAP) {
            return;
        }
        StatusInfo current = files.get(file);
        if (StatusInfo.equivalent(fi, current)) {
            if (StatusInfo.equivalent(FILE_STATUS_NEWLOCALLY, fi)) {
                if (Excludes.isIgnored(file)) {
                    Git.LOG.log(Level.FINE, "refreshFileStatus() file: {0} was LocallyNew but is NotSharable", file.getAbsolutePath()); // NOI18N
                    fi = FILE_STATUS_EXCLUDED;
                } else {
                    return;
                }
            } else if (!StatusInfo.equivalent(FILE_STATUS_REMOVEDLOCALLY, fi)) {
                return;
            }
        }
        if (StatusInfo.equivalent(FILE_STATUS_NEWLOCALLY, fi)) {
            if (StatusInfo.equivalent(FILE_STATUS_EXCLUDED, current)) {
                Git.LOG.log(Level.FINE, "refreshFileStatus() file: {0} was LocallyNew but is Excluded", file.getAbsolutePath()); // NOI18N
                return;
            } else if (current == null) {
                if (Excludes.isIgnored(file)) {
                    Git.LOG.log(Level.FINE, "refreshFileStatus() file: {0} was LocallyNew but current is null and is not NotSharable", file.getAbsolutePath()); // NOI18N
                    fi = FILE_STATUS_EXCLUDED;
                }
            }
        }
        file = FileUtil.normalizeFile(file);
        dir = FileUtil.normalizeFile(dir);
        Map<File, StatusInfo> newFiles = new HashMap<File, StatusInfo>(files);
        if (fi.getStatus() == StatusInfo.STATUS_UNKNOWN) {
            newFiles.remove(file);
            turbo.writeEntry(file, FILE_STATUS_MAP, null);  // remove mapping in case of directories
        } else if (fi.getStatus() == StatusInfo.STATUS_VERSIONED_UPTODATE && file.isFile()) {
            newFiles.remove(file);
        } else {
            newFiles.put(file, fi);
        }
        assert files.containsKey(dir) == false;
        turbo.writeEntry(dir, FILE_STATUS_MAP, newFiles);

        if (interestingFiles == null) {
            fireFileStatusChanged(file, current, fi);
        } else if (alwaysFireEvent) {
            fireFileStatusChanged(file, null, fi);
        }
        return;
    }

    private boolean needRecursiveRefresh(StatusInfo fi, StatusInfo current) {
        if (fi.getStatus() == StatusInfo.STATUS_NOTVERSIONED_EXCLUDED ||
                current != null && current.getStatus() == StatusInfo.STATUS_NOTVERSIONED_EXCLUDED) {
            return true;
        }
        if (fi.getStatus() == StatusInfo.STATUS_NOTVERSIONED_NOTMANAGED ||
                current != null && current.getStatus() == StatusInfo.STATUS_NOTVERSIONED_NOTMANAGED) {
            return true;
        }
        return false;
    }

    /**
     * Refreshes information about a given file or directory ONLY if its status is already cached. The
     * only exception are non-existing files (new-in-repository) whose statuses are cached in all cases.
     *
     * @param file
     * @param repositoryStatus
     */
    public void refreshCached(File file, File repositoryStatus) {
        refresh(file, repositoryStatus);
    }

    /**
     * Refreshes status of the specfied file or all files inside the
     * specified directory.
     *
     * @param file
     */
    public void refreshCached(File root) {
        if (!root.isDirectory()) {
            refresh(root, StatusCache.REPOSITORY_STATUS_UNKNOWN);
            return;
        }

        File repository = git.getTopmostManagedParent(root);
        if (repository == null) {
            return;
        }
        File roots[] = new File[1];
        roots[0] = root;
        File[] files = listFiles(roots, ~0);
        if (files.length == 0) {
            return;
        }
        Map<File, StatusInfo> allFiles;
        try {
            allFiles = GitCommand.getAllStatus(repository, root);
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                StatusInfo fi = allFiles.get(file);
                if (fi == null) // We have a file in the cache which seems to have disappeared
                {
                    refresh(file, StatusCache.REPOSITORY_STATUS_UNKNOWN);
                } else {
                    refreshFileStatus(file, fi, null);
                }
            }
        } catch (IOException ex) {
            Git.LOG.log(Level.FINE, "refreshCached() file: {0} {1} { 2} ", new Object[]{repository.getAbsolutePath(), root.getAbsolutePath(), ex.toString()}); // NOI18N
        }
    }

    /**
     * Refreshes status of all files inside given context.
     *
     * @param ctx context to refresh
     */
    public void refreshCached(VCSContext ctx) {
        for (File root : ctx.getRootFiles()) {
            refreshCached(root);
        }
    }

    public void addToCache(Set<File> files) {
        StatusInfo fi = new StatusInfo(StatusInfo.STATUS_NOTVERSIONED_NEWLOCALLY, null, false);
        HashMap<File, Map<File, StatusInfo>> dirMap = new HashMap<File, Map<File, StatusInfo>>(files.size());

        for (File file : files) {
            File parent = file.getParentFile();
            file = FileUtil.normalizeFile(file);
            parent = FileUtil.normalizeFile(parent);
            Map<File, StatusInfo> currentDirMap = dirMap.get(parent);
            if (currentDirMap == null) {
                // 20 is a guess at number of files in a directory
                currentDirMap = new HashMap<File, StatusInfo>(20);
                dirMap.put(parent, currentDirMap);
            }
            currentDirMap.put(file, fi);
        }
        for (File dir : dirMap.keySet()) {
            dir = FileUtil.normalizeFile(dir);
            Map<File, StatusInfo> currentDirMap = dirMap.get(dir);
            turbo.writeEntry(dir, FILE_STATUS_MAP, currentDirMap);
        }
    }
    // --- Package private contract ------------------------------------------
    Map<File, StatusInfo> getAllModifiedFiles() {
        return cacheProvider.getAllModifiedValues();
    }

    /**
     * Refreshes given directory and all subdirectories.
     *
     * @param dir directory to refresh
     */
    void directoryContentChanged(File dir) {
        Map originalFiles = (Map) turbo.readEntry(dir, FILE_STATUS_MAP);
        if (originalFiles != null) {
            for (Iterator i = originalFiles.keySet().iterator(); i.hasNext();) {
                File file = (File) i.next();
                refresh(file, StatusCache.REPOSITORY_STATUS_UNKNOWN);
            }
        }
    }

    // --- Private methods ---------------------------------------------------
    private boolean isNotManagedByDefault(File dir) {
        return !dir.exists();
    }

    /**
     * Scans all files in the given folder, computes and stores their CVS status.
     *
     * @param dir directory to scan
     * @return Map map to be included in the status cache (File => StatusInfo)
     */
    private Map<File, StatusInfo> scanFolder(File dir, Map<File, StatusInfo> interestingFiles) {
        File[] files = dir.listFiles();
        if (files == null) {
            if (interestingFiles == null) {
                files = new File[0];
            } else {
                files = interestingFiles.keySet().toArray(new File[interestingFiles.keySet().size()]);
            }
        }
        Map<File, StatusInfo> folderFiles = new HashMap<File, StatusInfo>(files.length);

        Git.LOG.log(Level.FINE, "scanFolder(): {0}", dir); // NOI18N
        if (git.isAdministrative(dir)) {
            folderFiles.put(dir, FILE_STATUS_EXCLUDED_DIRECTORY); // Excluded dir
            return folderFiles;
        }

        File rootManagedFolder = git.getTopmostManagedParent(dir);
        if (rootManagedFolder == null) {
            // Only interested in looking for Git managed dirs
            for (File file : files) {
                if (file.isDirectory() && git.getTopmostManagedParent(file) != null) {
                    if (excludeFile(file)) {
                        Git.LOG.log(Level.FINE, "scanFolder NotMng Ignored Dir {0}: exclude SubDir: {1}", // NOI18N
                                new Object[]{dir.getAbsolutePath(), file.getName()});
                        folderFiles.put(file, FILE_STATUS_EXCLUDED_DIRECTORY); // Excluded dir
                    } else {
                        Git.LOG.log(Level.FINE, "scanFolder NotMng Dir {0}: up to date Dir: {1}", // NOI18N
                                new Object[]{dir.getAbsolutePath(), file.getName()});
                        folderFiles.put(file, FILE_STATUS_UPTODATE_DIRECTORY);
                    }
                // Do NOT put any unmanaged dir's (FILE_STATUS_NOTMANAGED_DIRECTORY) or
                // files (FILE_STATUS_NOTMANAGED) into the folderFiles
                }
            }
            return folderFiles;
        }

        if (Excludes.isIgnored(dir)) {
            for (File file : files) {
                if (GitUtils.isPartOfGitMetadata(file)) {
                    continue;
                }
                if (file.isDirectory()) {
                    folderFiles.put(file, FILE_STATUS_EXCLUDED_DIRECTORY); // Excluded dir
                    Git.LOG.log(Level.FINE, "scanFolder Mng Ignored Dir {0}: exclude SubDir: {1}", // NOI18N
                            new Object[]{dir.getAbsolutePath(), file.getName()});
                } else {
                    Git.LOG.log(Level.FINE, "scanFolder Mng Ignored Dir {0}: exclude File: {1}", // NOI18N
                            new Object[]{dir.getAbsolutePath(), file.getName()});
                    folderFiles.put(file, FILE_STATUS_EXCLUDED);
                }
            }
            return folderFiles;
        }

        if (interestingFiles == null) {
            interestingFiles = GitCommand.getInterestingStatus(rootManagedFolder, dir);
        }
        if (interestingFiles == null) {
            return folderFiles;
        }
        for (File file : files) {
            if (GitUtils.isPartOfGitMetadata(file)) {
                continue;
            }
            if (file.isDirectory()) {
                if (excludeFile(file)) {
                    Git.LOG.log(Level.FINE, "scanFolder Mng Dir {0}: exclude Dir: {1}", // NOI18N
                            new Object[]{dir.getAbsolutePath(), file.getName()});
                    folderFiles.put(file, FILE_STATUS_EXCLUDED_DIRECTORY); // Excluded dir
                } else {
                    Git.LOG.log(Level.FINE, "scanFolder Mng Dir {0}: up to date Dir: {1}", // NOI18N
                            new Object[]{dir.getAbsolutePath(), file.getName()});
                    folderFiles.put(file, FILE_STATUS_UPTODATE_DIRECTORY);
                }
            } else {
                StatusInfo fi = interestingFiles.get(file);
                if (fi == null) // We have removed -i from GitCommand.getInterestingFiles
                // so we might have a file we should be ignoring
                {
                    fi = createFileInformation(file, false);
                }
                if (fi != null && fi.getStatus() != StatusInfo.STATUS_VERSIONED_UPTODATE) {
                    folderFiles.put(file, fi);
                }
            }
        }
        return folderFiles;
    }

    private boolean exists(File file) {
        if (!file.exists()) {
            return false;
        }
        return file.getAbsolutePath().equals(FileUtil.normalizeFile(file).getAbsolutePath());
    }

    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        listenerSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        listenerSupport.removePropertyChangeListener(listener);
    }

    private void fireFileStatusChanged(File file, StatusInfo oldInfo, StatusInfo newInfo) {
        listenerSupport.firePropertyChange(PROP_FILE_STATUS_CHANGED, null, new ChangedEvent(file, oldInfo, newInfo));
    }

    public void refreshDirtyFileSystems() {
        Set<FileSystem> filesystems = getFilesystemsToRefresh();
        FileSystem[] fstoRefresh = new FileSystem[filesystems.size()];
        synchronized (filesystems) {
            fstoRefresh = filesystems.toArray(new FileSystem[filesystems.size()]);
            filesystems.clear();
        }
        for (int i = 0; i < fstoRefresh.length; i++) {
            // don't call refresh() in synchronized (filesystems). It may lead to a deadlock.
            fstoRefresh[i].refresh(true);
        }
    }

    private Set<FileSystem> getFilesystemsToRefresh() {
        if (filesystemsToRefresh == null) {
            filesystemsToRefresh = new HashSet<FileSystem>();
        }
        return filesystemsToRefresh;
    }

    private boolean excludeFile(File file) {
        return git.isAdministrative(file) || Excludes.isIgnored(file);
    }

    private static final class NotManagedMap extends AbstractMap<File, StatusInfo> {

        public Set<Entry<File, StatusInfo>> entrySet() {
            return Collections.emptySet();
        }
    }

    public static class ChangedEvent {

        private File file;
        private StatusInfo oldInfo;
        private StatusInfo newInfo;

        public ChangedEvent(File file, StatusInfo oldInfo, StatusInfo newInfo) {
            this.file = file;
            this.oldInfo = oldInfo;
            this.newInfo = newInfo;
        }

        public File getFile() {
            return file;
        }

        public StatusInfo getOldInfo() {
            return oldInfo;
        }

        public StatusInfo getNewInfo() {
            return newInfo;
        }
    }
}
