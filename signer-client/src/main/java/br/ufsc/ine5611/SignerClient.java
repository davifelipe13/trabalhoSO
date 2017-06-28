package br.ufsc.ine5611;

import org.apache.commons.io.output.CloseShieldOutputStream;

import java.io.*;
import static java.lang.ProcessBuilder.Redirect.Type.READ;
import static java.lang.ProcessBuilder.Redirect.Type.WRITE;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import static java.nio.file.Files.size;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class SignerClient {
    private Scanner scanner;
    private PrintStream ps;

    public SignerClient(OutputStream outputStream, InputStream inputStream) {
        scanner = new Scanner(inputStream);
        try {
            ps = new PrintStream(new CloseShieldOutputStream(outputStream), true, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void sign(File mappedFile) throws SignerException {
        ps.printf("SIGN %s\n", mappedFile.getAbsolutePath());
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.equals("OK SIGN"))
                break;
            if (line.equals("ERROR BEGIN SIGN")) {
                StringBuilder errorMessage = new StringBuilder();
                while (scanner.hasNextLine()) {
                    String errorLine = scanner.nextLine();
                    if (errorLine.equals("ERROR END SIGN"))
                        break;
                    errorMessage.append(errorLine).append("\n");
                }
                throw new SignerException(errorMessage.toString());
            }
        }
        
    }

    public void end() {
        ps.printf("END\n");
    }
    
    private static byte[] getExpectedSignature(File file) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
        try (FileInputStream in = new FileInputStream(file)){
            while(in.available() > 0)
                md.update((byte) in.read());
        }
        return md.digest();
    }
    
    public void process() {
        FileChannel ch = FileChannel.open(path, READ, WRITE);
        MappedByteBuffer mb = ch.map(READ_WRITE, 0, size);
        mb.put((byte)127);
        mb.position(666);
        int value = mb.getInt();


    }
}
