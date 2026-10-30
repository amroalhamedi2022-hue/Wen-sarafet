package app.wensarafet.tracker;

/**
 * Publish a verified encrypted Drive backup without ever truncating the last
 * known-good document. SAF providers do not promise atomic replace/rename:
 * retain the old backup under a unique recovery name before publishing.
 *
 * The store abstraction keeps failure/recovery behavior JVM-testable without
 * Android's DocumentsProvider. Recovery files are intentionally NOT pruned:
 * cloud providers can report a successful rename before remote sync finishes.
 */
final class DriveBackupPublication {
    static final String LATEST_NAME = "Wen-Sarafet-latest.wsb";
    static final String PENDING_PREFIX = "Wen-Sarafet-pending-";
    static final String PREVIOUS_PREFIX = "Wen-Sarafet-previous-";

    interface Store<T> {
        T find(String name) throws Exception;
        T create(String name) throws Exception;
        void write(T document) throws Exception;
        void verify(T document) throws Exception;
        T rename(T document, String newName) throws Exception;
        String name(T document) throws Exception;
        void delete(T document) throws Exception;
    }

    private DriveBackupPublication() {}

    static <T> void publish(Store<T> store, String token) throws Exception {
        if (token == null || !token.matches("[A-Za-z0-9-]{8,64}"))
            throw new IllegalArgumentException("Invalid backup document token");
        String pendingName = PENDING_PREFIX + token + ".wsb";
        T pending = store.create(pendingName);
        if (pending == null) throw new IllegalStateException("Unable to create pending backup");
        boolean verified = false;
        String previousName = null;
        try {
            if (!pendingName.equals(store.name(pending)))
                throw new IllegalStateException("Provider changed pending backup filename");
            store.write(pending);
            // Read back and authenticate the entire ciphertext BEFORE touching
            // the old latest. A successful OutputStream.close is insufficient.
            store.verify(pending);
            verified = true;

            T current = store.find(LATEST_NAME);
            if (current != null) {
                previousName = PREVIOUS_PREFIX + token + ".wsb";
                T previous = store.rename(current, previousName);
                if (previous == null || !previousName.equals(store.name(previous)))
                    throw new IllegalStateException("Unable to preserve previous Drive backup");
            }
            T published = store.rename(pending, LATEST_NAME);
            if (published == null || !LATEST_NAME.equals(store.name(published))
                    || store.find(LATEST_NAME) == null)
                throw new IllegalStateException("Unable to publish verified Drive backup");
        } catch (Exception failure) {
            if (previousName != null) {
                try {
                    // If promotion did succeed despite a provider error, do not
                    // overwrite the newly published verified backup.
                    if (store.find(LATEST_NAME) == null) {
                        T previous = store.find(previousName);
                        if (previous == null)
                            throw new IllegalStateException("Previous backup recovery document missing");
                        T restored = store.rename(previous, LATEST_NAME);
                        if (restored == null || !LATEST_NAME.equals(store.name(restored)))
                            throw new IllegalStateException("Unable to restore previous Drive backup name");
                    }
                } catch (Exception rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
            }
            if (!verified) {
                // A partial or unauthenticated pending file is not a recovery
                // candidate. Never delete an authenticated pending document:
                // it may be the only new recoverable backup after rename failure.
                try { store.delete(pending); }
                catch (Exception cleanupFailure) { failure.addSuppressed(cleanupFailure); }
            }
            throw failure;
        }
    }
}
