package udman;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import udman.dtb.DOS2Binary;
import udman.dtb.DOS2BinaryProcessingException;
import udman.tapeimage.PWMChunk;
import udman.tapeimage.TapeImage;
import udman.tapeimage.TapeImageChunk;

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
           readFromStream(bis);
           setFilespec(filespec);
        }
        
    }
    
    public UtilityDisk(InputStream is) throws IOException {
        
        fileProxies = new ArrayList<>();
        
        try (BufferedInputStream bis = new BufferedInputStream(is)) {
           readFromStream(bis);
           setFilespec("");
        }
    }
    
    private void readFromStream(BufferedInputStream bis) throws IOException {
         readATRHeader(bis);
            readCode(bis);
            readIdentification(bis);
            if (!isPristine) readData(bis);
        
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
            
            if (headerSector[0]==0xFF) continue;
            
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
            int remainder = (fileLen + 3) % 128;
            if (remainder != 0) {
                int skipBytes = 128 - ((fileLen + 3) % 128);

                for (int i = 0; i < skipBytes; i++) {
                    int oneByte = bis.read();
                    if (oneByte == -1) {
                        throw new IOException("EOF Reached prematurely");
                    }
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
        
        byte[] diskImageData;
        
        try (  ByteArrayOutputStream tempBaos = new ByteArrayOutputStream();
                BufferedOutputStream tempBos= new BufferedOutputStream(tempBaos) ) {
            
            int writtenSectors=0;
            
            /*Write the .ATR header*/
            writeBytes(atrHeaderData,tempBos);
            
            /*Write boot code*/
            writeBytes(bootCodeData,tempBos);
            writtenSectors+=bootCodeData.length/128;
            
            /*Write filler data*/
            for(int i=0;i<dummySectorCount;i++) {
                writeEmptySector(tempBos);
                writtenSectors++;
            }
            
            /*Write identification*/
            writeBytes(idSectorData,tempBos);
            writtenSectors++;
            
            /*Check pristine indicators. If we have data, we must
              change the pristine sector indicators to indicate so*/
            if (pristineSectorData[0]==0x55 && !fileProxies.isEmpty()) {
                for(int i=0;i<pristineSectorData.length;i++) {
                    pristineSectorData[i]=0xC0;
                }
            }
            
            writeBytes(pristineSectorData,tempBos);
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
                
                writeBytes(headerSector,tempBos);
                writtenSectors++;
                
                /*Write the data*/
                tempBos.write('D');
                tempBos.write(oneProxy.getFileData().length%256);
                tempBos.write(oneProxy.getFileData().length/256);
                writeBytes(oneProxy.getFileData(),tempBos);
                
                /*Pad the last sector*/
                int remainder = ((oneProxy.getFileData().length + 3) % 128);
                if (remainder != 0) {

                    int paddingBytes = 128 - remainder;
                    for (int i = 0; i < paddingBytes; i++) {
                        tempBos.write(0xFF);
                    }
                }
                
                writtenSectors+=((oneProxy.getFileData().length+3)/128);
                if (remainder!=0) writtenSectors++;
                
            }
            
            /*Now put the EOF indication*/
            int[] eofSecData = makeEmptySectorData();
            eofSecData[0]='E';
            
            writeBytes(eofSecData,tempBos);
            writtenSectors++;
            
            /*Now pad the disk image to its rightful size*/
            int emptySectors = totalSectors-writtenSectors;
            
            if (emptySectors<0) {
                throw new IOException("Disk image size exceeded");
            }
            
            for(int i=0;i<emptySectors;i++) {
                writeEmptySector(tempBos);
            }
            
            tempBos.flush();
            diskImageData=tempBaos.toByteArray();
            
        }
        catch (IOException ioe) {
            throw ioe;
        }
        
        /*Once we are sure the disk image is valid, write it to file*/
        try (FileOutputStream fos = new FileOutputStream(filespec);
                BufferedOutputStream fileBos = new BufferedOutputStream(fos)) {
            
            fileBos.write(diskImageData);
            fileBos.flush();

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
    
    public FileProxy importMonolithicBinary(String filespec) throws Exception {
        
        DOS2Binary dtb = new DOS2Binary(filespec);
        dtb.analyzeFromFile();
        
        if (!dtb.isMonolithic()) {
            throw new DOS2BinaryProcessingException("The binary file is not monolithic");
        }
        
        DOS2Binary.MonolithicConversionInfoCrate convInfo = dtb.getMonolithicBinaryFileConversionInfo();
        
        
        int[] nameChars = new int[10];
        for(int i=0;i<nameChars.length;i++) nameChars[i]=0x20;
        
        File f = new File(filespec);
        String name = f.getName();
        name=name.substring(0,Math.min(name.length(),10)).toUpperCase();
        
        for(int i=0;i<name.length();i++) {
            nameChars[i]=name.charAt(i);
        }
        
        FileProxy newProxy = new FileProxy(
                convInfo.data,
                0x03,
                convInfo.loadAddress,
                convInfo.data.length,
                convInfo.runAddress,
                nameChars
        );
        
        return newProxy;
        
    }
    
    
    private enum TapeScanState {
        LOOK_FOR_HEADER,
        LOOK_FOR_DATA
    }
    
    public List<FileProxy> importTapeImage(String filespec) throws Exception {
        
        TapeImage ti = new TapeImage();
        ti.parse(filespec);
        
        int chunkCount = ti.getChunkCount();
        List<PWMChunk> pwmdChunks = new ArrayList<>();
        
        ArrayList<FileProxy> proxies = new ArrayList<>();
        
        /*Extract all pwmd chunks*/
        for(int i=0;i<chunkCount;i++) {
            TapeImageChunk c = ti.getChunkAt(i);
            if (c instanceof PWMChunk && c.getType().equals("pwmd")) {
                pwmdChunks.add((PWMChunk)c);
            }
        }
        
        TapeScanState state = TapeScanState.LOOK_FOR_HEADER;
        int[] lastHeaderFound = null;
        
        for(PWMChunk oneChunk:pwmdChunks) {
            
            if (state==TapeScanState.LOOK_FOR_HEADER) {
                int[] headerData = oneChunk.getData();
                if (headerData.length==19 && headerData[0]==0x00) {
                    int chsum = headerData[0];
                    for (int i=1;i<headerData.length-1;i++) {
                        chsum = chsum ^ headerData[i];
                    }
                    if (chsum==headerData[headerData.length-1]) {
                        lastHeaderFound=headerData;
                        state= TapeScanState.LOOK_FOR_DATA;
                    }
                }
                
            }
            
            else if (state==TapeScanState.LOOK_FOR_DATA) {
                int[] bodyData = oneChunk.getData();
                
                /*Check the beginning*/
                if (bodyData[0]!=0xFF) {
                    state = TapeScanState.LOOK_FOR_HEADER;
                    continue;
                }
                /*Check checksum*/
                int chsum = bodyData[0];
                for(int i=1;i<bodyData.length-1;i++) {
                    chsum = chsum ^ bodyData[i];
                }
                /*If not match, then reset state*/
                if (bodyData[bodyData.length-1]!=chsum) {
                    state = TapeScanState.LOOK_FOR_HEADER;
                    continue;
                }
                
                /*Get fields*/
                int type = lastHeaderFound[1];
                int loadAddr = lastHeaderFound[1+10+1]+lastHeaderFound[1+10+2]*256;
                int length = lastHeaderFound[1+10+3]+lastHeaderFound[1+10+4]*256;
                int runAddr = lastHeaderFound[1+10+5]+lastHeaderFound[1+10+6]*256;
                
                int[] nameChars = new int[10];
                for(int i=0;i<10;i++) {
                    nameChars[i]=lastHeaderFound[2+i];
                }
                
                /*If the length in the header is a match, create a proxy*/
                if (length==bodyData.length-2) {
                    proxies.add(new FileProxy(Arrays.copyOfRange(bodyData, 1, bodyData.length-1) ,type,loadAddr,length,runAddr,nameChars));
                }
                
                /*In any case, look for header*/
                state = TapeScanState.LOOK_FOR_HEADER;
                
            }
            
            
        }
        
        return proxies;
        
    }

    
    
}
