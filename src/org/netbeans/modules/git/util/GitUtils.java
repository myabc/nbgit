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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.queries.SharabilityQuery;
import org.netbeans.modules.git.FileInformation;
import org.netbeans.modules.git.FileStatusCache;
import org.netbeans.modules.git.Git;
import org.netbeans.modules.git.GitException;
import org.netbeans.modules.git.GitModuleConfig;
import org.netbeans.modules.git.ui.status.SyncFileNode;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;

/**
 *
 * @author jrice
 * @author alexbcoles
 */
public class GitUtils {

    private static final Pattern httpPasswordPattern = Pattern.compile("(https*://)(\\w+\\b):(\\b\\S*)@"); //NOI18N
    private static final String httpPasswordReplacementStr = "$1$2:\\*\\*\\*\\*@"; //NOI18N
    
    private static final Pattern metadataPattern = Pattern.compile(".*\\" + File.separatorChar + "(\\.)git(\\" + File.separatorChar + ".*|$)"); // NOI18N
    
    // IGNORE SUPPORT GIT: following file patterns are added to {Git repos}/.gitignore and Git will ignore any files
    // that match these patterns, reporting "I"status for them // NOI18N
    //private static final String [] GIT_IGNORE_FILES = { ".orig", "\\.orig\\..*$", "\\.chg\\..*$", ".rej", "\\.conflict\\~$"}; // NOI18N
    private static final String [] GIT_IGNORE_FILES = { ".orig" };
    private static final String GIT_IGNORE_ORIG_FILES = "\\.orig$"; // NOI18N
    private static final String GIT_IGNORE_ORIG_ANY_FILES = "\\.orig\\..*$"; // NOI18N
    private static final String GIT_IGNORE_CHG_ANY_FILES = "\\.chg\\..*$"; // NOI18N
    private static final String GIT_IGNORE_REJ_ANY_FILES = "\\.rej$"; // NOI18N
    private static final String GIT_IGNORE_CONFLICT_ANY_FILES = "\\.conflict\\~$"; // NOI18N
    
    private static final String FILENAME_GITIGNORE = ".gitignore"; // NOI18N

    private static HashMap<String, Set<Pattern>> ignorePatterns;

