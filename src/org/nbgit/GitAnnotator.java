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

import org.nbgit.util.GitUtils;
import java.awt.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import javax.swing.Action;
import org.nbgit.ui.commit.CommitAction;
import org.nbgit.ui.custom.CustomMenu;
import org.nbgit.ui.diff.DiffAction;
import org.nbgit.ui.init.InitAction;
import org.nbgit.ui.log.LogAction;
import org.nbgit.ui.properties.PropertiesAction;
import org.nbgit.ui.status.StatusAction;
import org.nbgit.ui.update.RevertModificationsAction;
import org.nbgit.ui.update.UpdateAction;
import org.nbgit.util.HtmlFormatter;
import org.netbeans.api.project.Project;
import org.netbeans.modules.versioning.spi.VCSAnnotator;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.netbeans.modules.versioning.spi.VersioningSupport;
import org.netbeans.modules.versioning.util.Utils;
import org.openide.nodes.Node;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

/**
 * Responsible for coloring file labels and file icons in the IDE and providing
 * IDE with menu items.
 *
 * @author Maros Sandor
 * @author alexbcoles
 */
public class GitAnnotator extends VCSAnnotator {

    private static final int INITIAL_ACTION_ARRAY_LENGTH = 25;
    private static final int STATUS_BADGEABLE =
            StatusInfo.STATUS_VERSIONED_UPTODATE |
            StatusInfo.STATUS_NOTVERSIONED_NEWLOCALLY |
            StatusInfo.STATUS_VERSIONED_MODIFIEDLOCALLY;
    public static String ANNOTATION_REVISION = "revision"; // NOI18N
    public static String ANNOTATION_STATUS = "status"; // NOI18N
    public static String ANNOTATION_FOLDER = "folder"; // NOI18N
    public static String[] LABELS = new String[]{ANNOTATION_REVISION, ANNOTATION_STATUS, ANNOTATION_FOLDER};
    private StatusCache cache;
    private File folderToScan;
    private ConcurrentLinkedQueue<File> dirsToScan = new ConcurrentLinkedQueue<File>();
    private RequestProcessor.Task scanTask;
    private static final RequestProcessor rp = new RequestProcessor("GitAnnotateScan", 1, true); // NOI18N
    private final HtmlFormatter format;

    public GitAnnotator() {
        format = HtmlFormatter.getInstance();
        cache = Git.getInstance().getStatusCache();
        scanTask = rp.create(new ScanTask());
    }

    @Override
    public String annotateName(String name, VCSContext context) {
        int includeStatus = StatusInfo.STATUS_VERSIONED_UPTODATE | StatusInfo.STATUS_LOCAL_CHANGE | StatusInfo.STATUS_NOTVERSIONED_EXCLUDED;

        StatusInfo mostImportantInfo = null;
        File mostImportantFile = null;
        boolean folderAnnotation = false;

        for (final File file : context.getRootFiles()) {
            StatusInfo info = cache.getCachedStatus(file, true);
            if (info == null) {
                File parentFile = file.getParentFile();
                Git.LOG.log(Level.FINE, "null cached status for: {0} {1} {2}", new Object[]{file, folderToScan, parentFile});
                folderToScan = parentFile;
                reScheduleScan(1000);
                info = new StatusInfo(StatusInfo.STATUS_VERSIONED_UPTODATE, false);
            }
            int status = info.getStatus();
            if ((status & includeStatus) == 0) {
                continue;
            }
            if (isMoreImportant(info, mostImportantInfo)) {
                mostImportantInfo = info;
                mostImportantFile = file;
                folderAnnotation = file.isDirectory();
            }
        }

        if (folderAnnotation == false && context.getRootFiles().size() > 1) {
            folderAnnotation = !Utils.shareCommonDataObject(context.getRootFiles().toArray(new File[context.getRootFiles().size()]));
        }
        if (mostImportantInfo == null) {
            return null;
        }
        if (folderAnnotation) {
            return format.annotateFolderNameHtml(name, mostImportantInfo, mostImportantFile);
        }
        return format.annotateNameHtml(name, mostImportantInfo, mostImportantFile);
    }

