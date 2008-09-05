/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
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

import java.io.File;
import java.io.Serializable;
import java.util.ResourceBundle;
import org.openide.util.NbBundle;

/**
 * Immutable class encapsulating status of a file.
 *
 * @author Maros Sandor
 */
public class StatusInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * There is nothing known about the file, it may not even exist.
     */
    public static final int STATUS_UNKNOWN = 0;
    /**
     * The file is not managed by the module, i.e. the user does not wish it to be under control of this
     * versioning system module. All files except files under versioned roots have this status.
     */
    public static final int STATUS_NOTVERSIONED_NOTMANAGED = 1;
    /**
     * The file exists locally but is NOT under version control because it should not be (i.e. is has
     * the Ignore property set or resides under an excluded folder). The file itself IS under a versioned root.
     */
    public static final int STATUS_NOTVERSIONED_EXCLUDED = 2;
    /**
     * The file exists locally but is NOT under version control, mostly because it has not been added
     * to the repository yet.
     */
    public static final int STATUS_NOTVERSIONED_NEWLOCALLY = 4;
    /**
     * The file is under version control and is in sync with repository.
     */
    public static final int STATUS_VERSIONED_UPTODATE = 8;
    /**
     * The file is modified locally and was not yet modified in repository.
     */
    public static final int STATUS_VERSIONED_MODIFIEDLOCALLY = 16;
    /**
     * The file was not modified locally but an updated version exists in repository.
     */
    public static final int STATUS_VERSIONED_MODIFIEDINREPOSITORY = 32;
    /**
     * Merging during update resulted in merge conflict. Conflicts in the local copy must be resolved before
     * the file can be commited.
     */
    public static final int STATUS_VERSIONED_CONFLICT = 64;
    /**
     * The file was modified both locally and remotely and these changes may or may not result in
     * merge conflict.
     */
    public static final int STATUS_VERSIONED_MERGE = 128;
    /**
     * The file does NOT exist locally and exists in repository, it has beed removed locally, waits
     * for commit.
     */
    public static final int STATUS_VERSIONED_REMOVEDLOCALLY = 256;
    /**
     * The file does NOT exist locally but exists in repository and has not yet been downloaded.
     */
    public static final int STATUS_VERSIONED_NEWINREPOSITORY = 512;
    /**
     * The file has been removed from repository.
     */
    public static final int STATUS_VERSIONED_REMOVEDINREPOSITORY = 1024;
    /**
     * The file does NOT exist locally and exists in repository, it has beed removed locally.
     */
    public static final int STATUS_VERSIONED_DELETEDLOCALLY = 2048;
    /**
     * The file exists locally and has beed scheduled for addition to repository. This status represents
     * state after the 'add' command.
     */
    public static final int STATUS_VERSIONED_ADDEDLOCALLY = 4096;
    public static final int STATUS_VERSIONED_COPIEDLOCALLY = 8192;
    public static final int STATUS_ALL = ~0;
    /**
     * All statuses except <tt>STATUS_NOTVERSIONED_NOTMANAGED</tt>
     *
     * <p>Note: it covers ignored files.
     */
    public static final int STATUS_MANAGED = StatusInfo.STATUS_ALL & ~StatusInfo.STATUS_NOTVERSIONED_NOTMANAGED;
    public static final int STATUS_VERSIONED = StatusInfo.STATUS_VERSIONED_UPTODATE |
        StatusInfo.STATUS_VERSIONED_MODIFIEDLOCALLY |
        StatusInfo.STATUS_VERSIONED_MODIFIEDINREPOSITORY |
        StatusInfo.STATUS_VERSIONED_CONFLICT |
        StatusInfo.STATUS_VERSIONED_MERGE |
        StatusInfo.STATUS_VERSIONED_REMOVEDLOCALLY |
        StatusInfo.STATUS_VERSIONED_REMOVEDINREPOSITORY |
        StatusInfo.STATUS_VERSIONED_DELETEDLOCALLY |
        StatusInfo.STATUS_VERSIONED_ADDEDLOCALLY |
        StatusInfo.STATUS_VERSIONED_COPIEDLOCALLY;
    public static final int STATUS_IN_REPOSITORY = StatusInfo.STATUS_VERSIONED_UPTODATE |
        StatusInfo.STATUS_VERSIONED_MODIFIEDLOCALLY |
        StatusInfo.STATUS_VERSIONED_MODIFIEDINREPOSITORY |
        StatusInfo.STATUS_VERSIONED_CONFLICT |
        StatusInfo.STATUS_VERSIONED_MERGE |
        StatusInfo.STATUS_VERSIONED_REMOVEDLOCALLY |
        StatusInfo.STATUS_VERSIONED_NEWINREPOSITORY |
        StatusInfo.STATUS_VERSIONED_REMOVEDINREPOSITORY |
        StatusInfo.STATUS_VERSIONED_DELETEDLOCALLY;
    public static final int STATUS_LOCAL_CHANGE =
        StatusInfo.STATUS_NOTVERSIONED_NEWLOCALLY |
        StatusInfo.STATUS_VERSIONED_ADDEDLOCALLY |
        StatusInfo.STATUS_VERSIONED_COPIEDLOCALLY |
        StatusInfo.STATUS_VERSIONED_CONFLICT |
        StatusInfo.STATUS_VERSIONED_DELETEDLOCALLY |
        StatusInfo.STATUS_VERSIONED_MERGE |
        StatusInfo.STATUS_VERSIONED_REMOVEDLOCALLY |
        StatusInfo.STATUS_VERSIONED_MODIFIEDLOCALLY;
    /**
     * Modified, in conflict, scheduled for removal or addition;
     * or deleted but with existing entry record.
     */
    public static final int STATUS_REVERTIBLE_CHANGE =
        StatusInfo.STATUS_VERSIONED_ADDEDLOCALLY |
        StatusInfo.STATUS_VERSIONED_COPIEDLOCALLY |
        StatusInfo.STATUS_VERSIONED_CONFLICT |
        StatusInfo.STATUS_VERSIONED_MERGE |
        StatusInfo.STATUS_VERSIONED_REMOVEDLOCALLY |
        StatusInfo.STATUS_VERSIONED_DELETEDLOCALLY |
        StatusInfo.STATUS_VERSIONED_MODIFIEDLOCALLY;
    public static final int STATUS_REMOTE_CHANGE =
        StatusInfo.STATUS_VERSIONED_MERGE |
        StatusInfo.STATUS_VERSIONED_MODIFIEDINREPOSITORY |
        StatusInfo.STATUS_VERSIONED_NEWINREPOSITORY |
        StatusInfo.STATUS_VERSIONED_REMOVEDINREPOSITORY;
    /**
     * Status constant.
     */
    private final int status;
    /**
     * More detailed information about a file, you may disregard the field if not needed.
     */
    private transient File entry;
    /**
     * Directory indicator, mainly because of files that may have been deleted so file.isDirectory() won't work.
     */
    private final boolean isDirectory;

    /**
     * For deserialization purposes only.
     */
    public StatusInfo()
    {
        status = 0;
        isDirectory = false;
    }

    public StatusInfo(int status, File entry, boolean isDirectory)
    {
        this.status = status;
        this.entry = entry;
        this.isDirectory = isDirectory;
    }

    StatusInfo(int status, boolean isDirectory)
    {
        this(status, null, isDirectory);
    }

    /**
     * Retrieves the status constant representing status of the file.
     *
     * @return one of status constants
     */
    public int getStatus()
    {
        return status;
    }

    public boolean isDirectory()
    {
        return isDirectory;
    }

    /**
     * Retrieves file's Status.
     *
     * @param file file this information belongs to or null if you do not want the entry to be read from disk
     * in case it is not loaded yet
     * @return Status parsed entry form the .svn/entries file or null if the file does not exist,
     * is not versioned or its entry is invalid
     */
    public File getStatus(File file)
    {
        if (entry == null && file != null)
            readEntry(file);
        return entry;
    }

    private void readEntry(File file)
    {
        // Fetches File info from .svn directory:
        // entry = Subversion.getInstance().getClient(true).getSingleStatus(file);
        entry = null;       // TODO: read your detailed information about the file here, or disregard the entry field
    }

    /**
     * Returns localized text representation of status.
     *
     * @return status name, for multistatuses prefers local
     * status name.
     */
    public String getStatusText()
    {
        return getStatusText(~0);
    }

    /**
     * Returns localized text representation of status.
     *
     * @param displayStatuses statuses bitmask
     *
     * @return status name, for multistatuses prefers local
     * status name, for masked <tt>""</tt>. // NOI18N
     */
    public String getStatusText(int displayStatuses)
    {
        int stat = this.status & displayStatuses;
        ResourceBundle loc = NbBundle.getBundle(StatusInfo.class);
        if (stat == StatusInfo.STATUS_UNKNOWN)
            return loc.getString("CTL_FileInfoStatus_Unknown");
        else if (StatusInfo.match(stat, StatusInfo.STATUS_NOTVERSIONED_EXCLUDED))
            return loc.getString("CTL_FileInfoStatus_Excluded");
        else if (StatusInfo.match(stat, StatusInfo.STATUS_NOTVERSIONED_NEWLOCALLY))
            return loc.getString("CTL_FileInfoStatus_NewLocally");
        else if (StatusInfo.match(stat, StatusInfo.STATUS_VERSIONED_ADDEDLOCALLY))
            /* FIXME
            if (entry != null && entry.isCopied())
            return loc.getString("CTL_FileInfoStatus_AddedLocallyCopied");
             */
            return loc.getString("CTL_FileInfoStatus_AddedLocally"); // NOI18N
        else if (StatusInfo.match(stat, StatusInfo.STATUS_VERSIONED_COPIEDLOCALLY))
            return loc.getString("CTL_FileInfoStatus_AddedLocallyCopied");
        else if (StatusInfo.match(stat, StatusInfo.STATUS_VERSIONED_UPTODATE))
            return loc.getString("CTL_FileInfoStatus_UpToDate");
        else if (StatusInfo.match(stat, StatusInfo.STATUS_VERSIONED_CONFLICT))
            return loc.getString("CTL_FileInfoStatus_Conflict");
        else if (StatusInfo.match(stat, StatusInfo.STATUS_VERSIONED_MERGE))
            return loc.getString("CTL_FileInfoStatus_Merge");
        else if (StatusInfo.match(stat, StatusInfo.STATUS_VERSIONED_DELETEDLOCALLY))
            return loc.getString("CTL_FileInfoStatus_DeletedLocally");
        else if (StatusInfo.match(stat, StatusInfo.STATUS_VERSIONED_REMOVEDLOCALLY))
            return loc.getString("CTL_FileInfoStatus_RemovedLocally");
        else if (StatusInfo.match(stat, StatusInfo.STATUS_VERSIONED_MODIFIEDLOCALLY))
            return loc.getString("CTL_FileInfoStatus_ModifiedLocally");
        else if (StatusInfo.match(stat, StatusInfo.STATUS_VERSIONED_NEWINREPOSITORY))
            return loc.getString("CTL_FileInfoStatus_NewInRepository");
        else if (StatusInfo.match(stat, StatusInfo.STATUS_VERSIONED_MODIFIEDINREPOSITORY))
            return loc.getString("CTL_FileInfoStatus_ModifiedInRepository");
        else if (StatusInfo.match(stat, StatusInfo.STATUS_VERSIONED_REMOVEDINREPOSITORY))
            return loc.getString("CTL_FileInfoStatus_RemovedInRepository");
        else
            return "";
    }

    /**
     * @return short status name for local changes, for remote
     * changes returns <tt>""</tt> // NOI18N
     */
    public String getShortStatusText()
    {
        ResourceBundle loc = NbBundle.getBundle(StatusInfo.class);
        if (StatusInfo.match(status, StatusInfo.STATUS_NOTVERSIONED_EXCLUDED))
            return loc.getString("CTL_FileInfoStatus_Excluded_Short");
        else if (StatusInfo.match(status, StatusInfo.STATUS_NOTVERSIONED_NEWLOCALLY))
            return loc.getString("CTL_FileInfoStatus_NewLocally_Short");
        else if (StatusInfo.match(status, StatusInfo.STATUS_VERSIONED_ADDEDLOCALLY)) /* {
            if (entry != null && entry.isCopied())
            return loc.getString("CTL_FileInfoStatus_AddedLocallyCopied_Short");
             */

            return loc.getString("CTL_FileInfoStatus_AddedLocally_Short"); // NOI18N
        else if (StatusInfo.match(status, StatusInfo.STATUS_VERSIONED_COPIEDLOCALLY))
            return loc.getString("CTL_FileInfoStatus_AddedLocallyCopied_Short");
        else if (status == StatusInfo.STATUS_VERSIONED_REMOVEDLOCALLY)
            return loc.getString("CTL_FileInfoStatus_RemovedLocally_Short");
        else if (status == StatusInfo.STATUS_VERSIONED_DELETEDLOCALLY)
            return loc.getString("CTL_FileInfoStatus_DeletedLocally_Short");
        else if (StatusInfo.match(status, StatusInfo.STATUS_VERSIONED_MODIFIEDLOCALLY))
            return loc.getString("CTL_FileInfoStatus_ModifiedLocally_Short");
        else if (StatusInfo.match(status, StatusInfo.STATUS_VERSIONED_CONFLICT))
            return loc.getString("CTL_FileInfoStatus_Conflict_Short");
        else
            return "";
    }

    private static boolean match(int status, int mask)
    {
        return (status & mask) != 0;
    }

    /**
     * Two StatusInfo objects are equivalent if their status contants are equal AND they both reperesent a file (or
     * both represent a directory) AND Entries they cache, if they can be compared, are equal.
     *
     * @param other object to compare to
     * @return true if status constants of both object are equal, false otherwise
     */
    public static boolean equivalent(StatusInfo main, StatusInfo other)
    {
        if (other == null || main.getStatus() != other.getStatus() || main.isDirectory() != other.isDirectory())
            return false;

        File e1 = main.getStatus(null);
        File e2 = other.getStatus(null);
        return e1 == e2 || e1 == null || e2 == null || equal(e1, e2);
    }

    /**
     * Replacement for missing Entry.equals(). It is implemented as a separate method to maintain compatibility.
     *
     * @param e1 first entry to compare
     * @param e2 second Entry to compare
     * @return true if supplied entries contain equivalent information
     */
    private static boolean equal(File e1, File e2)
    {
        // TODO: use your own logic here
        return true;
    }

    @Override
    public String toString()
    {
        return "Text: " + status + " " + getStatusText(status); // NOI18N
    }

}
