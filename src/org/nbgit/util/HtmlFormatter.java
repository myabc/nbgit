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
package org.nbgit.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.regex.Pattern;
import org.nbgit.Git;
import org.nbgit.GitModuleConfig;
import org.nbgit.StatusInfo;
import org.netbeans.modules.versioning.spi.VersioningSupport;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

/**
 * Format annotations in HTML.
 */
public class HtmlFormatter {

    private static MessageFormat uptodateFormat = getFormat("uptodateFormat");  // NOI18N
    private static MessageFormat newLocallyFormat = getFormat("newLocallyFormat");  // NOI18N
    private static MessageFormat addedLocallyFormat = getFormat("addedLocallyFormat"); // NOI18N
    private static MessageFormat modifiedLocallyFormat = getFormat("modifiedLocallyFormat"); // NOI18N
    private static MessageFormat removedLocallyFormat = getFormat("removedLocallyFormat"); // NOI18N
    private static MessageFormat deletedLocallyFormat = getFormat("deletedLocallyFormat"); // NOI18N
    private static MessageFormat excludedFormat = getFormat("excludedFormat"); // NOI18N
    private static MessageFormat conflictFormat = getFormat("conflictFormat"); // NOI18N
    private static final int STATUS_TEXT_ANNOTABLE =
            StatusInfo.STATUS_NOTVERSIONED_EXCLUDED |
            StatusInfo.STATUS_NOTVERSIONED_NEWLOCALLY |
            StatusInfo.STATUS_VERSIONED_UPTODATE |
            StatusInfo.STATUS_VERSIONED_MODIFIEDLOCALLY |
            StatusInfo.STATUS_VERSIONED_CONFLICT |
            StatusInfo.STATUS_VERSIONED_REMOVEDLOCALLY |
            StatusInfo.STATUS_VERSIONED_DELETEDLOCALLY |
            StatusInfo.STATUS_VERSIONED_ADDEDLOCALLY;
    private static final Pattern lessThan = Pattern.compile("<");  // NOI18N
    private static HtmlFormatter instance;
    private String emptyFormat;
    private Boolean needRevisionForFormat;
    private MessageFormat format;

    public static HtmlFormatter getInstance() {
        if (instance == null) {
            instance = new HtmlFormatter();
        }
        return instance;
    }

    private HtmlFormatter() {
        initDefaults();
    }

