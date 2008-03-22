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

import java.awt.Image;
import java.io.File;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.swing.Action;
import org.netbeans.api.project.Project;
import org.netbeans.modules.git.ui.annotate.AnnotateAction;
import org.netbeans.modules.git.ui.clone.CloneAction;
import org.netbeans.modules.git.ui.clone.CloneExternalAction;
import org.netbeans.modules.git.ui.commit.CommitAction;
import org.netbeans.modules.git.ui.create.CreateAction;
import org.netbeans.modules.git.ui.diff.DiffAction;
import org.netbeans.modules.git.ui.diff.ExportDiffAction;
import org.netbeans.modules.git.ui.diff.ImportDiffAction;
import org.netbeans.modules.git.ui.ignore.IgnoreAction;
import org.netbeans.modules.git.ui.log.IncomingAction;
import org.netbeans.modules.git.ui.log.LogAction;
import org.netbeans.modules.git.ui.log.OutAction;
import org.netbeans.modules.git.ui.merge.MergeAction;
import org.netbeans.modules.git.ui.properties.PropertiesAction;
import org.netbeans.modules.git.ui.pull.FetchAction;
import org.netbeans.modules.git.ui.pull.PullAction;
import org.netbeans.modules.git.ui.pull.PullOtherAction;
import org.netbeans.modules.git.ui.push.PushAction;
import org.netbeans.modules.git.ui.push.PushOtherAction;
import org.netbeans.modules.git.ui.rollback.BackoutAction;
import org.netbeans.modules.git.ui.rollback.RollbackAction;
import org.netbeans.modules.git.ui.rollback.StripAction;
import org.netbeans.modules.git.ui.status.StatusAction;
import org.netbeans.modules.git.ui.update.ConflictResolvedAction;
import org.netbeans.modules.git.ui.update.ResolveConflictsAction;
import org.netbeans.modules.git.ui.update.RevertModificationsAction;
import org.netbeans.modules.git.ui.update.UpdateAction;
import org.netbeans.modules.git.ui.view.ViewAction;
import org.netbeans.modules.git.util.GitCommand;
import org.netbeans.modules.git.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSAnnotator;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.netbeans.modules.versioning.spi.VersioningSupport;
import org.netbeans.modules.versioning.util.Utils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

/**
 * Responsible for coloring file labels and file icons in the IDE and providing IDE with menu items.
 *
 * @author Maros Sandor
 */
public class GitAnnotator extends VCSAnnotator {

    private static final int INITIAL_ACTION_ARRAY_LENGTH = 25;
    private static MessageFormat uptodateFormat = getFormat("uptodateFormat");  // NOI18N
    private static MessageFormat newLocallyFormat = getFormat("newLocallyFormat");  // NOI18N
    private static MessageFormat addedLocallyFormat = getFormat("addedLocallyFormat"); // NOI18N
    private static MessageFormat modifiedLocallyFormat = getFormat("modifiedLocallyFormat"); // NOI18N
    private static MessageFormat removedLocallyFormat = getFormat("removedLocallyFormat"); // NOI18N
    private static MessageFormat deletedLocallyFormat = getFormat("deletedLocallyFormat"); // NOI18N
    private static MessageFormat excludedFormat = getFormat("excludedFormat"); // NOI18N
    private static MessageFormat conflictFormat = getFormat("conflictFormat"); // NOI18N
    
    private static final int STATUS_TEXT_ANNOTABLE =
            FileInformation.STATUS_NOTVERSIONED_EXCLUDED |
            FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY |
            FileInformation.STATUS_VERSIONED_UPTODATE |
            FileInformation.STATUS_VERSIONED_MODIFIEDLOCALLY |
            FileInformation.STATUS_VERSIONED_CONFLICT |
            FileInformation.STATUS_VERSIONED_REMOVEDLOCALLY |
            FileInformation.STATUS_VERSIONED_DELETEDLOCALLY |
            FileInformation.STATUS_VERSIONED_ADDEDLOCALLY;
    
    private static final Pattern lessThan = Pattern.compile("<");  // NOI18N
    
