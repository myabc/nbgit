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

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.netbeans.api.options.OptionsDisplayer;
import org.netbeans.api.queries.SharabilityQuery;
import org.netbeans.modules.git.FileInformation;
import org.netbeans.modules.git.FileStatus;
import org.netbeans.modules.git.Git;
import org.netbeans.modules.git.GitException;
import org.netbeans.modules.git.GitModuleConfig;
import org.netbeans.modules.git.OutputLogger;
import org.netbeans.modules.git.config.GitConfigFiles;
import org.netbeans.modules.git.ui.log.GitLogMessage;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 * 
 * 
 * @author alexbcoles
 */
public class GitCommand {

    public static final String GIT_COMMAND = "git";  // NOI18N
    public static final String GITK_COMMAND = "gitk"; 
    
    private static final String GIT_STATUS_CMD = "status";  // NOI18N // need -A to see ignored files, specified in .gitignore, see man gitignore for details
    private static final String GIT_OPT_REPOSITORY = "--repository"; // NOI18N
    private static final String GIT_OPT_BUNDLE = "--bundle"; // NOI18N
    private static final String GIT_OPT_CWD_CMD = "--cwd"; // NOI18N
    private static final String GIT_OPT_USERNAME = "--user"; // NOI18N
    
    private static final String GIT_OPT_FOLLOW = "--follow"; // NOI18N
    private static final String GIT_STATUS_FLAG_ALL_CMD = "-marduicC"; // NOI18N
    private static final String GIT_FLAG_REV_CMD = "--rev"; // NOI18N
    private static final String GIT_STATUS_FLAG_TIP_CMD = "tip"; // NOI18N
    private static final String GIT_STATUS_FLAG_REM_DEL_CMD = "-rd"; // NOI18N
    private static final String GIT_STATUS_FLAG_INCLUDE_CMD = "-I"; // NOI18N
    private static final String GIT_STATUS_FLAG_INCLUDE_GLOB_CMD = "glob:"; // NOI18N
    private static final String GIT_STATUS_FLAG_INCLUDE_END_CMD = "*"; // NOI18N
    private static final String GIT_STATUS_FLAG_INTERESTING_CMD = "-marduC"; // NOI18N
    private static final String GIT_STATUS_FLAG_UNKNOWN_CMD = "-u"; // NOI18N
    private static final String GIT_HEAD_STR = "HEAD"; // NOI18N
    private static final String GIT_FLAG_DATE_CMD = "--date"; // NOI18N
    
    private static final String GIT_COMMIT_CMD = "commit"; // NOI18N
    private static final String GIT_COMMIT_OPT_LOGFILE_CMD = "--logfile"; // NOI18N
    private static final String GIT_COMMIT_TEMPNAME = "gitcommit"; // NOI18N
    private static final String GIT_COMMIT_TEMPNAME_SUFFIX = ".gitm"; // NOI18N
    private static final String GIT_COMMIT_DEFAULT_MESSAGE = "[no commit message]"; // NOI18N
    
    private static final String GIT_REVERT_CMD = "revert"; // NOI18N
    private static final String GIT_REVERT_NOBACKUP_CMD = "--no-backup"; // NOI18N
    private static final String GIT_ADD_CMD = "add"; // NOI18N
    
    private static final String GIT_BRANCH_CMD = "branch"; // NOI18N
    private static final String GIT_BRANCH_REV_CMD = "tip"; // NOI18N
    private static final String GIT_BRANCH_REV_TEMPLATE_CMD = "--template={rev}\\n"; // NOI18N
    private static final String GIT_BRANCH_SHORT_CS_TEMPLATE_CMD = "--template={node|short}\\n"; // NOI18N
    private static final String GIT_BRANCH_INFO_TEMPLATE_CMD = "--template={branches}:{rev}:{node|short}\\n"; // NOI18N
    private static final String GIT_GET_PREVIOUS_TEMPLATE_CMD = "--template={files}\\n"; // NOI18N
    
    private static final String GIT_CREATE_CMD = "init"; // NOI18N
    private static final String GIT_CLONE_CMD = "clone"; // NOI18N
    
    private static final String GIT_UPDATE_ALL_CMD = "update"; // NOI18N
    private static final String GIT_UPDATE_FORCE_ALL_CMD = "-C"; // NOI18N
    
    private static final String GIT_REMOVE_CMD = "rm"; // NOI18N
    private static final String GIT_REMOVE_FLAG_FORCE_CMD = "-f"; // NOI18N
    
    private static final String GIT_LOG_CMD = "log"; // NOI18N
    private static final String GIT_OUT_CMD = "out"; // NOI18N
    private static final String GIT_LOG_LIMIT_ONE_CMD = "-l 1"; // NOI18N
    private static final String GIT_LOG_LIMIT_CMD = "-l"; // NOI18N
    private static final String GIT_LOG_TEMPLATE_SHORT_CMD = "--template={rev}\\n{desc|firstline}\\n{date|gitdate}\\n{node|short}\\n"; // NOI18N
    private static final String GIT_LOG_TEMPLATE_LONG_CMD = "--template={rev}\\n{desc}\\n{date|gitdate}\\n{node|short}\\n"; // NOI18N

    private static final String GIT_LOG_NO_MERGES_CMD = "-M";
    private static final String GIT_LOG_DEBUG_CMD = "--debug";
    private static final String GIT_LOG_TEMPLATE_HISTORY_CMD = 
            "--template=rev:{rev}\\nauth:{author}\\ndesc:{desc}\\ndate:{date|gitdate}\\nid:{node|short}\\n" + // NOI18N
            "file_mods:{files}\\nfile_adds:{file_adds}\\nfile_dels:{file_dels}\\nfile_copies:\\nendCS:\\n"; // NOI18N
    private static final String GIT_LOG_REVISION_OUT = "rev:"; // NOI18N
    private static final String GIT_LOG_AUTHOR_OUT = "auth:"; // NOI18N
    private static final String GIT_LOG_DESCRIPTION_OUT = "desc:"; // NOI18N
    private static final String GIT_LOG_DATE_OUT = "date:"; // NOI18N
    private static final String GIT_LOG_ID_OUT = "id:"; // NOI18N
    private static final String GIT_LOG_FILEMODS_OUT = "file_mods:"; // NOI18N
    private static final String GIT_LOG_FILEADDS_OUT = "file_adds:"; // NOI18N
    private static final String GIT_LOG_FILEDELS_OUT = "file_dels:"; // NOI18N
    private static final String GIT_LOG_FILECOPIESS_OUT = "file_copies:"; // NOI18N
    private static final String GIT_LOG_ENDCS_OUT = "endCS:"; // NOI18N

    private static final String GIT_CSET_TEMPLATE_CMD = "--template={rev}:{node|short}\\n"; // NOI18N
    private static final String GIT_REV_TEMPLATE_CMD = "--template={rev}\\n"; // NOI18N
    private static final String GIT_CSET_TARGET_TEMPLATE_CMD = "--template={rev} ({node|short})\\n"; // NOI18N
    
    private static final String GIT_CAT_CMD = "cat"; // NOI18N
    private static final String GIT_FLAG_OUTPUT_CMD = "--output"; // NOI18N
    
    private static final String GIT_ANNOTATE_CMD = "annotate"; // NOI18N
    private static final String GIT_ANNOTATE_FLAGN_CMD = "--number"; // NOI18N
    private static final String GIT_ANNOTATE_FLAGU_CMD = "--user"; // NOI18N
    
    private static final String GIT_EXPORT_CMD = "export"; // NOI18N
    private static final String GIT_IMPORT_CMD = "import"; // NOI18N

    private static final String GIT_RENAME_CMD = "mv"; // NOI18N
    private static final String GIT_RENAME_AFTER_CMD = "-A"; // NOI18N
    private static final String GIT_PATH_DEFAULT_CMD = "paths"; // NOI18N
    private static final String GIT_PATH_DEFAULT_OPT = "default"; // NOI18N
    private static final String GIT_PATH_DEFAULT_PUSH_OPT = "default-push"; // NOI18N
 
    private static final String GIT_MERGE_CMD = "merge"; // NOI18N
    private static final String GIT_MERGE_FORCE_CMD = "-f"; // NOI18N
    private static final String GIT_MERGE_ENV = "EDITOR=success || $TEST -s"; // NOI18N

    public static final String GIT_GITK_PATH_SOLARIS10 = "/usr/demo/gitk"; // NOI18N
    private static final String GIT_GITK_PATH_SOLARIS10_ENV = "PATH=/usr/bin/:/usr/sbin:/bin:"+ GIT_GITK_PATH_SOLARIS10; // NOI18N
    
    private static final String GIT_PULL_CMD = "pull"; // NOI18N
    private static final String GIT_UPDATE_CMD = "-u"; // NOI18N
    private static final String GIT_PUSH_CMD = "push"; // NOI18N
    private static final String GIT_UNBUNDLE_CMD = "unbundle"; // NOI18N
    private static final String GIT_ROLLBACK_CMD = "rollback"; // NOI18N
    
    private static final String GIT_BACKOUT_CMD = "backout"; // NOI18N
    private static final String GIT_BACKOUT_MERGE_CMD = "--merge"; // NOI18N
    private static final String GIT_BACKOUT_COMMIT_MSG_CMD = "-m"; // NOI18N
    private static final String GIT_BACKOUT_REV_CMD = "-r"; // NOI18N
 
    private static final String GIT_STRIP_CMD = "strip"; // NOI18N
    private static final String GIT_STRIP_EXT_CMD = "extensions.mq="; // NOI18N
    private static final String GIT_STRIP_NOBACKUP_CMD = "-n"; // NOI18N
    private static final String GIT_STRIP_FORCE_MULTIHEAD_CMD = "-f"; // NOI18N

    private static final String GIT_VERSION_CMD = "version"; // NOI18N
    private static final String GIT_INCOMING_CMD = "incoming"; // NOI18N
    private static final String GIT_OUTGOING_CMD = "outgoing"; // NOI18N
    private static final String GIT_VIEW_CMD = "view"; // NOI18N
    private static final String GIT_VERBOSE_CMD = "-v"; // NOI18N
    private static final String GIT_CONFIG_OPTION_CMD = "--config"; // NOI18N
    private static final String GIT_FETCH_EXT_CMD = "extensions.fetch="; // NOI18N
    private static final String GIT_FETCH_CMD = "fetch"; // NOI18N
    public static final String GIT_PROXY_ENV = "http_proxy="; // NOI18N
    
    private static final String GIT_MERGE_NEEDED_ERR = "(run 'git heads' to see heads, 'git merge' to merge)"; // NOI18N
    public static final String GIT_MERGE_CONFLICT_ERR = "conflicts detected in "; // NOI18N
    public static final String GIT_MERGE_CONFLICT_WIN1_ERR = "merging"; // NOI18N
    public static final String GIT_MERGE_CONFLICT_WIN2_ERR = "failed!"; // NOI18N
    private static final String GIT_MERGE_MULTIPLE_HEADS_ERR = "abort: repo has "; // NOI18N
    private static final String GIT_MERGE_UNCOMMITTED_ERR = "abort: outstanding uncommitted merges"; // NOI18N

    private static final String GIT_MERGE_UNAVAILABLE_ERR = "is not recognized as an internal or external command";

    private static final String GIT_NO_CHANGES_ERR = "no changes found"; // NOI18N
    private final static String GIT_CREATE_NEW_BRANCH_ERR = "abort: push creates new remote branches!"; // NOI18N
    private final static String GIT_HEADS_CREATED_ERR = "(+1 heads)"; // NOI18N
    private final static String GIT_NO_GIT_CMD_FOUND_ERR = "git: not found";
    private final static String GIT_ARG_LIST_TOO_LONG_ERR = "Arg list too long";
            
    private final static String GIT_HEADS_CMD = "heads"; // NOI18N
    
    private static final String GIT_NO_REPOSITORY_ERR = "There is no Git repository here"; // NOI18N
    private static final String GIT_NO_RESPONSE_ERR = "no suitable response from remote git!"; // NOI18N
    private static final String GIT_NOT_REPOSITORY_ERR = "does not appear to be an git repository"; // NOI18N
    private static final String GIT_REPOSITORY = "repository"; // NOI18N
    private static final String GIT_NOT_FOUND_ERR = "not found!"; // NOI18N
    private static final String GIT_UPDATE_SPAN_BRANCHES_ERR = "abort: update spans branches"; // NOI18N
    private static final String GIT_ALREADY_TRACKED_ERR = " already tracked!"; // NOI18N
    private static final String GIT_NOT_TRACKED_ERR = " no tracked!"; // NOI18N
    private static final String GIT_CANNOT_READ_COMMIT_MESSAGE_ERR = "abort: can't read commit message"; // NOI18N
    private static final String GIT_CANNOT_RUN_ERR = "Cannot run program"; // NOI18N
    private static final String GIT_ABORT_ERR = "abort: "; // NOI18N
    private static final String GIT_ABORT_PUSH_ERR = "abort: push creates new remote branches!"; // NOI18N
    private static final String GIT_ABORT_NO_FILES_TO_COPY_ERR = "abort: no files to copy"; // NOI18N
    private static final String GIT_ABORT_NO_DEFAULT_PUSH_ERR = "abort: repository default-push not found!"; // NOI18N
    private static final String GIT_ABORT_NO_DEFAULT_ERR = "abort: repository default not found!"; // NOI18N
    private static final String GIT_ABORT_POSSIBLE_PROXY_ERR = "abort: error: node name or service name not known"; // NOI18N
    private static final String GIT_ABORT_UNCOMMITTED_CHANGES_ERR = "abort: outstanding uncommitted changes"; // NOI18N
    private static final String GIT_BACKOUT_MERGE_NEEDED_ERR = "(use \"backout --merge\" if you want to auto-merge)";    
    private static final String GIT_ABORT_BACKOUT_MERGE_CSET_ERR = "abort: cannot back out a merge changeset without --parent"; // NOI18N"
    
