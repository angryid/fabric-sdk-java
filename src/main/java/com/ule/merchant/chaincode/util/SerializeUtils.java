package com.ule.merchant.chaincode.util;

import com.ule.merchant.chaincode.model.ChainCodeUser;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.hyperledger.fabric.sdk.helper.Utils;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

public class SerializeUtils {

    //序列化用户
    public static void serializeUser(String name, ChainCodeUser user) throws IOException {
        new ObjectOutputStream(Files.newOutputStream(Paths.get(".temp/" + name + ".hfuser"))).writeObject(user);
    }

    //尝试反序列化
    public static ChainCodeUser tryDeserialize(String name) throws Exception {
        return !Files.exists(Paths.get(".temp/" + name + ".hfuser")) ? null :
                (ChainCodeUser) new ObjectInputStream(Files.newInputStream(Paths.get(".temp/" + name + ".hfuser"))).readObject();
    }

    /**
     * Compress the contents of given directory using Tar and Gzip to an in-memory byte array.
     *
     * @param sourceDirectory  the source directory.
     * @param pathPrefix       a path to be prepended to every file name in the .tar.gz output, or {@code null} if no prefix is required.
     * @param chaincodeMetaInf
     * @return the compressed directory contents.
     * @throws IOException
     */
    public static byte[] generateTarGz(File sourceDirectory, String pathPrefix, File chaincodeMetaInf) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(500000);

        String sourcePath = sourceDirectory.getAbsolutePath();

        TarArchiveOutputStream archiveOutputStream = new TarArchiveOutputStream(new GzipCompressorOutputStream(bos));
        archiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        try {
            Collection<File> childrenFiles = org.apache.commons.io.FileUtils.listFiles(sourceDirectory, null, true);

            ArchiveEntry archiveEntry;
            FileInputStream fileInputStream;
            for (File childFile : childrenFiles) {
                String childPath = childFile.getAbsolutePath();
                String relativePath = childPath.substring((sourcePath.length() + 1), childPath.length());

                if (pathPrefix != null) {
                    relativePath = Utils.combinePaths(pathPrefix, relativePath);
                }

                relativePath = FilenameUtils.separatorsToUnix(relativePath);

                archiveEntry = new TarArchiveEntry(childFile, relativePath);
                fileInputStream = new FileInputStream(childFile);
                archiveOutputStream.putArchiveEntry(archiveEntry);

                try {
                    IOUtils.copy(fileInputStream, archiveOutputStream);
                } finally {
                    fileInputStream.close();
                    archiveOutputStream.closeArchiveEntry();
                }

            }

            if (null != chaincodeMetaInf) {
                childrenFiles = org.apache.commons.io.FileUtils.listFiles(chaincodeMetaInf, null, true);

                final URI metabase = chaincodeMetaInf.toURI();

                for (File childFile : childrenFiles) {

                    final String relativePath = Paths.get("META-INF", metabase.relativize(childFile.toURI()).getPath()).toString();

                    archiveEntry = new TarArchiveEntry(childFile, relativePath);
                    fileInputStream = new FileInputStream(childFile);
                    archiveOutputStream.putArchiveEntry(archiveEntry);

                    try {
                        IOUtils.copy(fileInputStream, archiveOutputStream);
                    } finally {
                        fileInputStream.close();
                        archiveOutputStream.closeArchiveEntry();
                    }

                }

            }
        } finally {
            archiveOutputStream.close();
        }
        return bos.toByteArray();
    }
}
