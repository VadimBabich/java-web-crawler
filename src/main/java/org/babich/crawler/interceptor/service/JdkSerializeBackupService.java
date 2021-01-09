/*
 * @author Vadim Babich
 */
package org.babich.crawler.interceptor.service;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import org.babich.crawler.api.BackupService;
import org.babich.crawler.api.Page;
import org.babich.crawler.api.PageContext;
import org.babich.crawler.api.PageProcessingInterceptor;
import org.babich.crawler.api.messages.PageRecovered;
import org.babich.crawler.api.messages.CrawlerStopped;
import org.babich.crawler.event.LocalEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JDK serialization to backup and restore Crawler state in case of failure.
 * <br/>There is also a listener for a {@code ProcessingCompleted} message.
 * If the Crawler crashes, the {@code ProcessingCompleted#abnormal} flag will be set and then the backup process will
 * begin.
 * <p/>
 * The information is saved to disk as a file and the {@code BACKUP_PATH_PREFERENCE_KEY} preference will keep this file
 * path until the next time the scanner is run.
 * <p/>
 * The Crawler state is serialized as a set of pages, divided into two folders -
 * {@code BACKUP_PROCESSED_PAGES_FOLDER_NAME} contains processed pages
 * and BACKUP_FOUND_PAGES_FOLDER_NAME folder contains unprocessed pages.
 * <p/>This class is an implementation of the {@code PageProcessingInterceptor} contract
 * that is used to capture the pages that were processed prior to the failure.
 */
public class JdkSerializeBackupService implements PageProcessingInterceptor, BackupService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String BACKUP_PATH_PREFERENCE_KEY = "backupPath";
    private static final String BACKUP_PROCESSED_PAGES_FOLDER_NAME = "processed";
    private static final String BACKUP_FOUND_PAGES_FOLDER_NAME = "found";

    private final Preferences preferences = Preferences.userNodeForPackage(getClass());
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final Set<Page> processedPages = new HashSet<>();
    private final Set<Page> foundPages = new HashSet<>();


    private LocalEventBus eventBus;

    private JdkSerializeBackupService() {
    }

    public JdkSerializeBackupService(LocalEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void backup() {
        try {
            Path path = Paths.get(Files.createTempDirectory("backup").toString()
                    , LocalDateTime.now().format(dateTimeFormatter) + ".zip");

            URI uri = URI.create("jar:" + path.toUri());
            logger.info("Started backup to file {}.", path);

            try (FileSystem zipFs = FileSystems.newFileSystem(uri, Collections.singletonMap("create", "true"))) {
                Path processedPagesDir = Files.createDirectory(zipFs.getPath(BACKUP_PROCESSED_PAGES_FOLDER_NAME));
                processedPages.forEach(backupPageTo(processedPagesDir));

                Path foundPagesDir = Files.createDirectory(zipFs.getPath(BACKUP_FOUND_PAGES_FOLDER_NAME));
                Sets.difference(foundPages, processedPages).forEach(backupPageTo(foundPagesDir));
            }

            preferences.put(BACKUP_PATH_PREFERENCE_KEY, path.toString());

            logger.info("Backup successfully completed.");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public Collection<Page> restoreFor(PageContext pageContext) {
        final Set<Page> resultSetOfPages = new HashSet<>();

        if (isNotAvailableForRecovery()) {
            logger.debug("The crawler has no sources for recovery.");
            return Collections.emptySet();
        }

        URI uri = URI.create("jar:" + getBackupPath().toUri());
        logger.info("Recovering from source: {}.", getBackupPath());

        Consumer<Page> updateContext = updateContext(pageContext);

        try (FileSystem zipFs = FileSystems.newFileSystem(uri, Collections.singletonMap("create", "false"))) {

            readBackupFiles(zipFs.getPath(BACKUP_PROCESSED_PAGES_FOLDER_NAME), readPageAnd(updateContext
                    .andThen(this::sendProcessedPageMessage)
                    .andThen(processedPages::add)));

            readBackupFiles(zipFs.getPath(BACKUP_FOUND_PAGES_FOLDER_NAME), readPageAnd(updateContext
                    .andThen(this::sendFoundPageMessage)
                    .andThen(resultSetOfPages::add)));

        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }

        logger.info("Recovering successfully completed");
        return resultSetOfPages;
    }

    @Override
    public void afterProcessing(Page page, List<Page> successorPages) {
        processedPages.add(page);
        foundPages.addAll(successorPages);
    }

    @Subscribe
    public void onStopCrawler(CrawlerStopped message) {
        if (!message.isAbnormal()) {
            preferences.remove(BACKUP_PATH_PREFERENCE_KEY);
            return;
        }

        logger.info("Crawler {} stopped abnormally.", message.getPayload());
        try {
            backup();
        } catch (Exception exception) {
            logger.error("Cannot serialize processing state", exception);
        }
    }

    private boolean isNotAvailableForRecovery() {
        return null == getBackupPath();
    }

    private Path getBackupPath() {
        String backupPath;
        if (null == (backupPath = preferences.get(BACKUP_PATH_PREFERENCE_KEY, null))) {
            return null;
        }
        return Paths.get(backupPath);
    }

    private void readBackupFiles(Path dir, Consumer<Path> fileConsumer) {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir)) {
            files.forEach(fileConsumer);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private Consumer<Page> backupPageTo(Path dir) {
        return page -> {
            Path path = dir.resolve(page.getPageName());
            try (ObjectOutputStream objectOutputStream =
                    new ObjectOutputStream(Files.newOutputStream(path, WRITE, TRUNCATE_EXISTING, CREATE))) {
                objectOutputStream.writeObject(page);
                logger.debug("Backup page {} to the file {}", page, path);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        };
    }

    private Consumer<Path> readPageAnd(Consumer<Page> pageConsumer) {
        return path -> {
            try (ObjectInputStream objectInputStream = new ObjectInputStream(Files.newInputStream(path))) {
                Object o = objectInputStream.readObject();
                if (!(o instanceof Page)) {
                    logger.error("Backup object {} is not a class {}", o.getClass(), Page.class);
                    return;
                }

                Page page = (Page) o;
                logger.debug("Found page for recovering {}.", page);

                pageConsumer.accept(page);

            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            } catch (ClassNotFoundException notFoundException) {
                logger.error("Reading backup objects with error:", notFoundException);
            }
        };
    }

    private Consumer<Page> updateContext(PageContext pageContext) {
        return page -> {
            PageContext context = page.getPageContextRef().get();
            page.getPageContextRef().set(context
                    .transform(builder -> builder.pageProcessing(pageContext.getPageProcessing())));
        };
    }

    private void sendProcessedPageMessage(Page page){
        eventBus.post(new PageRecovered(page, true));
    }

    private void sendFoundPageMessage(Page page){
        eventBus.post(new PageRecovered(page));
    }
}