    private void initDefaults() {
        Field[] fields = HtmlFormatter.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            String name = fields[i].getName();
            if (name.endsWith("Format")) // NOI18N
            {
                initDefaultColor(name.substring(0, name.length() - 6));
            }
        }
        refresh();
    }

    public void refresh() {
        String string = GitModuleConfig.getDefault().getAnnotationFormat();
        if (string != null && !string.trim().equals("")) { // NOI18N
            needRevisionForFormat = isRevisionInAnnotationFormat(string);
            string = string.replaceAll("\\{revision\\}", "\\{0\\}");           // NOI18N
            string = string.replaceAll("\\{status\\}", "\\{1\\}");           // NOI18N
            string = string.replaceAll("\\{folder\\}", "\\{2\\}");           // NOI18N
            format = new MessageFormat(string);
            emptyFormat = format.format(new String[]{"", "", ""}, new StringBuffer(), null).toString().trim(); // NOI18N
        }
    }

    public static boolean isRevisionInAnnotationFormat(String str) {
        if (str.indexOf("{revision}") != -1) // NOI18N
        {
            return true;
        } else {
            return false;
        }
    }

    private void initDefaultColor(String name) {
        String color = System.getProperty("git.color." + name);  // NOI18N
        if (color == null) {
            return;
        }
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
            Field field = HtmlFormatter.class.getDeclaredField(name + "Format");  // NOI18N
            MessageFormat msgFormat = new MessageFormat("<font color=\"" + colorString + "\">{0}</font><font color=\"#999999\">{1}</font>");  // NOI18N
            field.set(null, msgFormat);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid color name");  // NOI18N
        }
    }

    public String annotateNameHtml(File file, StatusInfo info) {
        return annotateNameHtml(file.getName(), info, file);
    }

    public String annotateNameHtml(String name, StatusInfo mostImportantInfo, File mostImportantFile) {
        // Git: The codes used to show the status of files are:
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
                if (status == StatusInfo.STATUS_VERSIONED_UPTODATE && sticky == null) {
                    textAnnotation = "";
                } else if (status == StatusInfo.STATUS_VERSIONED_UPTODATE) {
                    textAnnotation = " [" + sticky + "]";
                } else if (sticky == null) {
                    String statusText = mostImportantInfo.getShortStatusText();
                    if (!statusText.equals("")) // NOI18N
                    {
                        textAnnotation = " [" + mostImportantInfo.getShortStatusText() + "]";
                    } else {
                        textAnnotation = "";
                    }
                } else {
                    textAnnotation = " [" + mostImportantInfo.getShortStatusText() + "; " + sticky + "]";
                }
            }
        } else {
            textAnnotation = "";
        }
        if (textAnnotation.length() > 0) {
            textAnnotation = NbBundle.getMessage(HtmlFormatter.class, "textAnnotation", textAnnotation);
        }
        if (0 != (status & StatusInfo.STATUS_NOTVERSIONED_EXCLUDED)) {
            return excludedFormat.format(new Object[]{name, textAnnotation});
        } else if (0 != (status & StatusInfo.STATUS_VERSIONED_DELETEDLOCALLY)) {
            return deletedLocallyFormat.format(new Object[]{name, textAnnotation});
        } else if (0 != (status & StatusInfo.STATUS_VERSIONED_REMOVEDLOCALLY)) {
            return removedLocallyFormat.format(new Object[]{name, textAnnotation});
        } else if (0 != (status & StatusInfo.STATUS_NOTVERSIONED_NEWLOCALLY)) {
            return newLocallyFormat.format(new Object[]{name, textAnnotation});
        } else if (0 != (status & StatusInfo.STATUS_VERSIONED_ADDEDLOCALLY)) {
            return addedLocallyFormat.format(new Object[]{name, textAnnotation});
        } else if (0 != (status & StatusInfo.STATUS_VERSIONED_MODIFIEDLOCALLY)) {
            return modifiedLocallyFormat.format(new Object[]{name, textAnnotation});
        } else if (0 != (status & StatusInfo.STATUS_VERSIONED_UPTODATE)) {
            return uptodateFormat.format(new Object[]{name, textAnnotation});
        } else if (0 != (status & StatusInfo.STATUS_VERSIONED_CONFLICT)) {
            return conflictFormat.format(new Object[]{name, textAnnotation});
        } else if (0 != (status & StatusInfo.STATUS_NOTVERSIONED_NOTMANAGED)) {
            return name;
        } else if (status == StatusInfo.STATUS_UNKNOWN) {
            return name;
        } else {
            throw new IllegalArgumentException("Uncomparable status: " + status);
        }
    }

    private static MessageFormat getFormat(String key) {
        String format = NbBundle.getMessage(HtmlFormatter.class, key);
        return new MessageFormat(format);
    }

    private String htmlEncode(String name) {
        if (name.indexOf('<') == -1) {
            return name;
        }
        return lessThan.matcher(name).replaceAll("&lt;"); // NOI18N
    }

    /**
     * Applies custom format.
     */
    private String formatAnnotation(StatusInfo info, File file) {
        String statusString = "";  // NOI18N
        int status = info.getStatus();
        if (status != StatusInfo.STATUS_VERSIONED_UPTODATE) {
            statusString = info.getShortStatusText();
        }
        String revisionString = "";     // NOI18N
        String binaryString = "";       // NOI18N

        if (needRevisionForFormat) {
            if ((status & StatusInfo.STATUS_NOTVERSIONED_EXCLUDED) == 0) {
                try {
                    File root = Git.getInstance().getTopmostManagedParent(file);
                    Repository repo = Git.getInstance().getRepository(root);
                    ObjectId branch = repo.resolve(repo.getFullBranch());
                    String absPath = file.getAbsolutePath();
                    String relPath = absPath.replace(root.getAbsolutePath(), "");
                    RevWalk walk = new RevWalk(repo);
                    RevCommit start = walk.parseCommit(branch);
                    TreeFilter filter = PathFilter.create(relPath);

                    walk.setTreeFilter(filter);
                    walk.markStart(start);

                    for (RevCommit commit : walk) {
                        revisionString = commit.getId().name();
                        break;
                    }
                    walk.dispose();

                } catch (IOException ex) {
                    NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(ex);
                    DialogDisplayer.getDefault().notifyLater(e);
                }
            }
        }
        String stickyString = null;
        if (stickyString == null) {
            stickyString = "";
        }
        Object[] arguments = new Object[]{
            revisionString,
            statusString,
            stickyString,
        };

        String annotation = format.format(arguments, new StringBuffer(), null).toString().trim();
        if (annotation.equals(emptyFormat)) {
            return "";
        } else {
            return " " + annotation;
        }
    }

    public String annotateFolderNameHtml(String name, StatusInfo mostImportantInfo, File mostImportantFile) {
        String nameHtml = htmlEncode(name);
        if (mostImportantInfo.getStatus() == StatusInfo.STATUS_NOTVERSIONED_EXCLUDED) {
            return excludedFormat.format(new Object[]{nameHtml, ""});
        }
        String fileName = mostImportantFile.getName();
        if (fileName.equals(name)) {
            return uptodateFormat.format(new Object[]{nameHtml, ""});        // Label top level repository nodes with a repository name label when:
        // Display Name (name) is different from its repo name (repo.getName())
        }
        fileName = null;
        File repo = Git.getInstance().getTopmostManagedParent(mostImportantFile);
        if (repo != null && repo.equals(mostImportantFile)) {
            if (!repo.getName().equals(name)) {
                fileName = repo.getName();
            }
        }
        if (fileName != null) {
            return uptodateFormat.format(new Object[]{nameHtml, " [" + fileName + "]"}); // NOI18N
        } else {
            return uptodateFormat.format(new Object[]{nameHtml, ""}); // NOI18N
        }
    }
}
