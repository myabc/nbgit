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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import org.netbeans.modules.versioning.util.TableSorter;
import org.netbeans.modules.versioning.util.Utils;
import org.openide.util.NbPreferences;
import org.openide.util.Utilities;
import org.eclipse.jgit.lib.FileBasedConfig;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryConfig;
import org.eclipse.jgit.lib.UserConfig;
import org.eclipse.jgit.util.SystemReader;

/**
 * Stores Git module configuration.
 *
 * @author alexbcoles
 */
public class GitModuleConfig {

    public static final String PROP_IGNORED_FILEPATTERNS = "ignoredFilePatterns"; // NOI18N
    public static final String PROP_COMMIT_EXCLUSIONS = "commitExclusions"; // NOI18N
    public static final String PROP_DEFAULT_VALUES = "defaultValues"; // NOI18N
    public static final String PROP_RUN_VERSION = "runVersion"; // NOI18N
    public static final String KEY_EXECUTABLE_BINARY = "gitExecBinary"; // NOI18N
    public static final String KEY_EXPORT_FILENAME = "gitExportFilename"; // NOI18N
    public static final String KEY_EXPORT_FOLDER = "gitExportFolder"; // NOI18N
    public static final String KEY_IMPORT_FOLDER = "gitImportFolder"; // NOI18N
    public static final String KEY_ANNOTATION_FORMAT = "annotationFormat"; // NOI18N
    public static final String SAVE_PASSWORD = "savePassword"; // NOI18N
    public static final String KEY_BACKUP_ON_REVERTMODS = "backupOnRevert"; // NOI18N
    public static final String KEY_SHOW_HISTORY_MERGES = "showHistoryMerges"; // NOI18N
    public static final String KEY_SIGN_OFF_COMMITS = "signOffCommits"; // NOI18N
    public static final String KEY_STRIP_SPACE = "stripSpace"; // NOI18N
    public static final String TEXT_ANNOTATIONS_FORMAT_DEFAULT = "{DEFAULT}"; // NOI18N
    private static final GitModuleConfig INSTANCE = new GitModuleConfig();
    private static String userEmail;
    private static String userName;

    static {
        FileBasedConfig baseConfig = SystemReader.getInstance().openUserConfig();
        try {
            baseConfig.load();
            UserConfig user = baseConfig.get(UserConfig.KEY);
            userEmail = user.getAuthorEmail();
            userName = user.getAuthorName();
        } catch (Throwable error) {
            userEmail = userName = "";
        }
    }

    public static GitModuleConfig getDefault() {
        return INSTANCE;
    }
    private Set<String> exclusions;
    // properties ~~~~~~~~~~~~~~~~~~~~~~~~~

    public Preferences getPreferences() {
        return NbPreferences.forModule(GitModuleConfig.class);
    }

    public boolean isExcludedFromCommit(String path) {
        return getCommitExclusions().contains(path);
    }

    /**
     * @param paths collection of paths, of File.getAbsolutePath()
     */
    public void addExclusionPaths(Collection<String> paths) {
        Set<String> exclusions = getCommitExclusions();
        if (exclusions.addAll(paths)) {
            Utils.put(getPreferences(), PROP_COMMIT_EXCLUSIONS, new ArrayList<String>(exclusions));
        }
    }

    /**
     * @param paths collection of paths, File.getAbsolutePath()
     */
    public void removeExclusionPaths(Collection<String> paths) {
        Set<String> exclusions = getCommitExclusions();
        if (exclusions.removeAll(paths)) {
            Utils.put(getPreferences(), PROP_COMMIT_EXCLUSIONS, new ArrayList<String>(exclusions));
        }
    }

    public String getExecutableBinaryPath() {
        return getPreferences().get(KEY_EXECUTABLE_BINARY, ""); // NOI18N
    }

    public boolean getBackupOnRevertModifications() {
        return getPreferences().getBoolean(KEY_BACKUP_ON_REVERTMODS, true);
    }

    public void setBackupOnRevertModifications(boolean bBackup) {
        getPreferences().putBoolean(KEY_BACKUP_ON_REVERTMODS, bBackup);
    }

    public boolean getSignOffCommits() {
        return getPreferences().getBoolean(KEY_SIGN_OFF_COMMITS, false);
    }

    public void setSignOffCommits(boolean signOff) {
        getPreferences().putBoolean(KEY_SIGN_OFF_COMMITS, signOff);
    }

    public boolean getStripSpace() {
        return getPreferences().getBoolean(KEY_STRIP_SPACE, false);
    }

    public void setStripSpace(boolean signOff) {
        getPreferences().putBoolean(KEY_STRIP_SPACE, signOff);
    }

    public boolean getShowHistoryMerges() {
        return getPreferences().getBoolean(KEY_SHOW_HISTORY_MERGES, true);
    }

    public void setShowHistoryMerges(boolean bShowMerges) {
        getPreferences().putBoolean(KEY_SHOW_HISTORY_MERGES, bShowMerges);
    }

    /**
     * This method returns the email address specified in $HOME/.gitconfig
     * or a default email address if none is found.
     */
    public String getEmail() {
        return userEmail;
    }

    /**
     * This method returns the username specified in $HOME/.gitconfig
     * or a default username if none is found.
     */
    public String getUserName() {
        return userName;
    }

    public void setEmail(String email) {
        userEmail = email;
    }

    public void setUserName(String name) {
        userName = name;
    }

    public Boolean isEmailValid(String email) {
        if (email.equals(userEmail)) {
            return true;
        }
        return email.indexOf("@") != -1;
    }

    public Boolean isUserNameValid(String name) {
        if (name.equals(userName)) {
            return true;
        }
        if (name.length() == 0) {
            return true;
        }
        return true;
    }

    public Properties getProperties(File file) {
        Properties props = new Properties();
        Repository repo = Git.getInstance().getRepository(file);
        RepositoryConfig config = repo.getConfig();

        props.setProperty("user.email", config.getAuthorEmail()); // NOI18N
        props.setProperty("user.name", config.getAuthorName()); // NOI18N

        boolean signOff = config.getBoolean("nbgit", "signoff", getSignOffCommits());
        props.setProperty("nbgit.signoff", signOff ? "yes" : "no"); // NOI18N

        boolean stripSpace = config.getBoolean("nbgit", "stripspace", getStripSpace());
        props.setProperty("nbgit.stripspace", stripSpace ? "yes" : "no"); // NOI18N

        return props;
    }

    public String getAnnotationFormat() {
        return getPreferences().get(KEY_ANNOTATION_FORMAT, getDefaultAnnotationFormat());
    }

    public String getDefaultAnnotationFormat() {
        return "[{" + GitAnnotator.ANNOTATION_STATUS + "} {" + GitAnnotator.ANNOTATION_FOLDER + "}]"; // NOI18N
    }

    public void setAnnotationFormat(String annotationFormat) {
        getPreferences().put(KEY_ANNOTATION_FORMAT, annotationFormat);
    }

    private synchronized Set<String> getCommitExclusions() {
        if (exclusions == null) {
            exclusions = new HashSet<String>(Utils.getStringList(getPreferences(), PROP_COMMIT_EXCLUSIONS));
        }
        return exclusions;
    }
}
