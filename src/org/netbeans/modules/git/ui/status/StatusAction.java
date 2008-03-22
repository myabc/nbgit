package org.netbeans.modules.git.ui.status;


import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.Action;
import org.netbeans.modules.git.FileInformation;
import org.netbeans.modules.git.FileStatusCache;
import org.netbeans.modules.git.Git;
import org.netbeans.modules.git.GitException;
import org.netbeans.modules.git.GitProgressSupport;
import org.netbeans.modules.git.ui.actions.ContextAction;
import org.netbeans.modules.git.util.GitCommand;
import org.netbeans.modules.git.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.netbeans.modules.versioning.util.Utils;

/**
 * Status action for Git: 
 * git status - show changed files in the working directory
 * 
 * @author John Rice
 */
public class StatusAction extends ContextAction {
    
    private final VCSContext context;

    public StatusAction(String name, VCSContext context) {
        this.context = context;
        putValue(Action.NAME, name);
    }
    
    public void performAction(ActionEvent ev) {
        File [] files = context.getRootFiles().toArray(new File[context.getRootFiles().size()]);
        if (files == null || files.length == 0) return;
                
        final GitVersioningTopComponent stc = GitVersioningTopComponent.findInstance();
        stc.setContentTitle(Utils.getContextDisplayName(context)); 
        stc.setContext(context);
        stc.open(); 
        stc.requestActive();
        stc.performRefreshAction();
    }
    
    @Override
    public boolean isEnabled() {
        return GitUtils.getRootFile(context) != null;
    } 

    /**
     * Connects to repository and gets recent status.
     */
    public static void executeStatus(final VCSContext context, GitProgressSupport support) {

        if (context == null || context.getRootFiles().size() == 0) {
            return;
        }
        File repository = GitUtils.getRootFile(context);
        if (repository == null) {
            return;
        }

        try {
            FileStatusCache cache = Git.getInstance().getFileStatusCache();
            Calendar start = Calendar.getInstance();
            cache.refreshCached(context);
            Calendar end = Calendar.getInstance();
            Git.LOG.log(Level.FINE, "executeStatus: refreshCached took {0} millisecs", end.getTimeInMillis() - start.getTimeInMillis()); // NOI18N

            for (File root :  context.getRootFiles()) {
                if(support.isCanceled()) {
                    return;
                }
                if (root.isDirectory()) {
                    Map<File, FileInformation> interestingFiles;
                    interestingFiles = GitCommand.getInterestingStatus(repository, root);
                    if (!interestingFiles.isEmpty()){
                        Collection<File> files = interestingFiles.keySet();

                        Map<File, Map<File,FileInformation>> interestingDirs = 
                                GitUtils.getInterestingDirs(interestingFiles, files);

                        start = Calendar.getInstance();
                        for (File file : files) {
                             if(support.isCanceled()) {
                                 return;
                             }
                             FileInformation fi = interestingFiles.get(file);
                             
                             cache.refreshFileStatus(file, fi, 
                                     interestingDirs.get(file.isDirectory()? file: file.getParentFile())); 
                        }
                        end = Calendar.getInstance();
                        Git.LOG.log(Level.FINE, "executeStatus: process interesting files took {0} millisecs", end.getTimeInMillis() - start.getTimeInMillis()); // NOI18N
                    } 
                } else {
                    cache.refresh(root, FileStatusCache.REPOSITORY_STATUS_UNKNOWN); 
                }
            }
        } catch (GitException ex) {
            support.annotate(ex);
        }
    }
}