    @Override
    public Image annotateIcon(Image icon, VCSContext context) {
        boolean folderAnnotation = false;
        for (File file : context.getRootFiles()) {
            if (file.isDirectory()) {
                folderAnnotation = true;
                break;
            }
        }

        if (folderAnnotation == false && context.getRootFiles().size() > 1) {
            folderAnnotation = !Utils.shareCommonDataObject(context.getRootFiles().toArray(new File[context.getRootFiles().size()]));
        }
        if (folderAnnotation == false) {
            return null;
        }
        boolean isVersioned = false;
        for (File file : context.getRootFiles()) {
            // There is an assumption here that annotateName was already
            // called and StatusCache.getStatus was scheduled if
            // StatusCache.getCachedStatus returned null.
            StatusInfo info = cache.getCachedStatus(file, true);
            if ((info != null && (info.getStatus() & STATUS_BADGEABLE) != 0)) {
                isVersioned = true;
                break;
            }
        }
        if (!isVersioned) {
            return null;
        }
        boolean allExcluded = true;
        boolean modified = false;

        Map<File, StatusInfo> map = cache.getAllModifiedFiles();
        Map<File, StatusInfo> modifiedFiles = new HashMap<File, StatusInfo>();
        for (File file : map.keySet()) {
            StatusInfo info = map.get(file);
            if ((info.getStatus() & StatusInfo.STATUS_LOCAL_CHANGE) != 0) {
                modifiedFiles.put(file, info);
            }
        }

        for (File file : context.getRootFiles()) {
            if (VersioningSupport.isFlat(file)) {
                for (File mf : modifiedFiles.keySet()) {
                    if (mf.getParentFile().equals(file)) {
                        StatusInfo info = modifiedFiles.get(mf);
                        if (info.isDirectory()) {
                            continue;
                        }
                        int status = info.getStatus();
                        if (status == StatusInfo.STATUS_VERSIONED_CONFLICT) {
                            Image badge = ImageUtilities.loadImage("org/nbgit/resources/icons/conflicts-badge.png", true);  // NOI18N
                            return ImageUtilities.mergeImages(icon, badge, 16, 9);
                        }
                        modified = true;
                        allExcluded &= isExcludedFromCommit(mf.getAbsolutePath());
                    }
                }
            } else {
                for (File mf : modifiedFiles.keySet()) {
                    if (Utils.isAncestorOrEqual(file, mf)) {
                        StatusInfo info = modifiedFiles.get(mf);
                        int status = info.getStatus();
                        if ((status == StatusInfo.STATUS_NOTVERSIONED_NEWLOCALLY || status == StatusInfo.STATUS_VERSIONED_ADDEDLOCALLY) && file.equals(mf)) {
                            continue;
                        }
                        if (status == StatusInfo.STATUS_VERSIONED_CONFLICT) {
                            Image badge = ImageUtilities.loadImage("org/nbgit/resources/icons/conflicts-badge.png", true); // NOI18N
                            return ImageUtilities.mergeImages(icon, badge, 16, 9);
                        }
                        modified = true;
                        allExcluded &= isExcludedFromCommit(mf.getAbsolutePath());
                    }
                }
            }
        }

        if (modified && !allExcluded) {
            Image badge = ImageUtilities.loadImage("org/nbgit/resources/icons/modified-badge.png", true); // NOI18N
            return ImageUtilities.mergeImages(icon, badge, 16, 9);
        } else {
            return null;
        }
    }

