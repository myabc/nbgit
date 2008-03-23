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
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import org.netbeans.modules.git.config.GitConfigFiles;
import org.netbeans.modules.git.ui.repository.RepositoryConnection;
import org.netbeans.modules.git.util.GitCommand;
import org.netbeans.modules.versioning.util.TableSorter;
import org.netbeans.modules.versioning.util.Utils;
import org.openide.util.NbPreferences;

/**
 * Stores Git module configuration.
 * 
 * @author alexbcoles
 */
public class GitModuleConfig {
    
    public static final String PROP_IGNORED_FILEPATTERNS    = "ignoredFilePatterns";                        // NOI18N
    public static final String PROP_COMMIT_EXCLUSIONS       = "commitExclusions";                           // NOI18N
    public static final String PROP_DEFAULT_VALUES          = "defaultValues";                              // NOI18N
    public static final String PROP_RUN_VERSION             = "runVersion";                                 // NOI18N
    public static final String KEY_EXECUTABLE_BINARY        = "gitExecBinary";                              // NOI18N
    public static final String KEY_EXPORT_FILENAME          = "gitExportFilename";                          // NOI18N
    public static final String KEY_EXPORT_FOLDER            = "gitExportFolder";                          // NOI18N
    public static final String KEY_IMPORT_FOLDER            = "gitImportFolder";                          // NOI18N
    public static final String KEY_ANNOTATION_FORMAT        = "annotationFormat";                           // NOI18N
    public static final String SAVE_PASSWORD                = "savePassword";                               // NOI18N
    public static final String KEY_BACKUP_ON_REVERTMODS = "backupOnRevert";                               // NOI18N
    public static final String KEY_SHOW_HITORY_MERGES = "showHistoryMerges";                               // NOI18N

    private static final String RECENT_URL = "repository.recentURL";                                        // NOI18N
    private static final String SHOW_CLONE_COMPLETED = "cloneCompleted.showCloneCompleted";        // NOI18N  

    private static final String SET_MAIN_PROJECT = "cloneCompleted.setMainProject";        // NOI18N  

    private static final String URL_EXP = "annotator.urlExp";                                               // NOI18N
    private static final String ANNOTATION_EXP = "annotator.annotationExp";                                 // NOI18N
    
    public static final String TEXT_ANNOTATIONS_FORMAT_DEFAULT = "{DEFAULT}";                               // NOI18N           

    private static final String DEFAULT_EXPORT_FILENAME = "%b_%r_%h";                                  // NOI18N
    private static final GitModuleConfig INSTANCE = new GitModuleConfig();   
    
    private static String userName;

    public static GitModuleConfig getDefault() {
        return INSTANCE;
    }
    
    private Set<String> exclusions;
    
      // properties ~~~~~~~~~~~~~~~~~~~~~~~~~

    public Preferences getPreferences() {
        return NbPreferences.forModule(GitModuleConfig.class);
    }
    
    public boolean getShowCloneCompleted() {
        return getPreferences().getBoolean(SHOW_CLONE_COMPLETED, true);
    }
    
    public boolean getSetMainProject() {
        return getPreferences().getBoolean(SET_MAIN_PROJECT, true);
    }
    
    public Pattern [] getIgnoredFilePatterns() {
        return getDefaultFilePatterns();
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
        return (String) getPreferences().get(KEY_EXECUTABLE_BINARY, ""); // NOI18N
    }
    public boolean getBackupOnRevertModifications() {
        return getPreferences().getBoolean(KEY_BACKUP_ON_REVERTMODS, true);
    }
    
    public void setBackupOnRevertModifications(boolean bBackup) {
        getPreferences().putBoolean(KEY_BACKUP_ON_REVERTMODS, bBackup);
    }
    
    public boolean getShowHistoryMerges() {
        return getPreferences().getBoolean(KEY_SHOW_HITORY_MERGES, true);
    }

