package app.wensarafet.tracker;

import java.io.File;
import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Regression: millisecond-named restore files collided across concurrent calls. */
public final class BackupRestoreStagingTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        File directory = Files.createTempDirectory("wen-restore-staging-test").toFile();
        File live = new File(directory, "wen_sarafet.db");
        File preexisting = new File(directory, "wen_sarafet.pre_restore.123456.db");
        Files.writeString(preexisting.toPath(), "valuable rollback");
        final int threads = 12, perThread = 25;
        Set<String> stagePaths = ConcurrentHashMap.newKeySet();
        Set<String> rollbackPaths = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            @SuppressWarnings("unchecked")
            Future<?>[] jobs = new Future<?>[threads];
            for (int t = 0; t < threads; t++) {
                jobs[t] = pool.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < perThread; i++) {
                            File temp = BackupRestoreStaging.createTemporaryDatabase(directory);
                            File rollback = BackupRestoreStaging.newRollbackDestination(live);
                            check(temp.exists(), "Staging file must be exclusively reserved");
                            check(stagePaths.add(temp.getCanonicalPath()), "Staging filename collision");
                            check(rollbackPaths.add(rollback.getCanonicalPath()), "Rollback filename collision");
                            check(!rollback.exists(), "Must not overwrite an existing recovery file");
                            check(rollback.getName().startsWith("wen_sarafet.pre_restore."),
                                    "Recovery file naming contract changed");
                        }
                    } catch (Exception error) {
                        throw new RuntimeException(error);
                    }
                });
            }
            start.countDown();
            for (Future<?> job : jobs) job.get();
            check(stagePaths.size() == threads * perThread, "Lost staging reservations");
            check(rollbackPaths.size() == threads * perThread, "Lost rollback destinations");
            check("valuable rollback".equals(Files.readString(preexisting.toPath())),
                    "Existing recovery copy was modified");
            System.out.println("BackupRestoreStagingTest passed: concurrent uniqueness and old recovery preserved");
        } finally {
            pool.shutdownNow();
            for (String path : stagePaths) Files.deleteIfExists(new File(path).toPath());
            Files.deleteIfExists(preexisting.toPath());
            if (!directory.delete()) throw new AssertionError("Staging test left temporary files");
        }
    }
}
