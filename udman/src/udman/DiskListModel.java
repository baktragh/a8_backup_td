package udman;

import java.util.ArrayList;
import javax.swing.AbstractListModel;
import javax.swing.ListSelectionModel;

public class DiskListModel extends AbstractListModel<FileProxy> {
    
    private UtilityDisk disk;

    void delete(int[] selIndices) {
        
        int offset = 0;
        for (int oneIndex:selIndices) {
            disk.getProxies().remove(oneIndex-offset);
            fireIntervalRemoved(this, oneIndex-offset, oneIndex-offset);
            offset++;
        }
    }
    
    enum MoveDirection {
        UP,
        DOWN
    }
    
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
    
    public UtilityDisk getDisk() {
        return disk;
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

    void itemUpdate(FileProxy fp) {
        int index = disk.getProxies().indexOf(fp);
        if (index!=-1) {
            fireContentsChanged(this, index, index);
        }
    }
    
    boolean moveElements(int[] indexes,MoveDirection direction,ListSelectionModel selModel) {
        
        boolean movable=true;
        int prevIndex=-1;
        
        ArrayList<Integer> selIndexes = new ArrayList<>();
        
        int mainDiff;
        int oldItemDiff;
        int startItem;
        int endItem;
        int stuckIndex;
        
        if (direction==MoveDirection.UP) {
            mainDiff=+1;
            oldItemDiff=-1;
            startItem=0;
            endItem=indexes.length;
            stuckIndex=0;
        }
        else {
            mainDiff=-1;
            oldItemDiff=+1;
            startItem=indexes.length-1;
            endItem=-1;
            stuckIndex=disk.getProxies().size()-1;
        }
        
        for (int z=startItem;z!=endItem;z+=mainDiff) {
            
            int currIndex = indexes[z];
            
            /*If zeroth index, then not movable and we are done*/
            if (currIndex==stuckIndex) {
                movable=false;
                prevIndex=currIndex;
                selIndexes.add(currIndex);
                continue;
            }
            
            /*If previous item was not movable and we are previous+1, item is not movable either*/
            if (prevIndex==currIndex+oldItemDiff && movable==false) {
                prevIndex=currIndex;
                selIndexes.add(currIndex);
                continue;
            }
            
            movable=true;
            
            /*Get references to the items*/
            FileProxy oldItem = disk.getProxies().get(currIndex+oldItemDiff);
            FileProxy currentItem = disk.getProxies().get(currIndex);
            
            /*Swap*/
            disk.getProxies().set(currIndex, oldItem);
            disk.getProxies().set(currIndex+oldItemDiff,currentItem);
            selIndexes.add(currIndex+oldItemDiff);
            
            /*Update prev index*/
            prevIndex=currIndex;
            
        }
        
        if (movable==false) return movable;
        
        for (int selIndex:selIndexes) {
            fireContentsChanged(this,selIndex, selIndex+mainDiff);
        }
        
        selModel.clearSelection();
        
        for (int selindex:selIndexes) {
            selModel.addSelectionInterval(selindex, selindex);
        }
        
        return movable;
        
        
    }

}