    public void setShowHistoryMerges(boolean bShowMerges) {
        getPreferences().putBoolean(KEY_SHOW_HITORY_MERGES, bShowMerges);
    }
    
    public void setExecutableBinaryPath(String path) {
        getPreferences().put(KEY_EXECUTABLE_BINARY, path);
    }

    public String getExportFolder() {
        return (String) getPreferences().get(KEY_EXPORT_FOLDER, System.getProperty("user.home")); // NOI18N
    }
    
    public void setExportFolder(String path) {
        getPreferences().put(KEY_EXPORT_FOLDER, path);
    }

    public String getImportFolder() {
        return (String) getPreferences().get(KEY_IMPORT_FOLDER, System.getProperty("user.home")); // NOI18N
    }
    
    public void setImportFolder(String path) {
        getPreferences().put(KEY_IMPORT_FOLDER, path);
    }

    public String getExportFilename() {
        String str = (String) getPreferences().get(KEY_EXPORT_FILENAME, ""); // NOI18N
        if (str.trim().length() == 0) str = DEFAULT_EXPORT_FILENAME;
        return str;
    }
    
    public void setExportFilename(String path) {
        getPreferences().put(KEY_EXPORT_FILENAME, path);
    }

    /**
     * This method returns the username specified in $HOME/.gitconfig
     * or a default username if none is found.
     */
    public String getUserName() {
        userName = GitConfigFiles.getInstance().getUserName();
        if (userName.length() == 0) {
            String userId = System.getProperty("user.name"); // NOI18N
            String hostName;
            try {
                hostName = InetAddress.getLocalHost().getHostName();
            } catch (Exception ex) {
                return userName;
            }
            userName = userId + " <" + userId + "@" + hostName + ">"; // NOI18N
        }
        return userName;
    }

    public void addGitkExtension() {
        GitConfigFiles.getInstance().setProperty("XXXXX", "");
    }
    
    public void setUserName(String name) {
        GitConfigFiles.getInstance().setUserName(name);
    }

    public Boolean isUserNameValid(String name) {
        if (userName == null) getUserName();
        if (name.equals(userName)) return true;
        if (name.length() == 0) return true;
        return GitMail.isUserNameValid(name);
    }

    public Boolean isExecPathValid(String name) {
        if (name.length() == 0) return true;
        File file = new File(name, GitCommand.GIT_COMMAND); // NOI18N
        // I would like to call canExecute but that requires Java SE 6.
        if(file.exists() && file.isFile()) return true;
        
        // TODO: add Legacy OS (Win32) support
        //file = new File(name, GitCommand.GIT_COMMAND + GitCommand.GIT_WINDOWS_EXE); // NOI18N
        return file.exists() && file.isFile();
    }

    public Properties getProperties(File file) {
        Properties props = new Properties();
        GitConfigFiles hgconfig = new GitConfigFiles(file); 
        String name = hgconfig.getUserName(false);
        if (name.length() == 0) 
            name = getUserName();
        if (name.length() > 0) 
            props.setProperty("username", name); // NOI18N
        else
            props.setProperty("username", ""); // NOI18N
        name = hgconfig.getDefaultPull(false);
        if (name.length() > 0) 
            props.setProperty("default-pull", name); // NOI18N
        else
            props.setProperty("default-pull", ""); // NOI18N
        name = hgconfig.getDefaultPush(false);
        if (name.length() > 0) 
            props.setProperty("default-push", name); // NOI18N
        else
            props.setProperty("default-push", ""); // NOI18N
        return props;
    }

    public void clearProperties(File file, String section) {
        getHgConfigFiles(file).clearProperties(section);
    }

    public void removeProperty(File file, String section, String name) {
        getHgConfigFiles(file).removeProperty(section, name);
    }

    public void setProperty(File file, String name, String value) {
        getHgConfigFiles(file).setProperty(name, value);
    }

    public void setProperty(File file, String section, String name, String value, boolean allowEmpty) {
        getHgConfigFiles(file).setProperty(section, name, value, allowEmpty);
    }

