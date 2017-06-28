package br.ufsc.ine5611;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class App{
    
    public static void main(String[] args) throws InterruptedException {
        try {
            if(args.length == 2){
                Path originalPath = Paths.get(args[1]); //caminho do arquivo original
                Path tempPath = Files.createTempFile("teste", ".txt"); //caminho do arquivo temporário

                FileChannel originalFile = FileChannel.open(originalPath, new OpenOption[]{StandardOpenOption.READ, StandardOpenOption.WRITE}); //cria um FileChannel para acessar o arquivo original, e da permissão de leitura e escrita
                FileChannel tempFile = FileChannel.open(tempPath, new OpenOption[]{StandardOpenOption.READ, StandardOpenOption.WRITE}); //cria um FileChannel para acessar o arquivo temporario e da permissão de leitura e escrita

                MappedByteBuffer mapedTemp = tempFile.map(FileChannel.MapMode.READ_WRITE, 0, originalFile.size()+4+32);// cria uma região de memória compartilhada com o tamanho do arquivo em bytes + 4 bytes + 32bytes e mapeia o arquivo original para lá
                ByteBuffer originalBuffer = originalFile.map(FileChannel.MapMode.READ_WRITE, 0, originalFile.size());// 

                originalFile.read(originalBuffer); //le os bytes do arquivo original

                mapedTemp.putInt(0,(int)originalFile.size());

                originalBuffer.position(0);
                mapedTemp.position(4);
                while(originalBuffer.remaining()>0){
                    mapedTemp.put(originalBuffer.get());
                }

                ProcessBuilder builderPai = new ProcessBuilder().command(args[0]);
                Process pai = builderPai.start();

                byte[] signature2 = new byte[32];
                mapedTemp.position((int)originalFile.size()+4);
                for(int i = 0;i<signature2.length;i++){
                    signature2[i] = mapedTemp.get();
                }
                System.out.println("Sem assinatura\n"+Base64.getEncoder().encodeToString(signature2));
                
                OutputStreamWriter otw = new OutputStreamWriter(pai.getOutputStream());
                otw.write("SIGN "+ tempPath.toAbsolutePath()+"\n");
                otw.flush();

                pai.waitFor();

                byte[] signature = new byte[32];
                mapedTemp.position((int)originalFile.size()+4);
                for(int i = 0;i<signature.length;i++){
                    signature[i] = mapedTemp.get();
                }
                
                System.out.println("Criada\n"+Base64.getEncoder().encodeToString(signature));

                System.out.println("Esperada:\n"+Base64.getEncoder().encodeToString(getExpectedSignature(originalPath.toFile())));
            
            } else{
                throw new IOException("Argumentos invalidos");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    private static byte[] getExpectedSignature(File file){
        MessageDigest md = null;
        try{
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
        }
        
        try(FileInputStream in = new FileInputStream(file)){
            while(in.available()>0){
                md.update((byte) in.read());
            }
        } catch(IOException ex){
        }
        return md.digest();
    }
}
