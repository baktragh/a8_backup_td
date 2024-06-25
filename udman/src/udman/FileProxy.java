package udman;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class FileProxy {
    
    private final int[] fileData;
    private int type;
    private int load;
    private int run;
    private int length;
    private int[] nameChars;
    
    @Override
    public String toString() {
        
        StringBuilder sb = new StringBuilder(10);
        for(int oneChar:nameChars) {
            sb.append((char)oneChar);
        }
        return String.format("%02X %10s %04X %04X %04X",type,sb.toString(),load,run,length);
    }

    public FileProxy(int[] fileData, int type, int load, int length, int run, int[] nameChars) {
        this.fileData = fileData;
        this.type = type;
        this.load = load;
        this.run = run;
        this.length = length;
        this.nameChars = nameChars;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getLoad() {
        return load;
    }

    public void setLoad(int load) {
        this.load = load;
    }

    public int getRun() {
        return run;
    }

    public void setRun(int run) {
        this.run = run;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int[] getNameChars() {
        return nameChars;
    }

    public void setNameChars(int[] nameChars) {
        this.nameChars = nameChars;
    }

    public int[] getFileData() {
        return fileData;
    }
    
    public int[] getHeaderData() {
         int[] headerData = new int[17];
            headerData[0] = getType();
            for (int i = 0; i < 10; i++) {
                headerData[1 + i] = getNameChars()[i];
            }
            headerData[11 + 0] = getLoad() % 256;
            headerData[11 + 1] = getLoad() / 256;
            headerData[11 + 2] = getLength() % 256;
            headerData[11 + 3] = getLength() / 256;
            headerData[11 + 4] = getRun() % 256;
            headerData[11 + 5] = getRun() / 256;
            
            return headerData;
    }

    InputStream toStream() {
        byte[] b =new byte[2];
        b[0]=0x3F;
        b[1]=0x3F;
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        return bais;
        
    }
    
    
    
   
    
    
}
