package com.gathin.tvhome;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Small file cache. Text entries store their own timestamp so a ttl check does not
 * depend on the filesystem clock. Image files use the timestamp passed to {@link #putBytes}.
 */
final class DiskCache {
    private final File textDir;
    private final File byteDir;

    DiskCache(File root) {
        textDir = new File(root, "text");
        byteDir = new File(root, "img");
    }

    synchronized void putText(String key, String body, long now) {
        if (key == null || body == null) return;
        try {
            JSONObject o = new JSONObject();
            o.put("at", now);
            o.put("body", body);
            write(textFile(key), o.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }

    synchronized String getText(String key, long now, long ttl) {
        Entry entry = readText(key);
        if (entry == null || !CacheTtl.fresh(entry.at, now, ttl)) return null;
        return entry.body;
    }

    /** Last stored body, including after the ttl. Null when nothing was stored. */
    synchronized String getTextAnyAge(String key) {
        Entry entry = readText(key);
        return entry == null ? null : entry.body;
    }

    synchronized void putBytes(String key, byte[] data, long now) {
        if (key == null || data == null || data.length == 0) return;
        File file = byteFile(key);
        write(file, data);
        if (now > 0) file.setLastModified(now);
    }

    synchronized byte[] getBytes(String key, long now, long ttl) {
        File file = byteFile(key);
        if (!file.isFile() || file.length() == 0) return null;
        if (!CacheTtl.fresh(file.lastModified(), now, ttl)) {
            file.delete();
            return null;
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            return null;
        }
    }

    /** Deletes the oldest image files until the directory is within {@code cap} bytes. */
    synchronized void trimBytes(long cap) {
        if (cap < 0) cap = 0;
        File[] files = byteDir.listFiles();
        if (files == null || files.length == 0) return;
        ArrayList<File> list = new ArrayList<>();
        long total = 0;
        for (File file : files) {
            if (!file.isFile()) continue;
            list.add(file);
            total += file.length();
        }
        list.sort(Comparator.comparingLong(File::lastModified));
        int i = 0;
        while (total > cap && i < list.size()) {
            File file = list.get(i++);
            long size = file.length();
            if (file.delete()) total -= size;
        }
    }

    private Entry readText(String key) {
        File file = textFile(key);
        if (!file.isFile()) return null;
        try {
            JSONObject o = new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            String body = o.optString("body", "");
            if (body.isEmpty()) return null;
            return new Entry(o.optLong("at", 0), body);
        } catch (Exception e) {
            return null;
        }
    }

    private File textFile(String key) {
        if (!textDir.isDirectory()) textDir.mkdirs();
        return new File(textDir, safe(key));
    }

    private File byteFile(String key) {
        if (!byteDir.isDirectory()) byteDir.mkdirs();
        return new File(byteDir, safe(key));
    }

    private static void write(File file, byte[] data) {
        File dir = file.getParentFile();
        if (dir != null && !dir.isDirectory()) dir.mkdirs();
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(data);
            out.getFD().sync();
        } catch (Exception e) {
            tmp.delete();
            return;
        }
        if (file.exists() && !file.delete()) {
            tmp.delete();
            return;
        }
        if (!tmp.renameTo(file)) tmp.delete();
    }

    private static String safe(String key) {
        String s = key.replaceAll("[^A-Za-z0-9._-]", "_");
        if (s.isEmpty()) s = "entry";
        if (s.length() > 80) s = s.substring(0, 80);
        return s;
    }

    private static final class Entry {
        final long at;
        final String body;

        Entry(long at, String body) {
            this.at = at;
            this.body = body;
        }
    }
}
