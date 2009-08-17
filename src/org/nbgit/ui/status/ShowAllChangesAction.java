package org.nbgit.ui.status;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.actions.SystemAction;

/**
 * Open the Versioning status view for all projects.
 *
 * @author Maros Sandor
 */
public class ShowAllChangesAction extends SystemAction {

    public ShowAllChangesAction() {
    }

    public String getName() {
        return NbBundle.getMessage(ShowAllChangesAction.class, "CTL_MenuItem_ShowAllChanges_Label"); // NOI18N
    }

    public HelpCtx getHelpCtx() {
        return new HelpCtx(getClass());
    }

    public void actionPerformed(ActionEvent e) {
        RequestProcessor.getDefault().post(new Runnable() {

            public void run() {
                async();
            }
        });
    }

    private void async() {
        try {
            setEnabled(false);
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    GitVersioningTopComponent stc = GitVersioningTopComponent.findInstance();
                    stc.setContext(null);
                    stc.open();
                }
            });

            Project[] projects = OpenProjects.getDefault().getOpenProjects();
            List<Node> allNodes = new ArrayList<Node>();
            for (int i = 0; i < projects.length; i++) {
                AbstractNode node = new AbstractNode(new Children.Array(), projects[i].getLookup());
                allNodes.add(node);
            }

            final VCSContext ctx = VCSContext.forNodes(allNodes.toArray(new Node[allNodes.size()]));
            final String title;
            if (projects.length == 1) {
                Project project = projects[0];
                ProjectInformation pinfo = ProjectUtils.getInformation(project);
                title = pinfo.getDisplayName();
            } else {
                title = NbBundle.getMessage(ShowAllChangesAction.class, "CTL_ShowAllChanges_WindowTitle", Integer.toString(projects.length)); // NOI18N
            }
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    final GitVersioningTopComponent stc = GitVersioningTopComponent.findInstance();
                    stc.setContentTitle(title);
                    stc.setContext(ctx);
                    stc.open();
                    stc.requestActive();
                    if (shouldPostRefresh()) {
                        stc.performRefreshAction();
                    }
                }
            });

        } finally {
            setEnabled(true);
        }

    }

    protected boolean shouldPostRefresh() {
        return true;
    }
}
