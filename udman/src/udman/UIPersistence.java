package udman;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class UIPersistence implements Serializable {
    
    String diskFolder;
    String extractFolder;
    boolean extractTapes;
    boolean extractBinaries;
    boolean extractForceBinaries;
    boolean extractSequentialNaming;
    boolean extractLongNames;
    String importFolder;
    boolean extractBigCas;
    boolean largeFont;
    
    private UIPersistence() {
        
        diskFolder="";
        extractFolder="";
        extractTapes=false;
        extractBinaries=true;
        extractForceBinaries=false;
        extractSequentialNaming=false;
        extractLongNames=true;
        importFolder="";
        extractBigCas=false;
        largeFont=false;
        
    }
    
    public static UIPersistence getInstance() {
        if (oneInstance==null) {
            oneInstance= new UIPersistence();
        }
        
        return oneInstance;
    }
    
    private static UIPersistence oneInstance=null;
    
    public void load() {
        try {
            String filespec = System.getProperty("user.home")+System.getProperty("file.separator")+".udman.ser";
            try (FileInputStream fis = new FileInputStream(filespec); ObjectInputStream ois = new ObjectInputStream(fis)) {
                oneInstance = (UIPersistence)ois.readObject();
            }
        }
        catch(Exception e) {
            /*Left blank*/
            
        }
        
    }
    
    public void save() {
        try {
            String filespec = System.getProperty("user.home")+System.getProperty("file.separator")+".udman.ser";
            try (FileOutputStream fos = new FileOutputStream(filespec); ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(oneInstance);
            }
        }
        catch(Exception e) {
            /*Left blank*/
            
        }
    }

    
}
