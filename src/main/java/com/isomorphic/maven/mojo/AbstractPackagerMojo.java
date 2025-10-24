package com.isomorphic.maven.mojo;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.google.common.base.Splitter;
import com.isomorphic.maven.packaging.Module;
import com.isomorphic.maven.packaging.*;
import com.isomorphic.maven.util.HttpRequestManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.ProjectBuildingRequest.RepositoryMerging;
import org.apache.maven.project.ProjectModelResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.isomorphic.maven.packaging.License.*;
import static com.isomorphic.maven.packaging.Product.REIFY_ONSITE;

/**
 * A base class meant to deal with prerequisites to install / deploy goals,
 * which are basically to resolve the files in a given distribution to a
 * collection of Maven artifacts suitable for installation or deployment to some
 * Maven repository.
 * <p>
 * The resulting artifacts are provided to this object's {@link #doExecute(Set)}
 * method.
 */
public abstract class AbstractPackagerMojo extends AbstractBaseMojo {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPackagerMojo.class);

    private static final HttpHost HOST = new HttpHost("www.smartclient.com", -1, "https");

    private HttpRequestManager httpWorker;

    // Note, @parameter definitions and docs for these properties factored down to InstallMojo
    // and DeployMojo because they are not applicable to everything that extends this base
    // class, and there does not appear to be a way to suppress or hide the docs per-subclass
    // The variables were also renamed to prevent any possibility that they would be
    // auto-mapped by Maven
    protected Boolean incAnalytics = false;
    protected Boolean incAI = false;
    protected Boolean incMessaging = false;

    /**
     * The date on which the Isomorphic build was made publicly available at <a
     * href
     * ="http://www.smartclient.com/builds/">http://www.smartclient.com/builds
     * /</a>, in yyyy-MM-dd format. e.g., 2025-10-13. Used to determine both
     * remote and local file locations.
     * <br>
     * Note that if no value is provided, and {@link #skipDownload} is not true, an attempt is
     * made to discover the date of the latest distribution currently published to the
     * Isomorphic build server.
     * <br>
     * <b>Default value is</b>: <code>The date of the most recent distribution (with caveats)</code>.
     *
     * @since 1.0.0
     */
    @Parameter(property = "buildDate")
    protected String buildDate;

    /**
     * The Isomorphic version number of the specified {@link #product}. e.g.,
     * 13.1p, 15.0d. Used to determine both remote and local file locations.
     *
     * @since 1.0.0
     */
    @Parameter(property = "buildNumber", required = true)
    protected String buildNumber;

    protected License license;
    protected String skins;

    /**
     * If true, any file previously downloaded / unpacked will be overwritten
     * with this execution. Useful in the case of an interrupted download. Note
     * that this setting has no effect on unzip operations.
     *
     * @since 1.0.0
     */
    @Parameter(property = "overwrite", defaultValue = "false")
    protected Boolean overwrite;

    protected Boolean copyToLatest = false;

    /**
     * If true, no attempt is made to download any remote distribution. Files
     * will be loaded instead from a path constructed of the following parts
     * (e.g., C:/downloads/SmartGWT/PowerEdition/4.1d/2013-12-25/zip):
     *
     * <ul>
     * <li>{@link #workdir}</li>
     * <li>{@link #product}</li>
     * <li>{@link #license}</li>
     * <li>{@link #buildNumber}</li>
     * <li>{@link #buildDate}</li>
     * <li>"zip"</li>
     * </ul>
     *
     * @since 1.0.0
     */
    @Parameter(property = "skipDownload", defaultValue = "false")
    protected Boolean skipDownload;

    protected Boolean skipExtract = false;
    protected Product product;
    protected Boolean snapshots = false;

    /**
     * The path to some directory that is to be used for storing downloaded
     * files, working copies, and so on.
     *
     * @since 1.0.0
     */
    @Parameter(property = "workdir", defaultValue = "${java.io.tmpdir}/${project.artifactId}")
    protected File workdir;

    /**
     * The id of a <a
     * href="http://maven.apache.org/settings.html#Servers">server
     * configuration</a> containing authentication credentials for the
     * smartclient.com website, used to download licensed products.
     * <p>
     * Not strictly necessary for unprotected (LGPL) distributions.
     *
     * @since 1.0.0
     */
    @Parameter(property = "serverId", defaultValue = "smartclient-developer")
    protected String serverId;

    /**
     * The point where a subclass is able to manipulate the collection of
     * artifacts prepared for it by this object's {@link #execute()} method.
     *
     * @param artifacts
     *            A collection of Maven artifacts resulting from the download
     *            and preparation of a supported Isomorphic SDK.
     * @throws MojoExecutionException
     *             When any fatal error occurs. e.g., there is no distribution
     *             to work with.
     * @throws MojoFailureException
     *             When any non-fatal error occurs. e.g., documentation cannot
     *             be copied to some other folder.
     */
    public abstract void doExecute(Set<Module> artifacts) throws MojoExecutionException,
        MojoFailureException;

    public void doExecute(File basedir) throws MojoExecutionException, MojoFailureException {
        throw new MojoExecutionException("The doExecute() override that accepts a File object is not supported for " +
                                            "goal " + getClass().getAnnotation(Mojo.class).name());
    }

    /**
     * Provides some initialization and validation steps around the collection
     * and transformation of an Isomorphic SDK.
     *
     * @throws MojoExecutionException
     *             When any fatal error occurs.
     * @throws MojoFailureException
     *             When any non-fatal error occurs.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {

        // allow execution to proceed without login credentials - it may be that
        // they're not required
        UsernamePasswordCredentials credentials = getCredentials(serverId);
        if (credentials == null) {
            LOGGER.warn("No server configured with id '{}'.  Will be unable to authenticate.", serverId);
        }

        String buildNumberFormat = "\\d.*\\.\\d.*[d|p]";
        if (!buildNumber.matches(buildNumberFormat)) {
            throw new MojoExecutionException(String.format(
                "buildNumber '%s' must take the form [major].[minor].[d|p].  e.g., 4.1d",
                buildNumber, buildNumberFormat));
        }

        httpWorker = new HttpRequestManager(HOST, credentials, settings.getActiveProxy());
        Downloads dl = new Downloads(httpWorker);

        try {
            if (! skipDownload) {
                httpWorker.login();
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            dateFormat.setLenient(false);
            if (buildDate == null) {
                if (skipDownload) {
                    throw new MojoExecutionException("A buildDate value is required when the skipDownload parameter is true.");
                }

                Distribution d = Distribution.get(product, license);

                LOGGER.info("No buildDate provided.  Contacting Isomorphic build server to "
                    + "look for the most recent distribution...");

                String link = dl.findCurrentBuild(d, buildNumber);

                if (link == null) {
                    throw new MojoExecutionException("No build found for the given distribution (" +
                                    product.getName() + "/" + license.getName() + ")");
                }

                LOGGER.debug("Extracting date from server response: '{}'", link);

                buildDate = StringUtils.substringAfterLast(link, "/");

                LOGGER.info("buildDate set to '{}'", buildDate);

            }

            dateFormat.parse(buildDate);

            File basedir = FileUtils.getFile(workdir, product.toString(), license.toString(),
                buildNumber, buildDate);

            // add optional modules to the list of downloads
            List<License> licenses = new ArrayList<License>();
            licenses.add(license);
            if (license == POWER || license == ENTERPRISE) {
                if (incAnalytics) {
                    licenses.add(ANALYTICS_MODULE);
                }
                if (incMessaging) {
                    licenses.add(MESSAGING_MODULE);
                }
                if (incAI) {
                    licenses.add(AI_MODULE);
                }
            }

            // collect the maven artifacts and send them along to the abstract method
            Set<Module> artifacts = collect(licenses, basedir);

            // Although this is a Maven plugin, the install-reify-onsite and
            // upgrade-reify-onsite targets do not deal with Maven assets - they are deployment
            // tasks, and they copy files to the target directory structure, even if they are
            // files that would be managed by Maven in a Maven project (eg, JAR files).  So we
            // can just skip straight to the  Mojo's execute() method for REIFY_ONSITE targets

            if (product != REIFY_ONSITE) {
                String[] executables = {"bat", "sh", "command"};
                Collection<File> scripts = FileUtils.listFiles(basedir, executables, true);

                if (copyToLatest) {
                    File bookmarkable = new File(basedir.getParent(), "latest");
                    LOGGER.info("Copying distribution to '{}'", bookmarkable.getAbsolutePath());
                    try {
                        FileUtils.forceMkdir(bookmarkable);
                        FileUtils.cleanDirectory(bookmarkable);
                        FileUtils.copyDirectory(basedir, bookmarkable,
                                FileFilterUtils.notFileFilter(FileFilterUtils.nameFileFilter("zip")));

                        scripts.addAll(FileUtils.listFiles(bookmarkable, executables, true));

                    } catch (IOException e) {
                        throw new MojoFailureException("Unable to copy distribution contents", e);
                    }
                }

                for (File script : scripts) {
                    script.setExecutable(true);
                    LOGGER.debug("Enabled execute permissions on file '{}'", script.getAbsolutePath());
                }
                doExecute(artifacts);
            } else {
                doExecute(basedir);
            }

        } catch (ParseException e) {
            throw new MojoExecutionException(String.format(
                "buildDate '%s' must take the form yyyy-MM-dd.", buildDate));
        } finally {
            httpWorker.logout();
        }

    }

    /**
     * Download the specified distributions, if necessary, extract resources
     * from them, and use the results to create Maven artifacts as appropriate:
     * <p>
     * Try to install all of the main artifacts - e.g., those found in lib/*.jar
     * and assembly/*.zip
     * <br>
     * Try to match main artifacts to 'subartifacts' by name and attach them
     * (with classifiers as necessary)
     *
     * @param downloads
     *            The list of licenses top be included in the distribution.
     *            Multiple licenses are only allowed to support the inclusion of
     *            optional modules.
     * @param basedir
     *            The directory into which results should be written
     * @return A collection of Maven artifacts resulting from the download and
     *         preparation of a supported Isomorphic SDK.
     * @throws MojoExecutionException
     *             When any fatal error occurs.
     */
    private Set<Module> collect(List<License> downloads, File basedir)
        throws MojoExecutionException {

        File downloadTo = new File(basedir, "zip");
        downloadTo.mkdirs();

        Downloads downloadManager = new Downloads(httpWorker);
        downloadManager.setToFolder(downloadTo);
        downloadManager.setOverwriteExistingFiles(overwrite);

        File[] existing = downloadTo.listFiles();
        List<Distribution> distributions = new ArrayList<Distribution>();
        try {
            if (!skipDownload) {
                distributions.addAll(downloadManager.fetch(product, buildNumber, buildDate,
                    downloads.toArray(new License[0])));
            } else if (existing != null) {
                LOGGER.info("Creating local distribution from '{}'",
                    downloadTo.getAbsolutePath());
                Distribution distribution = Distribution.get(product, license);
                distribution.getFiles().addAll(Arrays.asList(existing));
                distributions.add(distribution);
            }

            if (!skipExtract) {
                LOGGER.info("Unpacking downloaded file/s to '{}'", basedir);
                for (Distribution distribution : distributions) {
                    distribution.unpack(basedir);
                    if (skins != null || product == REIFY_ONSITE) {
                        skin(basedir, distribution.getSkinResources());
                    }
                }
            }

            if (product == REIFY_ONSITE) {
                return null;
            }

            // TODO it'd be better if this didn't have to know where the files were located after unpacking

            // it doesn't strictly read this way, but we're looking for
            // lib/*.jar, pom/*.xml, assembly/*.zip
            Collection<File> files = FileUtils.listFiles(
                basedir,
                FileFilterUtils.or(FileFilterUtils.suffixFileFilter("jar"),
                    FileFilterUtils.suffixFileFilter("xml"),
                    FileFilterUtils.suffixFileFilter("zip")),
                FileFilterUtils.or(FileFilterUtils.nameFileFilter("lib"),
                    FileFilterUtils.nameFileFilter("pom"),
                    FileFilterUtils.nameFileFilter("assembly"))
            );

            if (files.isEmpty()) {
                throw new MojoExecutionException(String.format("There don't appear to be any files to work with at '%s'.  Check earlier log entries for clues.", basedir.getAbsolutePath()));
            }

            Set<Module> result = new TreeSet<Module>();
            for (File file : files) {
                try {

                    String base = FilenameUtils.getBaseName(file.getName().replaceAll("_", "-"));

                    // poms don't need anything else
                    if ("xml".equals(FilenameUtils.getExtension(file.getName()))) {
                        result.add(new Module(getModelFromFile(file)));
                        continue;
                    }

                    // for each jar/zip, find the matching pom
                    IOFileFilter filter = new WildcardFileFilter(base + ".pom", IOCase.INSENSITIVE);
                    Collection<File> poms = FileUtils.listFiles(basedir, filter,
                        TrueFileFilter.INSTANCE);
                    if (poms.size() != 1) {
                        LOGGER.warn("Expected to find exactly 1 POM matching artifact with name '{}', but found {}.  Skpping installation.",base, poms.size());
                        continue;
                    }

                    Model model = getModelFromFile(poms.iterator().next());
                    Module module = new Module(model, file);

                    /*
                     * Find the right javadoc bundle, matched on prefix. e.g.,
                     * smartgwt-eval -> smartgwt-javadoc isomorphic-core-rpc ->
                     * isomorphic-javadoc and add it to the main artifact with
                     * the javadoc classifier. This seems appropriate as long as
                     * a) there is no per-jar javadoc b) naming conventions are
                     * adhered to (or can be corrected by plugin at extraction)
                     */
                    int index = base.indexOf("-");
                    String prefix = base.substring(0, index);

                    Collection<File> doc = FileUtils.listFiles(new File(basedir, "doc"),
                        FileFilterUtils.prefixFileFilter(prefix),
                        FileFilterUtils.nameFileFilter("lib"));

                    if (doc.size() != 1) {
                        LOGGER.debug("Found {} javadoc attachments with prefix '{}'.  Skipping attachment.", doc.size(), prefix);
                    } else {
                        module.attach(doc.iterator().next(), "javadoc");
                    }

                    result.add(module);
                } catch (ModelBuildingException e) {
                    throw new MojoExecutionException("Error building model from POM", e);
                }
            }
            return result;
        } catch (IOException e) {
            throw new MojoExecutionException("Failure during assembly collection", e);
        }
    }

    /**f
     * Read the given POM so it can be used as the source of coordinates, etc.
     * during artifact construction. Note that if this object's
     * {@link #snapshots} property is true, and we're working with a development
     * build ({@link #buildNumber} ends with 'd'), the POM is modified to remove
     * the SNAPSHOT qualifier.
     *
     * @param pom
     *            the POM file containing the artifact metadata
     * @return A Maven model to be used at
     *         {@link com.isomorphic.maven.packaging.Module#Module(Model)}
     *         Module construction
     * @throws ModelBuildingException
     *             if the Model cannot be built from the given POM
     * @throws IOException
     *             if the Model cannot be built from the given POM
     */
    private Model getModelFromFile(File pom) throws ModelBuildingException, IOException {

        if (buildNumber.endsWith("d") && !snapshots) {
            LOGGER.info(
                "Rewriting file to remove SNAPSHOT qualifier from development POM '{}'",
                pom.getName());
            String content = FileUtils.readFileToString(pom);
            content = content.replaceAll("-SNAPSHOT", "");
            FileUtils.write(pom, content);
        }

        ProjectModelResolver resolver = new ProjectModelResolver(repositorySystemSession, null, repositorySystem, remoteRepositoryManager, project.getRemoteProjectRepositories(), RepositoryMerging.POM_DOMINANT, null);
        ModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelResolver(resolver);
        request.setPomFile(pom);

        Model model = modelBuilder.buildRawModel(pom, 0, false).get();
        Parent parent = model.getParent();
        if (parent != null) {
                model.setGroupId(parent.getGroupId());
                model.setVersion(parent.getVersion());
        }

        return model;
    }

    protected void skin(File basedir, Map<String, String> skinResources) throws MojoExecutionException, IOException {

        LOGGER.info("Pruning unwanted skins...");

        Map<String, String> props = new HashMap<>();
        props.put("create", "false");

        List<String> requested = new ArrayList<>(Splitter.on(",").trimResults().splitToList(skins.toLowerCase()));

        // preserve for simplicity
        requested.add("enterprise");
        requested.add("fonts");
        requested.add("toolskin");
        requested.add("toolskinnative");

        Set<String> keys = skinResources.keySet();
        for(String key : keys) {
            String archive = basedir.getCanonicalPath() + "/" + key;
            String skinDir = skinResources.get(key);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Opening file at '{}' for modification", archive);
            }
            URI uri = URI.create("jar:file:" + archive);
            try (FileSystem fs = FileSystems.newFileSystem(uri, props)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(fs.getPath(skinDir), Files::isDirectory)) {
                    for (Path path : ds) {
                        String skin = path.getFileName().toString().toLowerCase().replace("/", "");

                        if (requested.contains(skin)) {
                            LOGGER.debug("Preserving '{}' skin", skin);
                            continue;
                        }
                        LOGGER.info("Deleting '{}' skin resources at '{}'", skin, path);
                        FileSystemUtils.deleteRecursively(path);
                    }
                } catch (NotDirectoryException ignore) {}
            }
        }
    }
}
