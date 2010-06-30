/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Jonas Fonseca <fonseca@diku.dk>
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file.
 *
 * This particular file is subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
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
package org.nbgit.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.TreeEntry;

/**
 * Build a checkout of files from a revision.
 */
public class CheckoutBuilder extends ClientBuilder {

    private static final String BACKUP_EXT = ".orig";
    private final HashMap<RevisionEntry, File> fileMappings = new HashMap<RevisionEntry, File>();
    private boolean backup;
    private Tree tree;
    private DirCache index;

    private CheckoutBuilder(Repository repository) {
        super(repository);
    }

    /**
     * Create builder for repository.
     *
     * @param repository to use for the builder.
     * @return the builder.
     */
    public static CheckoutBuilder create(Repository repository) {
        return new CheckoutBuilder(repository);
    }

    /**
     * Create builder from working directory.
     *
     * @param workDir of the repository.
     * @return the builder.
     * @throws IOException if loading the repository fails.
     */
    public static CheckoutBuilder create(File workDir) throws IOException {
        return create(toRepository(workDir));
    }

    /**
     * Set revision to check out.
     *
     * @param revision to checkout.
     * @return the builder.
     * @throws IOException if the builder fails to load the revision.
     * @throws IllegalArgumentException if the builder fails to resolve
     *  the revision.
     */
    public CheckoutBuilder revision(String revision)
            throws IOException, IllegalArgumentException {
        tree = repository.mapTree(revision);
        if (tree == null)
            throw new IllegalArgumentException(revision);
        return this;
    }

    /**
     * Set file to be checked out to a specific destination.
     *
     * @param file to be checked out.
     * @param destination where the file should be checked out.
     * @return the builder.
     * @throws FileNotFoundException if the file cannot be resolved.
     * @throws IOException if checking of file existance fails.
     */
    public CheckoutBuilder file(File file, File destination)
            throws IOException, FileNotFoundException {
        String path = toPath(file);
        ObjectId blobId = null;
        int modeBits = 0;

        if (tree != null) {
            TreeEntry entry = tree.findBlobMember(path);
            if (entry != null) {
                blobId = entry.getId();
                modeBits = entry.getMode().getBits();
            }
        } else {
            if (index == null)
                index = DirCache.read(repository);
            DirCacheEntry entry = index.getEntry(path);
            if (entry != null) {
                blobId = entry.getObjectId();
                modeBits = entry.getRawMode();
            }
        }
        if (blobId == null)
            throw new FileNotFoundException(path);
        fileMappings.put(RevisionEntry.create(path, blobId, modeBits), destination);
        return this;
    }

    /**
     * Set files to be checked out. The destination of the files will be the
     * path of the file, which means their path relative to the repository's
     * working directory.
     *
     * @param files to be checked out.
     * @return the builder.
     * @throws FileNotFoundException if the file cannot be resolved.
     * @throws IOException if checking of file existance fails.
     */
    public CheckoutBuilder files(Collection<File> files)
            throws IOException, FileNotFoundException {
        for (File file : files) {
            file(file, file);
        }
        return this;
    }

    /**
     * Whether to backup existing files that would otherwise be overwritten.
     *
     * @param backup files?
     * @return the builder.
     */
    public CheckoutBuilder backup(boolean backup) {
        this.backup = backup;
        return this;
    }

    /**
     * Perform the checkout. Non-existing files added before a revision
     * was set will be ignored.
     *
     * @throws IOException if the checkout fails.
     */
    public void checkout() throws IOException {
        for (Map.Entry<RevisionEntry, File> mapping : fileMappings.entrySet()) {
            RevisionEntry entry = mapping.getKey();
            File file = mapping.getValue();
            if (backup)
                backupFile(file);
            checkoutEntry(entry.getObjectId(), entry.getModeBits(), file);
        }
    }

    private void backupFile(File file) throws IOException {
        String extension = BACKUP_EXT;

        for (int i = 0; i < 1024; i++) {
            File backupFile = new File(file.getAbsolutePath() + extension);
            if (!backupFile.exists()) {
                file.renameTo(backupFile);
                break;
            }
            extension = "." + i + BACKUP_EXT;
        }
    }

    /*
     * Code originally from GitIndex.
     */
    private void checkoutEntry(ObjectId blobId, int modeBits, File file) throws IOException {
        file.delete();
        file.getParentFile().mkdirs();

        FileChannel channel = new FileOutputStream(file).getChannel();
        try {
    	    byte[] bytes = repository.openBlob(blobId).getBytes();
	    ByteBuffer buffer = ByteBuffer.wrap(bytes);
	    if (channel.write(buffer) != bytes.length)
    	        throw new IOException("Could not write file " + file);
        } finally {
            channel.close();
        }
        setExecutable(file, FileMode.EXECUTABLE_FILE.equals(modeBits));
    }
}
