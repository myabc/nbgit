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
import java.io.IOException;
import java.util.Collection;
import org.nbgit.OutputLogger;
import org.eclipse.jgit.lib.Commit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectWriter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;

/**
 * Build a commit and write it to a repository.
 */
public class CommitBuilder extends ClientBuilder {

    private final IndexBuilder index;
    private PersonIdent personIdent;
    private String message;

    private CommitBuilder(Repository repository, IndexBuilder index) {
        super(repository);
        this.index = index;
        personIdent = new PersonIdent(repository);
    }

    /**
     * Create builder for repository.
     *
     * @param repository to use for the builder.
     * @return the builder.
     */
    public static CommitBuilder create(Repository repository) throws IOException {
        return new CommitBuilder(repository, IndexBuilder.create(repository));
    }

    /**
     * Create builder for repository root.
     *
     * @param workDir of the repository.
     * @return the builder.
     */
    public static CommitBuilder create(File workDir) throws IOException {
        return create(toRepository(workDir));
    }

    /**
     * Set logger to use for error and informational messages.
     *
     * @param logger to use for messages.
     * @return the builder.
     */
    public CommitBuilder log(OutputLogger logger) {
        index.log(logger);
        return log(CommitBuilder.class, logger);
    }

    /**
     * Mark files to be added (or updated if modified) in the commit.
     *
     * @param files to commit.
     * @return the builder.
     */
    public CommitBuilder addAll(Collection<File> files) throws IOException {
        index.addAll(files);
        return this;
    }

    /**
     * Mark files to be deleted in the commit.
     *
     * @param files to commit.
     * @return the builder.
     */
    public CommitBuilder deleteAll(Collection<File> files) {
        index.deleteAll(files);
        return this;
    }

    /**
     * Set the commit message.
     *
     * @param message to use for the commit.
     * @return the builder.
     */
    public CommitBuilder message(String message) {
        this.message = message;
        return this;
    }

    /**
     * Set the author and commit time for the commit.
     *
     * @param time to use for the commit.
     * @param timeZone to use for the commit.
     * @return the builder.
     */
    public CommitBuilder time(long time, int timeZone) {
        personIdent = new PersonIdent(personIdent, time, timeZone);
        return this;
    }

    /**
     * Write the commit to the repository.
     *
     * @throws IOException if creation of the commit fails.
     */
    public void write() throws IOException {
        index.write();
        final RefUpdate ru = repository.updateRef(Constants.HEAD);
        ObjectId[] parentIds;
        if (ru.getOldObjectId() != null)
            parentIds = new ObjectId[]{ru.getOldObjectId()};
        else
            parentIds = new ObjectId[0];

        ObjectId id = writeCommit(index.writeTree(), parentIds);
        if (!updateRef(ru, id))
            logger.output("Failed to update " + ru.getName() + " to commit " + id + ".");
    }

    private ObjectId writeCommit(ObjectId treeId, ObjectId[] parentIds) throws IOException {
        Commit commit = new Commit(repository, parentIds);

        commit.setTreeId(treeId);
        commit.setMessage(message.replaceAll("\r", "\n"));
        commit.setAuthor(personIdent);
        commit.setCommitter(personIdent);

        ObjectWriter writer = new ObjectWriter(repository);
        return writer.writeCommit(commit);
    }

    private boolean updateRef(RefUpdate ru, ObjectId id) throws IOException {
        ru.setNewObjectId(id);
        ru.setRefLogMessage(buildReflogMessage(), false);
        ru.update();
        return ru.getOldObjectId() != null
                ? ru.getResult() == RefUpdate.Result.FAST_FORWARD
                : ru.getResult() == RefUpdate.Result.NEW;
    }

    private String buildReflogMessage() {
        String firstLine = message;
        int newlineIndex = message.indexOf("\n");

        if (newlineIndex > 0) {
            firstLine = message.substring(0, newlineIndex);
        }
        return "\tcommit: " + firstLine;
    }

}
