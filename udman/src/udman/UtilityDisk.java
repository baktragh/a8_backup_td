package udman;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UtilityDisk {
    
    private int totalSectors;
    
    private int[] atrHeaderData;
    private int[] bootCodeData;
    private int[] idSectorData;
    private int[] pristineSectorData;
    
    private String idString;
    private boolean isPristine;
    private int dummySectorCount;
    
    private final List<FileProxy> fileProxies;
    private String fileSpec;
    private String fileName;
    
    
    
    
    public UtilityDisk(String filespec) throws IOException {
        
        fileProxies = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(filespec); BufferedInputStream bis = new BufferedInputStream(fis)) {
            
            readATRHeader(bis);
            readCode(bis);
            readIdentification(bis);
            if (!isPristine) readData(bis);
            
        }
        
        setFilespec(filespec);
        
    }
    
    final public void setFilespec(String filespec) {
        this.fileSpec=filespec;
        File f = new File(filespec);
        fileName=f.getName();
    }
    
    private void readData(BufferedInputStream bis) throws IOException {
        
        /*For all data*/
        while(true) {
            
            /*Check for 'E' or 'H' sector code*/
            int[] headerSector = readSector(bis);
            if (headerSector[0]!='E' && headerSector[0]!='H') {
                throw new IOException("Unexpected sector code found. Expected 'H' or 'E', but found "+String.format("$%02X",headerSector[0]) );
            }
            
            /*For 'E' sector, we are done*/
            if (headerSector[0]=='E') break;
            
            /*For 'H' sector, read the rest of the file*/
            int secId = bis.read();
            int lenLo = bis.read();
            int lenHi = bis.read();
            
            if (secId==-1 || lenLo==-1 || lenHi==-1) {
                throw new IOException("EOF Reached prematurely");
            }
            
            if (secId!='D') {
                throw new IOException("Unexpected sector code found. Expected 'D', but found "+String.format("$%02X",headerSector[secId]) );
            }
            
            int fileLen = lenLo+256*lenHi;
            int[] fileData = new int[fileLen];
            
            for (int i=0;i<fileLen;i++) {
                int oneByte = bis.read();
                if (oneByte==-1) {
                     throw new IOException("EOF Reached prematurely");
                }
                fileData[i]=oneByte;
            }
            
            /*Read the remainder of the last sector*/
            int remainder = 128-((fileLen+3) % 128);
            for (int i=0;i<remainder;i++) {
                int oneByte = bis.read();
                if (oneByte==-1) {
                     throw new IOException("EOF Reached prematurely");
                }
            }
            
            int[] nameChars = new int[10];
            for (int i=0;i<10;i++) {
                nameChars[i]=headerSector[3+1+i];
            }
            
            /*Now construct the FilePeoxy*/
            FileProxy fp = new FileProxy(
                    fileData,
                    headerSector[3+0],
                    headerSector[3+10+1]+256*headerSector[3+10+2],
                    headerSector[3+10+3]+256*headerSector[3+10+4],
                    headerSector[3+10+5]+256*headerSector[3+10+6],
                    nameChars
            );
            
            fileProxies.add(fp);
        }
        
    }
    
    
    private void readIdentification(BufferedInputStream bis) throws IOException {
    
        dummySectorCount=0;
        
        /*Read the ID sector and pristine sector*/
        while (true) {

            idSectorData = readSector(bis);
            if (idSectorData[0] == 0x00) {
                dummySectorCount++;
                continue;
            }

            /*Construct the version string*/
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < idSectorData.length; i++) {
                int oneByte = idSectorData[i];
                if (oneByte != 0) {
                    sb.append((char) oneByte);
                }
                else {
                    break;
                }
            }

            String s = sb.toString();
            if (!s.startsWith(("TURGEN BACKUP T/D"))) {
                throw new IOException("Identification string is missing. The disk image is not a BACKUP T/D utility disk. Found: " + s);
            }
            else {
                idString=s;
                break;
            }
        }
        
        /*Check the pristine sector*/
        pristineSectorData = readSector(bis);
        
        int emptyCount=0;
        int fullCount=0;
        
        for (int i=0;i<pristineSectorData.length;i++) {
            if (pristineSectorData[i]==0x55) emptyCount++;
            if (pristineSectorData[i]==0xC0) fullCount++;
        }
        
        if (!(((emptyCount==128 && fullCount==0) || (emptyCount==0 && fullCount==128)))) {
            throw new IOException("The pristine indication sector data is invalid. The disk image is not a BACKUP T/D utility disk");
        }
        
        /*Indicate pristine disk*/
        isPristine=(emptyCount==128);
    }
    
    
    private void readATRHeader(BufferedInputStream bis) throws IOException {
        
        /*Read all 16 header bytes*/
        atrHeaderData = new int[16];
        
        for (int i=0;i<atrHeaderData.length;i++) {
            atrHeaderData[i]=bis.read();
            if (atrHeaderData[i]==-1) {
                throw new IOException("EOF Reached prematurely. The file is not a valid ATR disk image");
            }
        }
        
        /*Check the ATR header*/
        if (atrHeaderData[0]!=0x96 && atrHeaderData[1]!=0x02) {
            throw new IOException("Invalid identification bytes. The file is not a valid ATR disk image.");
        }
        
        /*Check how big the sector is. It must be 128 bytes*/
        int sectorSize = atrHeaderData[4]+256*atrHeaderData[5];
        if (sectorSize!=128) {
            throw new IOException("Invalid sector size. The disk image is not a BACKUP T/D utility disk.");
        }
        
        /*Check how many sectors we have*/
        int sizeWordHi = atrHeaderData[6]+256*atrHeaderData[7];
        int sizeWordLo = atrHeaderData[2]+256*atrHeaderData[3];
        
        int sizeInBytes = (sizeWordHi*65536+sizeWordLo)*16;
        totalSectors = sizeInBytes/sectorSize;
        
        if (totalSectors<35) {
            throw new IOException("The disk image holds less than 35 sectors. The disk image is not a BACKUP T/D utility disk.");
        }
        
    }
    
    private void readCode(BufferedInputStream bis) throws IOException {
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            int flag = bis.read();
            int numSectors = bis.read();
            int loadLo = bis.read();
            int loadHi = bis.read();
            int initLo = bis.read();
            int initHi = bis.read();
            
            if (flag==-1 || numSectors==-1 || loadLo==-1 || loadHi ==-1 || initLo==-1 || initHi==-1) {
                throw new IOException("EOF Reached prematurely. The disk image is corrupted"); 
            }
            
            baos.write(flag);
            baos.write(numSectors);
            baos.write(loadLo);
            baos.write(loadHi);
            baos.write(initLo);
            baos.write(initHi);
            
            /*Now read the rest of the sector*/
            for(int i=0;i<128-6;i++) {
                int oneByte = bis.read();
                if (oneByte==-1) {
                    throw new IOException("EOF Reached prematurely. The disk image is corrupted");
                }
                baos.write(oneByte);
            }
            
            /*Now read the rest of the bootCodeData sectors*/
            if (numSectors==0) numSectors = 256;
            numSectors--;
            
            for (int i = 0; i < numSectors; i++) {
                for (int k = 0; k < 128; k++) {
                    int oneByte = bis.read();
                    if (oneByte == -1) {
                        throw new IOException("EOF Reached prematurely. The disk image is corrupted");
                    }
                    baos.write(oneByte);
                }
            }
            
            /*Now flush the bootCodeData*/
            byte[] codeBytes = baos.toByteArray();
            bootCodeData = getAsIntArray(codeBytes,codeBytes.length);
        }
    }
    
    private int[] readSector(BufferedInputStream bis) throws IOException {
        int[] sectorBytes = new int[128];
        for (int i = 0; i < sectorBytes.length; i++) {
            int oneByte = bis.read();
            if (oneByte == -1) {
                throw new IOException("EOF Reached prematurely. The disk image is corrupted");
            }
            sectorBytes[i] = oneByte;
        }
        return sectorBytes;

    }
    private static int[] getAsIntArray(byte[] byteArray, int numBytes) {

        int[] intArray = new int[numBytes];

        byte b;

        for (int i = 0; i < numBytes; i++) {
            b = byteArray[i];
            intArray[i] = (b < 0) ? b + 256 : b;
        }

        return intArray;
    }
    
    public void printOut() {
        System.out.format("BACKUP T/D Utility disk, %d sectors %n",totalSectors);
        System.out.format("Boot code size: %d %n",bootCodeData.length);
        System.out.format("Identification: %s %n",idString);
        
        for(FileProxy oneProxy:fileProxies) {
            System.out.format("%s%n",oneProxy.toString());
        }
    }
    
    public List<FileProxy> getProxies() {
        return fileProxies;
    }

    public String getFileName() {
        return fileName;
    }
    
    public String getFilespec() {
        return fileSpec;
    }

    String getStatusInfo() {
        return String.format("ID: %s Sectors: %d, Boot code: %d",idString, totalSectors,bootCodeData.length);
    }
    
    public void writeImage(String filespec) throws IOException {
        
        try (FileOutputStream fos = new FileOutputStream(filespec); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            
            int writtenSectors=0;
            
            /*Write the .ATR header*/
            writeBytes(atrHeaderData,bos);
            
            /*Write boot code*/
            writeBytes(bootCodeData,bos);
            writtenSectors+=bootCodeData.length/128;
            
            /*Write filler data*/
            for(int i=0;i<dummySectorCount;i++) {
                writeEmptySector(bos);
                writtenSectors++;
            }
            
            /*Write identification*/
            writeBytes(idSectorData,bos);
            writtenSectors++;
            
            /*Write pristine indicators*/
            writeBytes(pristineSectorData,bos);
            writtenSectors++;
            
            /*Now write records of all files*/
            for (FileProxy oneProxy:fileProxies) {
                
                /*Make the header sector*/
                int[] headerSector = makeEmptySectorData();
                
                headerSector[0]='H';
                headerSector[1]=0x11;
                headerSector[2]=0x00;
                
                headerSector[3]=oneProxy.getType();
                for(int i=0;i<10;i++) {
                    headerSector[4+i]=oneProxy.getNameChars()[i];
                }
                
                headerSector[4+10+0]=oneProxy.getLoad()%256;
                headerSector[4+10+1]=oneProxy.getLoad()/256;
                headerSector[4+10+2]=oneProxy.getLength()%256;
                headerSector[4+10+3]=oneProxy.getLength()/256;
                headerSector[4+10+4]=oneProxy.getRun()%256;
                headerSector[4+10+5]=oneProxy.getRun()/256;
                
                writeBytes(headerSector,bos);
                writtenSectors++;
                
                /*Write the data*/
                bos.write('D');
                bos.write(oneProxy.getFileData().length%256);
                bos.write(oneProxy.getFileData().length/256);
                writeBytes(oneProxy.getFileData(),bos);
                
                /*Pad the last sector*/
                int remainder = 128-((oneProxy.getFileData().length+3)%128);
                for (int i=0;i<remainder;i++) {
                    bos.write(0xFF);
                }
                
                writtenSectors+=((oneProxy.getFileData().length+3)/128);
                if (remainder!=0) writtenSectors++;
                
            }
            
            /*Now put the EOF indication*/
            int[] eofSecData = makeEmptySectorData();
            eofSecData[0]='E';
            
            writeBytes(eofSecData,bos);
            writtenSectors++;
            
            /*Now pad the disk image to its rightful size*/
            int emptySectors = totalSectors-writtenSectors;
            for(int i=0;i<emptySectors;i++) {
                writeEmptySector(bos);
            }
            
        }
        
        
    }
    
    private void writeBytes(int[] bytes,BufferedOutputStream bos) throws IOException {
        for (int oneByte:bytes) {
            bos.write(oneByte);
        }
    }
    
    private void writeEmptySector(BufferedOutputStream bos) throws IOException {
        for (int i=0;i<128;i++) {
            bos.write(0);
        }
    }
    
    private int[] makeEmptySectorData() {
        int[] emptyData = new int[128];
        for(int i=0;i<emptyData.length;i++) {
            emptyData[i]=0xFF;
        }
        return emptyData;
    }

    
    
}
