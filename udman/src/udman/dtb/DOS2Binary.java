package udman.dtb;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * DOS 2 Binary file.
 */
public class DOS2Binary {
    
    private final String filename;
    private final ArrayList<Segment> segmentList;
    private boolean isAnalyzed;
    private int fileLength;
    
     public DOS2Binary(String fileSpec) {
        filename = fileSpec;
        segmentList = new ArrayList<>();
        isAnalyzed = false;
        
    }

    public String[] getListing() {

        /*Array of the strings*/
        String[] s = new String[segmentList.size()];

        for (int i = 0; i < segmentList.size(); i++) {
            s[i] = segmentList.get(i).toString();
        }

        return s;
    }

    public void analyzeFromFile() throws IOException, DOS2BinaryException {

        /*Check size. Maximum size is up to 16 MB*/
        File f = new File(filename);
        if (f.exists() && f.isFile()) {
            long l = f.length();
            if (l > 16 * 1_024 * 1_024) {
                throw new DOS2BinaryException(filename, "Binary file is too long. File size exceeds 16 MB.", 0);
            }
        }
        
        byte[] filebData;
        
        try (FileInputStream fis = new FileInputStream(filename); BufferedInputStream bis = new BufferedInputStream(fis, 4096)) {

            /*Get all the data from the file*/
            filebData = bis.readAllBytes();
        }

        /*Convert the data to array if integers*/
        int[] fileData = getAsIntArray(filebData,filebData.length);

        analyze(fileData, true);

    }

    public void analyzeFromData(int[] fileData) throws IOException, DOS2BinaryException {
        analyzeFromData(fileData, true);
    }

    public void analyzeFromData(int[] fileData, boolean headerRequired) throws IOException, DOS2BinaryException {
        analyze(fileData, headerRequired);
    }

    private void analyze(int[] fileData, boolean headerRequired) throws IOException, DOS2BinaryException {

        int pos = 0;
        fileLength = fileData.length;
        int b1;
        int b2;
        int w1;
        int w2;
        
        /*Begin analysis*/
        /*If a header is required to be present, check for header (255 255)*/
        if (headerRequired == true) {
            
            if (fileLength<2) {
                throw new DOS2BinaryException(filename,"The binary file is too short to have a header",0);
            }
            
            if (fileData[0] != 255 || fileData[1] != 255) {
                throw new DOS2BinaryException(filename, "Binary file header not found. First two bytes do not have values of 255 [0xFF]", 0);
            }
            pos = 2;
        } else if (fileData[0] == 255 && fileData[1] == 255) {
            pos = 2;
        } else {
            pos = 0;
        }

        /*Segment header must have at least 2 bytes*/
        while (pos < fileLength) {

            int lastSegPos = pos;

            try {

                /*Is there another 255 255*/
                b1 = fileData[pos];
                b2 = fileData[pos + 1];

                /*If so, update position*/
                if (b1 == 255 && b2 == 255) {
                    pos += 2;
                }

                /*Get first address and last address*/
                w1 = fileData[pos] + 256 * fileData[pos + 1];
                pos += 2;

                w2 = fileData[pos] + 256 * fileData[pos + 1];
                pos += 2;
                
                /*Possible compressed segment*/
                
                /*Standard, non-compressed segment*/
                
                    /*Check for negative segment size*/
                    if (w2 < w1) {
                        throw new DOS2BinaryException(filename, getNegativeSegmentSizeMessage(w1, w2), lastSegPos);
                    }

                    /*Create new segment*/
                    int[] newSegmentData = new int[w2 - w1 + 1];
                    System.arraycopy(fileData, pos, newSegmentData, 0, newSegmentData.length);
                    Segment s = new Segment(w1, newSegmentData, lastSegPos);

                    /*Add segment to the list*/
                    this.segmentList.add(s);

                    /*Advance*/
                    pos += newSegmentData.length;
                

            } catch (ArrayIndexOutOfBoundsException ae) {
                throw new DOS2BinaryException(filename, "Segment or segment header continues beyond end of binary file", pos);
            }

        }/*End of main loop*/

        isAnalyzed = true;

    }
    
    

    


    /**
     * Get number of data segments
     *
     * @return Number of data segments
     */
    public int getSegmentWithoutVectorCount() {
        Iterator<Segment> it = segmentList.iterator();
        int count = 0;
        while (it.hasNext()) {
            Segment seg = it.next();
            if (seg.hasNoVector() == true) {
                count++;
            }
        }
        return count;
    }