    @Override
    public Action[] getActions(VCSContext ctx, VCSAnnotator.ActionDestination destination) {
        // TODO: get resource strings for all actions:
        ResourceBundle loc = NbBundle.getBundle(GitAnnotator.class);
        Node[] nodes = ctx.getElements().lookupAll(Node.class).toArray(new Node[0]);
        File[] files = ctx.getRootFiles().toArray(new File[ctx.getRootFiles().size()]);
        File root = GitUtils.getRootFile(ctx);
        boolean noneVersioned = root == null;
        boolean onlyFolders = onlyFolders(files);
        boolean onlyProjects = onlyProjects(nodes);

        List<Action> actions = new ArrayList<Action>(INITIAL_ACTION_ARRAY_LENGTH);
        if (destination == VCSAnnotator.ActionDestination.MainMenu) {
            actions.add(new InitAction(loc.getString("CTL_MenuItem_Create"), ctx)); // NOI18N
            actions.add(null);
            actions.add(new StatusAction(loc.getString("CTL_PopupMenuItem_Status"), ctx)); // NOI18N
            actions.add(new DiffAction(loc.getString("CTL_PopupMenuItem_Diff"), ctx)); // NOI18N
            actions.add(new UpdateAction(loc.getString("CTL_PopupMenuItem_Update"), ctx)); // NOI18N
            actions.add(new CommitAction(loc.getString("CTL_PopupMenuItem_Commit"), ctx)); // NOI18N
            actions.add(null);
            /*
            actions.add(new ExportDiffAction(loc.getString("CTL_PopupMenuItem_ExportDiff"), ctx)); // NOI18N
            actions.add(new ApplyDiffAction(loc.getString("CTL_PopupMenuItem_ImportDiff"), ctx)); // NOI18N
            actions.add(null);
            if (root != null) {
            actions.add(new CloneAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_CloneLocal",  // NOI18N
            root.getName()), ctx));
            }
            actions.add(new CloneExternalAction(loc.getString("CTL_PopupMenuItem_CloneOther"), ctx));     // NOI18N
            actions.add(null);
            actions.add(new FetchAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_FetchLocal"), ctx)); // NOI18N
            actions.add(new PushAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_PushLocal"), ctx)); // NOI18N
            actions.add(new PushOtherAction(loc.getString("CTL_PopupMenuItem_PushOther"), ctx)); // NOI18N
            actions.add(new PullAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_PullLocal"), ctx)); // NOI18N
            actions.add(new PullOtherAction(loc.getString("CTL_PopupMenuItem_PullOther"), ctx)); // NOI18N
            actions.add(new MergeAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_Merge"), ctx)); // NOI18N
            actions.add(null);
            AnnotateAction tempA = new AnnotateAction(loc.getString("CTL_PopupMenuItem_ShowAnnotations"), ctx); // NOI18N
            if (tempA.visible(nodes))
            tempA = new AnnotateAction(loc.getString("CTL_PopupMenuItem_HideAnnotations"), ctx);
            actions.add(tempA);
             */
            actions.add(new LogAction(loc.getString("CTL_PopupMenuItem_Log"), ctx)); // NOI18N
        /*
            actions.add(new IncomingAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_ShowIncoming"), ctx)); // NOI18N
            actions.add(new OutAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_ShowOut"), ctx)); // NOI18N
            actions.add(new ViewAction(loc.getString("CTL_PopupMenuItem_View"), ctx)); // NOI18N
             */
            actions.add(null);
            actions.add(new RevertModificationsAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_Revert"), ctx)); // NOI18N
        /*
            actions.add(new StashAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_Stash"), ctx));
            actions.add(new StripAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_Strip"), ctx)); // NOI18N
            actions.add(new BackoutAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_Backout"), ctx)); // NOI18N
            actions.add(new RollbackAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_Rollback"), ctx)); // NOI18N
            actions.add(new ResolveConflictsAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_Resolve"), ctx)); // NOI18N
             * */
        /*
            if (!onlyProjects && !onlyFolders) {
            IgnoreAction tempIA = new IgnoreAction(loc.getString("CTL_PopupMenuItem_Ignore"), ctx); // NOI18N
            actions.add(tempIA);
            }
             * */
            actions.add(null);
            actions.add(new CustomMenu(ctx, true));
            actions.add(null);
            actions.add(new PropertiesAction(loc.getString("CTL_PopupMenuItem_Properties"), ctx)); // NOI18N
        } else if (noneVersioned) {
            actions.add(new InitAction(loc.getString("CTL_PopupMenuItem_Create"), ctx));
        } else {
            actions.add(new StatusAction(loc.getString("CTL_PopupMenuItem_Status"), ctx)); // NOI18N
            actions.add(new DiffAction(loc.getString("CTL_PopupMenuItem_Diff"), ctx)); // NOI18N
            actions.add(new UpdateAction(loc.getString("CTL_PopupMenuItem_Update"), ctx)); // NOI18N
            actions.add(new CommitAction(loc.getString("CTL_PopupMenuItem_Commit"), ctx)); // NOI18N
            actions.add(null);
            /*
            if (root != null)
            actions.add(new CloneAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_CloneLocal", // NOI18N
            root.getName()), ctx));
            
            actions.add(null);
            actions.add(new FetchAction(NbBundle.getMessage(GitAnnotator.class,
            "CTL_PopupMenuItem_FetchLocal"), ctx)); // NOI18N
            actions.add(new PushAction(NbBundle.getMessage(GitAnnotator.class,
            "CTL_PopupMenuItem_PushLocal"), ctx)); // NOI18N
            actions.add(new PullAction(NbBundle.getMessage(GitAnnotator.class,
            "CTL_PopupMenuItem_PullLocal"), ctx)); // NOI18N
            actions.add(new MergeAction(NbBundle.getMessage(GitAnnotator.class,
            "CTL_PopupMenuItem_Merge"), ctx)); // NOI18N
            actions.add(null);
            if (!onlyFolders) {
            AnnotateAction tempA = new AnnotateAction(loc.getString("CTL_PopupMenuItem_ShowAnnotations"), ctx);  // NOI18N
            if (tempA.visible(nodes))
            tempA = new AnnotateAction(loc.getString("CTL_PopupMenuItem_HideAnnotations"), ctx);
            actions.add(tempA);
            }
             */
            actions.add(new LogAction(loc.getString("CTL_PopupMenuItem_Log"), ctx)); // NOI18N
        /*
            actions.add(new IncomingAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_ShowIncoming"), ctx)); // NOI18N
            actions.add(new OutAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_ShowOut"), ctx)); // NOI18N
            actions.add(new ViewAction(loc.getString("CTL_PopupMenuItem_View"), ctx)); // NOI18N
             */
            actions.add(null);
            actions.add(new RevertModificationsAction(NbBundle.getMessage(GitAnnotator.class,
                    "CTL_PopupMenuItem_Revert"), ctx)); // NOI18N
        /*
            actions.add(new StripAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_Strip"), ctx)); // NOI18N
            actions.add(new BackoutAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_Backout"), ctx)); // NOI18N
            actions.add(new RollbackAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_Rollback"), ctx)); // NOI18N
            actions.add(new ResolveConflictsAction(NbBundle.getMessage(GitAnnotator.class,
                    "CTL_PopupMenuItem_Resolve"), ctx)); // NOI18N
            if (!onlyProjects && !onlyFolders) {
                actions.add(new ConflictResolvedAction(NbBundle.getMessage(GitAnnotator.class,
                        "CTL_PopupMenuItem_MarkResolved"), ctx));
            }
             * */
            /*
            if (!onlyProjects && !onlyFolders) {
            IgnoreAction tempIA = new IgnoreAction(loc.getString("CTL_PopupMenuItem_Ignore"), ctx); // NOI18N
            actions.add(tempIA);
            }
             * */
            actions.add(null);
            actions.add(new PropertiesAction(loc.getString("CTL_PopupMenuItem_Properties"), ctx)); // NOI18N
        }

        return actions.toArray(new Action[actions.size()]);
    }

    private boolean isMoreImportant(StatusInfo a, StatusInfo b) {
        if (b == null) {
            return true;
        }
        if (a == null) {
            return false;
        }
        return GitUtils.getComparableStatus(a.getStatus()) < GitUtils.getComparableStatus(b.getStatus());
    }

    private boolean isExcludedFromCommit(String absolutePath) {
        return false;
    }

    private boolean isNothingVersioned(File[] files) {
        for (File file : files) {
            if ((cache.getStatus(file).getStatus() & StatusInfo.STATUS_MANAGED) != 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean onlyProjects(Node[] nodes) {
        if (nodes == null) {
            return false;
        }
        for (Node node : nodes) {
            if (node.getLookup().lookup(Project.class) == null) {
                return false;
            }
        }
        return true;
    }

    private boolean onlyFolders(File[] files) {
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                return false;
            }
            if (!files[i].exists() && !cache.getStatus(files[i]).isDirectory()) {
                return false;
            }
        }
        return true;
    }

    private void reScheduleScan(int delayMillis) {
        File dirToScan = dirsToScan.peek();
        if (!folderToScan.equals(dirToScan)) {
            if (!dirsToScan.offer(folderToScan)) {
                Git.LOG.log(Level.FINE, "reScheduleScan failed to add to dirsToScan queue: {0} ", folderToScan);
            }
        }
        scanTask.schedule(delayMillis);
    }

    private class ScanTask implements Runnable {

        public void run() {
            Thread.interrupted();
            File dirToScan = dirsToScan.poll();
            if (dirToScan != null) {
                cache.getScannedFiles(dirToScan, null);
                dirToScan = dirsToScan.peek();
                if (dirToScan != null) {
                    scanTask.schedule(1000);
                }
            }
        }
    }
}