    public void setProperty(File file, String section, String name, String value) {
        getHgConfigFiles(file).setProperty(section, name, value);
    }

    /*
     * Get all properties for a particular section
     */
    public Properties getProperties(File file, String section) {
        return getHgConfigFiles(file).getProperties(section);
    }

    private GitConfigFiles getHgConfigFiles(File file) {
        if (file == null) {
            return GitConfigFiles.getInstance();
        } else {
            return new GitConfigFiles(file); 
        }
    }

    public String getAnnotationFormat() {
        return (String) getPreferences().get(KEY_ANNOTATION_FORMAT, getDefaultAnnotationFormat());                
    }
    
    public String getDefaultAnnotationFormat() {
        return "[{" + GitAnnotator.ANNOTATION_STATUS + "} {" + GitAnnotator.ANNOTATION_FOLDER + "}]"; // NOI18N
    }

    public void setAnnotationFormat(String annotationFormat) {
        getPreferences().put(KEY_ANNOTATION_FORMAT, annotationFormat);        
    }

    public boolean getSavePassword() {
        return getPreferences().getBoolean(SAVE_PASSWORD, true);
    }

    public void setSavePassword(boolean bl) {
        getPreferences().putBoolean(SAVE_PASSWORD, bl);
    }

    public void setShowCloneCompleted(boolean bl) {
        getPreferences().putBoolean(SHOW_CLONE_COMPLETED, bl);
    }
    
    public void setSetMainProject(boolean bl) {
        getPreferences().putBoolean(SET_MAIN_PROJECT, bl);
    }
    
    public RepositoryConnection getRepositoryConnection(String url) {
        List<RepositoryConnection> rcs = getRecentUrls();
        for (Iterator<RepositoryConnection> it = rcs.iterator(); it.hasNext();) {
            RepositoryConnection rc = it.next();
            if(url.equals(rc.getUrl())) {
                return rc;
            }            
        }
        return null;
    }            
    
    public void insertRecentUrl(RepositoryConnection rc) {        
        Preferences prefs = getPreferences();
        
        List<String> urlValues = Utils.getStringList(prefs, RECENT_URL);        
        for (Iterator<String> it = urlValues.iterator(); it.hasNext();) {
            String rcOldString = it.next();
            RepositoryConnection rcOld =  RepositoryConnection.parse(rcOldString);
            if(rcOld.equals(rc)) {
                Utils.removeFromArray(prefs, RECENT_URL, rcOldString);
            }
        }        
        Utils.insert(prefs, RECENT_URL, RepositoryConnection.getString(rc), -1);                
    }    

    public void setRecentUrls(List<RepositoryConnection> recentUrls) {
        List<String> urls = new ArrayList<String>(recentUrls.size());
       
        int idx = 0;
        for (Iterator<RepositoryConnection> it = recentUrls.iterator(); it.hasNext();) {
            idx++;
            RepositoryConnection rc = it.next();
            urls.add(RepositoryConnection.getString(rc));            
        }
        Preferences prefs = getPreferences();
        Utils.put(prefs, RECENT_URL, urls);            
    }
    
    public List<RepositoryConnection> getRecentUrls() {
        Preferences prefs = getPreferences();
        List<String> urls = Utils.getStringList(prefs, RECENT_URL);                
        List<RepositoryConnection> ret = new ArrayList<RepositoryConnection>(urls.size());
        for (Iterator<String> it = urls.iterator(); it.hasNext();) {
            RepositoryConnection rc = RepositoryConnection.parse(it.next());
            ret.add(rc);
        }
        return ret;
    }
            
    //public void setAnnotationExpresions(List<AnnotationExpression> exps) {
    //    List<String> urlExp = new ArrayList<String>(exps.size());
    //    List<String> annotationExp = new ArrayList<String>(exps.size());        
        