    private static final String GIT_NO_CHANGE_NEEDED_ERR = "no change needed"; // NOI18N
    private static final String GIT_NO_ROLLBACK_ERR = "no rollback information available"; // NOI18N
    private static final String GIT_NO_UPDATES_ERR = "0 files updated, 0 files merged, 0 files removed, 0 files unresolved"; // NOI18N
    private static final String GIT_NO_VIEW_ERR = "git: unknown command 'view'"; // NOI18N
    private static final String GIT_GITK_NOT_FOUND_ERR = "sh: gitk: not found"; // NOI18N
    private static final String GIT_NO_SUCH_FILE_ERR = "No such file"; // NOI18N

    private static final String GIT_NO_REV_STRIP_ERR = "abort: unknown revision"; // NOI18N
    private static final String GIT_LOCAL_CHANGES_STRIP_ERR = "abort: local changes found"; // NOI18N
    private static final String GIT_MULTIPLE_HEADS_STRIP_ERR = "no rollback information available"; // NOI18N

    private static final char GIT_STATUS_CODE_MODIFIED = 'M' + ' ';    // NOI18N // STATUS_VERSIONED_MODIFIEDLOCALLY
    private static final char GIT_STATUS_CODE_ADDED = 'A' + ' ';      // NOI18N // STATUS_VERSIONED_ADDEDLOCALLY
    private static final char GIT_STATUS_CODE_REMOVED = 'R' + ' ';   // NOI18N  // STATUS_VERSIONED_REMOVEDLOCALLY - still tracked, git update will recover, git commit
    private static final char GIT_STATUS_CODE_CLEAN = 'C' + ' ';     // NOI18N  // STATUS_VERSIONED_UPTODATE
    private static final char GIT_STATUS_CODE_DELETED = '!' + ' ';    // NOI18N // STATUS_VERSIONED_DELETEDLOCALLY - still tracked, git update will recover, git commit no effect
    private static final char GIT_STATUS_CODE_NOTTRACKED = '?' + ' '; // NOI18N // STATUS_NOTVERSIONED_NEWLOCALLY - not tracked
    private static final char GIT_STATUS_CODE_IGNORED = 'I' + ' ';     // NOI18N // STATUS_NOTVERSIONED_EXCLUDE - not shown by default
    private static final char GIT_STATUS_CODE_CONFLICT = 'X' + ' ';    // NOI18N // STATUS_VERSIONED_CONFLICT - TODO when Git status supports conflict markers
    
    private static final char GIT_STATUS_CODE_ABORT = 'a' + 'b';    // NOI18N
    public static final String GIT_STR_CONFLICT_EXT = ".conflict~"; // NOI18N

    private static final String GIT_EPOCH_PLUS_ONE_YEAR = "1971-01-01"; // NOI18N    
    

    /**
     * Merge working directory with the head revision
     * Merge the contents of the current working directory and the
     * requested revision. Files that changed between either parent are
     * marked as changed for the next commit and a commit must be
     * performed before any further updates are allowed.
     *
     * @param File repository of the Git repository's root directory
     * @param Revision to merge with, if null will merge with default tip rev
     * @return git merge output
     * @throws GitException
     */
    public static List<String> doMerge(File repository, String revStr) throws GitException {
        if (repository == null ) return null;
        List<String> command = new ArrayList<String>();
        List<String> env = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_MERGE_CMD);
        command.add(GIT_MERGE_FORCE_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        if(revStr != null)
             command.add(revStr);
        env.add(GIT_MERGE_ENV);
        
        List<String> list = execEnv(command, env);
        return list;
    }
    
    /**
     * Update the working directory to the tip revision.
     * By default, update will refuse to run if doing so would require
     * merging or discarding local changes.
     *
     * @param File repository of the Git repository's root directory
     * @param Boolean force an Update and overwrite any modified files in the  working directory
     * @param String revision to be updated to
     * @param Boolean throw exception on error
     * @return git update output
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doUpdateAll(File repository, boolean bForce, String revision, boolean bThrowException) throws GitException {
        if (repository == null ) return null;
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_UPDATE_ALL_CMD);
        if (bForce) command.add(GIT_UPDATE_FORCE_ALL_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        if (revision != null){
            command.add(revision);
        }

        List<String> list = exec(command);
        if (bThrowException) {
            if (!list.isEmpty()) {
                if  (isErrorUpdateSpansBranches(list.get(0))) {
                    throw new GitException(NbBundle.getMessage(GitCommand.class, "MSG_WARN_UPDATE_MERGE_TEXT"));
                } else if (isMergeAbortUncommittedMsg(list.get(0))) {
                    throw new GitException(NbBundle.getMessage(GitCommand.class, "MSG_WARN_UPDATE_COMMIT_TEXT"));
                }
            }
        }
        return list;
    }
    
    public static List<String> doUpdateAll(File repository, boolean bForce, String revision) throws GitException {
        return doUpdateAll(repository, bForce, revision, true);
    }
    
    /**
     * Roll back the last transaction in this repository.
     * 
     * Transactions are used to encapsulate the effects of all commands
     * that create new changesets or propagate existing changesets into a
     * repository. For example, the following commands are transactional,
     * and their effects can be rolled back:
     * commit, import, pull, push (with this repository as destination)
     * unbundle
     * There is only one level of rollback, and there is no way to undo a rollback.
     *
     * @param File repository of the Git repository's root directory
     * @return git update output
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doRollback(File repository, OutputLogger logger) throws GitException {
        if (repository == null ) return null;
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_ROLLBACK_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());

        List<String> list = exec(command);
        if (list.isEmpty())
            handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_ROLLBACK_FAILED"), logger);
        
        return list;
    }
    public static List<String> doBackout(File repository, String revision, 
            boolean doMerge, String commitMsg, OutputLogger logger) throws GitException {
        if (repository == null ) return null;
        List<String> env = new ArrayList<String>();
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_BACKOUT_CMD);
        if(doMerge){
            command.add(GIT_BACKOUT_MERGE_CMD);
            env.add(GIT_MERGE_ENV);
        }

        if (commitMsg != null && !commitMsg.equals("")) { // NOI18N
            command.add(GIT_BACKOUT_COMMIT_MSG_CMD);
            command.add(commitMsg);
        } else {
            command.add(GIT_BACKOUT_COMMIT_MSG_CMD);
            command.add(NbBundle.getMessage(GitCommand.class, "MSG_BACKOUT_MERGE_COMMIT_MSG", revision));  // NOI18N          
        }

        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        if (revision != null){
            command.add(GIT_BACKOUT_REV_CMD);            
            command.add(revision);
        }
        
        List<String> list;
        if(doMerge){
            list = execEnv(command, env);
        }else{
            list = exec(command);            
        }
        if (list.isEmpty())
            handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_BACKOUT_FAILED"), logger);
        
        return list;
    }
    
    public static List<String> doStrip(File repository, String revision, 
            boolean doForceMultiHead, boolean doBackup, OutputLogger logger) throws GitException {
        if (repository == null ) return null;
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_CONFIG_OPTION_CMD);
        command.add(GIT_STRIP_EXT_CMD);
        command.add(GIT_STRIP_CMD);
        if(doForceMultiHead){
            command.add(GIT_STRIP_FORCE_MULTIHEAD_CMD);
        }
        if(!doBackup){
            command.add(GIT_STRIP_NOBACKUP_CMD);
        }
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        if (revision != null){
            command.add(revision);
        }

        List<String> list = exec(command);
        if (list.isEmpty())
            handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_STRIP_FAILED"), logger);
        
        return list;
    }

    /**
     * Returns the version of Git, e.g. 1.5.3.7.
     * 
     * @return String
     */
    public static String getGitVersion() {
        List<String> list = new LinkedList<String>();
        try {
            list = execForVersionCheck();
        } catch (GitException ex) {
            // Ignore Exception
            return null;
        }
        if (!list.isEmpty()) {
            int start = list.get(0).indexOf('(');
            int end = list.get(0).indexOf(')');
            if (start != -1 && end != -1) {
                return list.get(0).substring(start + 9, end);
            }
        }
        return null;
    }
    
