package org.netbeans.modules.git.ui.stash;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import org.netbeans.modules.git.FileInformation;
import org.netbeans.modules.git.FileStatusCache;
import org.netbeans.modules.git.Git;
import org.netbeans.modules.git.ui.actions.ContextAction;
import org.netbeans.modules.versioning.spi.VCSContext;

/**
 * Stash Action for Git:
 * git stash - stash changes in a dirty working directory away
 * 
 * @author alexbcoles
 */
public final class StashAction extends ContextAction {

    private final VCSContext context;
    
    public StashAction(String name, VCSContext context) {
        this.context = context;
        putValue(Action.NAME, name);
    }

    @Override
    public boolean isEnabled () {
        FileStatusCache cache = Git.getInstance().getFileStatusCache();
        return cache.containsFileOfStatus(context, FileInformation.STATUS_LOCAL_CHANGE);
    }
    
    @Override
    protected void performAction(ActionEvent event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}