    //    int idx = 0;
    //    for (Iterator<AnnotationExpression> it = exps.iterator(); it.hasNext();) {
    //        idx++;
    //        AnnotationExpression exp = it.next();            
    //        urlExp.add(exp.getUrlExp());
    //        annotationExp.add(exp.getAnnotationExp());            
    //    }

    //    Preferences prefs = getPreferences();
    //    Utils.put(prefs, URL_EXP, urlExp);        
    //    Utils.put(prefs, ANNOTATION_EXP, annotationExp);                
    //}

    //public List<AnnotationExpression> getAnnotationExpresions() {
    //    Preferences prefs = getPreferences();
    //    List<String> urlExp = Utils.getStringList(prefs, URL_EXP);
    //    List<String> annotationExp = Utils.getStringList(prefs, ANNOTATION_EXP);        
              
    //    List<AnnotationExpression> ret = new ArrayList<AnnotationExpression>(urlExp.size());                
    //    for (int i = 0; i < urlExp.size(); i++) {                                        
    //        ret.add(new AnnotationExpression(urlExp.get(i), annotationExp.get(i)));
    //    }
    //    if(ret.size() < 1) {
    //        ret = getDefaultAnnotationExpresions();
    //    }
    //    return ret;
    //}

    //public List<AnnotationExpression> getDefaultAnnotationExpresions() {
    //    List<AnnotationExpression> ret = new ArrayList<AnnotationExpression>(1);
    //    ret.add(new AnnotationExpression(".*/(branches|tags)/(.+?)/.*", "\\2"));     // NOI18N 
    //    return ret;
    //}
    
    // TODO: persist state

    private TableSorter importTableSorter;
    private TableSorter commitTableSorter;
    
    public TableSorter getImportTableSorter() {
        return importTableSorter;        
    }

    public void setImportTableSorter(TableSorter sorter) {
        importTableSorter = sorter;        
    }

    public TableSorter getCommitTableSorter() {
        return commitTableSorter;
    }

    public void setCommitTableSorter(TableSorter sorter) {
        commitTableSorter = sorter;
    }
    
    // private methods ~~~~~~~~~~~~~~~~~~
    
    private synchronized Set<String> getCommitExclusions() {
        if (exclusions == null) {
            exclusions = new HashSet<String>(Utils.getStringList(getPreferences(), PROP_COMMIT_EXCLUSIONS));
        }
        return exclusions;
    }
    
    private static Pattern[] getDefaultFilePatterns() {
        return new Pattern [] {
                        Pattern.compile("cvslog\\..*"), // NOI18N
                        Pattern.compile("\\.make\\.state"), // NOI18N
                        Pattern.compile("\\.nse_depinfo"), // NOI18N
                        Pattern.compile(".*~"), // NOI18N
                        Pattern.compile("#.*"), // NOI18N
                        Pattern.compile("\\.#.*"), // NOI18N
                        Pattern.compile(",.*"), // NOI18N
                        Pattern.compile("_\\$.*"), // NOI18N
                        Pattern.compile(".*\\$"), // NOI18N
                        Pattern.compile(".*\\.old"), // NOI18N
                        Pattern.compile(".*\\.bak"), // NOI18N
                        Pattern.compile(".*\\.BAK"), // NOI18N
                        Pattern.compile(".*\\.orig"), // NOI18N
                        Pattern.compile(".*\\.rej"), // NOI18N
                        Pattern.compile(".*\\.del-.*"), // NOI18N
                        Pattern.compile(".*\\.a"), // NOI18N
                        Pattern.compile(".*\\.olb"), // NOI18N
                        Pattern.compile(".*\\.o"), // NOI18N
                        Pattern.compile(".*\\.obj"), // NOI18N
                        Pattern.compile(".*\\.so"), // NOI18N
                        Pattern.compile(".*\\.exe"), // NOI18N
                        Pattern.compile(".*\\.Z"), // NOI18N
                        Pattern.compile(".*\\.elc"), // NOI18N
                        Pattern.compile(".*\\.ln"), // NOI18N
                    };
    }
    
    
}