package org.nbgit.ui.status;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import javax.swing.Action;
import org.nbgit.StatusInfo;
import org.nbgit.Git;
import org.nbgit.ui.GitFileNode;
import org.nbgit.ui.diff.DiffAction;
import org.nbgit.util.GitUtils;
import org.nbgit.util.HtmlFormatter;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Sheet;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.Lookups;

/**
 * The node that is rendered in the SyncTable view. It gets values to display
 * from the GitFileNode which serves as the 'data' node for this 'visual' node.
 *
 * @author Maros Sandor
 */
public class SyncFileNode extends AbstractNode {

    private GitFileNode node;
    static final String COLUMN_NAME_NAME = "name"; // NOI18N;
    static final String COLUMN_NAME_PATH = "path"; // NOI18N;
    static final String COLUMN_NAME_STATUS = "status"; // NOI18N;
    static final String COLUMN_NAME_BRANCH = "branch"; // NOI18N;
    private String htmlDisplayName;
    private RequestProcessor.Task repoload;
    private final VersioningPanel panel;

    public SyncFileNode(GitFileNode node, VersioningPanel _panel) {
        this(Children.LEAF, node, _panel);
    }

    private SyncFileNode(Children children, GitFileNode node, VersioningPanel _panel) {
        super(children, Lookups.fixed(node.getLookupObjects()));
        this.node = node;
        this.panel = _panel;
        initProperties();
        refreshHtmlDisplayName();
    }

    public File getFile() {
        return node.getFile();
    }

    public StatusInfo getFileInformation() {
        return node.getInformation();
    }

    @Override
    public String getName() {
        return node.getName();
    }

    @Override
    public Action getPreferredAction() {
        // TODO: getPreferedAction
        if (node.getInformation().getStatus() == StatusInfo.STATUS_VERSIONED_CONFLICT) {
            return null;
        }
        return new DiffAction(null, GitUtils.getCurrentContext(null));
    }

    /**
     * Provide cookies to actions.
     * If a node represents primary file of a DataObject
     * it has respective DataObject cookies.
     */
    @SuppressWarnings("unchecked") // Adding getCookie(Class<Cookie> klass) results in name clash
    @Override
    public Cookie getCookie(Class klass) {
        FileObject fo = FileUtil.toFileObject(getFile());
        if (fo != null) {
            try {
                DataObject dobj = DataObject.find(fo);
                if (fo.equals(dobj.getPrimaryFile())) {
                    return dobj.getCookie(klass);
                }
            } catch (DataObjectNotFoundException e) {
                // ignore file without data objects
            }
        }
        return super.getCookie(klass);
    }

    private void initProperties() {
        if (node.getFile().isDirectory()) {
            setIconBaseWithExtension("org/openide/loaders/defaultFolder.gif"); // NOI18N
        }
        Sheet sheet = Sheet.createDefault();
        Sheet.Set ps = Sheet.createPropertiesSet();

        ps.put(new NameProperty());
        ps.put(new PathProperty());
        ps.put(new StatusProperty());
        ps.put(new BranchProperty());

        sheet.put(ps);
        setSheet(sheet);
    }

    private void refreshHtmlDisplayName() {
        StatusInfo info = node.getInformation();
        int status = info.getStatus();
        // Special treatment: Mergeable status should be annotated as Conflict in Versioning view according to UI spec
        if (status == StatusInfo.STATUS_VERSIONED_MERGE) {
            status = StatusInfo.STATUS_VERSIONED_CONFLICT;
        }
        htmlDisplayName = HtmlFormatter.getInstance().annotateNameHtml(node.getFile().getName(), info, null);
        fireDisplayNameChange(node.getName(), node.getName());
    }

    @Override
    public String getHtmlDisplayName() {
        return htmlDisplayName;
    }

    public void refresh() {
        refreshHtmlDisplayName();
    }

    private abstract class SyncFileProperty extends org.openide.nodes.PropertySupport.ReadOnly {

        @SuppressWarnings("unchecked")
        protected SyncFileProperty(String name, Class type, String displayName, String shortDescription) {
            super(name, type, displayName, shortDescription);
        }

        @Override
        public String toString() {
            try {
                return getValue().toString();
            } catch (Exception e) {
                Git.LOG.log(Level.INFO, null, e);
                return e.getLocalizedMessage();
            }
        }
    }

    private class BranchProperty extends SyncFileProperty {

        public BranchProperty() {
            super(COLUMN_NAME_BRANCH, String.class, NbBundle.getMessage(SyncFileNode.class, "BK2001"), NbBundle.getMessage(SyncFileNode.class, "BK2002")); // NOI18N
        }

        public Object getValue() {
            String branchInfo = panel.getDisplayBranchInfo();
            return branchInfo == null ? "" : branchInfo; // NOI18N
        }
    }

    private class PathProperty extends SyncFileProperty {

        private String shortPath;

        public PathProperty() {
            super(COLUMN_NAME_PATH, String.class, NbBundle.getMessage(SyncFileNode.class, "BK2003"), NbBundle.getMessage(SyncFileNode.class, "BK2004")); // NOI18N
            shortPath = GitUtils.getRelativePath(node.getFile());
            setValue("sortkey", shortPath + "\t" + SyncFileNode.this.getName()); // NOI18N
        }

        public Object getValue() throws IllegalAccessException, InvocationTargetException {
            return shortPath;
        }
    }

    // XXX it's not probably called, are there another Node lifecycle events
    @Override
    public void destroy() throws IOException {
        super.destroy();
        if (repoload != null) {
            repoload.cancel();
        }
    }

    private class NameProperty extends SyncFileProperty {

        public NameProperty() {
            super(COLUMN_NAME_NAME, String.class, NbBundle.getMessage(SyncFileNode.class, "BK2005"), NbBundle.getMessage(SyncFileNode.class, "BK2006")); // NOI18N
            setValue("sortkey", SyncFileNode.this.getName()); // NOI18N
        }

        public Object getValue() throws IllegalAccessException, InvocationTargetException {
            return SyncFileNode.this.getDisplayName();
        }
    }
    private static final String[] zeros = new String[]{"", "00", "0", ""}; // NOI18N

    private class StatusProperty extends SyncFileProperty {

        public StatusProperty() {
            super(COLUMN_NAME_STATUS, String.class, NbBundle.getMessage(SyncFileNode.class, "BK2007"), NbBundle.getMessage(SyncFileNode.class, "BK2008")); // NOI18N
            String shortPath = GitUtils.getRelativePath(node.getFile()); // NOI18N
            String sortable = Integer.toString(GitUtils.getComparableStatus(node.getInformation().getStatus()));
            setValue("sortkey", zeros[sortable.length()] + sortable + "\t" + shortPath + "\t" + SyncFileNode.this.getName()); // NOI18N
        }

        public Object getValue() throws IllegalAccessException, InvocationTargetException {
            StatusInfo finfo = node.getInformation();
            //TODO: finfo.getEntry(node.getFile());  // XXX not interested in return value, side effect loads ISVNStatus structure
            int mask = panel.getDisplayStatuses();
            return finfo.getStatusText(mask);
        }
    }
}
