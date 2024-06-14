package udman;

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
        return String.format("%02X %10S %04X %04X %04X",type,sb.toString(),load,run,length);
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
    
    
    
   
    
    
}