    /**
     * Number of segments with RUN or INIT Vector
     *
     * @return Number of segments
     */
    public int getSegmentWithVectorCount() {
        Iterator<Segment> it = segmentList.iterator();
        int count = 0;
        while (it.hasNext()) {
            Segment seg = it.next();
            if (!seg.hasNoVector() == true) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get offsets of segments that have INIT vectors
     *
     * @return Array of offsets
     */
    public int[] getInitLocations() {

        Iterator<Segment> it = segmentList.iterator();
        int count = 0;
        while (it.hasNext()) {
            Segment seg = it.next();
            if (seg.hasFullInitVector()) {
                count++;
            }
        }

        int[] initLocations = new int[count];

        it = segmentList.iterator();
        int index = 0;
        while (it.hasNext()) {
            Segment seg = it.next();
            if (seg.hasFullInitVector()) {
                initLocations[index] = seg.getEndRba();
                index++;
            }
        }

        return initLocations;
    }

    /**
     * Get file statistics
     *
     * @return Array with file statistics
     */
    public int[] getFileStatistics() {
        int[] retVal = new int[7];

        retVal[0] = fileLength;
        /*File size*/
        retVal[1] = segmentList.size();
        /*Total numbe of non-vector segments*/
        retVal[2] = getSegmentWithoutVectorCount();
        /*Segments with vector*/
        retVal[3] = getSegmentWithVectorCount();
        
        return retVal;
    }

    /**
     * Test whether some data segment covers defined memory area. Useful to test
     * compatibility with some binary loaders
     *
     * @param firstAdr First address of tested memory area
     * @param lastAdr Last address of tested memory area
     * @return true if some segment covers defined memory area
     */
    public boolean coversMemory(int firstAdr, int lastAdr) {

        int l = segmentList.size();
        Segment s;
        boolean b;
        for (int i = 0; i < l; i++) {
            b = true;
            s = segmentList.get(i);
            if ((s.getFirstAddress() < firstAdr) && (s.getLastAddress() < firstAdr)) {
                b = false;
            }
            if ((s.getFirstAddress() > lastAdr) && (s.getLastAddress() > lastAdr)) {
                b = false;
            }
            if (b == true) {
                return b;
            }
        }

        return false;
    }

    /**
     * Test whether file contain at least one INIT vector
     *
     * @return true When file contains at least one INIT vector
     */
    public boolean hasInitVector() {
        Iterator<Segment> it = segmentList.iterator();
        while (it.hasNext()) {
            Segment seg = it.next();
            if (seg.hasInitVector() == true) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @return
     */
    public boolean hasRunVector() {
        Iterator<Segment> it = segmentList.iterator();
        while (it.hasNext()) {
            Segment seg = it.next();
            if (seg.hasRunVector() == true) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasCompressedSegment() {
        Iterator<Segment> it = segmentList.iterator();
        while (it.hasNext()) {
            Segment seg = it.next();
            if (seg.isCompressed()==true) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @return
     */
    public String getFileName() {
        return filename;
    }

    /**
     * Determine whether the binary file is monolithic
     *
     * @return True when the binary file is monolithic
     */
    public boolean isMonolithic() {

        int segmentCount = getTotalSegmentCount();

        /*More than two segments or INIT vector are show-stoppers*/
        if (segmentCount > 2 || hasInitVector()) {
            return false;
        }
        
        /*Compression is not tolerated*/
        if (hasCompressedSegment()) return false;

        /*One segment is always OK, unless there is a partial RUN vector*/
        Iterator<Segment> segIter = segmentList.iterator();
        Segment s1 = segIter.next();
        if (segmentCount == 1) {
            if (!s1.hasPartialRunVector()) {
                return true;
            }
            return false;
        }

        /*If there are two segments, we must ensure that:
          1. There is at least one byte of non-vector data
          2. Only one of the segments has non-vector data
          3. There is exactly one full specified RUN vector
         */
        Segment s2 = segIter.next();

        /* Both have non-vector data - not monolithic*/
        if (s1.hasNonVectorData() == true && s2.hasNonVectorData() == true) {
            return false;
        }
        /* None has non-vector data - not monolithic*/
        if (!s1.hasNonVectorData() && !s2.hasNonVectorData()) {
            return false;
        }
        /* Both have RUN vector - not monolithic*/
        if (s1.hasRunVector() && s2.hasRunVector()) {
            return false;
        }
        /* None have full RUN vector - not monolithic*/
        if (!s1.hasFullRunVector() && !s2.hasFullRunVector()) {
            return false;
        }

        /*All conditions met*/
        return true;

    }


    /**
     *
     * @return
     */
    public int getTotalSegmentCount() {
        return segmentList.size();
    }

    /**
     * Get number of bytes required to replace effects of INIT segments and RUN
     * segment
     *
     * @return
     */
    public int getExtraCodeForMergeLength() {
        int initCounter = 0;
        int hasRun = 0;

        Iterator<Segment> it = segmentList.iterator();

        while (it.hasNext()) {
            Segment seg = it.next();
            if (seg.hasFullInitVector()) {
                initCounter++;
            }
            if (seg.hasFullRunVector()) {
                hasRun = 1;
            }
        }

        return (initCounter + hasRun) * 3;

    }

    /**
     *
     * @return
     */
    public boolean isIsAnalyzed() {
        return isAnalyzed;
    }

    /**
     *
     * @return
     */
    public Iterator<Segment> getSegmentListIterator() {
        return segmentList.iterator();
    }

    private String getNegativeSegmentSizeMessage(int w1, int w2) {

        StringBuilder sb = new StringBuilder(24);
        sb.append("Segment with negative size found ");
        sb.append('(');
        sb.append(String.format("%05d",w1));
        sb.append('-');
        sb.append(String.format("%05d",w2));
        sb.append(" [");
        sb.append(String.format("%04X",w1));
        sb.append('-');
        sb.append(String.format("%04X",w2));
        sb.append("])");

        return sb.toString();

    }

    /**
     *
     * @return
     */
    public int getFileLength() {
        return fileLength;
    }

    /**
     *
     */
    public void createArtificialRunVector() {
        Iterator<Segment> it = segmentList.iterator();

        Segment seg = null;

        /*Get first segment*/
        if (it.hasNext()) {
            seg = it.next();
        }
        
        if (seg==null) return;

        int[] segData = new int[2];
        segData[0] = seg.getFirstAddress() % 256;
        segData[1] = seg.getFirstAddress() / 256;

        Segment runVectorSegment = new Segment(736, segData, 0);
        this.segmentList.add(runVectorSegment);

    }


    /**
     * Get information required to perform conversion of monolithic binary file
     *
     * @return MonolithicConversionInfoCrate with information
     * @throws DOS2BinaryProcessingException
     */
    public MonolithicConversionInfoCrate getMonolithicBinaryFileConversionInfo() throws DOS2BinaryProcessingException {

        /* Check if the file is monolithic*/
        if (!isMonolithic()) {
            throw new DOS2BinaryProcessingException(("Internal error: getMonolithichBinaryFileConversionInfo() called on non-monolithic binary file"));
        }

        /*Create crate*/
        MonolithicConversionInfoCrate crate = new MonolithicConversionInfoCrate();

        /*Populate the crate*/
        Iterator<Segment> segIter = segmentList.iterator();
        int segmentCount = getTotalSegmentCount();

        Segment s1 = segIter.next();

        /*With one segment*/
        if (segmentCount == 1) {
            crate.data = s1.getData();
            if (s1.hasRunVector() == true) {
                crate.runAddress = s1.getRunVector();
            } else {
                crate.runAddress = s1.getFirstAddress();
            }
            crate.loadAddress = s1.getFirstAddress();
        } /*With two segments*/ else {
            Segment s2 = segIter.next();

            if (s1.hasNonVectorData() == true) {
                crate.data = s1.getData();
                crate.runAddress = s2.getRunVector();
                crate.loadAddress = s1.getFirstAddress();
            } else {
                crate.data = s2.getData();
                crate.runAddress = s1.getRunVector();
                crate.loadAddress = s2.getFirstAddress();
            }

        }

        return crate;

    }

    
    /**
     *
     */
    public static class MonolithicConversionInfoCrate {
        
        /**
         * Data
         */
        public int[] data;
        /**
         * Run address
         */
        public int runAddress;
        /**
         * Load address
         */
        public int loadAddress;
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
    
    
    
    
    
    
    

}