    /**
     * Pull changes from the default pull locarion and update working directory.
     * By default, update will refuse to run if doing so would require
     * merging or discarding local changes.
     *
     * @param File repository of the Git repository's root directory
     * @return git pull output
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doPull(File repository, OutputLogger logger) throws GitException {
        return doPull(repository, null, logger);
    }
    
    /**
     * Pull changes from the specified repository and update working directory.
     * By default, update will refuse to run if doing so would require
     * merging or discarding local changes.
     *
     * @param File repository of the Git repository's root directory
     * @param String source repository to pull from
     * @return git pull output
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doPull(File repository, String from, OutputLogger logger) throws GitException {
        if (repository == null ) return null;
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_PULL_CMD);
        command.add(GIT_UPDATE_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        if (from != null) {
            command.add(from);
        }

        List<String> list;
        String defaultPull = new GitConfigFiles(repository).getDefaultPull(false);
        String proxy = getGlobalProxyIfNeeded(defaultPull, true, logger);
        if(proxy != null){
            List<String> env = new ArrayList<String>(); 
            env.add(GIT_PROXY_ENV + proxy);
            list = execEnv(command, env);
        }else{
            list = exec(command);
        }

        if (!list.isEmpty() && 
             isErrorAbort(list.get(list.size() -1))) {
            handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_COMMAND_ABORTED"), logger);
        }
        return list;
    }
    
   /**
     * Unbundle changes from the specified local source repository and
     * update working directory.
     * By default, update will refuse to run if doing so would require
     * merging or discarding local changes.
     *
     * @param File repository of the Git repository's root directory
     * @param File bundle identfies the compressed changegroup file to be applied
     * @return git unbundle output
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doUnbundle(File repository, File bundle, OutputLogger logger) throws GitException {
        if (repository == null ) return null;
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_UNBUNDLE_CMD);
        command.add(GIT_UPDATE_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        if (bundle != null) {
            command.add(bundle.getAbsolutePath());
        }

        List<String> list = exec(command);
        if (!list.isEmpty() && 
             isErrorAbort(list.get(list.size() -1))) {
            handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_COMMAND_ABORTED"), logger);
        }
        return list;
    }
 
    /**
     * Show the changesets that would be pulled if a pull
     * was requested from the default pull location
     *
     * @param File repository of the Git repository's root directory
     * @return git incoming output
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doIncoming(File repository, OutputLogger logger) throws GitException {
        return doIncoming(repository, null, null, logger);
    }

    /**
     * Show the changesets that would be pulled if a pull
     * was requested from the specified repository
     *
     * @param File repository of the Git repository's root directory
     * @param String source repository to query
     * @param File bundle to store downloaded changesets.
     * @return git incoming output
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doIncoming(File repository, String from, File bundle, OutputLogger logger) throws GitException {
        if (repository == null ) return null;
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_INCOMING_CMD);
        command.add(GIT_VERBOSE_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        if (bundle != null) {
            command.add(GIT_OPT_BUNDLE);
            command.add(bundle.getAbsolutePath());
        }
        if (from != null) {
            command.add(from);
        }

        List<String> list;
        String defaultPull = new GitConfigFiles(repository).getDefaultPull(false);
        String proxy = getGlobalProxyIfNeeded(defaultPull, false, null);
        if(proxy != null){
            List<String> env = new ArrayList<String>(); 
            env.add(GIT_PROXY_ENV + proxy);
            list = execEnv(command, env);
        }else{
            list = exec(command);
        }

        if (!list.isEmpty() && 
             isErrorAbort(list.get(list.size() -1))) {
            handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_COMMAND_ABORTED"), logger);
        }
        return list;
    }
    
    /**
     * Show the changesets that would be pushed if a push
     * was requested to the specified local source repository
     *
     * @param File repository of the Git repository's root directory
     * @param String source repository to query
     * @return git outgoing output
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doOutgoing(File repository, String to, OutputLogger logger) throws GitException {
        if (repository == null ) return null;
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_OUTGOING_CMD);
        command.add(GIT_VERBOSE_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        command.add(to);

        List<String> list;
        String defaultPush = new GitConfigFiles(repository).getDefaultPush(false);
        String proxy = getGlobalProxyIfNeeded(defaultPush, false, null);
        if(proxy != null){
            List<String> env = new ArrayList<String>(); 
            env.add(GIT_PROXY_ENV + proxy);
            list = execEnv(command, env);
        }else{
            list = exec(command);
        }
        if (!list.isEmpty() && 
             isErrorAbort(list.get(list.size() -1))) {
            handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_COMMAND_ABORTED"), logger);
        }
        return list;
    }

    /**
     * Push changes to the specified repository
     * By default, push will refuse to run if doing so would create multiple heads
     *
     * @param File repository of the Git repository's root directory
     * @param String source repository to push to
     * @return git push output
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doPush(File repository, String to, OutputLogger logger) throws GitException {
        if (repository == null || to == null ) return null;
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_PUSH_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        command.add(to);

        List<String> list;
        String defaultPush = new GitConfigFiles(repository).getDefaultPush(false);
        String proxy = getGlobalProxyIfNeeded(defaultPush, true, logger);
        if(proxy != null){
            List<String> env = new ArrayList<String>(); 
            env.add(GIT_PROXY_ENV + proxy);
            list = execEnv(command, env);
        }else{
            list = exec(command);
        }


        if (!list.isEmpty() && 
            !isErrorAbortPush(list.get(list.size() -1)) &&
            isErrorAbort(list.get(list.size() -1))) {
            handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_COMMAND_ABORTED"), logger);
        }
        return list;
    }

    /**
     * Run the command git view for the specified repository
     *
     * @param File repository of the Git repository's root directory
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doView(File repository, OutputLogger logger) throws GitException {
        if (repository == null) return null;
        List<String> command = new ArrayList<String>();
        List<String> env = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_VIEW_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        
        List<String> list;

        if(GitUtils.isSolaris()){
            env.add(GIT_GITK_PATH_SOLARIS10_ENV);
            list = execEnv(command, env);
        }else{
            list = exec(command);
        }

        if (!list.isEmpty()) {
            if (isErrorNoView(list.get(list.size() -1))) {
                throw new GitException(NbBundle.getMessage(GitCommand.class, "MSG_WARN_NO_VIEW_TEXT"));
             }
            else if (isErrorGitkNotFound(list.get(0))) {
                throw new GitException(NbBundle.getMessage(GitCommand.class, "MSG_WARN_GITK_NOT_FOUND_TEXT"));
            } else if (isErrorAbort(list.get(list.size() -1))) {
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_COMMAND_ABORTED"), logger);
            }
        } 
        return list;
    }

    static File getRootFile(VCSContext ctx) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    private static String getGlobalProxyIfNeeded(String defaultPath, boolean bOutputDetails, OutputLogger logger){
        String proxy = null;
        if(defaultPath != null && 
                (defaultPath.startsWith("http:") || defaultPath.startsWith("https:"))){ // NOI18N
            GitProxySettings ps = new GitProxySettings();
            if (ps.isManualSetProxy()) {
                if ((defaultPath.startsWith("http:") && !ps.getHttpHost().equals(""))||
                    (defaultPath.startsWith("https:") && !ps.getHttpHost().equals("") && ps.getHttpsHost().equals(""))) { // NOI18N
                    proxy = ps.getHttpHost();
                    if (proxy != null && !proxy.equals("")) {
                        proxy += ps.getHttpPort() > -1 ? ":" + Integer.toString(ps.getHttpPort()) : ""; // NOI18N
                    } else {
                        proxy = null;
                    }                    
                } else if (defaultPath.startsWith("https:") && !ps.getHttpsHost().equals("")) { // NOI18N
                    proxy = ps.getHttpsHost();
                    if (proxy != null && !proxy.equals("")) {
                        proxy += ps.getHttpsPort() > -1 ? ":" + Integer.toString(ps.getHttpsPort()) : ""; // NOI18N
                    } else {
                        proxy = null;
                    }
                }
            }
        }
        if(proxy != null && bOutputDetails){
            logger.output(NbBundle.getMessage(GitCommand.class, "MSG_USING_PROXY_INFO", proxy)); // NOI18N
        }
        return proxy;
    }
    /**
     * Run the fetch extension for the specified repository.
     *
     * @param File repository of the Git repository's root directory
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doFetch(File repository, OutputLogger logger) throws GitException {
        if (repository == null) return null;
        List<String> command = new ArrayList<String>();
        
        command.add(getGitCommand());
        command.add(GIT_CONFIG_OPTION_CMD);
        command.add(GIT_FETCH_EXT_CMD);
        command.add(GIT_FETCH_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        
        List<String> list;
        String defaultPull = new GitConfigFiles(repository).getDefaultPull(false);
        String proxy = getGlobalProxyIfNeeded(defaultPull, true, logger);
        if(proxy != null){
            List<String> env = new ArrayList<String>(); 
            env.add(GIT_PROXY_ENV + proxy);
            list = execEnv(command, env);
        }else{
            list = exec(command);
        }

        if (!list.isEmpty()) {
            if (isErrorAbort(list.get(list.size() -1))) {
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_COMMAND_ABORTED"), logger);
            }
        } 
        return list;
    }
    
    private static List<GitLogMessage> processLogMessages(List<String> list, final List<GitLogMessage> messages) {
        String rev, author, desc, date, id, fm, fa, fd, fc;
        if (list != null && !list.isEmpty()) {
            rev = author = desc = date = id = fm = fa = fd = fc = null;
            boolean bEnd = false;
            for (String s : list) {
                if (s.indexOf(GIT_LOG_REVISION_OUT) == 0) {
                    rev = s.substring(GIT_LOG_REVISION_OUT.length()).trim();
                } else if (s.indexOf(GIT_LOG_AUTHOR_OUT) == 0) {
                    author = s.substring(GIT_LOG_AUTHOR_OUT.length()).trim();
                } else if (s.indexOf(GIT_LOG_DESCRIPTION_OUT) == 0) {
                    desc = s.substring(GIT_LOG_DESCRIPTION_OUT.length()).trim();
                } else if (s.indexOf(GIT_LOG_DATE_OUT) == 0) {
                    date = s.substring(GIT_LOG_DATE_OUT.length()).trim();
                } else if (s.indexOf(GIT_LOG_ID_OUT) == 0) {
                    id = s.substring(GIT_LOG_ID_OUT.length()).trim();
                } else if (s.indexOf(GIT_LOG_FILEMODS_OUT) == 0) {
                    fm = s.substring(GIT_LOG_FILEMODS_OUT.length()).trim();
                } else if (s.indexOf(GIT_LOG_FILEADDS_OUT) == 0) {
                    fa = s.substring(GIT_LOG_FILEADDS_OUT.length()).trim();
                } else if (s.indexOf(GIT_LOG_FILEDELS_OUT) == 0) {
                    fd = s.substring(GIT_LOG_FILEDELS_OUT.length()).trim();
                } else if (s.indexOf(GIT_LOG_FILECOPIESS_OUT) == 0) {
                    fc = s.substring(GIT_LOG_FILECOPIESS_OUT.length()).trim();
                } else if (s.indexOf(GIT_LOG_ENDCS_OUT) == 0) {
                    bEnd = true;
                } else {
                    // Ignore all other lines
                }

                if (rev != null & bEnd) {
                    messages.add(new GitLogMessage(rev, author, desc, date, id, fm, fa, fd, fc));
                    rev = author = desc = date = id = fm = fa = fd = fc = null;
                    bEnd = false;
                }
            }
        }
        return messages;
    }
    
    public static GitLogMessage[] getIncomingMessages(final String rootUrl, String toRevision, boolean bShowMerges,  OutputLogger logger) {
        final List<GitLogMessage> messages = new ArrayList<GitLogMessage>(0);  
        final File root = new File(rootUrl);
        
        try {

            List<String> list = new LinkedList<String>();
            list = GitCommand.doIncomingForSearch(root, toRevision, bShowMerges, logger);
            processLogMessages(list, messages);

        } catch (GitException ex) {
            NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(ex);
            DialogDisplayer.getDefault().notifyLater(e);
        } finally {
            logger.closeLog();
        }
        
        return messages.toArray(new GitLogMessage[0]);
    }       
   
    public static GitLogMessage[] getOutMessages(final String rootUrl, boolean bShowMerges, OutputLogger logger) {
        final List<GitLogMessage> messages = new ArrayList<GitLogMessage>(0);  
        final File root = new File(rootUrl);
        
        try {

            List<String> list = new LinkedList<String>();
            list = GitCommand.doOut(root, bShowMerges, logger);
            processLogMessages(list, messages);

        } catch (GitException ex) {
            NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(ex);
            DialogDisplayer.getDefault().notifyLater(e);
        } finally {
            logger.closeLog();
        }
        
        return messages.toArray(new GitLogMessage[0]);
    }       
   
    public static GitLogMessage[] getLogMessages(final String rootUrl, 
            final Set<File> files, String fromRevision, String toRevision, boolean bShowMerges,  OutputLogger logger) {
        final List<GitLogMessage> messages = new ArrayList<GitLogMessage>(0);  
        final File root = new File(rootUrl);
        
        try {
            String headRev = GitCommand.getLastRevision(root, null);
            if (headRev == null) {
                return messages.toArray(new GitLogMessage[0]);
            }

            List<String> list = new LinkedList<String>();
            list = GitCommand.doLogForHistory(root, 
                    files != null ? new ArrayList<File>(files) : null,
                    fromRevision, toRevision, headRev, bShowMerges, logger);
            processLogMessages(list, messages);
            
        } catch (GitException ex) {
            NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(ex);
            DialogDisplayer.getDefault().notifyLater(e);
        } finally {
            logger.closeLog();
        }
        
        return messages.toArray(new GitLogMessage[0]);
   }
     
    /**
     * Determines whether repository requires a merge - has more than 1 heads
     *
     * @param File repository of the Git repository's root directory
     * @return Boolean which is true if the repository needs a merge
     */
    public static Boolean isMergeRequired(File repository) {
        if (repository == null ) return false;
        
        try {
            List<String> list = getHeadRevisions(repository);

            if (!list.isEmpty() && list.size() > 1){
                Git.LOG.log(Level.FINE, "isMergeRequired(): TRUE " + list); // NOI18N
                return true;
            }else{
                Git.LOG.log(Level.FINE, "isMergeRequired(): FALSE " + list); // NOI18N
                return false;
            }
        } catch (GitException e) {
            return false;
        }
    }
  
    /**
     * Determines whether anything has been committed to the repository
     *
     * @param File repository of the Git repository's root directory
     * @return Boolean which is true if the repository has revision history.
     */
    public static Boolean hasHistory(File repository) {
        if (repository == null ) return false;
        
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_LOG_CMD);
        command.add(GIT_LOG_LIMIT_ONE_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());

