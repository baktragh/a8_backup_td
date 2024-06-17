package udman;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;


public class Extractor {

    private final ExtractorConfig ec;
    
    public Extractor(ExtractorConfig ec) {
        this.ec = ec;
    }

    int extract() throws Exception {
        
        /*For all proxies*/
        
        int counter=0;
        
        for(FileProxy oneProxy:ec.fileProxies) {
            
            /*Prepare normalized file name*/
            StringBuilder sb = new StringBuilder();
            for(int oneChar:oneProxy.getNameChars()) {
                sb.append((char)oneChar);
            }
            
            String originalName = sb.toString().trim();
            String normalizedName = normalizeName(originalName);
            
            /*Determine extension*/
            String fullExtension;
            String prefixExtension;
            String finalExtension;
            
            int fileType = oneProxy.getType();
            if (fileType==0x03 || fileType==0x04 || ec.forceBinary) {
                fullExtension=".xex";
                prefixExtension=".x";
            }
            else if (fileType==0xFF || fileType==0xFE) {
                fullExtension=".bas";
                prefixExtension=".b";
            }
            else  {
                fullExtension=".dat";
                prefixExtension=".d";
            }
            
            if (!ec.longNames && ec.sequentialNames)  {
                finalExtension=prefixExtension+String.format("%02X",counter);
            }
            else {
                finalExtension=fullExtension;
            }
            
            /*Determine flat file name*/
            int maxNameLength=ec.longNames?10:8;
            String shortenedName = normalizedName.substring(0,Math.min(maxNameLength, normalizedName.length()));
            
            /*Determine final full name*/
            
            String finalName;
            
            if (ec.sequentialNames) {
                if (ec.longNames) {
                    finalName=String.format("%03d",counter)+"_"+shortenedName+finalExtension;
                }
                else {
                    finalName=(shortenedName+finalExtension).toUpperCase();
                    
                }
            }
            else {
                
                finalName=shortenedName+finalExtension;
                if (!ec.longNames) {
                    finalName=finalName.toUpperCase();
                }
            }
            
            if (ec.toBinary) {
                exportBinary(oneProxy,finalName,fileType);
            }
            
            if (ec.toCas) {
                String casExtension;
                String casName;
                
                if (!ec.longNames && ec.sequentialNames) {
                    casExtension=String.format(".c%02X",counter);
                }
                else {
                    casExtension=".cas";
                }
                
                if (ec.sequentialNames) {
                    if (ec.longNames) {
                    casName=String.format("%03d",counter)+"_"+shortenedName+casExtension; 
                    }
                    else {
                        casName=(shortenedName+casExtension).toUpperCase();
                    }
                }
                else {
                    casName=shortenedName+casExtension;
                    if (!ec.longNames) {
                        casName=casName.toUpperCase();
                    }
                }
                
                exportToCas(oneProxy,casName);
            }
            counter++;
            
        }
        
        
        return counter;
    }
    
    private String normalizeName(String originalName) {
       StringBuilder sb = new StringBuilder();
       char[] origChars = originalName.toCharArray();
       
       for(char oneChar:origChars) {
           
           /*Remove inverse video*/
           if ((oneChar & 0x80)==0x80) {
               oneChar =(char)(oneChar & 0x7F);
           }
           
           /*Whitespace is converted to undescores*/
           if (Character.isWhitespace(oneChar)) {
               sb.append('_');
           }
           /*Letters and digits are intact*/
           else if (Character.isLetterOrDigit(oneChar)) {
               sb.append(oneChar);
           }
           /*Unknown characters are transformed to underscores*/
           else {
               sb.append("_");
           }
       }
       
       return sb.toString();
    }

    private void exportBinary(FileProxy oneProxy, String finalName,int fileType) throws Exception {
        
        try (FileOutputStream fos = new FileOutputStream(ec.outputDirectory+System.getProperty("file.separator")+finalName); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            
            boolean doHeader = (ec.forceBinary || (fileType==0x03));
            
            if (doHeader) {
                bos.write(0xFF);
                bos.write(0xFF);
                bos.write(oneProxy.getLoad()%256);
                bos.write(oneProxy.getLoad()/256);
                bos.write((oneProxy.getLoad() + oneProxy.getLength()-1)%256);
                bos.write((oneProxy.getLoad() + oneProxy.getLength()-1)/256);
            }
            
            for (int oneByte:oneProxy.getFileData()) {
                bos.write(oneByte);
            }
            
            if (doHeader) {
                bos.write(0xE0);
                bos.write(0x02);
                bos.write(0xE1);
                bos.write(0x02);
                bos.write(oneProxy.getRun()%256);
                bos.write(oneProxy.getRun()/256);
            }
            
        }
        
    }

    private void exportToCas(FileProxy oneProxy, String casName) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
    public static class ExtractorConfig {
        boolean toBinary;
        boolean toCas;
        boolean forceBinary;
        boolean longNames;
        boolean sequentialNames;
        String outputDirectory;
        ArrayList<FileProxy> fileProxies;

        public ExtractorConfig(boolean toBinary, boolean toCas, boolean forceBinary, boolean longNames, boolean sequentialNames, String outputDirectory, ArrayList<FileProxy> fileProxies) {
            this.toBinary = toBinary;
            this.toCas = toCas;
            this.forceBinary = forceBinary;
            this.longNames = longNames;
            this.sequentialNames = sequentialNames;
            this.outputDirectory = outputDirectory;
            this.fileProxies = fileProxies;
        }
        
        
        
    }

}
