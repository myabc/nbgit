package org.nbgit.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.nbgit.StatusCache;
import org.nbgit.Git;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.queries.SharabilityQuery;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public class GitIgnore {

    private GitIgnore()
    {
    }

    // IGNORE SUPPORT GIT: following file patterns are added to {Git repos}/.gitignore and Git will ignore any files
    // that match these patterns, reporting "I"status for them // NOI18N
    //private static final String [] GIT_IGNORE_FILES = { ".orig", "\\.orig\\..*$", "\\.chg\\..*$", ".rej", "\\.conflict\\~$"}; // NOI18N
    private static final String[] GIT_IGNORE_FILES = {".orig"};
    private static final String GIT_IGNORE_ORIG_FILES = "\\.orig$"; // NOI18N
    private static final String GIT_IGNORE_ORIG_ANY_FILES = "\\.orig\\..*$"; // NOI18N
    private static final String GIT_IGNORE_CHG_ANY_FILES = "\\.chg\\..*$"; // NOI18N
    private static final String GIT_IGNORE_REJ_ANY_FILES = "\\.rej$"; // NOI18N
    private static final String GIT_IGNORE_CONFLICT_ANY_FILES = "\\.conflict\\~$"; // NOI18N
    private static final String FILENAME_GITIGNORE = ".gitignore"; // NOI18N
    private static HashMap<String, Set<Pattern>> ignorePatterns;

    private static void resetIgnorePatterns(File file)
    {
        if (ignorePatterns == null)
            return;
        String key = file.getAbsolutePath();
        ignorePatterns.remove(key);
    }

    private static Set<Pattern> getIgnorePatterns(File file)
    {
        if (ignorePatterns == null)
            ignorePatterns = new HashMap<String, Set<Pattern>>();
        String key = file.getAbsolutePath();
        Set<Pattern> patterns = ignorePatterns.get(key);
        if (patterns == null) {
            patterns = new HashSet<Pattern>(5);
            addIgnorePatterns(patterns, file);
            ignorePatterns.put(key, patterns);
        }
        return patterns;
    }

    public static boolean isSharable(File file)
    {
        return SharabilityQuery.getSharability(file) != SharabilityQuery.NOT_SHARABLE;
    }

    /**
     * isIgnored - checks to see if this is a file Git should ignore
     *
     * @param File file to check
     * @return boolean true - ignore, false - not ignored
     */
    public static boolean isIgnored(File file)
    {
        return isIgnored(file, true);
    }

    public static boolean isIgnored(File file, boolean checkSharability)
    {
        // FIXME Disabled for now.
        if (true != false)
            return false;
        if (file == null)
            return false;
        String path = file.getPath();
        String name = file.getName();
        File topFile = Git.getInstance().getTopmostManagedParent(file);

        // We assume that the toplevel directory should not be ignored.
        if (topFile == null || topFile.equals(file))
            return false;

        // We assume that the Project should not be ignored.
        if (file.isDirectory()) {
            ProjectManager projectManager = ProjectManager.getDefault();
            if (projectManager.isProject(FileUtil.toFileObject(file)))
                return false;
        }

        Set<Pattern> patterns = getIgnorePatterns(topFile);

        for (Iterator i = patterns.iterator(); i.hasNext();) {
            Pattern pattern = (Pattern) i.next();
            if (pattern.matcher(path).find())
                return true;
        }

        if (FILENAME_GITIGNORE.equals(name))
            return false;
        if (checkSharability && !isSharable(file))
            return true;
        return false;
    }

    /**
     * createIgnored - creates .gitignore file in the repository in which
     * the given file belongs. This .ignore file ensures Git will ignore
     * the files specified in GIT_IGNORE_FILES list
     *
     * @param path to repository to place .gitignore file
     */
    public static void createIgnored(File path)
    {
        if (path == null)
            return;
        BufferedWriter fileWriter = null;
        Git git = Git.getInstance();
        File root = git.getTopmostManagedParent(path);
        if (root == null)
            return;
        File ignore = new File(root, FILENAME_GITIGNORE);

        try {
            if (!ignore.exists()) {
                fileWriter = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(ignore)));
                for (String name : GIT_IGNORE_FILES) {
                    fileWriter.write(name + "\n"); // NOI18N
                }
            } else
                addToExistingIgnoredFile(ignore);
        } catch (IOException ex) {
            Git.LOG.log(Level.FINE, "createIgnored(): File {0} - {1}", // NOI18N
                new Object[]{ignore.getAbsolutePath(), ex.toString()});
        } finally {
            try {
                if (fileWriter != null)
                    fileWriter.close();
                git.getStatusCache().refresh(ignore, StatusCache.REPOSITORY_STATUS_UNKNOWN);
            } catch (IOException ex) {
                Git.LOG.log(Level.FINE, "createIgnored(): File {0} - {1}", // NOI18N
                    new Object[]{ignore.getAbsolutePath(), ex.toString()});
            }
        }
    }

    private static int GIT_NUM_PATTERNS_TO_CHECK = 5;

    private static void addToExistingIgnoredFile(File gitIgnoreFile)
    {
        if (gitIgnoreFile == null || !gitIgnoreFile.exists() || !gitIgnoreFile.canWrite())
            return;
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
        assert (GIT_IGNORE_FILES.length == GIT_NUM_PATTERNS_TO_CHECK);

        try {
            tempFile = new File(gitIgnoreFile.getAbsolutePath() + ".tmp"); // NOI18N
            if (tempFile == null)
                return;

            br = new BufferedReader(new FileReader(gitIgnoreFile));
            pw = new PrintWriter(new FileWriter(tempFile));

            String line = null;
            while ((line = br.readLine()) != null) {
                if (!bOrigAnyPresent && line.equals(GIT_IGNORE_ORIG_ANY_FILES))
                    bOrigAnyPresent = true;
                else if (!bOrigPresent && line.equals(GIT_IGNORE_ORIG_FILES))
                    bOrigPresent = true;
                else if (!bChgAnyPresent && line.equals(GIT_IGNORE_CHG_ANY_FILES))
                    bChgAnyPresent = true;
                else if (!bRejAnyPresent && line.equals(GIT_IGNORE_REJ_ANY_FILES))
                    bRejAnyPresent = true;
                else if (!bConflictAnyPresent && line.equals(GIT_IGNORE_CONFLICT_ANY_FILES))
                    bConflictAnyPresent = true;
                pw.println(line);
                pw.flush();
            }
            // If not found add as required
            if (!bOrigAnyPresent) {
                pw.println(GIT_IGNORE_ORIG_ANY_FILES);
                pw.flush();
            }
            if (!bOrigPresent) {
                pw.println(GIT_IGNORE_ORIG_FILES);
                pw.flush();
            }
            if (!bChgAnyPresent) {
                pw.println(GIT_IGNORE_CHG_ANY_FILES);
                pw.flush();
            }
            if (!bRejAnyPresent) {
                pw.println(GIT_IGNORE_REJ_ANY_FILES);
                pw.flush();
            }
            if (!bConflictAnyPresent) {
                pw.println(GIT_IGNORE_CONFLICT_ANY_FILES);
                pw.flush();
            }

        } catch (IOException ex) {
            // Ignore
        } finally {
            try {
                if (pw != null)
                    pw.close();
                if (br != null)
                    br.close();

                boolean bAnyAdditions = !bOrigAnyPresent || !bOrigPresent ||
                    !bChgAnyPresent || !bRejAnyPresent || !bConflictAnyPresent;
                if (bAnyAdditions) {
                    if (!GitUtils.confirmDialog(GitUtils.class, "MSG_IGNORE_FILES_TITLE", "MSG_IGNORE_FILES")) { // NOI18N
                        tempFile.delete();
                        return;
                    }
                    if (tempFile != null && tempFile.isFile() && tempFile.canWrite() && gitIgnoreFile != null) {
                        gitIgnoreFile.delete();
                        tempFile.renameTo(gitIgnoreFile);
                    }
                } else
                    tempFile.delete();
            } catch (IOException ex) {
                // Ignore
            }
        }
    }

    private static void addIgnorePatterns(Set<Pattern> patterns, File file)
    {
        Set<String> shPatterns;
        try {
            shPatterns = readIgnoreEntries(file);
        } catch (IOException e) {
            // ignore invalid entries
            return;
        }
        for (Iterator i = shPatterns.iterator(); i.hasNext();) {
            String shPattern = (String) i.next();
            if ("!".equals(shPattern)) // NOI18N
                patterns.clear();
            else
                try {
                    patterns.add(Pattern.compile(shPattern));
                } catch (Exception e) {
                    // unsupported pattern
                }
        }
    }

    private static Boolean ignoreContainsSyntax(File directory) throws IOException
    {
        File gitIgnore = new File(directory, FILENAME_GITIGNORE);
        Boolean val = false;

        if (!gitIgnore.canRead())
            return val;

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
                    line = line.substring(0, indexOfHash - 1);
                }
                String[] array = line.split(" ");
                if (array[0].equals("syntax:")) {
                    val = true;
                    break;
                }
            }
        } finally {
            if (r != null)
                try {
                    r.close();
                } catch (IOException e) {
                }
        }
        return val;
    }

    private static Set<String> readIgnoreEntries(File directory) throws IOException
    {
        File gitIgnore = new File(directory, FILENAME_GITIGNORE);

        Set<String> entries = new HashSet<String>(5);
        if (!gitIgnore.canRead())
            return entries;

        String s;
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(gitIgnore));
            while ((s = r.readLine()) != null) {
                String line = s.trim();
                if (line.length() == 0)
                    continue;
                int indexOfHash = line.indexOf("#");
                if (indexOfHash != -1) {
                    if (indexOfHash == 0)
                        continue;
                    line = line.substring(0, indexOfHash - 1);
                }
                String[] array = line.split(" ");
                if (array[0].equals("syntax:"))
                    continue;
                entries.addAll(Arrays.asList(array));
            }
        } finally {
            if (r != null)
                try {
                    r.close();
                } catch (IOException e) {
                }
        }
        return entries;
    }

    private static String computePatternToIgnore(File directory, File file)
    {
        String name = file.getAbsolutePath().substring(directory.getAbsolutePath().length() + 1);
        return name.replace(' ', '?').replace(File.separatorChar, '/');
    }

    private static void writeIgnoreEntries(File directory, Set entries) throws IOException
    {
        File gitIgnore = new File(directory, FILENAME_GITIGNORE);
        FileObject fo = FileUtil.toFileObject(gitIgnore);

        if (entries.size() == 0) {
            if (fo != null)
                fo.delete();
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
            if (w != null)
                w.close();
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
    public static void addIgnored(File directory, File[] files) throws IOException
    {
        if (ignoreContainsSyntax(directory)) {
            GitUtils.warningDialog(GitUtils.class, "MSG_UNABLE_TO_IGNORE_TITLE", "MSG_UNABLE_TO_IGNORE");
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
    public static void removeIgnored(File directory, File[] files) throws IOException
    {
        if (ignoreContainsSyntax(directory)) {
            GitUtils.warningDialog(GitUtils.class, "MSG_UNABLE_TO_UNIGNORE_TITLE", "MSG_UNABLE_TO_UNIGNORE");
            return;
        }
        Set entries = readIgnoreEntries(directory);
        for (File file : files) {
            String patterntoIgnore = computePatternToIgnore(directory, file);
            entries.remove(patterntoIgnore);
        }
        writeIgnoreEntries(directory, entries);
    }

}
