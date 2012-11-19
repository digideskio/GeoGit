/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.cli.porcelain;

import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.api.porcelain.CloneOp;
import org.geogit.api.porcelain.InitOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.CLIInjectorBuilder;
import org.geogit.cli.GeogitCLI;
import org.geogit.repository.Repository;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Throwables;

/**
 * Clones a repository into a newly created directory, creates remote-tracking branches for each
 * branch in the cloned repository (visible using {@code geogit branch -r}), and creates and checks
 * out an initial branch that is forked from the cloned repository's currently active branch.
 * <p>
 * After the clone, a plain {@code geogit fetch} without arguments will update all the
 * remote-tracking branches, and a {@code geogit pull} without arguments will in addition merge the
 * remote master branch into the current master branch, if any.
 * <p>
 * This default configuration is achieved by creating references to the remote branch heads under
 * {@code refs/remotes/origin} and by initializing {@code remote.origin.url} and
 * {@code remote.origin.fetch} configuration variables.
 * <p>
 * CLI proxy for {@link CloneOp}
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit clone [--branch <name>] <repository> [<directory>]}
 * </ul>
 * 
 * @see CloneOp
 */
@Parameters(commandNames = "clone", commandDescription = "Clone a repository into a new directory")
public class Clone extends AbstractCommand implements CLICommand {

    @Parameter(names = { "-b", "--branch" }, description = "Branch to checkout when clone is finished.")
    private String branch;

    @Parameter(description = "<repository> [<directory>]")
    private List<String> args;

    /**
     * Executes the clone command using the provided options.
     * 
     * @param cli
     * @see org.geogit.cli.AbstractCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void runInternal(GeogitCLI cli) throws Exception {
        checkState(args != null && args.size() > 0, "You must specify a repository to clone.");
        checkState(args.size() < 3, "Too many arguments provided.");

        try {
            final File repoDir;
            {
                File currDir = cli.getPlatform().pwd();
                if (args != null && args.size() == 2) {
                    String target = args.get(1);
                    File f = new File(target);
                    if (!f.isAbsolute()) {
                        f = new File(currDir, target).getCanonicalFile();
                    }
                    repoDir = f;
                    if (!repoDir.exists() && !repoDir.mkdirs()) {
                        throw new IllegalStateException("Can't create directory "
                                + repoDir.getAbsolutePath());
                    }
                } else {
                    repoDir = currDir;
                }
            }

            GeoGIT geogit = new GeoGIT(cli.getGeogitInjector(), repoDir);

            Repository repository = geogit.command(InitOp.class).call();
            checkState(repository != null,
                    "Destination path already exists and is not an empty directory.");
            cli.setGeogit(geogit);
            repository.setInjectorBuilder(new CLIInjectorBuilder());

        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        CloneOp clone = cli.getGeogit().command(CloneOp.class);
        clone.setProgressListener(cli.getProgressListener());
        clone.setBranch(branch).setRepositoryURL(args.get(0));

        clone.call();
    }
}
