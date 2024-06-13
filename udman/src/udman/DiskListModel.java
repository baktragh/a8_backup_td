package udman;

import javax.swing.AbstractListModel;

public class DiskListModel extends AbstractListModel<FileProxy> {
    
    private UtilityDisk disk;
    
    public DiskListModel() {
        disk=null;
    }
    
    public void setDisk(UtilityDisk disk) {
        this.disk=disk;
        int numProxies = disk.getProxies().size();
        
        int firstIndex=0;
        int lastIndex=(numProxies<1)?0:numProxies-1;
        
        fireContentsChanged(this, firstIndex,lastIndex);
    }

    @Override
    public int getSize() {
        if (disk==null) {
            return 0;
        }
        else {
            return disk.getProxies().size();
        }
    }

    @Override
    public FileProxy getElementAt(int index) {
        
        if (disk==null) {
            return null;
        }
        else {
            return disk.getProxies().get(index);
        }
    }

}