        try {
            List<String> list = exec(command);
            if (!list.isEmpty() && isErrorAbort(list.get(0)))
                return false;
            else
                return !list.isEmpty();
        } catch (GitException e) {
            return false;
        }
    }

    /**
     * Determines the previous name of the specified file.
     * 
     * We make the assumption that the previous file name is in the
     * list of files returned by git log command immediately befor
     * the file we started with.
     *
     * @param File repository of the Git repository's root directory
     * @param File file of the file whose previous name is required
     * @param String revision which the revision to start from.
     * @return File for the previous name of the file
     */
    private static File getPreviousName(File repository, File file, String revision) throws GitException {
        if (repository == null ) return null;
        if (revision == null ) return null;
        
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_LOG_CMD);
        command.add(GIT_OPT_FOLLOW);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        command.add(GIT_FLAG_REV_CMD);
        command.add(revision);
        command.add(GIT_GET_PREVIOUS_TEMPLATE_CMD);
       
        command.add(file.getAbsolutePath());

        List<String> list = exec(command);
        try {
            list = exec(command);
            if (!list.isEmpty() && isErrorAbort(list.get(0)))
                return null;
        } catch (GitException e) {
            return null;
        }
        String[] fileNames = list.get(0).split(" ");
        for (int j = fileNames.length -1 ; j > 0; j--) {
            File name = new File(repository, fileNames[j]);
            if (name.equals(file)) {
               return new File(repository, fileNames[j-1]); 
            }
        }
        return null;
    }
    
    /**
     * Retrives the log information with just first line of commit message for the specified file.
     *
     * @param File repository of the Git repository's root directory
     * @param File of file which revision history is to be retrieved.
     * @return List<String> list of the log entries for the specified file.
     * @throws org.netbeans.modules.git.GitException
     */
     public static List<String> doLogShort(File repository, File file, OutputLogger logger) throws GitException {
        return doLog(repository, file, GIT_LOG_TEMPLATE_SHORT_CMD, false, logger);
     }
     
     /**
     * Retrives the log information with the full commit message for the specified file.
     *
     * @param File repository of the Git repository's root directory
     * @param File of file which revision history is to be retrieved.
     * @return List<String> list of the log entries for the specified file.
     * @throws org.netbeans.modules.git.GitException
     */
     public static List<String> doLogLong(File repository, File file, OutputLogger logger) throws GitException {
        return doLog(repository, file, GIT_LOG_TEMPLATE_LONG_CMD, false, logger);
     }

     /**
     * Retrives the log information for the specified file, as defined by the LOG_TEMPLATE.
     *
     * @param File repository of the Git repository's root directory
     * @param File of file which revision history is to be retrieved.
     * @param String Template specifying how output should be returned
     * @param boolean flag indicating if debug param should be used - required to get all file mod, add, del info
     * @return List<String> list of the log entries for the specified file.
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doLog(File repository, File file, String LOG_TEMPLATE, boolean bDebug, OutputLogger logger) throws GitException {
        if (repository == null ) return null;
        
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_LOG_CMD);
        if (!file.isDirectory()) {
            command.add(GIT_OPT_FOLLOW);
        }
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        if(bDebug){
            command.add(GIT_LOG_DEBUG_CMD);
        }
        command.add(LOG_TEMPLATE);            
        command.add(file.getAbsolutePath());

        List<String> list = exec(command);
        if (!list.isEmpty()) {
            if (isErrorNoRepository(list.get(0))) {
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_NO_REPOSITORY_ERR"), logger);
             } else if (isErrorAbort(list.get(0))) {
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_COMMAND_ABORTED"), logger);
             }
        }
        return list;
    }
    
    /**
     * Retrives the log information for the specified files.
     *
     * @param File repository of the Git repository's root directory
     * @param List<File> of files which revision history is to be retrieved.
     * @param String Template specifying how output should be returned
     * @param boolean flag indicating if debug param should be used - required to get all file mod, add, del info
     * @return List<String> list of the log entries for the specified file.
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doLog(File repository, List<File> files, String LOG_TEMPLATE, boolean bDebug, OutputLogger logger) throws GitException {
        if (repository == null ) return null;
        if (files != null && files.isEmpty()) return null;
        
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_VERBOSE_CMD);
        command.add(GIT_LOG_CMD);
        boolean doFollow = true;
        if( files != null){
            for (File f : files) {
                if (f.isDirectory()) {
                    doFollow = false;
                    break;
                }
            }
        }
        if (doFollow) {
            command.add(GIT_OPT_FOLLOW);
        }
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        if(bDebug){
            command.add(GIT_LOG_DEBUG_CMD);
        }
        if(LOG_TEMPLATE != null){
            command.add(LOG_TEMPLATE);
        }
        if( files != null){
            for (File f : files) {
                command.add(f.getAbsolutePath());
            }
        }
        
        List<String> list = exec(command);
        if (!list.isEmpty()) {
            if (isErrorNoRepository(list.get(0))) {
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_NO_REPOSITORY_ERR"), logger);
             } else if (isErrorAbort(list.get(0))) {
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_COMMAND_ABORTED"), logger);
             }
        }
        return list;
    }
    
    /**
     * Retrives the log information for the specified files.
     *
     * @param File repository of the Git repository's root directory
     * @param List<File> of files which revision history is to be retrieved.
     * @param String Template specifying how output should be returned
     * @param boolean flag indicating if debug param should be used - required to get all file mod, add, del info
     * @return List<String> list of the log entries for the specified file.
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doLogForHistory(File repository, List<File> files, 
            String from, String to, String headRev, boolean bShowMerges, OutputLogger logger) throws GitException {
        if (repository == null ) return null;
        if (files != null && files.isEmpty()) return null;
        
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_VERBOSE_CMD);
        command.add(GIT_LOG_CMD);
        boolean doFollow = true;
        if( files != null){
            for (File f : files) {
                if (f.isDirectory()) {
                    doFollow = false;
                    break;
                }
            }
        }
        if (doFollow) {
            command.add(GIT_OPT_FOLLOW);
        }
        if(!bShowMerges){
            command.add(GIT_LOG_NO_MERGES_CMD);
        }
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        command.add(GIT_LOG_DEBUG_CMD);
        
        String dateStr = handleRevDates(from, to);
        if(dateStr != null){
            command.add(GIT_FLAG_DATE_CMD);
            command.add(dateStr);
        }  
        String revStr = handleRevNumbers(from, to, headRev);
        if(dateStr == null && revStr != null){
            command.add(GIT_FLAG_REV_CMD);
            command.add(revStr);
        }        
        command.add(GIT_LOG_TEMPLATE_HISTORY_CMD);

        if( files != null){
            for (File f : files) {
                command.add(f.getAbsolutePath());
            }
        }  
        List<String> list = exec(command);
        if (!list.isEmpty()) {
            if (isErrorNoRepository(list.get(0))) {
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_NO_REPOSITORY_ERR"), logger);
             } else if (isErrorAbort(list.get(0))) {
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_COMMAND_ABORTED"), logger);
             }
        }
        return list;
    }
    
    /**
     * Retrives the Out information for the specified repository
     *
     * @param File repository of the Git repository's root directory
     * @return List<String> list of the out entries for the specified repo.
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doOut(File repository, boolean bShowMerges, OutputLogger logger) throws GitException {
        if (repository == null ) return null;
        
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_OUT_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        if(!bShowMerges){
            command.add(GIT_LOG_NO_MERGES_CMD);
        }
        command.add(GIT_LOG_DEBUG_CMD);
        
        command.add(GIT_LOG_TEMPLATE_HISTORY_CMD);

        List<String> list;
        String defaultPush = new GitConfigFiles(repository).getDefaultPush(false);
        String proxy = getGlobalProxyIfNeeded(defaultPush, false, null);
        if(proxy != null){
            List<String> env = new ArrayList<String>(); 
            env.add(GIT_PROXY_ENV + proxy);
            list = execEnv(command, env);
        }else{
            list = exec(command);
        }
        if (!list.isEmpty()) {
            if(isErrorNoDefaultPush(list.get(0))){
                // Ignore
            }else if (isErrorNoRepository(list.get(0))) {
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_NO_REPOSITORY_ERR"), logger);
             } else if (isErrorAbort(list.get(0))) {
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_COMMAND_ABORTED"), logger);
             }
        }
        return list;
    }

        /**
     * Retrives the Incoming changeset information for the specified repository
     *
     * @param File repository of the Git repository's root directory
     * @return List<String> list of the out entries for the specified repo.
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doIncomingForSearch(File repository, String to, boolean bShowMerges, OutputLogger logger) throws GitException {
        if (repository == null ) return null;
        
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_INCOMING_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        if(!bShowMerges){
            command.add(GIT_LOG_NO_MERGES_CMD);
        }
        command.add(GIT_LOG_DEBUG_CMD);        
        String revStr = handleIncomingRevNumber(to);
        if(revStr != null){
            command.add(GIT_FLAG_REV_CMD);
            command.add(revStr);
        }        
        command.add(GIT_LOG_TEMPLATE_HISTORY_CMD);

        List<String> list = exec(command);
        String defaultPull = new GitConfigFiles(repository).getDefaultPull(false);
        String proxy = getGlobalProxyIfNeeded(defaultPull, false, null);
        if(proxy != null){
            List<String> env = new ArrayList<String>(); 
            env.add(GIT_PROXY_ENV + proxy);
            list = execEnv(command, env);
        }else{
            list = exec(command);
        }

        if (!list.isEmpty()) {
            if (isErrorNoDefaultPath(list.get(0))) {
            // Ignore
            } else if (isErrorNoRepository(list.get(0))) {
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_NO_REPOSITORY_ERR"), logger);
            } else if (isErrorAbort(list.get(0)) || isErrorAbort(list.get(list.size() - 1))) {
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_COMMAND_ABORTED"), logger);
            }
        }
        return list;
    }

    private static String handleRevDates(String from, String to){
        // Check for Date range:
        Date fromDate = null;
        Date toDate = null;
        Date currentDate = new Date(); // Current Date            
        Date epochPlusOneDate = null;
        
        try {
            epochPlusOneDate = new SimpleDateFormat("yyyy-MM-dd").parse(GIT_EPOCH_PLUS_ONE_YEAR); // NOI18N
        } catch (ParseException ex) {
            // Ignore invalid dates
        }

        // Set From date
        try {
            if(from != null) 
                fromDate = new SimpleDateFormat("yyyy-MM-dd").parse(from); // NOI18N
        } catch (ParseException ex) {
            // Ignore invalid dates
        }

        // Set To date
        try {
            if(to != null) 
                toDate = new SimpleDateFormat("yyyy-MM-dd").parse(to); // NOI18N
        } catch (ParseException ex) {
            // Ignore invalid dates
        }

        // If From date is set, but To date is not - default To date to current date
        if( fromDate != null && toDate == null && to == null){
            toDate = currentDate;           
            to = new SimpleDateFormat("yyyy-MM-dd").format(toDate);
        }
        // If To date is set, but From date is not - default From date to 1971-01-01
        if (fromDate == null && from == null  && toDate != null) {
            fromDate = epochPlusOneDate; 
            from = GIT_EPOCH_PLUS_ONE_YEAR; // NOI18N
        }
        
        // If using dates make sure both From and To are set to dates
        if( (fromDate != null && toDate == null && to != null) || 
                (fromDate == null && from != null && toDate != null)){
            GitUtils.warningDialog(GitCommand.class,"MSG_SEARCH_HISTORY_TITLE",// NOI18N
                    "MSG_SEARCH_HISTORY_WARN_BOTHDATES_NEEDED_TEXT");   // NOI18N
            return null;
        }

        if(fromDate != null && toDate != null){            
            // Check From date - default to 1971-01-01 if From date is earlier than this
            if(epochPlusOneDate != null && fromDate.before(epochPlusOneDate)){
                fromDate = epochPlusOneDate;
                from = GIT_EPOCH_PLUS_ONE_YEAR; // NOI18N
            }
            // Set To date - default to current date if To date is later than this
            if(currentDate != null && toDate.after(currentDate)){
                toDate = currentDate;
                to = new SimpleDateFormat("yyyy-MM-dd").format(toDate);
            }
        
            // Make sure the From date is before the To date
            if( fromDate.after(toDate)){
                GitUtils.warningDialog(GitCommand.class,"MSG_SEARCH_HISTORY_TITLE",// NOI18N
                        "MSG_SEARCH_HISTORY_WARN_FROM_BEFORE_TODATE_NEEDED_TEXT");   // NOI18N
                return null;
            }
            return from + " to " + to; // NOI18N
        }
        return null;
    }

    private static String handleIncomingRevNumber(String to) {
        int toInt = -1;

        // Handle users entering head or tip for revision, instead of a number
        if (to != null && (to.equalsIgnoreCase(GIT_STATUS_FLAG_TIP_CMD) || to.equalsIgnoreCase(GIT_HEAD_STR))) {
            to = GIT_STATUS_FLAG_TIP_CMD;
        }
        try {
            toInt = Integer.parseInt(to);
        } catch (NumberFormatException e) {
            // ignore invalid numbers
        }

        return (toInt > -1) ? to : GIT_STATUS_FLAG_TIP_CMD;
    }

    private static String handleRevNumbers(String from, String to, String headRev){
        int fromInt = -1;
        int toInt = -1;
        int headRevInt = -1;

        // Handle users entering head or tip for revision, instead of a number
        if(from != null && (from.equalsIgnoreCase(GIT_STATUS_FLAG_TIP_CMD) || from.equalsIgnoreCase(GIT_HEAD_STR)))
            from = headRev;
        if(to != null && (to.equalsIgnoreCase(GIT_STATUS_FLAG_TIP_CMD) || to.equalsIgnoreCase(GIT_HEAD_STR)))
            to = headRev;
        
        try{
            fromInt = Integer.parseInt(from);
        }catch (NumberFormatException e){
            // ignore invalid numbers
        }
        try{
            toInt = Integer.parseInt(to);
        }catch (NumberFormatException e){
            // ignore invalid numbers
        }
        try{
            headRevInt = Integer.parseInt(headRev);
        }catch (NumberFormatException e){
            // ignore invalid numbers
        }
        
        // Handle out of range revisions
        if (headRevInt > -1 && toInt > headRevInt) {
            to = headRev;
            toInt = headRevInt;
        }
        if (headRevInt > -1 && fromInt > headRevInt) {
            from = headRev;
            fromInt = headRevInt;
        }
        
        // Handle revision ranges
        String revStr = null;
        if (fromInt > -1 && toInt > -1){
            revStr = from + ":" + to;
        }else if (fromInt > -1){
            revStr = from + (headRevInt != -1 ? ":" + headRevInt: "");
        }else if (toInt > -1){
            revStr = "0:" + to;
        }
        
        return revStr;
    }
    /**
     * Retrieves the base revision of the specified file to the
     * specified output file.
     *
     * @param File repository of the Git repository's root directory
     * @param File file in the Git repository
     * @param File outFile to contain the contents of the file
     * @throws org.netbeans.modules.git.GitException
     */
    public static void doCat(File repository, File file, File outFile, OutputLogger logger) throws GitException {
        doCat(repository, file, outFile, "tip", false, logger); //NOI18N
    }
    
    /**
     * Retrieves the specified revision of the specified file to the
     * specified output file.
     *
     * @param File repository of the Git repository's root directory
     * @param File file in the Git repository
     * @param File outFile to contain the contents of the file
     * @param String of revision for the revision of the file to be
     * printed to the output file.
     * @return List<String> list of all the log entries
     * @throws org.netbeans.modules.git.GitException
     */
    public static void doCat(File repository, File file, File outFile, String revision, OutputLogger logger) throws GitException {
        doCat(repository, file, outFile, revision, true, logger); //NOI18N
    }

    public static void doCat(File repository, File file, File outFile, String revision, boolean retry, OutputLogger logger) throws GitException {
        if (repository == null) return;
        if (file == null) return;
        
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_CAT_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        command.add(GIT_FLAG_OUTPUT_CMD);
        command.add(outFile.getAbsolutePath());

        if (revision != null) {
            command.add(GIT_FLAG_REV_CMD);
            command.add(revision);
        }
        command.add(file.getAbsolutePath());
        List<String> list = exec(command);
        
        if (!list.isEmpty()) {
            if (isErrorNoRepository(list.get(0))) {
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_NO_REPOSITORY_ERR"), logger);
             } else if (isErrorAbort(list.get(0))) {
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_COMMAND_ABORTED"), logger);
             }
        }
        if (outFile.length() == 0 && retry) {
            // Perhaps the file has changed its name
            String newRevision = Integer.toString(Integer.parseInt(revision)+1);
            File prevFile = getPreviousName(repository, file, newRevision); 
            if (prevFile != null) {
                doCat(repository, prevFile, outFile, revision, false, logger); //NOI18N
            }
        }
    }
    
    /**
     * Initialize a new repository in the given directory.  If the given
     * directory does not exist, it is created. Will throw a GitException
     * if the repository already exists.
     *
     * @param root for the Git repository
     * @return void
     * @throws org.netbeans.modules.git.GitException
     */
    public static void doCreate(File root, OutputLogger logger) throws GitException {
        if (root == null ) return;
        List<String> command = new ArrayList<String>();
        
        command.add(getGitCommand());
        command.add(GIT_CREATE_CMD);
        command.add(root.getAbsolutePath());

        List<String> list = exec(command);
        if (!list.isEmpty())
            handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_CREATE_FAILED"), logger);
    }
    
    /**
     * Clone an exisiting repository to the specified target directory
     *
     * @param File repository of the Git repository's root directory
     * @param target directory to clone to
     * @return clone output
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doClone(File repository, String target, OutputLogger logger) throws GitException {
        if (repository == null) return null;
        return doClone(repository.getAbsolutePath(), target, logger);
    }
    
    /**
     * Clone a repository to the specified target directory
     *
     * @param String repository of the Git repository
     * @param target directory to clone to
     * @return clone output
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doClone(String repository, String target, OutputLogger logger) throws GitException {
        if (repository == null || target == null) return null;
        
        // Ensure that parent directory of target exists, creating if necessary
        File fileTarget = new File (target);
        File parentTarget = fileTarget.getParentFile();
        try {
            if (!parentTarget.mkdir()) {
                if (!parentTarget.isDirectory()) {
                    Git.LOG.log(Level.WARNING, "File.mkdir() failed for : " + parentTarget.getAbsolutePath()); // NOI18N
                    throw (new GitException (NbBundle.getMessage(GitCommand.class, "MSG_UNABLE_TO_CREATE_PARENT_DIR"))); // NOI18N
                }
            }
        } catch (SecurityException e) {
            Git.LOG.log(Level.WARNING, "File.mkdir() for : " + parentTarget.getAbsolutePath() + " threw SecurityException " + e.getMessage()); // NOI18N
            throw (new GitException (NbBundle.getMessage(GitCommand.class, "MSG_UNABLE_TO_CREATE_PARENT_DIR"))); // NOI18N
        }
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_CLONE_CMD);
        command.add(GIT_VERBOSE_CMD);
        
        // TODO: Remove this for Git Port
        if (repository.startsWith("file://")) {
            command.add(repository.substring(7));
        } else {
            command.add(repository);
        }
        command.add(target);

        List<String> list = exec(command);
        if (!list.isEmpty()) {
            if (isErrorNoRepository(list.get(0))){
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_NO_REPOSITORY_ERR"), logger);
            }else if (isErrorNoResponse(list.get(list.size() -1))){
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_NO_RESPONSE_ERR"), logger);
            }else if (isErrorAbort(list.get(list.size() -1))){
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_COMMAND_ABORTED"), logger);
            }
        }
        return list;
    }
    
    /**
     * Commits the list of Locally Changed files to the Git Repository
     *
     * @param File repository of the Git repository's root directory
     * @param List<files> of files to be committed to Git
     * @param String for commitMessage
     * @return void
     * @throws org.netbeans.modules.git.GitException
     */
    public static void doCommit(File repository, List<File> commitFiles, String commitMessage, OutputLogger logger)  throws GitException {
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_COMMIT_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());

        String projectUserName = new GitConfigFiles(repository).getUserName(false);
        String globalUsername = GitConfigFiles.getInstance().getUserName();
        String username = null;
        if(projectUserName != null && projectUserName.length() > 0)
            username = projectUserName;
        else if (globalUsername != null && globalUsername.length() > 0)
           username = globalUsername;
    
        if(username != null ){
            command.add(GIT_OPT_USERNAME);
            command.add(username);
        }
        
        File tempfile = null;
        
        try {
            if (commitMessage == null || commitMessage.length() == 0) {
                commitMessage = GIT_COMMIT_DEFAULT_MESSAGE;
            }
            // Create temporary file.
            tempfile = File.createTempFile(GIT_COMMIT_TEMPNAME, GIT_COMMIT_TEMPNAME_SUFFIX);
                
            // Write to temp file
            BufferedWriter out = new BufferedWriter(new FileWriter(tempfile));
            out.write(commitMessage);
            out.close();
              
            command.add(GIT_COMMIT_OPT_LOGFILE_CMD);
            command.add(tempfile.getAbsolutePath());

            for(File f: commitFiles){
                command.add(f.getAbsolutePath());
            }
            List<String> list = exec(command);
            
            if (!list.isEmpty()
                    && (isErrorNotTracked(list.get(0)) || isErrorCannotReadCommitMsg(list.get(0))))
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_COMMIT_FAILED"), logger);
            
        }catch (IOException ex){
            throw new GitException(NbBundle.getMessage(GitCommand.class, "MSG_FAILED_TO_READ_COMMIT_MESSAGE"));
        }finally{
            if (commitMessage != null && tempfile != null){
                tempfile.delete();
            }
        }
    }
    
    
    /**
     * Rename a source file to a destination file.
     * git mv 
     *
     * @param File repository of the Git repository's root directory
     * @param File of sourceFile which was renamed
     * @param File of destFile to which sourceFile has been renaned
     * @param boolean whether to do a rename --after
     * @return void
     * @throws org.netbeans.modules.git.GitException
     */
    public static void doRename(File repository, File sourceFile, File destFile, OutputLogger logger)  throws GitException {
        doRename(repository, sourceFile, destFile, false, logger);
    }

    private static void doRename(File repository, File sourceFile, File destFile, boolean bAfter, OutputLogger logger)  throws GitException {
        if (repository == null) return;
        
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_RENAME_CMD);
        if (bAfter) command.add(GIT_RENAME_AFTER_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        command.add(GIT_OPT_CWD_CMD);
        command.add(repository.getAbsolutePath());

        command.add(sourceFile.getAbsolutePath().substring(repository.getAbsolutePath().length()+1));            
        command.add(destFile.getAbsolutePath().substring(repository.getAbsolutePath().length()+1));
        
        List<String> list = exec(command);
        if (!list.isEmpty() &&
             isErrorAbort(list.get(list.size() -1))) {
            if (!bAfter || !isErrorAbortNoFilesToCopy(list.get(list.size() -1))) {
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_RENAME_FAILED"), logger);
            }
        }
    }
    
    /**
     * Mark a source file as having been renamed to a destination file.
     * git mv -A.
     *
     * @param File repository of the Git repository's root directory
     * @param File of sourceFile which was renamed
     * @param File of destFile to which sourceFile has been renaned
     * @return void
     * @throws org.netbeans.modules.git.GitException
     */
    public static void doRenameAfter(File repository, File sourceFile, File destFile, OutputLogger logger)  throws GitException {
       doRename(repository, sourceFile, destFile, true, logger);
    }
    
    /**
     * Adds the list of Locally New files to the Git Repository.
     * Their status will change to added and they will be added on the next
     * git add.
     *
     * @param File repository of the Git repository's root directory
     * @param List<Files> of files to be added to Git
     * @return void
     * @throws org.netbeans.modules.git.GitException
     */
    public static void doAdd(File repository, List<File> addFiles, OutputLogger logger)  throws GitException {
        if (repository == null) return;
        if (addFiles.size() == 0) return;
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_ADD_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());

        for(File f: addFiles){
            if(f.isDirectory())
                continue;
            // We do not look for files to ignore as we should not here
            // with a file to be ignored.
            command.add(f.getAbsolutePath());
        }
        List<String> list = exec(command);
        if (!list.isEmpty() && isErrorAlreadyTracked(list.get(0)))
            handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_ALREADY_TRACKED"), logger);
    }

    /**
     * Reverts the list of files in the Git Repository to the specified revision
     *
     * @param File repository of the Git repository's root directory
     * @param List<Files> of files to be reverted
     * @param String revision to be reverted to
     * @return void
     * @throws org.netbeans.modules.git.GitException
     */
    public static void doRevert(File repository, List<File> revertFiles, 
            String revision, boolean doBackup, OutputLogger logger)  throws GitException {
        if (repository == null) return;
        if (revertFiles.size() == 0) return;
        
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_REVERT_CMD);
        if(!doBackup){
            command.add(GIT_REVERT_NOBACKUP_CMD);
        }
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        if (revision != null){
            command.add(GIT_FLAG_REV_CMD);
            command.add(revision);
        }

        for(File f: revertFiles){
            command.add(f.getAbsolutePath());
        }
        List<String> list = exec(command);
        if (!list.isEmpty() && isErrorNoChangeNeeded(list.get(0)))
            handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_REVERT_FAILED"), logger);
    }

    /**
     * Adds a Locally New file to the Git Repository
     * The status will change to added and they will be added on the next
     * git commit.
     *
     * @param File repository of the Git repository's root directory
     * @param File of file to be added to Git
     * @return void
     * @throws org.netbeans.modules.git.GitException
     */
    public static void doAdd(File repository, File file, OutputLogger logger)  throws GitException {
        if (repository == null) return;
        if (file == null) return;
        if (file.isDirectory()) return;
        // We do not look for file to ignore as we should not here
        // with a file to be ignored.
        
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_ADD_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());

        command.add(file.getAbsolutePath());
        List<String> list = exec(command);
        if (!list.isEmpty() && isErrorAlreadyTracked(list.get(0)))
            handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_ALREADY_TRACKED"), logger);
    }
    
    /**
     * Get the annotations for the specified file
     *
     * @param File repository of the Git repository
     * @param File file to be annotated
     * @param String revision of the file to be annotated
     * @return List<String> list of the annotated lines of the file
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doAnnotate(File repository, File file, String revision, OutputLogger logger) throws GitException {
        if (repository == null) return null;
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_ANNOTATE_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());

        if (revision != null) {
            command.add(GIT_FLAG_REV_CMD);
            command.add(revision);
        }
        command.add(GIT_ANNOTATE_FLAGN_CMD);
        command.add(GIT_ANNOTATE_FLAGU_CMD);
        command.add(GIT_OPT_FOLLOW);
        command.add(file.getAbsolutePath());
        List<String> list = exec(command);
        if (!list.isEmpty()) {
            if (isErrorNoRepository(list.get(0))) {
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_NO_REPOSITORY_ERR"), logger);
            } else if (isErrorNoSuchFile(list.get(0))) {
                // This can happen if we have multiple heads and the wrong
                // one was picked by default git annotation 
                if (revision == null) {
                    String rev = getLastRevision(repository, file);
                    if (rev != null) {
                        list = doAnnotate(repository, file, rev, logger);
                    } else {
                        list = null;
                    }
                } else {
                    list = null;
                }
            }
        }
        return list;
    }
  
    public static List<String> doAnnotate(File repository, File file, OutputLogger logger) throws GitException {
        return doAnnotate(repository, file, null, logger);
    }

    /**
     * Get the revisions this file has been modified in.
     *
     * @param File repository of the Git repository's root directory
     * @param files to query revisions for
     * @param Int limit on nunmber of revisions (-1 for no limit)
     * @return List<String> list of the revisions of the file - {<rev>:<short cset hash>}
     *         or null if no commits made yet.
     */
    public static List<String> getRevisionsForFile(File repository, File[] files, int limit) {
        if (repository == null) return null;
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_LOG_CMD);
        if (limit >= 0) {
                command.add(GIT_LOG_LIMIT_CMD);
                command.add(Integer.toString(limit));
        }
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        command.add(GIT_CSET_TARGET_TEMPLATE_CMD);
        if(files != null) {
            for (File file : files) {
                command.add(file.getAbsolutePath());
            }
        }

        List<String> list = new ArrayList<String>();
        try {
            list = exec(command);
        } catch (GitException ex) {
            // Ignore Exception
        }
        return list == null || list.isEmpty()? null: list;
    }

    /**
     * Get the revisions for a repository
     *
     * @param File repository of the Git repository's root directory
     * @return List<String> list of the revisions of the repository - {<rev>:<short cset hash>}
     *         or null if no commits made yet.
     */
    public static List<String> getRevisions(File repository, int limit) {
        if (repository == null) return null;
        return getRevisionsForFile(repository, null, limit);
    }
    
    /**
     * Get the pull default for the specified repository, i.e. the default
     * destination for git pull commmands.
     *
     * @param File repository of the Git repository's root directory
     * @return String for pull default
     */
    public static String getPullDefault(File repository) {
        return getPathDefault(repository, GIT_PATH_DEFAULT_OPT);
    }

    /**
     * Get the push default for the specified repository, i.e. the default
     * destination for git push commmands.
     *
     * @param File repository of the Git repository's root directory
     * @return String for push default
     */
    public static String getPushDefault(File repository) {
        return getPathDefault(repository, GIT_PATH_DEFAULT_PUSH_OPT);
    }

    private static String getPathDefault(File repository, String type) {
        if (repository == null) return null;
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_PATH_DEFAULT_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        command.add(type);

        String res = null;
        
        List<String> list = new LinkedList<String>();
        try {
            list = exec(command);
        } catch (GitException ex) {
            // Ignore Exception
        }
        if( !list.isEmpty()
                    && (!isErrorNotFound(list.get(0)))) {
            res = list.get(0);
        }
        return res;
    }
    
    /**
     * Returns the Git branch name if any for a repository
     *
     * @param File repository of the git repository's root directory
     * @return String branch name or null if not named
     * @throws org.netbeans.modules.git.GitException
     */
    public static String getBranchName(File repository) throws GitException {
        if (repository == null) return null;
        
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_BRANCH_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());

        List<String> list = exec(command);
        if (!list.isEmpty()){
            return list.get(0);
        }else{
            return null;
        }
    }
    
    /**
     * Returns the Git branch revision for a repository
     *
     * @param File repository of the Git repository's root directory
     * @return int value of revision for repository tip
     * @throws org.netbeans.modules.git.GitException
     */
    public static int getBranchRev(File repository) throws GitException {
        if (repository == null) return -1;
        
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_BRANCH_REV_CMD);
        command.add(GIT_BRANCH_REV_TEMPLATE_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());

        List<String> list = exec(command);
        if (!list.isEmpty()){
            return Integer.parseInt(list.get(0)); 
        }else{
            return -1;
        }
    }
    
    /**
     * Returns the Git branch name if any for a repository
     *
     * @param File repository of the git repository's root directory
     * @return String branch short change set hash
     * @throws org.netbeans.modules.git.GitException
     */
    public static String getBranchShortChangesetHash(File repository) throws GitException {
        if (repository == null) return null;
        
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_BRANCH_REV_CMD);
        command.add(GIT_BRANCH_SHORT_CS_TEMPLATE_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());

        List<String> list = exec(command);
        if (!list.isEmpty()){
            return list.get(0);
        }else{
            return null;
        }
    }
    /**
     * Returns the Git branch info for a repository
     *
     * @param File repository of the git repository's root directory
     * @return String of form :<branch>:<rev>:<shortchangeset>:
     * @throws org.netbeans.modules.git.GitException
     */
    public static String getBranchInfo(File repository) throws GitException {
        if (repository == null) return null;
        
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_BRANCH_REV_CMD);
        command.add(GIT_BRANCH_INFO_TEMPLATE_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());

        List<String> list = exec(command);
        if (!list.isEmpty()){
            return list.get(0);
        }else{
            return null;
        }
    }

    /**
     * Returns the revision number for the heads in a repository
     *
     * @param File repository of the Git repository's root directory
     * @return List<String> of revision numbers.
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> getHeadRevisions(File repository) throws GitException {
        return  getHeadInfo(repository, GIT_REV_TEMPLATE_CMD);
    }

    /**
     * Returns the revision number for the heads in a repository
     *
     * @param String repository of the Git repository
     * @return List<String> of revision numbers.
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> getHeadRevisions(String repository) throws GitException {
        return  getHeadInfo(repository, GIT_REV_TEMPLATE_CMD);
    }

    /**
     * Returns the changeset for the the heads in a repository
     *
     * @param File repository of the Git repository's root directory
     * @param File file of the file whose last changeset is to be returned.
     * @return List<String> of changeset ids.
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> getHeadChangeSetIds(File repository) throws GitException {
        return  getHeadInfo(repository, GIT_CSET_TARGET_TEMPLATE_CMD);
    }

    private static List<String> getHeadInfo(String repository, String template) throws GitException {
        if (repository == null) return null;
        
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_HEADS_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository);
        command.add(template);

        return exec(command);
    }

    private static List<String> getHeadInfo(File repository, String template) throws GitException {
        if (repository == null) return null;
        return getHeadInfo(repository.getAbsolutePath(), template);
    }

    /**
     * Returns the revision number for the last change to a file
     *
     * @param File repository of the Git repository's root directory
     * @param File file of the file whose last revision number is to be returned, if null test for repo
     * @return String in the form of a revision number.
     * @throws org.netbeans.modules.git.GitException
     */
    public static String getLastRevision(File repository, File file) throws GitException {
        return getLastChange(repository, file, GIT_REV_TEMPLATE_CMD);
    }

    /**
     * Returns the changeset for the last change to a file
     *
     * @param File repository of the Git repository's root directory
     * @param File file of the file whose last changeset is to be returned.
     * @return String in the form of a changeset id.
     * @throws org.netbeans.modules.git.GitException
     */
    public static String getLastChangeSetId(File repository, File file) throws GitException {
        return getLastChange(repository, file, GIT_CSET_TEMPLATE_CMD);
    }

    private static String getLastChange(File repository, File file, String template) throws GitException {

        if (repository == null) return null;
        
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_LOG_CMD);
        command.add(GIT_LOG_LIMIT_ONE_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        command.add(template);
        if( file != null)
            command.add(file.getAbsolutePath());

        List<String> list = exec(command);
        if (!list.isEmpty()) {
            return new StringBuffer(list.get(0)).toString();
        } else {
            return null;
        }
    }
    
    
    /**
     * Returns the Git status for a given file
     *
     * @param File repository of the git repository's root directory
     * @param cwd current working directory containing file to be checked
     * @param filename name of file whose status is to be checked
     * @return FileInformation for the given filename
     * @throws org.netbeans.modules.git.GitException
     */
    public static FileInformation getSingleStatus(File repository, String cwd, String filename)  throws GitException{
        FileInformation info = null;
        List<String> list = doSingleStatusCmd(repository, cwd, filename);
        if(list == null || list.isEmpty())
            return new FileInformation(FileInformation.STATUS_UNKNOWN,null, false);

        info =  getFileInformationFromStatusLine(list.get(0));
        // Handles Copy status
        // Could save copy source in FileStatus but for now we don't need it.
        // FileStatus used in Fileinformation.java:getStatusText() and getShortStatusText() to check if
        // file is Locally Copied when it's status is Locally Added
        if(list.size() == 2) {
            if (list.get(1).length() > 0){
                if (list.get(1).charAt(0) == ' '){
                
                    info =  new FileInformation(FileInformation.STATUS_VERSIONED_ADDEDLOCALLY,
                            new FileStatus(new File(new File(cwd), filename), true), false);
                    Git.LOG.log(Level.FINE, "getSingleStatus() - Copied: Locally Added {0}, Copy Source {1}", // NOI18N
                            new Object[] {list.get(0), list.get(1)} );
                }
            } else {
                Git.LOG.log(Level.FINE, "getSingleStatus() - Second line empty: first line: {0}", list.get(0)); // NOI18N
            }
        }
        
        // Handle Conflict Status
        // TODO: remove this if Git status supports Conflict marker
        if(existsConflictFile(cwd + File.separator + filename)){
            info =  new FileInformation(FileInformation.STATUS_VERSIONED_CONFLICT, null, false);
            Git.LOG.log(Level.FINE, "getSingleStatus(): CONFLICT StatusLine: {0} Status: {1}  {2} RepoPath:{3} cwd:{4} CONFLICT {5}", // NOI18N
                new Object[] {list.get(0), info.getStatus(), filename, repository.getAbsolutePath(), cwd,
                cwd + File.separator + filename + GitCommand.GIT_STR_CONFLICT_EXT} );
        }
        
        Git.LOG.log(Level.FINE, "getSingleStatus(): StatusLine: {0} Status: {1}  {2} RepoPath:{3} cwd:{4}", // NOI18N
                new Object[] {list.get(0), info.getStatus(), filename, repository.getAbsolutePath(), cwd} );
        return info;
    }
    
    /**
     * Returns the Git status for all files in a given  subdirectory of
     * a repository
     *
     * @param File repository of the git repository's root directory
     * @param File dir of the subdirectoy of interest. 
     * @return Map of files and status for all files in the specified subdirectory
     * @throws org.netbeans.modules.git.GitException
     */
    public static Map<File, FileInformation> getAllStatus(File repository, File dir)  throws GitException{
        return getDirStatusWithFlags(repository, dir, GIT_STATUS_FLAG_ALL_CMD, true);
    }
    
    /**
     * Returns the git status for all files in a given repository
     *
     * @param File repository of the Git repository's root directory
     * @return Map of files and status for all files under the repository root
     * @throws org.netbeans.modules.git.GitException
     */
    public static Map<File, FileInformation> getAllStatus(File repository)  throws GitException{
        return getAllStatusWithFlags(repository, GIT_STATUS_FLAG_ALL_CMD, true);
    }
    
    /**
     * Returns the git status for only files of interest to us in a given repository
     * that is modified, locally added, locally removed, locally deleted, locally new and ignored.
     *
     * @param File repository of the Git repository's root directory
     * @return Map of files and status for all files of interest under the repository root
     * @throws org.netbeans.modules.git.GitException
     */
    public static Map<File, FileInformation> getAllInterestingStatus(File repository)  throws GitException{
        return getAllStatusWithFlags(repository, GIT_STATUS_FLAG_INTERESTING_CMD, true);
    }
    
    /**
     * Returns the git status for only files of interest to us in a given directory in a repository
     * that is modified, locally added, locally removed, locally deleted, locally new and ignored.
     *
     * @param File repository of the Git repository's root directory
     * @param File dir of the directory of interest
     * @return Map of files and status for all files of interest in the directory of interest
     * @throws org.netbeans.modules.git.GitException
     */
    public static Map<File, FileInformation> getInterestingStatus(File repository, File dir)  throws GitException{
        return getDirStatusWithFlags(repository, dir, GIT_STATUS_FLAG_INTERESTING_CMD, true);
    }
    
    /**
     * Returns the git status for only files of interest to us in a given repository
     * that is modified, locally added, locally removed, locally deleted, locally new and ignored.
     *
     * @param File repository of the Git repository's root directory
     * @return Map of files and status for all files of interest under the repository root
     * @throws org.netbeans.modules.git.GitException
     */
    public static Map<File, FileInformation> getAllRemovedDeletedStatus(File repository)  throws GitException{
        return getAllStatusWithFlags(repository, GIT_STATUS_FLAG_REM_DEL_CMD, true);
    }
    
    /**
     * Returns the git status for only files of interest to us in a given directory in a repository
     * that is modified, locally added, locally removed, locally deleted, locally new and ignored.
     *
     * @param File repository of the Git repository's root directory
     * @param File dir of the directory of interest
     * @return Map of files and status for all files of interest in the specified directory
     * @throws org.netbeans.modules.git.GitException
     */
    public static Map<File, FileInformation> getRemovedDeletedStatus(File repository, File dir)  throws GitException{
        return getDirStatusWithFlags(repository, dir, GIT_STATUS_FLAG_REM_DEL_CMD, true);
    }
    
    /**
     * Returns the unknown files in a specified directory under a Git repository root
     *
     * @param File of the Git repository's root directory
     * @param File of the directory whose files are required
     * @return Map of files and status for all files under the repository root
     * @throws org.netbeans.modules.git.GitException
     */
    public static Map<File, FileInformation> getUnknownStatus(File repository, File dir)  throws GitException{
        Map<File, FileInformation> files = getDirStatusWithFlags(repository, dir, GIT_STATUS_FLAG_UNKNOWN_CMD, false);
        int share = SharabilityQuery.getSharability(dir == null ? repository : dir);
        for (Iterator i = files.keySet().iterator(); i.hasNext();) {
            File file = (File) i.next();
            if((share == SharabilityQuery.MIXED && SharabilityQuery.getSharability(file) == SharabilityQuery.NOT_SHARABLE) ||
               (share == SharabilityQuery.NOT_SHARABLE)) {
                i.remove();
             }
        }
        return files;
    }

    /**
     * Returns the unknown files under a Git repository root
     *
     * @param File repository of the Git repository's root directory
     * @return Map of files and status for all files under the repository root
     * @throws org.netbeans.modules.git.GitException
     */
    public static Map<File, FileInformation> getAllUnknownStatus(File repository)  throws GitException{
        return getUnknownStatus(repository, null);
    }
    
    /**
     * Remove the specified file from the Git Repository
     *
     * @param File repository of the Git repository's root directory
     * @param List<Files> of files to be added to Git
     * @param f path to be removed from the repository
     * @throws org.netbeans.modules.git.GitException
     */
    public static void doRemove(File repository, List<File> removeFiles, OutputLogger logger)  throws GitException {
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_REMOVE_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        command.add(GIT_REMOVE_FLAG_FORCE_CMD);
        for(File f: removeFiles){
            command.add(f.getAbsolutePath());
        }

        List<String> list = exec(command);
        if (!list.isEmpty() && isErrorAlreadyTracked(list.get(0)))
            handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_ALREADY_TRACKED"), logger);
    }
    
    /**
     * Remove the specified files from the Git Repository
     *
     * @param File repository of the git repository's root directory
     * @param f path to be removed from the repository
     * @throws org.netbeans.modules.git.GitException
     */
    public static void doRemove(File repository, File f, OutputLogger logger)  throws GitException {
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_REMOVE_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        command.add(GIT_REMOVE_FLAG_FORCE_CMD);
        command.add(f.getAbsolutePath());

        List<String> list = exec(command);
        if (!list.isEmpty() && isErrorAlreadyTracked(list.get(0)))
            handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_ALREADY_TRACKED"), logger);
    }
    
    /**
     * Export the diffs for the specified revision to the specified output file
    /**
     * Export the diffs for the specified revision to the specified output file
     *
     * @param File repository of the Git repository's root directory
     * @param revStr the revision whose diffs are to be exported
     * @param outputFileName path of the output file
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doExport(File repository, String revStr, String outputFileName, OutputLogger logger)  throws GitException {
        // Ensure that parent directory of target exists, creating if necessary
        File fileTarget = new File (outputFileName);
        File parentTarget = fileTarget.getParentFile();
        try {
            if (!parentTarget.mkdir()) {
                if (!parentTarget.isDirectory()) {
                    Git.LOG.log(Level.WARNING, "File.mkdir() failed for : " + parentTarget.getAbsolutePath()); // NOI18N
                    throw (new GitException (NbBundle.getMessage(GitCommand.class, "MSG_UNABLE_TO_CREATE_PARENT_DIR"))); // NOI18N
                }
            }
        } catch (SecurityException e) {
            Git.LOG.log(Level.WARNING, "File.mkdir() for : " + parentTarget.getAbsolutePath() + " threw SecurityException " + e.getMessage()); // NOI18N
            throw (new GitException (NbBundle.getMessage(GitCommand.class, "MSG_UNABLE_TO_CREATE_PARENT_DIR"))); // NOI18N
        }
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_EXPORT_CMD);
        command.add(GIT_VERBOSE_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        command.add(GIT_FLAG_OUTPUT_CMD);
        command.add(outputFileName);
        command.add(revStr);

        List<String> list = exec(command);
        if (!list.isEmpty() &&
             isErrorAbort(list.get(list.size() -1))) {
            handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_EXPORT_FAILED"), logger);
        }
        return list;
    }
    
    /**
     * Imports the diffs from the specified file
     *
     * @param File repository of the Git repository's root directory
     * @param File patchFile of the patch file
     * @throws org.netbeans.modules.git.GitException
     */
    public static List<String> doImport(File repository, File patchFile, OutputLogger logger)  throws GitException {
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_IMPORT_CMD);
        command.add(GIT_VERBOSE_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        command.add(GIT_OPT_CWD_CMD);
        command.add(repository.getAbsolutePath());
        command.add(patchFile.getAbsolutePath());

        List<String> list = exec(command);
        if (!list.isEmpty() &&
             isErrorAbort(list.get(list.size() -1))) {
            logger.output(list); // need the failure info from import
            handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_IMPORT_FAILED"), logger);
        }
        return list;
    }
    
    /**
     * Returns Map of Git file and status for files in a given repository as specified by the status flags
     */
    private static Map<File, FileInformation> getAllStatusWithFlags(File repository, String statusFlags, boolean bIgnoreUnversioned)  throws GitException{
        return getDirStatusWithFlags(repository, null, statusFlags, bIgnoreUnversioned);
    }
    
    private static Map<File, FileInformation> getDirStatusWithFlags(File repository, File dir, String statusFlags, boolean bIgnoreUnversioned)  throws GitException{
        if (repository == null) return null;
        
        List<FileStatus> statusList = new ArrayList<FileStatus>();
        FileInformation prev_info = null;
        List<String> list = doRepositoryDirStatusCmd(repository, dir, statusFlags);
        
        Map<File, FileInformation> repositoryFiles = new HashMap<File, FileInformation>(list.size());
        
        StringBuffer filePath = null;
        for(String statusLine: list){
            FileInformation info = getFileInformationFromStatusLine(statusLine);
            Git.LOG.log(Level.FINE, "getDirStatusWithFlags(): status line {0}  info {1}", new Object[]{statusLine, info}); // NOI18N
            if (statusLine.length() > 0) {
                if (statusLine.charAt(0) == ' ') {
                    // Locally Added but Copied
                    if (filePath != null) {
                        prev_info =  new FileInformation(FileInformation.STATUS_VERSIONED_ADDEDLOCALLY,
                                new FileStatus(new File(filePath.toString()), true), false);
                        Git.LOG.log(Level.FINE, "getDirStatusWithFlags(): prev_info {0}  filePath {1}", new Object[]{prev_info, filePath.toString()}); // NOI18N
                    } else {
                        Git.LOG.log(Level.FINE, "getDirStatusWithFlags(): repository path: {0} status flags: {1} status line {2} filepath == nullfor prev_info ", new Object[]{repository.getAbsolutePath(), statusFlags, statusLine}); // NOI18N
                    }
                    break;
                } else {
                    if (filePath != null) {
                        repositoryFiles.put(new File(filePath.toString()), prev_info);
                    }
                }
            }
            if(bIgnoreUnversioned){
                if(info.getStatus() == FileInformation.STATUS_NOTVERSIONED_NOTMANAGED ||
                        info.getStatus() == FileInformation.STATUS_UNKNOWN) continue;
            }else{
                if(info.getStatus() == FileInformation.STATUS_UNKNOWN) continue;
            }
            filePath = new StringBuffer(repository.getAbsolutePath()).append(File.separatorChar);
            StringBuffer sb = new StringBuffer(statusLine);
            sb.delete(0,2); // Strip status char and following 2 spaces: [MARC\?\!I][ ][ ]
            filePath.append(sb.toString());
            
            // Handle Conflict Status
            // TODO: remove this if Git status supports Conflict marker
            if (existsConflictFile(filePath.toString())) {
                info = new FileInformation(FileInformation.STATUS_VERSIONED_CONFLICT, null, false);
                Git.LOG.log(Level.FINE, "getDirStatusWithFlags(): CONFLICT repository path: {0} status flags: {1} status line {2} CONFLICT {3}", new Object[]{repository.getAbsolutePath(), statusFlags, statusLine, filePath.toString() + GitCommand.GIT_STR_CONFLICT_EXT}); // NOI18N
            }
            prev_info = info;
        }
        if (prev_info != null) {
            repositoryFiles.put(new File(filePath.toString()), prev_info);
        }
        
        if (list.size() < 10) {
            Git.LOG.log(Level.FINE, "getDirStatusWithFlags(): repository path: {0} status flags: {1} status list {2}", // NOI18N
                    new Object[] {repository.getAbsolutePath(), statusFlags, list} );
        } else {
            Git.LOG.log(Level.FINE, "getDirStatusWithFlags(): repository path: {0} status flags: {1} status list has {2} elements", // NOI18N
                    new Object[] {repository.getAbsolutePath(), statusFlags, list.size()} );
        }
        return repositoryFiles;
    }
    
    /**
     * Gets file information for a given git status output status line
     */
    private static FileInformation getFileInformationFromStatusLine(String status){
        FileInformation info = null;
        if (status == null || (status.length() == 0)) return new FileInformation(FileInformation.STATUS_VERSIONED_UPTODATE, null, false);
        
        char c0 = status.charAt(0);
        char c1 = status.charAt(1);
        switch(c0 + c1) {
        case GIT_STATUS_CODE_MODIFIED:
            info = new FileInformation(FileInformation.STATUS_VERSIONED_MODIFIEDLOCALLY,null, false);
            break;
        case GIT_STATUS_CODE_ADDED:
            info = new FileInformation(FileInformation.STATUS_VERSIONED_ADDEDLOCALLY,null, false);
            break;
        case GIT_STATUS_CODE_REMOVED:
            info = new FileInformation(FileInformation.STATUS_VERSIONED_REMOVEDLOCALLY,null, false);
            break;
        case GIT_STATUS_CODE_CLEAN:
            info = new FileInformation(FileInformation.STATUS_VERSIONED_UPTODATE,null, false);
            break;
        case GIT_STATUS_CODE_DELETED:
            info = new FileInformation(FileInformation.STATUS_VERSIONED_DELETEDLOCALLY,null, false);
            break;
        case GIT_STATUS_CODE_IGNORED:
            info = new FileInformation(FileInformation.STATUS_NOTVERSIONED_EXCLUDED,null, false);
            break;
        case GIT_STATUS_CODE_NOTTRACKED:
            info = new FileInformation(FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY,null, false);
            break;
        // Leave this here for whenever Git status suports conflict markers
        case GIT_STATUS_CODE_CONFLICT:
            info = new FileInformation(FileInformation.STATUS_VERSIONED_CONFLICT,null, false);
            break;
        case GIT_STATUS_CODE_ABORT:
            info = new FileInformation(FileInformation.STATUS_NOTVERSIONED_NOTMANAGED,null, false);
            break;
        default:
            info = new FileInformation(FileInformation.STATUS_UNKNOWN,null, false);
            break;
        }
        
        return info;
    }
    
    /**
     * Gets git status command output line for a given file
     */
    private static List<String> doSingleStatusCmd(File repository, String cwd, String filename)  throws GitException{
        String statusLine = null;
        
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_STATUS_CMD);
        command.add(GIT_STATUS_FLAG_ALL_CMD);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        command.add(GIT_OPT_CWD_CMD);
        command.add(repository.getAbsolutePath());

        
        command.add(new File(cwd, filename).getAbsolutePath().substring(repository.getAbsolutePath().length()+1));
        
        return exec(command);
    }
    
    /**
     * Gets git status command output list for the specified status flags for a given repository and directory
     */
    private static List<String> doRepositoryDirStatusCmd(File repository, File dir, String statusFlags)  throws GitException{
        List<String> command = new ArrayList<String>();

        command.add(getGitCommand());
        command.add(GIT_STATUS_CMD);

        command.add(statusFlags);
        command.add(GIT_OPT_REPOSITORY);
        command.add(repository.getAbsolutePath());
        command.add(GIT_OPT_CWD_CMD);
        command.add(repository.getAbsolutePath());
        if (dir != null) {
            command.add(dir.getAbsolutePath());
        } else {
            command.add(repository.getAbsolutePath());
        }
        
        List<String> list =  exec(command);
        if (!list.isEmpty() && isErrorNoRepository(list.get(0))) {
            OutputLogger logger = OutputLogger.getLogger(repository.getAbsolutePath());
            try {
                handleError(command, list, NbBundle.getMessage(GitCommand.class, "MSG_NO_REPOSITORY_ERR"), logger);
            } finally {
                logger.closeLog();
            }
        }
        return list;
    }
    
    /**
     * Returns the ouput from the given command
     *
     * @param command to execute
     * @return List of the command's output or an exception if one occured
     */
    private static List<String> execEnv(List<String> command, List<String> env) throws GitException{
        if( EventQueue.isDispatchThread()){
            Git.LOG.log(Level.FINE, "WARNING execEnv():  calling Git command in AWT Thread - could stall UI"); // NOI18N
        }        assert ( command != null && command.size() > 0);
        List<String> list = new ArrayList<String>();
        BufferedReader input = null;
        Process proc = null;
        try{
            if (command.size() > 10)  {
                List<String> smallCommand = new ArrayList<String>();
                int count = 0;
                for (Iterator i = command.iterator(); i.hasNext();) {
                    smallCommand.add((String)i.next());
                    if (count++ > 10) break;
                } 
                Git.LOG.log(Level.FINE, "execEnv(): " + smallCommand); // NOI18N
            } else {
                Git.LOG.log(Level.FINE, "execEnv(): " + command); // NOI18N
            }
            if(env != null && env.size() > 0){
                ProcessBuilder pb = new ProcessBuilder(command);
                Map<String, String> envOrig = pb.environment();
                for(String s: env){
                    envOrig.put(s.substring(0,s.indexOf('=')), s.substring(s.indexOf('=')+1)); 
                }
                proc = pb.start();
            }else{ 
                proc = new ProcessBuilder(command).start();
            }

            input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            
            String line;
            while ((line = input.readLine()) != null){
                list.add(line);
            }
            input.close();
            input = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            while ((line = input.readLine()) != null){
                list.add(line);
            }
            input.close();
            input = null;
            try {
                proc.waitFor();
                // By convention we assume that 255 (or -1) is a serious error.
                // For instance, the command line could be too long.
                if (proc.exitValue() == 255) {
                    Git.LOG.log(Level.FINE, "execEnv():  process returned 255"); // NOI18N
                    if (list.isEmpty()) {
                        Git.LOG.log(Level.SEVERE, "command: " + command); // NOI18N
                        throw new GitException(NbBundle.getMessage(GitCommand.class, "MSG_UNABLE_EXECUTE_COMMAND"));
                    }
                }
            } catch (InterruptedException e) {
                Git.LOG.log(Level.FINE, "execEnv():  process interrupted " + e); // NOI18N
            }
        }catch(InterruptedIOException e){
            // We get here is we try to cancel so kill the process
            Git.LOG.log(Level.FINE, "execEnv():  execEnv(): InterruptedIOException " + e); // NOI18N
            if (proc != null)  {
                try {
                    proc.getInputStream().close();
                    proc.getOutputStream().close();
                    proc.getErrorStream().close();
                } catch (IOException ioex) {
                //Just ignore. Closing streams.
                }
                proc.destroy();
            }
            throw new GitException(NbBundle.getMessage(GitCommand.class, "MSG_COMMAND_CANCELLED"));
        }catch(IOException e){
            // Git does not seem to be returning error status != 0
            // even when it fails when for instance adding an already tracked file to
            // the repository - we will have to examine the output in the context of the
            // calling func and raise exceptions there if needed
            Git.LOG.log(Level.SEVERE, "execEnv():  execEnv(): IOException " + e); // NOI18N
             
            // Handle low level Git failures
            if (isErrorArgsTooLong(e.getMessage())){
                assert(command.size()> 2);
                throw new GitException(NbBundle.getMessage(GitCommand.class, "MSG_ARG_LIST_TOO_LONG_ERR", 
                            command.get(1), command.size() -2 ));
            }else if (isErrorNoGit(e.getMessage()) || isErrorCannotRun(e.getMessage())){
                throw new GitException(NbBundle.getMessage(Git.class, "MSG_VERSION_NONE_MSG"));
            }else{
                throw new GitException(NbBundle.getMessage(GitCommand.class, "MSG_UNABLE_EXECUTE_COMMAND"));
            }
        }finally{
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ioex) {
                //Just ignore. Closing streams.
                }
                input = null;
            }
        }
        return list;
    }
    
    /**
     * Returns the ouput from the given command
     *
     * @param command to execute
     * @return List of the command's output or an exception if one occured
     */
    private static List<String> exec(List<String> command) throws GitException{
        if(!Git.getInstance().isGoodVersion()){
            return new ArrayList<String>();
        }
        return execEnv(command, null);
    }
    private static List<String> execForVersionCheck() throws GitException{
        List<String> command = new ArrayList<String>();
        command.add(getGitCommand());
        command.add(GIT_VERSION_CMD);

        return execEnv(command, null);
    }
    
    private static String getGitCommand() {
        String defaultPath = GitModuleConfig.getDefault().getExecutableBinaryPath();
        if (defaultPath == null || defaultPath.length() == 0) 
            return GIT_COMMAND;
        else
            return defaultPath + File.separatorChar + GIT_COMMAND;
    }

    private static void handleError(List<String> command, List<String> list, String message, OutputLogger logger) throws GitException{
        if (command != null && list != null && logger != null){
            Git.LOG.log(Level.WARNING, "command: " + GitUtils.replaceHttpPassword(command)); // NOI18N        
            Git.LOG.log(Level.WARNING, "output: " + GitUtils.replaceHttpPassword(list)); // NOI18N
            logger.outputInRed(NbBundle.getMessage(GitCommand.class, "MSG_COMMAND_ERR")); // NOI18N
            logger.output(NbBundle.getMessage(GitCommand.class, "MSG_COMMAND_INFO_ERR",
                    GitUtils.replaceHttpPassword(command), GitUtils.replaceHttpPassword(list))); // NOI18N
        }

        if (list != null && (isErrorPossibleProxyIssue(list.get(0)) || isErrorPossibleProxyIssue(list.get(list.size() - 1)))) {
            boolean bConfirmSetProxy;
            bConfirmSetProxy = GitUtils.confirmDialog(GitCommand.class, "MSG_POSSIBLE_PROXY_ISSUE_TITLE", "MSG_POSSIBLE_PROXY_ISSUE_QUERY"); // NOI18N
            if(bConfirmSetProxy){
                OptionsDisplayer.getDefault().open("General");              // NOI18N
            }
        } else {
            throw new GitException(message);
        }
    }

    public static boolean isMergeNeededMsg(String msg) {
        return msg.indexOf(GIT_MERGE_NEEDED_ERR) > -1;                       // NOI18N
    }

    public static boolean isBackoutMergeNeededMsg(String msg) {
        return msg.indexOf(GIT_BACKOUT_MERGE_NEEDED_ERR) > -1;                       // NOI18N
    }
    
    public static boolean isMergeConflictMsg(String msg) {
        if(Utilities.isWindows() ) {   
            return (msg.indexOf(GIT_MERGE_CONFLICT_WIN1_ERR) > -1) &&        // NOI18N
                    (msg.indexOf(GIT_MERGE_CONFLICT_WIN2_ERR) > -1);         // NOI18N
        }else{
            return msg.indexOf(GIT_MERGE_CONFLICT_ERR) > -1;                 // NOI18N
        }       
    }

    public static boolean isMergeUnavailableMsg(String msg) {
        return msg.indexOf(GIT_MERGE_UNAVAILABLE_ERR) > -1;                 // NOI18N
    }

    public static boolean isMergeAbortMultipleHeadsMsg(String msg) {
        return msg.indexOf(GIT_MERGE_MULTIPLE_HEADS_ERR) > -1;                                   // NOI18N
    }
    public static boolean isMergeAbortUncommittedMsg(String msg) {
        return msg.indexOf(GIT_MERGE_UNCOMMITTED_ERR) > -1;                                   // NOI18N
    }
     
    public static boolean isNoChanges(String msg) {
        return msg.indexOf(GIT_NO_CHANGES_ERR) > -1;                                   // NOI18N
    }
    
    private static boolean isErrorNoDefaultPush(String msg) {
        return msg.indexOf(GIT_ABORT_NO_DEFAULT_PUSH_ERR) > -1; // NOI18N
    }
    
    private static boolean isErrorNoDefaultPath(String msg) {
        return msg.indexOf(GIT_ABORT_NO_DEFAULT_ERR) > -1; // NOI18N
    }
    
    private static boolean isErrorPossibleProxyIssue(String msg) {
        return msg.indexOf(GIT_ABORT_POSSIBLE_PROXY_ERR) > -1; // NOI18N
    }
    
    private static boolean isErrorNoRepository(String msg) {
        return msg.indexOf(GIT_NO_REPOSITORY_ERR) > -1 ||
                 msg.indexOf(GIT_NOT_REPOSITORY_ERR) > -1 ||
                 (msg.indexOf(GIT_REPOSITORY) > -1 && msg.indexOf(GIT_NOT_FOUND_ERR) > -1); // NOI18N
    }
    
    private static boolean isErrorNoGit(String msg) {
        return msg.indexOf(GIT_NO_GIT_CMD_FOUND_ERR) > -1; // NOI18N
    }
    
    private static boolean isErrorArgsTooLong(String msg) {
        return msg.indexOf(GIT_ARG_LIST_TOO_LONG_ERR) > -1; // NOI18N
    }
    
    private static boolean isErrorCannotRun(String msg) {
        return msg.indexOf(GIT_CANNOT_RUN_ERR) > -1; // NOI18N
    }
    
    private static boolean isErrorUpdateSpansBranches(String msg) {
        return msg.indexOf(GIT_UPDATE_SPAN_BRANCHES_ERR) > -1; // NOI18N
    }

    private static boolean isErrorAlreadyTracked(String msg) {
        return msg.indexOf(GIT_ALREADY_TRACKED_ERR) > -1; // NOI18N
    }
    
    private static boolean isErrorNotTracked(String msg) {
        return msg.indexOf(GIT_NOT_TRACKED_ERR) > -1; // NOI18N
    }

    private static boolean isErrorNotFound(String msg) {
        return msg.indexOf(GIT_NOT_FOUND_ERR) > -1; // NOI18N
    }
    
    private static boolean isErrorCannotReadCommitMsg(String msg) {
        return msg.indexOf(GIT_CANNOT_READ_COMMIT_MESSAGE_ERR) > -1; // NOI18N
    }
    
    private static boolean isErrorAbort(String msg) {
        return msg.indexOf(GIT_ABORT_ERR) > -1; // NOI18N
    }

    public static boolean isErrorAbortPush(String msg) {
        return msg.indexOf(GIT_ABORT_PUSH_ERR) > -1; // NOI18N
    }

    public static boolean isErrorAbortNoFilesToCopy(String msg) {
        return msg.indexOf(GIT_ABORT_NO_FILES_TO_COPY_ERR) > -1; // NOI18N
    }

    private static boolean isErrorNoChangeNeeded(String msg) {
        return msg.indexOf(GIT_NO_CHANGE_NEEDED_ERR) > -1;    // NOI18N
    }
    
    public static boolean isCreateNewBranch(String msg) {
        return msg.indexOf(GIT_CREATE_NEW_BRANCH_ERR) > -1;                                   // NOI18N
    }
    
    public static boolean isHeadsCreated(String msg) {
        return msg.indexOf(GIT_HEADS_CREATED_ERR) > -1;                                   // NOI18N
    }
    
    public static boolean isNoRollbackPossible(String msg) {
        return msg.indexOf(GIT_NO_ROLLBACK_ERR) > -1;                                   // NOI18N
    }
    public static boolean isNoRevStrip(String msg) {
        return msg.indexOf(GIT_NO_REV_STRIP_ERR) > -1;                                   // NOI18N
    }
    public static boolean isLocalChangesStrip(String msg) {
        return msg.indexOf(GIT_LOCAL_CHANGES_STRIP_ERR) > -1;                                   // NOI18N
    }
    public static boolean isMultipleHeadsStrip(String msg) {
        return msg.indexOf(GIT_MULTIPLE_HEADS_STRIP_ERR) > -1;                                   // NOI18N
    }
    public static boolean isUncommittedChangesBackout(String msg) {
        return msg.indexOf(GIT_ABORT_UNCOMMITTED_CHANGES_ERR) > -1;                                   // NOI18N
    }
    public static boolean isMergeChangesetBackout(String msg) {
        return msg.indexOf(GIT_ABORT_BACKOUT_MERGE_CSET_ERR) > -1;                                   // NOI18N
    }
    
    public static boolean isNoUpdates(String msg) {
        return msg.indexOf(GIT_NO_UPDATES_ERR) > -1;                                   // NOI18N
    }
    
    private static boolean isErrorNoView(String msg) {
        return msg.indexOf(GIT_NO_VIEW_ERR) > -1;                                     // NOI18N
    }

    private static boolean isErrorGitkNotFound(String msg) {
        return msg.indexOf(GIT_GITK_NOT_FOUND_ERR) > -1;                               // NOI18N
    }

    private static boolean isErrorNoSuchFile(String msg) {
        return msg.indexOf(GIT_NO_SUCH_FILE_ERR) > -1;                               // NOI18N
    }

    private static boolean isErrorNoResponse(String msg) {
        return msg.indexOf(GIT_NO_RESPONSE_ERR) > -1;                               // NOI18N
    }

    public static void createConflictFile(String path) {
        try {
            File file = new File(path + GIT_STR_CONFLICT_EXT);

            boolean success = file.createNewFile();
            Git.LOG.log(Level.FINE, "createConflictFile(): File: {0} {1}", // NOI18N
                new Object[] {path + GIT_STR_CONFLICT_EXT, success? "Created": "Not Created"} ); // NOI18N
        } catch (IOException e) {
        }
    }
    
    public static void deleteConflictFile(String path) {
        boolean success = (new File(path + GIT_STR_CONFLICT_EXT)).delete();

        Git.LOG.log(Level.FINE, "deleteConflictFile(): File: {0} {1}", // NOI18N
                new Object[] {path + GIT_STR_CONFLICT_EXT, success? "Deleted": "Not Deleted"} ); // NOI18N
    }

    public static boolean existsConflictFile(String path) {        
        File file = new File(path + GIT_STR_CONFLICT_EXT);
        boolean bExists = file.canWrite();
        
        if (bExists) {
            Git.LOG.log(Level.FINE, "existsConflictFile(): File: {0} {1}", // NOI18N
                    new Object[] {path + GIT_STR_CONFLICT_EXT, "Exists"} ); // NOI18N
        }
        return bExists;
    }

    /**
     * This utility class should not be instantiated anywhere.
     */
    private GitCommand() {
    }
    
}