    private static final int STATUS_BADGEABLE = 
            FileInformation.STATUS_VERSIONED_UPTODATE |
            FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY |
            FileInformation.STATUS_VERSIONED_MODIFIEDLOCALLY;

    public static String ANNOTATION_REVISION    = "revision"; // NOI18N
    public static String ANNOTATION_STATUS      = "status"; // NOI18N
    public static String ANNOTATION_FOLDER      = "folder"; // NOI18N

    public static String[] LABELS = new String[] {ANNOTATION_REVISION, ANNOTATION_STATUS, ANNOTATION_FOLDER};

    private FileStatusCache cache;
    private MessageFormat format;
    private String emptyFormat;
    private Boolean needRevisionForFormat;
    private File folderToScan;
    private ConcurrentLinkedQueue<File> dirsToScan = new ConcurrentLinkedQueue<File>();
    private RequestProcessor.Task scanTask;
    private static final RequestProcessor rp = new RequestProcessor("GitAnnotateScan", 1, true); // NOI18N
    
    public GitAnnotator() {
        cache = Git.getInstance().getFileStatusCache();
        scanTask = rp.create(new ScanTask());
        initDefaults();
    }
   
    private void initDefaults() {
        Field [] fields = GitAnnotator.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            String name = fields[i].getName();
            if (name.endsWith("Format")) {  // NOI18N
                initDefaultColor(name.substring(0, name.length() - 6));
            }
        }
        refresh();
    }

    private void refresh() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void initDefaultColor(String name) {
        String color = System.getProperty("hg.color." + name);  // NOI18N
        if (color == null) return;
        setAnnotationColor(name, color);
    }


    /**
     * Changes annotation color of files.
     *
     * @param name name of the color to change. Can be one of:
     * newLocally, addedLocally, modifiedLocally, removedLocally, deletedLocally, newInRepository, modifiedInRepository,
     * removedInRepository, conflict, mergeable, excluded.
     * @param colorString new color in the format: 4455AA (RGB hexadecimal)
     */
    private void setAnnotationColor(String name, String colorString) {
        try {
            Field field = GitAnnotator.class.getDeclaredField(name + "Format");  // NOI18N
            MessageFormat format = new MessageFormat("<font color=\"" + colorString + "\">{0}</font><font color=\"#999999\">{1}</font>");  // NOI18N
            field.set(null, format);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid color name");  // NOI18N
        }
    }

    private static MessageFormat getFormat(String key) {
        String format = NbBundle.getMessage(GitAnnotator.class, key);
        return new MessageFormat(format);
    }
    
    @Override
    public String annotateName(String name, VCSContext context) {
        int includeStatus = FileInformation.STATUS_VERSIONED_UPTODATE | FileInformation.STATUS_LOCAL_CHANGE | FileInformation.STATUS_NOTVERSIONED_EXCLUDED;
        
        FileInformation mostImportantInfo = null;
        File mostImportantFile = null;
        boolean folderAnnotation = false;
        
        for (final File file : context.getRootFiles()) {
            FileInformation info = cache.getCachedStatus(file, true);
            if (info == null) {
                File parentFile = file.getParentFile();
                Git.LOG.log(Level.FINE, "null cached status for: {0} {1} {2}", new Object[] {file, folderToScan, parentFile});
                folderToScan = parentFile;
                reScheduleScan(1000);
                info = new FileInformation(FileInformation.STATUS_VERSIONED_UPTODATE, false);
            }
            int status = info.getStatus();
            if ((status & includeStatus) == 0) continue;
            
            if (isMoreImportant(info, mostImportantInfo)) {
                mostImportantInfo = info;
                mostImportantFile = file;
                folderAnnotation = file.isDirectory();
            }
        }
        
        if (folderAnnotation == false && context.getRootFiles().size() > 1) {
            folderAnnotation = !Utils.shareCommonDataObject(context.getRootFiles().toArray(new File[context.getRootFiles().size()]));
        }
        
        if (mostImportantInfo == null) return null;
        return folderAnnotation ?
            annotateFolderNameHtml(name, mostImportantInfo, mostImportantFile) :
            annotateNameHtml(name, mostImportantInfo, mostImportantFile);
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
        for (Iterator i = context.getRootFiles().iterator(); i.hasNext();) {
            File file = (File) i.next();
            // There is an assumption here that annotateName was already 
            // called and FileStatusCache.getStatus was scheduled if
            // FileStatusCache.getCachedStatus returned null.
            FileInformation info = cache.getCachedStatus(file, true);
            if ((info != null && (info.getStatus() & STATUS_BADGEABLE) != 0)) {
                isVersioned = true;
                break;
            }
        }
        if (!isVersioned) return null;
        
        boolean allExcluded = true;
        boolean modified = false;
        
        Map<File, FileInformation> map = cache.getAllModifiedFiles();
        Map<File, FileInformation> modifiedFiles = new HashMap<File, FileInformation>();
        for (Iterator i = map.keySet().iterator(); i.hasNext();) {
            File file = (File) i.next();
            FileInformation info = map.get(file);
            if ((info.getStatus() & FileInformation.STATUS_LOCAL_CHANGE) != 0) modifiedFiles.put(file, info);
        }
        
        for (Iterator i = context.getRootFiles().iterator(); i.hasNext();) {
            File file = (File) i.next();
            if (VersioningSupport.isFlat(file)) {
                for (Iterator j = modifiedFiles.keySet().iterator(); j.hasNext();) {
                    File mf = (File) j.next();
                    if (mf.getParentFile().equals(file)) {
                        FileInformation info = modifiedFiles.get(mf);
                        if (info.isDirectory()) continue;
                        int status = info.getStatus();
                        if (status == FileInformation.STATUS_VERSIONED_CONFLICT) {
                            Image badge = Utilities.loadImage("org/netbeans/modules/mercurial/resources/icons/conflicts-badge.png", true);  // NOI18N
                            return Utilities.mergeImages(icon, badge, 16, 9);
                        }
                        modified = true;
                        allExcluded &= isExcludedFromCommit(mf.getAbsolutePath());
                    }
                }
            } else {
                for (Iterator j = modifiedFiles.keySet().iterator(); j.hasNext();) {
                    File mf = (File) j.next();
                    if (Utils.isAncestorOrEqual(file, mf)) {
                        FileInformation info = modifiedFiles.get(mf);
                        int status = info.getStatus();
                        if ((status == FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY || status == FileInformation.STATUS_VERSIONED_ADDEDLOCALLY) && file.equals(mf)) {
                            continue;
                        }
                        if (status == FileInformation.STATUS_VERSIONED_CONFLICT) {
                            Image badge = Utilities.loadImage("org/netbeans/modules/mercurial/resources/icons/conflicts-badge.png", true); // NOI18N
                            return Utilities.mergeImages(icon, badge, 16, 9);
                        }
                        modified = true;
                        allExcluded &= isExcludedFromCommit(mf.getAbsolutePath());
                    }
                }
            }
        }
        
        if (modified && !allExcluded) {
            Image badge = Utilities.loadImage("org/netbeans/modules/mercurial/resources/icons/modified-badge.png", true); // NOI18N
            return Utilities.mergeImages(icon, badge, 16, 9);
        } else {
            return null;
        }
    }
    
    @Override
    public Action[] getActions(VCSContext ctx, VCSAnnotator.ActionDestination destination) {
        // TODO: get resource strings for all actions:
        ResourceBundle loc = NbBundle.getBundle(GitAnnotator.class);
        Node [] nodes = ctx.getElements().lookupAll(Node.class).toArray(new Node[0]);
        File [] files = ctx.getRootFiles().toArray(new File[ctx.getRootFiles().size()]);
        File root = GitUtils.getRootFile(ctx);
        boolean noneVersioned = root == null;
        boolean onlyFolders = onlyFolders(files);
        boolean onlyProjects = onlyProjects(nodes);

        List<Action> actions = new ArrayList<Action>(INITIAL_ACTION_ARRAY_LENGTH);
        if (destination == VCSAnnotator.ActionDestination.MainMenu) {
            actions.add(new CreateAction(loc.getString("CTL_MenuItem_Create"), ctx)); // NOI18N
            actions.add(null);
            actions.add(new StatusAction(loc.getString("CTL_PopupMenuItem_Status"), ctx)); // NOI18N
            actions.add(new DiffAction(loc.getString("CTL_PopupMenuItem_Diff"), ctx)); // NOI18N
            actions.add(new UpdateAction(loc.getString("CTL_PopupMenuItem_Update"), ctx)); // NOI18N
            actions.add(new CommitAction(loc.getString("CTL_PopupMenuItem_Commit"), ctx)); // NOI18N
            actions.add(null);
            actions.add(new ExportDiffAction(loc.getString("CTL_PopupMenuItem_ExportDiff"), ctx)); // NOI18N
            actions.add(new ImportDiffAction(loc.getString("CTL_PopupMenuItem_ImportDiff"), ctx)); // NOI18N

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
            if (tempA.visible(nodes)) {
                tempA = new AnnotateAction(loc.getString("CTL_PopupMenuItem_HideAnnotations"), ctx); // NOI18N
            }
            actions.add(tempA);
            actions.add(new LogAction(loc.getString("CTL_PopupMenuItem_Log"), ctx)); // NOI18N
            actions.add(new IncomingAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_ShowIncoming"), ctx)); // NOI18N
            actions.add(new OutAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_ShowOut"), ctx)); // NOI18N
            actions.add(new ViewAction(loc.getString("CTL_PopupMenuItem_View"), ctx)); // NOI18N
            actions.add(null);
            actions.add(new RevertModificationsAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_Revert"), ctx)); // NOI18N
            actions.add(new StripAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_Strip"), ctx)); // NOI18N
            actions.add(new BackoutAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_Backout"), ctx)); // NOI18N
            actions.add(new RollbackAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_Rollback"), ctx)); // NOI18N
            actions.add(new ResolveConflictsAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_Resolve"), ctx)); // NOI18N
            if (!onlyProjects && !onlyFolders) {
                IgnoreAction tempIA = new IgnoreAction(loc.getString("CTL_PopupMenuItem_Ignore"), ctx); // NOI18N
                actions.add(tempIA);
            }
            actions.add(null);
            actions.add(new PropertiesAction(loc.getString("CTL_PopupMenuItem_Properties"), ctx)); // NOI18N
        } else {
            if (noneVersioned){
                actions.add(new CreateAction(loc.getString("CTL_PopupMenuItem_Create"), ctx)); // NOI18N
            }else{
                actions.add(new StatusAction(loc.getString("CTL_PopupMenuItem_Status"), ctx)); // NOI18N
                actions.add(new DiffAction(loc.getString("CTL_PopupMenuItem_Diff"), ctx)); // NOI18N
                actions.add(new UpdateAction(loc.getString("CTL_PopupMenuItem_Update"), ctx)); // NOI18N
                actions.add(new CommitAction(loc.getString("CTL_PopupMenuItem_Commit"), ctx)); // NOI18N
                actions.add(null);
                if (root != null) {
                    actions.add(new CloneAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_CloneLocal",  // NOI18N
                            root.getName()), ctx));
                }

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
                    if (tempA.visible(nodes)) {
                        tempA = new AnnotateAction(loc.getString("CTL_PopupMenuItem_HideAnnotations"), ctx);  // NOI18N
                    }
                    actions.add(tempA);
                }
                actions.add(new LogAction(loc.getString("CTL_PopupMenuItem_Log"), ctx)); // NOI18N
                actions.add(new IncomingAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_ShowIncoming"), ctx)); // NOI18N
                actions.add(new OutAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_ShowOut"), ctx)); // NOI18N
                actions.add(new ViewAction(loc.getString("CTL_PopupMenuItem_View"), ctx)); // NOI18N
                actions.add(null);
                actions.add(new RevertModificationsAction(NbBundle.getMessage(GitAnnotator.class,
                        "CTL_PopupMenuItem_Revert"), ctx)); // NOI18N
                actions.add(new StripAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_Strip"), ctx)); // NOI18N
                actions.add(new BackoutAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_Backout"), ctx)); // NOI18N
                actions.add(new RollbackAction(NbBundle.getMessage(GitAnnotator.class, "CTL_PopupMenuItem_Rollback"), ctx)); // NOI18N
                actions.add(new ResolveConflictsAction(NbBundle.getMessage(GitAnnotator.class,
                        "CTL_PopupMenuItem_Resolve"), ctx)); // NOI18N
                if (!onlyProjects  && !onlyFolders) {
                    actions.add(new ConflictResolvedAction(NbBundle.getMessage(GitAnnotator.class,
                        "CTL_PopupMenuItem_MarkResolved"), ctx)); // NOI18N
                    
                    IgnoreAction tempIA = new IgnoreAction(loc.getString("CTL_PopupMenuItem_Ignore"), ctx);  // NOI18N
                    actions.add(tempIA);
                }
                actions.add(null);
                actions.add(new PropertiesAction(loc.getString("CTL_PopupMenuItem_Properties"), ctx)); // NOI18N
            }
        }
        return actions.toArray(new Action[actions.size()]);
    }
    
    /**
     * Applies custom format.
     */
    private String formatAnnotation(FileInformation info, File file) {
        String statusString = "";  // NOI18N
        int status = info.getStatus();
        if (status != FileInformation.STATUS_VERSIONED_UPTODATE) {
            statusString = info.getShortStatusText();
        }

        String revisionString = "";     // NOI18N
        String binaryString = "";       // NOI18N

        if (needRevisionForFormat) {
            if ((status & FileInformation.STATUS_NOTVERSIONED_EXCLUDED) == 0) {
                try {
                    File repository = Git.getInstance().getTopmostManagedParent(file);
                    String revStr = GitCommand.getLastRevision(repository, file);
                    if (revStr != null) {
                        revisionString = revStr;
                    }
                } catch (GitException ex) {
                    NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(ex);
                    DialogDisplayer.getDefault().notifyLater(e);
                }
            }
        }

        //String stickyString = SvnUtils.getCopy(file);
        String stickyString = null;
        if (stickyString == null) {
            stickyString = ""; // NOI18N
        }

        Object[] arguments = new Object[] {
            revisionString,
            statusString,
            stickyString,
        };

        String annotation = format.format(arguments, new StringBuffer(), null).toString().trim();
        if(annotation.equals(emptyFormat)) {
            return ""; // NOI18N
        } else {
            return " " + annotation; // NOI18N
        }
    }

    public String annotateNameHtml(File file, FileInformation info) {
        return annotateNameHtml(file.getName(), info, file);
    }

    public String annotateNameHtml(String name, FileInformation mostImportantInfo, File mostImportantFile) {
        // Hg: The codes used to show the status of files are:
        // M = modified
        // A = added
        // R = removed
        // C = clean
        // ! = deleted, but still tracked
        // ? = not tracked
        // I = ignored (not shown by default)
        
        name = htmlEncode(name);
        
        String textAnnotation;
        boolean annotationsVisible = VersioningSupport.getPreferences().getBoolean(VersioningSupport.PREF_BOOLEAN_TEXT_ANNOTATIONS_VISIBLE, false);
        int status = mostImportantInfo.getStatus();
        
        if (annotationsVisible && mostImportantFile != null && (status & STATUS_TEXT_ANNOTABLE) != 0) {
            if (format != null) {
                textAnnotation = formatAnnotation(mostImportantInfo, mostImportantFile);
            } else {
                //String sticky = SvnUtils.getCopy(mostImportantFile);
                String sticky = null;
                if (status == FileInformation.STATUS_VERSIONED_UPTODATE && sticky == null) {
                    textAnnotation = "";  // NOI18N
                } else if (status == FileInformation.STATUS_VERSIONED_UPTODATE) {
                    textAnnotation = " [" + sticky + "]"; // NOI18N
                } else if (sticky == null) {
                    String statusText = mostImportantInfo.getShortStatusText();
                    if(!statusText.equals("")) { // NOI18N
                        textAnnotation = " [" + mostImportantInfo.getShortStatusText() + "]"; // NOI18N
                    } else {
                        textAnnotation = ""; // NOI18N
                    }
                } else {
                    textAnnotation = " [" + mostImportantInfo.getShortStatusText() + "; " + sticky + "]"; // NOI18N
                }
            }
        } else {
            textAnnotation = ""; // NOI18N
        }

        if (textAnnotation.length() > 0) {
            textAnnotation = NbBundle.getMessage(GitAnnotator.class, "textAnnotation", textAnnotation); // NOI18N
        }

        if (0 != (status & FileInformation.STATUS_NOTVERSIONED_EXCLUDED)) {
            return excludedFormat.format(new Object [] { name, textAnnotation });
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_DELETEDLOCALLY)) {
            return deletedLocallyFormat.format(new Object [] { name, textAnnotation });
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_REMOVEDLOCALLY)) {
            return removedLocallyFormat.format(new Object [] { name, textAnnotation });
        } else if (0 != (status & FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY)) {
            return newLocallyFormat.format(new Object [] { name, textAnnotation });
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_ADDEDLOCALLY)) {
            return addedLocallyFormat.format(new Object [] { name, textAnnotation });
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_MODIFIEDLOCALLY)) {
            return modifiedLocallyFormat.format(new Object [] { name, textAnnotation });
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_UPTODATE)) {
            return uptodateFormat.format(new Object [] { name, textAnnotation });
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_CONFLICT)) {
            return conflictFormat.format(new Object [] { name, textAnnotation });
        } else if (0 != (status & FileInformation.STATUS_NOTVERSIONED_NOTMANAGED)) {
            return name;
        } else if (status == FileInformation.STATUS_UNKNOWN) {
            return name;
        } else {
            throw new IllegalArgumentException("Uncomparable status: " + status); // NOI18N
        }
    }
    
    private String htmlEncode(String name) {
        if (name.indexOf('<') == -1) return name;
        return lessThan.matcher(name).replaceAll("&lt;"); // NOI18N
    }
    
    private String annotateFolderNameHtml(String name, FileInformation mostImportantInfo, File mostImportantFile) {
        String nameHtml = htmlEncode(name);
        if (mostImportantInfo.getStatus() == FileInformation.STATUS_NOTVERSIONED_EXCLUDED){
            return excludedFormat.format(new Object [] { nameHtml, ""}); // NOI18N
        }
        String fileName = mostImportantFile.getName();
        if (fileName.equals(name)){
            return uptodateFormat.format(new Object [] { nameHtml, "" }); // NOI18N
        }
        
        // Label top level repository nodes with a repository name label when:
        // Display Name (name) is different from its repo name (repo.getName())
        fileName = null;
        File repo = Git.getInstance().getTopmostManagedParent(mostImportantFile);
        if(repo != null && repo.equals(mostImportantFile)){
            if (!repo.getName().equals(name)){
                fileName = repo.getName();
            }          
        }
        if (fileName != null)
            return uptodateFormat.format(new Object [] { nameHtml, " [" + fileName + "]" }); // NOI18N
        else
            return uptodateFormat.format(new Object [] { nameHtml, "" }); // NOI18N
    }
    
    private boolean isMoreImportant(FileInformation a, FileInformation b) {
        if (b == null) return true;
        if (a == null) return false;
        return getComparableStatus(a.getStatus()) < getComparableStatus(b.getStatus());
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
        } else if (0 != (status & FileInformation.STATUS_NOTVERSIONED_EXCLUDED)) {
            return 100;
        } else if (0 != (status & FileInformation.STATUS_NOTVERSIONED_NOTMANAGED)) {
            return 101;
        } else if (status == FileInformation.STATUS_UNKNOWN) {
            return 102;
        } else {
            throw new IllegalArgumentException("Uncomparable status: " + status); // NOI18N
        }
    }
    
    private boolean isExcludedFromCommit(String absolutePath) {
        return false;
    }
    
    private boolean isNothingVersioned(File[] files) {
        for (File file : files) {
            if ((cache.getStatus(file).getStatus() & FileInformation.STATUS_MANAGED) != 0) return false;
        }
        return true;
    }
    
    private static boolean onlyProjects(Node[] nodes) {
        if (nodes == null) return false;
        for (Node node : nodes) {
            if (node.getLookup().lookup(Project.class) == null) return false;
        }
        return true;
    }
    
    private boolean onlyFolders(File[] files) {
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) return false;
            if (!files[i].exists() && !cache.getStatus(files[i]).isDirectory()) return false;
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