    /**
     * addDaysToDate - add days (+days) or subtract (-days) from the given date
     *
     * @param int days to add or substract
     * @return Date new date that has been calculated
     */
    public static Date addDaysToDate(Date date, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, days);
        return c.getTime();
    }
    
    /**
     * getTodaysDateStr - return todays date as a YYYY-MM-DD string
     *
     * @return String todays date YYYY-MM-DD string
     */
    public static String getTodaysDateStr() {
        Date todaysDate = new Date();
        return new SimpleDateFormat("yyyy-MM-dd").format(todaysDate); // NOI18N
    }
    
    /**
     * getLastWeeksDateStr - return last weeks date as a YYYY-MM-DD string
     *
     * @return String last weeks YYYY-MM-DD date string
     */
    public static String getLastWeeksDateStr() {
        Date lastWeeksDate = GitUtils.addDaysToDate(new Date(), -7);
        return new SimpleDateFormat("yyyy-MM-dd").format(lastWeeksDate); // NOI18N
    }
    
    /**
     * isSolaris - check you are running onthe Solaris OS
     *
     * @return boolean true - on Solaris, false - not on Solaris
     */
    public static boolean isSolaris(){
        return System.getProperty("os.name").equals("SunOS"); // NOI18N
    }

    /**
     * replaceHttpPassword - replace any http or https passwords in the string
     *
     * @return String modified string with **** instead of passwords
     */
    public static String replaceHttpPassword(String s){
        Matcher m = httpPasswordPattern.matcher(s);
        return m.replaceAll(httpPasswordReplacementStr); 
    }
    
    /**
     * replaceHttpPassword - replace any http or https passwords in the List<String>
     *
     * @return List<String> containing modified strings with **** instead of passwords
     */
    public static List<String> replaceHttpPassword(List<String> list){
        if(list == null) return null;

        List<String> out = new ArrayList<String>(list.size());
        for(String s: list){
            out.add(replaceHttpPassword(s));
        } 
        return out;
    }

    /**
     * isInUserPath - check if passed in name is on the Users PATH environment setting
     *
     * @param name to check
     * @return boolean true - on PATH, false - not on PATH
     */
    public static boolean isInUserPath(String name) {
        String pathEnv = System.getenv().get("PATH");// NOI18N
        // Work around issues on Windows fetching PATH
        if(pathEnv == null) pathEnv = System.getenv().get("Path");// NOI18N
        if(pathEnv == null) pathEnv = System.getenv().get("path");// NOI18N
        String pathSeparator = System.getProperty("path.separator");// NOI18N
        if (pathEnv == null || pathSeparator == null) return false;

        String[] paths = pathEnv.split(pathSeparator);
        for (String path : paths) {
            File f = new File(path, name);
            // On Windows isFile will fail on gitk.cmd use !isDirectory
            if (f.exists() && !f.isDirectory()) {
                return true;
            }
        }
        return false;
    }

    /**
     * confirmDialog - display a confirmation dialog
     *
     * @param bundleLocation location of string resources to display
     * @param title of dialog to display    
     * @param query ask user
     * @return boolean true - answered Yes, false - answered No
     */
    public static boolean confirmDialog(Class bundleLocation, String title, String query) {
        int response = JOptionPane.showOptionDialog(null, NbBundle.getMessage(bundleLocation, query), NbBundle.getMessage(bundleLocation, title), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);

        if (response == JOptionPane.YES_OPTION) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * warningDialog - display a warning dialog
     *
     * @param bundleLocation location of string resources to display
     * @param title of dialog to display    
     * @param warning to display to the user
     */
     public static void warningDialog(Class bundleLocation, String title, String warning) {
        JOptionPane.showMessageDialog(null,
                NbBundle.getMessage(bundleLocation,warning),
                NbBundle.getMessage(bundleLocation,title),
                JOptionPane.WARNING_MESSAGE);
    }
    

    /**
     * stripDoubleSlash - converts '\\' to '\' in path on Windows
     *
     * @param String path to convert
     * @return String converted path
     */
    public static String stripDoubleSlash(String path){
        if(Utilities.isWindows()){                       
            return path.replace("\\\\", "\\");
        }
        return path;
    }
    /**
     * isLocallyAdded - checks to see if this file has been Locally Added to Git
     *
     * @param file to check
     * @return boolean true - ignore, false - not ignored
     */
    public static boolean isLocallyAdded(File file){
        if (file == null) return false;
        Git git = Git.getInstance();        

        if ((git.getFileStatusCache().getStatus(file).getStatus() & FileInformation.STATUS_VERSIONED_ADDEDLOCALLY) !=0)
            return true;
        else
            return false;
    }
    
    private static void resetIgnorePatterns(File file) {
        if (ignorePatterns == null) {
            return;
        }
        String key = file.getAbsolutePath();
        ignorePatterns.remove(key);
    }

    private static Set<Pattern> getIgnorePatterns(File file) {
        if (ignorePatterns == null) {
            ignorePatterns = new HashMap<String, Set<Pattern>>();
        }
        String key = file.getAbsolutePath();
        Set<Pattern> patterns = ignorePatterns.get(key);
        if (patterns == null) {
            patterns = new HashSet<Pattern>(5);
            addIgnorePatterns(patterns, file);
            ignorePatterns.put(key, patterns);
        }
        return patterns;
    }

    /**
     * isIgnored - checks to see if this is a file Git should ignore
     *
     * @param File file to check
     * @return boolean true - ignore, false - not ignored
     */
    public static boolean isIgnored(File file){
        return isIgnored(file, true);
    }

    public static boolean isIgnored(File file, boolean checkSharability){
        if (file == null) return false;
        String path = file.getPath();
        String name = file.getName();
        File topFile = Git.getInstance().getTopmostManagedParent(file);
        
        // We assume that the toplevel directory should not be ignored.
        if (topFile == null || topFile.equals(file)) {
            return false;
        }
        
        // We assume that the Project should not be ignored.
        if(file.isDirectory()){
            ProjectManager projectManager = ProjectManager.getDefault();
            if (projectManager.isProject(FileUtil.toFileObject(file))) {
                return false;
            }
        }

        Set<Pattern> patterns = getIgnorePatterns(topFile);

        for (Iterator i = patterns.iterator(); i.hasNext();) {
            Pattern pattern = (Pattern) i.next();
            if (pattern.matcher(path).find()) {
                return true;
            }
        }

        if (FILENAME_GITIGNORE.equals(name)) return false;
        if (checkSharability) {
            int sharability = SharabilityQuery.getSharability(file);
            if (sharability == SharabilityQuery.NOT_SHARABLE) return true;
            }
        return false;
    }

    /**
     * createIgnored - creates .gitignore file in the repository in which 
     * the given file belongs. This .ignore file ensures Git will ignore 
     * the files specified in GIT_IGNORE_FILES list
     *
     * @param path to repository to place .gitignore file
     */
    public static void createIgnored(File path){
        if( path == null) return;
        BufferedWriter fileWriter = null;
        Git git = Git.getInstance();
        File root = git.getTopmostManagedParent(path);
        if( root == null) return;
        File ignore = new File(root, FILENAME_GITIGNORE);
        
        try     {
            if (!ignore.exists()) {
                fileWriter = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(ignore)));
                for (String name : GIT_IGNORE_FILES) {
                    fileWriter.write(name + "\n"); // NOI18N
                }
            }else{
                addToExistingIgnoredFile(ignore);
            }
        } catch (IOException ex) {
            Git.LOG.log(Level.FINE, "createIgnored(): File {0} - {1}",  // NOI18N
                    new Object[] {ignore.getAbsolutePath(), ex.toString()});
        }finally {
            try {
                if(fileWriter != null) fileWriter.close();
                git.getFileStatusCache().refresh(ignore, FileStatusCache.REPOSITORY_STATUS_UNKNOWN);
            } catch (IOException ex) {
                Git.LOG.log(Level.FINE, "createIgnored(): File {0} - {1}",  // NOI18N
                        new Object[] {ignore.getAbsolutePath(), ex.toString()});
            }
        }
    }
    
    private static int GIT_NUM_PATTERNS_TO_CHECK = 5;
    private static void addToExistingIgnoredFile(File gitIgnoreFile) {
        if(gitIgnoreFile == null || !gitIgnoreFile.exists() || !gitIgnoreFile.canWrite()) return;
        File tempFile = null;
        BufferedReader br = null;
        PrintWriter pw = null;
        boolean bOrigAnyPresent = false;
        boolean bOrigPresent = false;
        boolean bChgAnyPresent = false;
        boolean bRejAnyPresent = false;
        boolean bConflictAnyPresent = false;
        
        // If new patterns are added to GIT_IGNORE_FILES, following code needs to
        // check for these new patterns
        assert( GIT_IGNORE_FILES.length == GIT_NUM_PATTERNS_TO_CHECK);
        
        try {
            tempFile = new File(gitIgnoreFile.getAbsolutePath() + ".tmp"); // NOI18N
            if (tempFile == null) return;
            
            br = new BufferedReader(new FileReader(gitIgnoreFile));
            pw = new PrintWriter(new FileWriter(tempFile));

            String line = null;            
            while ((line = br.readLine()) != null) {
                if(!bOrigAnyPresent && line.equals(GIT_IGNORE_ORIG_ANY_FILES)){
                    bOrigAnyPresent = true;
                }else if (!bOrigPresent && line.equals(GIT_IGNORE_ORIG_FILES)){
                    bOrigPresent = true;
                }else if (!bChgAnyPresent && line.equals(GIT_IGNORE_CHG_ANY_FILES)){
                    bChgAnyPresent = true;
                }else if (!bRejAnyPresent && line.equals(GIT_IGNORE_REJ_ANY_FILES)){
                    bRejAnyPresent = true;
                }else if (!bConflictAnyPresent && line.equals(GIT_IGNORE_CONFLICT_ANY_FILES)){
                    bConflictAnyPresent = true;
                }
                pw.println(line);
                pw.flush();
            }
            // If not found add as required
            if (!bOrigAnyPresent) {
                pw.println(GIT_IGNORE_ORIG_ANY_FILES );
                pw.flush();
            }
            if (!bOrigPresent) {
                pw.println(GIT_IGNORE_ORIG_FILES );
                pw.flush();
            }
            if (!bChgAnyPresent) {
                pw.println(GIT_IGNORE_CHG_ANY_FILES );
                pw.flush();
            }
            if (!bRejAnyPresent) {
                pw.println(GIT_IGNORE_REJ_ANY_FILES );
                pw.flush();
            }     
            if (!bConflictAnyPresent) {
                pw.println(GIT_IGNORE_CONFLICT_ANY_FILES );
                pw.flush();
            }     
            
        } catch (IOException ex) {
            // Ignore
        } finally {
            try {
                if(pw != null) pw.close();
                if(br != null) br.close();

                boolean bAnyAdditions = !bOrigAnyPresent || !bOrigPresent  || 
                        !bChgAnyPresent || !bRejAnyPresent || !bConflictAnyPresent;               
                if(bAnyAdditions){
                    if (!confirmDialog(GitUtils.class, "MSG_IGNORE_FILES_TITLE", "MSG_IGNORE_FILES")) { // NOI18N 
                        tempFile.delete();
                        return;
                    }
                    if(tempFile != null && tempFile.isFile() && tempFile.canWrite() && gitIgnoreFile != null){ 
                        gitIgnoreFile.delete();
                        tempFile.renameTo(gitIgnoreFile);
                    }
                }else{
                    tempFile.delete();
                }
            } catch (IOException ex) {
            // Ignore
            }
        }
    }

    private static void addIgnorePatterns(Set<Pattern> patterns, File file) {
        Set<String> shPatterns;
        try {
            shPatterns = readIgnoreEntries(file);
        } catch (IOException e) {
            // ignore invalid entries
            return;
        }
        for (Iterator i = shPatterns.iterator(); i.hasNext();) {
            String shPattern = (String) i.next();
            if ("!".equals(shPattern)) { // NOI18N
                patterns.clear();
            } else {
                try {
                    patterns.add(Pattern.compile(shPattern));
                } catch (Exception e) {
                    // unsupported pattern
                }
            }
        }
    }

    private static Boolean ignoreContainsSyntax(File directory) throws IOException {
        File gitIgnore = new File(directory, FILENAME_GITIGNORE);
        Boolean val = false;

        if (!gitIgnore.canRead()) return val;

        String s;
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(gitIgnore));
            while ((s = r.readLine()) != null) {
                String line = s.trim();
                int indexOfHash = line.indexOf("#");
                if (indexOfHash != -1) {
                    if (indexOfHash == 0)
                        continue;
                    line = line.substring(0, indexOfHash -1); 
                }
                String [] array = line.split(" ");
                if (array[0].equals("syntax:")) {
                    val = true;
                    break;
                }
            }
        } finally {
            if (r != null) try { r.close(); } catch (IOException e) {}
        }
        return val;
    }

    private static Set<String> readIgnoreEntries(File directory) throws IOException {
        File gitIgnore = new File(directory, FILENAME_GITIGNORE);

        Set<String> entries = new HashSet<String>(5);
        if (!gitIgnore.canRead()) return entries;

        String s;
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(gitIgnore));
            while ((s = r.readLine()) != null) {
                String line = s.trim();
                if (line.length() == 0) continue;
                int indexOfHash = line.indexOf("#");
                if (indexOfHash != -1) {
                    if (indexOfHash == 0)
                        continue;
                    line = line.substring(0, indexOfHash -1); 
                }
                String [] array = line.split(" ");
                if (array[0].equals("syntax:")) continue;
                entries.addAll(Arrays.asList(array));
            }
        } finally {
            if (r != null) try { r.close(); } catch (IOException e) {}
        }
        return entries;
    }

    private static String computePatternToIgnore(File directory, File file) {
        String name = file.getAbsolutePath().substring(directory.getAbsolutePath().length()+1);
        return name.replace(' ', '?').replace(File.separatorChar, '/');
    }

    private static void writeIgnoreEntries(File directory, Set entries) throws IOException {
        File gitIgnore = new File(directory, FILENAME_GITIGNORE);
        FileObject fo = FileUtil.toFileObject(gitIgnore);

        if (entries.size() == 0) {
            if (fo != null) fo.delete();
            return;
        }

        if (fo == null || !fo.isValid()) {
            fo = FileUtil.toFileObject(directory);
            fo = fo.createData(FILENAME_GITIGNORE);
        }
        FileLock lock = fo.lock();
        PrintWriter w = null;
        try {
            w = new PrintWriter(fo.getOutputStream(lock));
            for (Iterator i = entries.iterator(); i.hasNext();) {
                w.println(i.next());
            }
        } finally {
            lock.releaseLock();
            if (w != null) w.close();
            resetIgnorePatterns(directory);
        }
    }

    /**
     * addIgnored - Add the specified files to the .gitignore file in the 
     * specified repository.
     *
     * @param directory for repository for .gitignore file
     * @param files an array of Files to be added
     */
    public static void addIgnored(File directory, File[] files) throws IOException {
        if (ignoreContainsSyntax(directory)) {
            warningDialog(GitUtils.class, "MSG_UNABLE_TO_IGNORE_TITLE", "MSG_UNABLE_TO_IGNORE");
            return;
        }
        Set<String> entries = readIgnoreEntries(directory);
        for (File file : files) {
            String patterntoIgnore = computePatternToIgnore(directory, file);
            entries.add(patterntoIgnore);
        }
        writeIgnoreEntries(directory, entries);
    }

    /**
     * removeIgnored - Remove the specified files from the .gitignore file in 
     * the specified repository.
     *
     * @param directory for repository for .gitignore file
     * @param files an array of Files to be removed
     */
    public static void removeIgnored(File directory, File[] files) throws IOException {
        if (ignoreContainsSyntax(directory)) {
            warningDialog(GitUtils.class, "MSG_UNABLE_TO_UNIGNORE_TITLE", "MSG_UNABLE_TO_UNIGNORE");
            return;
        }
        Set entries = readIgnoreEntries(directory);
        for (File file : files) {
            String patterntoIgnore = computePatternToIgnore(directory, file);
            entries.remove(patterntoIgnore);
        }
        writeIgnoreEntries(directory, entries);
    }

    /**
     * Returns a Map keyed by Directory, containing a single File/FileInformation Map for each Directories file contents.
     *
     * @param Map of <File, FileInformation> interestingFiles to be processed and divided up into Files in Directory
     * @param Collection of <File> files to be processed against the interestingFiles
     * @return Map of Dirs containing Map of files and status for all files in each directory
     * @throws org.netbeans.modules.git.GitException;
     */
    public static Map<File, Map<File, FileInformation>> getInterestingDirs(Map<File, FileInformation> interestingFiles, Collection<File> files) {
        Map<File, Map<File, FileInformation>> interestingDirs = new HashMap<File, Map<File, FileInformation>>();

        Calendar start = Calendar.getInstance();
        for (File file : files) {
            if (file.isDirectory()) {
                if (interestingDirs.get(file) == null) {
                    interestingDirs.put(file, new HashMap<File, FileInformation>());
                }
            } else {
                File par = file.getParentFile();
                if (par != null) {
                    if (interestingDirs.get(par) == null) {
                        interestingDirs.put(par, new HashMap<File, FileInformation>());
                    }
                    FileInformation fi = interestingFiles.get(file);
                    interestingDirs.get(par).put(file, fi);
                }
            }
        }
        Calendar end = Calendar.getInstance();
        Git.LOG.log(Level.FINE, "getInterestingDirs: process interesting Dirs took {0} millisecs",  // NOI18N
                end.getTimeInMillis() - start.getTimeInMillis());
        return interestingDirs;
    }

    /**
     * Semantics is similar to {@link org.openide.windows.TopComponent#getActivatedNodes()} except that this
     * method returns File objects instead of Nodes. Every node is examined for Files it represents. File and Folder
     * nodes represent their underlying files or folders. Project nodes are represented by their source groups. Other
     * logical nodes must provide FileObjects in their Lookup.
     *
     * @param nodes null (then taken from windowsystem, it may be wrong on editor tabs #66700).
     * @param includingFileStatus if any activated file does not have this CVS status, an empty array is returned
     * @param includingFolderStatus if any activated folder does not have this CVS status, an empty array is returned
     * @return File [] array of activated files, or an empty array if any of examined files/folders does not have given status
     */
    public static VCSContext getCurrentContext(Node[] nodes, int includingFileStatus, int includingFolderStatus) {
        VCSContext context = getCurrentContext(nodes);
        FileStatusCache cache = Git.getInstance().getFileStatusCache();
        for (File file : context.getRootFiles()) {
            FileInformation fi = cache.getStatus(file);
            if (file.isDirectory()) {
                if ((fi.getStatus() & includingFolderStatus) == 0) {
                    return VCSContext.EMPTY;
                }
            } else {
                if ((fi.getStatus() & includingFileStatus) == 0) {
                    return VCSContext.EMPTY;
                }
            }
        }
        return context;
    }

    /**
     * Semantics is similar to {@link org.openide.windows.TopComponent#getActiva
    tedNodes()} except that this
     * method returns File objects instead of Nodes. Every node is examined for
    Files it represents. File and Folder
     * nodes represent their underlying files or folders. Project nodes are repr
    esented by their source groups. Other
     * logical nodes must provide FileObjects in their Lookup.
     *
     * @return File [] array of activated files
     * @param nodes or null (then taken from windowsystem, it may be wrong on ed
    itor tabs #66700).
     */
    public static VCSContext getCurrentContext(Node[] nodes) {
        if (nodes == null) {
            nodes = TopComponent.getRegistry().getActivatedNodes();
        }
        return VCSContext.forNodes(nodes);
    }

   /**
     * Returns path to repository root or null if not managed
     *
     * @param VCSContext
     * @return String of repository root path
     */
    public static String getRootPath(VCSContext context){
        File root = getRootFile(context);
        return (root == null) ? null: root.getAbsolutePath();
    }
    
   /**
     * Returns path to repository root or null if not managed
     *
     * @param VCSContext
     * @return String of repository root path
     */
    public static File getRootFile(VCSContext context){
        if (context == null) return null;
        Git git = Git.getInstance();
        File [] files = context.getRootFiles().toArray(new File[context.getRootFiles().size()]);
        if (files == null || files.length == 0) return null;
        
        File root = git.getTopmostManagedParent(files[0]);
        return root;
    }
    
   /**
     * Returns File object for Project Directory
     *
     * @param VCSContext
     * @return File object of Project Directory
     */
    public static File getProjectFile(VCSContext context){
        return getProjectFile(getProject(context));
    }

    public static Project getProject(VCSContext context){
        if (context == null) return null;
        File [] files = context.getRootFiles().toArray(new File[context.getRootFiles().size()]);

        for (File file : files) {
            /* We may be committing a LocallyDeleted file */
            if (!file.exists()) file = file.getParentFile();

            Project p = FileOwnerQuery.getOwner(FileUtil.toFileObject(file));
            if (p != null) {
                return p;
            } else {
                Git.LOG.log(Level.FINE, "GitUtils.getProjectFile(): No project for {0}",  // NOI18N
                    file);
            }
        }
        return null;
    }
    
    public static File getProjectFile(Project project){
        if (project == null) return null;

        FileObject fo = project.getProjectDirectory();
        return  FileUtil.toFile(fo);
    }

    public static File[] getProjectRootFiles(Project project){
        if (project == null) return null;
        Set<File> set = new HashSet<File>();

        Sources sources = ProjectUtils.getSources(project);
        SourceGroup [] sourceGroups = sources.getSourceGroups(Sources.TYPE_GENERIC);
        for (int j = 0; j < sourceGroups.length; j++) {
            SourceGroup sourceGroup = sourceGroups[j];
            FileObject srcRootFo = sourceGroup.getRootFolder();
            File rootFile = FileUtil.toFile(srcRootFo);
            set.add(rootFile);
        }
        return set.toArray(new File[set.size()]);
    }

    /**
     * Checks file location to see if it is part of Git metdata
     *
     * @param file file to check
     * @return true if the file or folder is a part of Git metadata, false otherwise
     */
    public static boolean isPartOfGitMetadata(File file) {
        return metadataPattern.matcher(file.getAbsolutePath()).matches();
    }

    /**
     * Forces refresh of Status for the given directory 
     *
     * @param start file or dir to begin refresh from
     * @return void
     */
    public static void forceStatusRefresh(File file) {
        if (Git.getInstance().isAdministrative(file)) return;
        
        try {
            FileStatusCache cache = Git.getInstance().getFileStatusCache();

            cache.refreshCached(file);
            File repository = Git.getInstance().getTopmostManagedParent(file);
            if (repository == null) {
                return;
            }
        
            if (file.isDirectory()) {
                Map<File, FileInformation> interestingFiles;
                interestingFiles = GitCommand.getInterestingStatus(repository, file);
                if (!interestingFiles.isEmpty()){
                    Collection<File> files = interestingFiles.keySet();
                    for (File aFile : files) {
                        FileInformation fi = interestingFiles.get(aFile);
                        cache.refreshFileStatus(aFile, fi, null);
                    }
                }
            }

        } catch (GitException ex) {
        }
    }

    /**
     * Forces refresh of Status for the specfied context.
     *
     * @param VCSContext context to be updated.
     * @return void
     */
    public static void forceStatusRefresh(VCSContext context) {
        for (File root :  context.getRootFiles()) {
            forceStatusRefresh(root);
        }
    }

    /**
     * Forces refresh of Status for the project of the specified context
     *
     * @param VCSContext ctx whose project is be updated.
     * @return void
     */
    public static void forceStatusRefreshProject(VCSContext context) {
        Project project = getProject(context);
        if (project == null) {
            return;
        }
        File[] files = getProjectRootFiles(project);
        for (int j = 0; j < files.length; j++) {
            forceStatusRefresh(files[j]);
        }
    }

    /**
     * Tests parent/child relationship of files.
     *
     * @param parent file to be parent of the second parameter
     * @param file file to be a child of the first parameter
     * @return true if the second parameter represents the same file as the first parameter OR is its descendant (child)
     */
    public static boolean isParentOrEqual(File parent, File file) {
        for (; file != null; file = file.getParentFile()) {
            if (file.equals(parent)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns path of file relative to root repository or a warning message
     * if the file is not under the repository root.
     *
     * @param File to get relative path from the repository root
     * @return String of relative path of the file from teh repository root
     */
    public static String getRelativePath(File file) {
            if (file == null){
                return NbBundle.getMessage(SyncFileNode.class, "LBL_Location_NotInRepository"); // NOI18N
            }
            String shortPath = file.getAbsolutePath();
            if (shortPath == null){
                return NbBundle.getMessage(SyncFileNode.class, "LBL_Location_NotInRepository"); // NOI18N
            }
            
            Git git = Git.getInstance();
            File rootManagedFolder = git.getTopmostManagedParent(file);
            if ( rootManagedFolder == null){
                return NbBundle.getMessage(SyncFileNode.class, "LBL_Location_NotInRepository"); // NOI18N
            }
            
            String root = rootManagedFolder.getAbsolutePath();
            if(shortPath.startsWith(root)) {
                return shortPath.substring(root.length()+1);
            }else{
                return NbBundle.getMessage(SyncFileNode.class, "LBL_Location_NotInRepository"); // NOI18N
            }
     }

    /**
     * Normalize flat files, Git treats folder as normal file
     * so it's necessary explicitly list direct descendants to
     * get classical flat behaviour.
     *
     * <p> E.g. revert on package node means:
     * <ul>
     *   <li>revert package folder properties AND
     *   <li>revert all modified (including deleted) files in the folder
     * </ul>
     *
     * @return files with given status and direct descendants with given status.
     */

    public static File[] flatten(File[] files, int status) {
        LinkedList<File> ret = new LinkedList<File>();

        FileStatusCache cache = Git.getInstance().getFileStatusCache();
        for (int i = 0; i<files.length; i++) {
            File dir = files[i];
            FileInformation info = cache.getStatus(dir);
            if ((status & info.getStatus()) != 0) {
                ret.add(dir);
            }
            File[] entries = cache.listFiles(dir);  // comparing to dir.listFiles() lists already deleted too
            for (int e = 0; e<entries.length; e++) {
                File entry = entries[e];
                info = cache.getStatus(entry);
                if ((status & info.getStatus()) != 0) {
                    ret.add(entry);
                }
            }
        }

        return ret.toArray(new File[ret.size()]);
    }

    /**
     * Utility method that returns all non-excluded modified files that are
     * under given roots (folders) and have one of specified statuses.
     *
     * @param context context to search
     * @param includeStatus bit mask of file statuses to include in result
     * @return File [] array of Files having specified status
     */
    public static File [] getModifiedFiles(VCSContext context, int includeStatus) {
        File[] all = Git.getInstance().getFileStatusCache().listFiles(context, includeStatus);
        List<File> files = new ArrayList<File>();
        for (int i = 0; i < all.length; i++) {
            File file = all[i];
            String path = file.getAbsolutePath();
            if (GitModuleConfig.getDefault().isExcludedFromCommit(path) == false) {
                files.add(file);
            }
        }

        // ensure that command roots (files that were explicitly selected by user) are included in Diff
        FileStatusCache cache = Git.getInstance().getFileStatusCache();
        for (File file : context.getRootFiles()) {
            if (file.isFile() && (cache.getStatus(file).getStatus() & includeStatus) != 0 && !files.contains(file)) {
                files.add(file);
            }
        }
        return files.toArray(new File[files.size()]);
    }

    /**
     * Checks if the file is binary.
     *
     * @param file file to check
     * @return true if the file cannot be edited in NetBeans text editor, false otherwise
     */
    public static boolean isFileContentBinary(File file) {
        FileObject fo = FileUtil.toFileObject(file);
        if (fo == null) return false;
        try {
            DataObject dao = DataObject.find(fo);
            return dao.getCookie(EditorCookie.class) == null;
        } catch (DataObjectNotFoundException e) {
            // not found, continue
        }
        return false;
    }

    /**
     * @return true if the buffer is almost certainly binary.
     * Note: Non-ASCII based encoding encoded text is binary,
     * newlines cannot be reliably detected.
     */
    public static boolean isBinary(byte[] buffer) {
        for (int i = 0; i<buffer.length; i++) {
            int ch = buffer[i];
            if (ch < 32 && ch != '\t' && ch != '\n' && ch != '\r') {
                return true;
            }
        }
        return false;
    }

    /**
     * Compares two {@link FileInformation} objects by importance of statuses they represent.
     */
    public static class ByImportanceComparator<T> implements Comparator<FileInformation> {
        public int compare(FileInformation i1, FileInformation i2) {
            return getComparableStatus(i1.getStatus()) - getComparableStatus(i2.getStatus());
        }
    }

    /**
     * Gets integer status that can be used in comparators. The more important the status is for the user,
     * the lower value it has. Conflict is 0, unknown status is 100.
     *
     * @return status constant suitable for 'by importance' comparators
     */
    public static int getComparableStatus(int status) {
        if (0 != (status & FileInformation.STATUS_VERSIONED_CONFLICT)) {
            return 0;
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_MERGE)) {
            return 1;
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_DELETEDLOCALLY)) {
            return 10;
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_REMOVEDLOCALLY)) {
            return 11;
       } else if (0 != (status & FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY)) {
            return 12;
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_ADDEDLOCALLY)) {
            return 13;
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_MODIFIEDLOCALLY)) {
            return 14;
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_REMOVEDINREPOSITORY)) {
            return 30;
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_NEWINREPOSITORY)) {
            return 31;
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_MODIFIEDINREPOSITORY)) {
            return 32;
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_UPTODATE)) {
            return 50;
        } else if (0 != (status & FileInformation.STATUS_NOTVERSIONED_EXCLUDED)){
            return 100;
        } else if (0 != (status & FileInformation.STATUS_NOTVERSIONED_NOTMANAGED)) {
            return 101;
        } else if (status == FileInformation.STATUS_UNKNOWN) {
            return 102;
        } else {
            throw new IllegalArgumentException("Uncomparable status: " + status); // NOI18N
        }
    }

    protected static int getFileEnabledStatus() {
        return ~0;
    }

    protected static int getDirectoryEnabledStatus() {
        return FileInformation.STATUS_MANAGED & ~FileInformation.STATUS_NOTVERSIONED_EXCLUDED;
    }

    /**
     * Rips an eventual username off - e.g. user@svn.host.org
     *
     * @param host - hostname with a userneame
     * @return host - hostname without the username
     */
    public static String ripUserFromHost(String host) {
        int idx = host.indexOf('@');
        if(idx < 0) {
            return host;
        } else {
            return host.substring(idx + 1);
        }
    }

    /**
     * This utility class should not be instantiated anywhere.
     */
    private GitUtils() {
    }
    
}