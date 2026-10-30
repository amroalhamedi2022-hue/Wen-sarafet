package app.wensarafet.tracker;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DriveBackupPublicationTest {
    private static final class FakeStore implements DriveBackupPublication.Store<String> {
        final Map<String, String> names = new LinkedHashMap<>();
        int serial;
        boolean failWrite, failVerify;
        String failRenameTo;
        boolean failAfterRename;
        int failuresRemaining = 1;

        String seed(String name) {
            String id = "id-" + (++serial);
            names.put(id, name);
            return id;
        }
        @Override public String find(String name) {
            for (Map.Entry<String, String> e : names.entrySet())
                if (name.equals(e.getValue())) return e.getKey();
            return null;
        }
        @Override public String create(String name) { return seed(name); }
        @Override public void write(String id) {
            if (failWrite) throw new IllegalStateException("write failed");
        }
        @Override public void verify(String id) {
            if (failVerify) throw new IllegalStateException("verification failed");
        }
        @Override public String rename(String id, String newName) {
            boolean fail = newName.equals(failRenameTo) && failuresRemaining-- > 0;
            if (fail && !failAfterRename)
                throw new IllegalStateException("rename failed");
            names.put(id, newName);
            if (fail)
                throw new IllegalStateException("rename reported failure after effect");
            return id;
        }
        @Override public String name(String id) { return names.get(id); }
        @Override public void delete(String id) { names.remove(id); }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static void fails(FakeStore store, String token) {
        try { DriveBackupPublication.publish(store, token); }
        catch (Exception expected) { return; }
        throw new AssertionError("Expected failure");
    }

    public static void main(String[] args) throws Exception {
        String latest = DriveBackupPublication.LATEST_NAME;
        String previous = DriveBackupPublication.PREVIOUS_PREFIX + "12345678.wsb";
        String pending = DriveBackupPublication.PENDING_PREFIX + "12345678.wsb";

        FakeStore fresh = new FakeStore();
        DriveBackupPublication.publish(fresh, "12345678");
        check(fresh.find(latest) != null, "first upload must become latest");
        check(fresh.names.size() == 1, "first upload must not leave pending");

        FakeStore replacement = new FakeStore();
        String oldId = replacement.seed(latest);
        DriveBackupPublication.publish(replacement, "12345678");
        check(oldId.equals(replacement.find(previous)), "previous latest must be recoverable");
        check(replacement.find(latest) != null && !oldId.equals(replacement.find(latest)),
                "new verified archive must become latest");
        check(replacement.names.size() == 2, "no pending on success");

        FakeStore write = new FakeStore();
        String writeOld = write.seed(latest);
        write.failWrite = true;
        fails(write, "12345678");
        check(writeOld.equals(write.find(latest)), "failed write must preserve latest");
        check(write.find(pending) == null, "incomplete pending must be removed");

        FakeStore verify = new FakeStore();
        String verifyOld = verify.seed(latest);
        verify.failVerify = true;
        fails(verify, "12345678");
        check(verifyOld.equals(verify.find(latest)), "failed read-back must preserve latest");
        check(verify.find(pending) == null, "unauthenticated pending must be removed");

        FakeStore archiveRename = new FakeStore();
        String archiveOld = archiveRename.seed(latest);
        archiveRename.failRenameTo = previous;
        fails(archiveRename, "12345678");
        check(archiveOld.equals(archiveRename.find(latest)),
                "failed archive rename must leave latest untouched");
        check(archiveRename.find(pending) != null, "verified pending is recoverable");

        FakeStore publishRename = new FakeStore();
        String publishOld = publishRename.seed(latest);
        publishRename.failRenameTo = latest;
        fails(publishRename, "12345678");
        check(publishOld.equals(publishRename.find(latest)),
                "failed promotion must roll previous name back");
        check(publishRename.find(pending) != null, "verified pending must survive failed promotion");

        FakeStore renameThenThrow = new FakeStore();
        String renamedOld = renameThenThrow.seed(latest);
        renameThenThrow.failRenameTo = previous;
        renameThenThrow.failAfterRename = true;
        fails(renameThenThrow, "12345678");
        check(renamedOld.equals(renameThenThrow.find(latest)),
                "provider failure after archive rename must be rolled back");

        FakeStore promotionThenThrow = new FakeStore();
        String promotionOld = promotionThenThrow.seed(latest);
        promotionThenThrow.failRenameTo = latest;
        promotionThenThrow.failAfterRename = true;
        fails(promotionThenThrow, "12345678");
        check(promotionOld.equals(promotionThenThrow.find(previous)),
                "previous must survive ambiguous promotion result");
        check(promotionThenThrow.find(latest) != null,
                "never overwrite a verified latest on ambiguous provider error");

        FakeStore noOld = new FakeStore();
        noOld.failRenameTo = latest;
        fails(noOld, "12345678");
        check(noOld.find(pending) != null, "first verified backup survives failed publish");

        try { DriveBackupPublication.publish(new FakeStore(), "../unsafe"); }
        catch (IllegalArgumentException expected) {
            System.out.println("DriveBackupPublicationTest passed: safe rotation, failure recovery, no old-backup truncation");
            return;
        }
        throw new AssertionError("Unsafe token must be rejected");
    }
}
