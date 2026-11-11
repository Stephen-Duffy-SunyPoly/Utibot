package uti.bot;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtil {
    static byte[] downloadContent(String url){
        try {
            URL uri = new URI(url).toURL();
            URLConnection con = uri.openConnection();
            if(con instanceof HttpURLConnection http){
               http.connect();
               int code = http.getResponseCode();
               if(code != 200){
                   throw new RuntimeException("Download returned non 200 code: "+code+" "+http);
               }
               InputStream dataIn = http.getInputStream();
               byte[] response = dataIn.readAllBytes();
               dataIn.close();
               return response;
            } else {
                throw new RuntimeException("Unknown connection created: "+con);
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void decompressZipBytesToDisc(byte[] zipBytes, String outputFolder) throws IOException {
        File destDir = new File(outputFolder);
        if (!destDir.exists()) {
            boolean ignored = destDir.mkdirs();
        }
        String targetDirPath = destDir.getCanonicalPath();
        // Wrap the raw byte array in an input stream
        try (ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes);
             ZipInputStream zis = new ZipInputStream(bais)) {

            ZipEntry entry;
            byte[] buffer = new byte[4096];

            // Iterate through each entry inside the ZIP archive
            while ((entry = zis.getNextEntry()) != null) {
                // Resolve entry destination path safely
                File newFile = new File(destDir, entry.getName());
                String newFilePath = newFile.getCanonicalPath();

                // Zip Slip Validation: Check if the entry targets outside the target directory
                if (!newFilePath.startsWith(targetDirPath + File.separator)) {
                    throw new IOException("Entry is outside of the target directory: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    boolean ignored = newFile.mkdirs();
                } else {
                    // Create parent directories if they don't exist yet
                    File parent = newFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        boolean ignored = parent.mkdirs();
                    }

                    // Write the uncompressed content to the target file
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }

    }

    public static void recursiveDelete(File toDelete){
        String[] files = toDelete.list();
        if(files!=null)
            for(String fName: files){
                File subFile = new File(toDelete, fName);
                if(subFile.isDirectory()){
                    recursiveDelete(subFile);
                } else {
                    boolean ignored = subFile.delete();
                }
            }
        boolean ignored = toDelete.delete();
    }

    public static void copyFolder(Path source, Path target) throws IOException {
        // Traverse the source directory structure
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(sourcePath -> {
                try {
                    // Resolve the target path for each file/folder relative to the root target
                    Path targetPath = target.resolve(source.relativize(sourcePath));

                    // Copy the file or directory
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy: " + sourcePath, e);
                }
            });
        } catch (RuntimeException e) {
            // Unpack and throw the actual IOException
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    public static void moveFilesUpDirectory(Path innerFolder) throws IOException {
        // The parent directory (one level up)
        Path parentFolder = innerFolder.getParent();

        if (parentFolder == null) {
            return;
        }

        // List immediate contents of the inner folder and move them
        try (Stream<Path> contents = Files.list(innerFolder)) {
            contents.forEach(source -> {
                // Construct target path: parent_folder + original file/folder name
                Path target = parentFolder.resolve(source.getFileName());
                try {
                    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Moved: " + source.getFileName());
                } catch (IOException e) {
                    System.err.println("Failed to move: " + source.getFileName() + " - " + e.getMessage());
                }
            });
        }
    }
}
