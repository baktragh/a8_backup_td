package udman;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class Extractor {

    private final ExtractorConfig ec;

    public Extractor(ExtractorConfig ec) {
        this.ec = ec;
    }

    int extract() throws Exception {

        /*For all proxies*/
        int counter = 0;
        
        /*If nothing to extract, then just return*/
        if (!ec.toBinary && !ec.toCas) return counter;

        for (FileProxy oneProxy : ec.fileProxies) {

            /*Prepare normalized file name*/
            StringBuilder sb = new StringBuilder();
            for (int oneChar : oneProxy.getNameChars()) {
                sb.append((char) oneChar);
            }

            String originalName = sb.toString().trim();
            String normalizedName = normalizeName(originalName);

            /*Determine extension*/
            String fullExtension;
            String prefixExtension;
            String finalExtension;

            int fileType = oneProxy.getType();
            if (fileType == 0x03 || fileType == 0x04 || ec.forceBinary) {
                fullExtension = ".xex";
                prefixExtension = ".x";
            }
            else if (fileType == 0xFF || fileType == 0xFE) {
                fullExtension = ".bas";
                prefixExtension = ".b";
            }
            else {
                fullExtension = ".dat";
                prefixExtension = ".d";
            }

            if (!ec.longNames && ec.sequentialNames) {
                finalExtension = prefixExtension + String.format("%02X", counter);
            }
            else {
                finalExtension = fullExtension;
            }

            /*Determine flat file name*/
            int maxNameLength = ec.longNames ? 10 : 8;
            String shortenedName = normalizedName.substring(0, Math.min(maxNameLength, normalizedName.length()));

            /*Determine final full name*/
            String finalName;

            if (ec.sequentialNames) {
                if (ec.longNames) {
                    finalName = String.format("%03d", counter) + "_" + shortenedName + finalExtension;
                }
                else {
                    finalName = (shortenedName + finalExtension).toUpperCase();

                }
            }
            else {

                finalName = shortenedName + finalExtension;
                if (!ec.longNames) {
                    finalName = finalName.toUpperCase();
                }
            }

            if (ec.toBinary) {
                exportBinary(oneProxy, finalName, fileType);
            }

            if (ec.toCas) {
                String casExtension;
                String casName;

                if (!ec.longNames && ec.sequentialNames) {
                    casExtension = String.format(".c%02X", counter);
                }
                else {
                    casExtension = ".cas";
                }

                if (ec.sequentialNames) {
                    if (ec.longNames) {
                        casName = String.format("%03d", counter) + "_" + shortenedName + casExtension;
                    }
                    else {
                        casName = (shortenedName + casExtension).toUpperCase();
                    }
                }
                else {
                    casName = shortenedName + casExtension;
                    if (!ec.longNames) {
                        casName = casName.toUpperCase();
                    }
                }

                exportToCas(oneProxy, casName);
            }
            counter++;

        }

        return counter;
    }

    private String normalizeName(String originalName) {
        StringBuilder sb = new StringBuilder();
        char[] origChars = originalName.toCharArray();

        for (char oneChar : origChars) {

            /*Remove inverse video*/
            if ((oneChar & 0x80) == 0x80) {
                oneChar = (char) (oneChar & 0x7F);
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

    private void exportBinary(FileProxy oneProxy, String finalName, int fileType) throws Exception {

        try (FileOutputStream fos = new FileOutputStream(ec.outputDirectory + System.getProperty("file.separator") + finalName);
                BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            boolean doHeader = (fileType != 0x04 && (ec.forceBinary || (fileType == 0x03)));

            if (doHeader) {
                bos.write(0xFF);
                bos.write(0xFF);
                bos.write(oneProxy.getLoad() % 256);
                bos.write(oneProxy.getLoad() / 256);
                bos.write((oneProxy.getLoad() + oneProxy.getLength() - 1) % 256);
                bos.write((oneProxy.getLoad() + oneProxy.getLength() - 1) / 256);
            }

            for (int oneByte : oneProxy.getFileData()) {
                bos.write(oneByte);
            }

            if (doHeader) {
                bos.write(0xE0);
                bos.write(0x02);
                bos.write(0xE1);
                bos.write(0x02);
                bos.write(oneProxy.getRun() % 256);
                bos.write(oneProxy.getRun() / 256);
            }

        }

    }

    private void exportToCas(FileProxy oneProxy, String casName) throws Exception {

        try (FileOutputStream fos = new FileOutputStream(ec.outputDirectory + System.getProperty("file.separator") + casName);
                BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            /*Write FUJI and PWMS*/
            final char[] headerBlob = {'F', 'U', 'J', 'I',
                0x00, 0x00, 0x00, 0x00,
                'p', 'w', 'm', 's',
                0x02, 0x00, 0x06, 0x00, 0x44, 0xAC};

            for (char oneByte : headerBlob) {
                bos.write(oneByte);
            }

            /*Prepare block prefix - header*/
            char[] blockPrefixHeader = {
                'p', 'w', 'm', 'c',
                0x03, 0x00, 0x00, 0x00, 0x20, 0x00, 0x0C,
                'p', 'w', 'm', 'l',
                0x04, 0x00, 0x00, 0x00, 0x05, 0x00, 0x05, 0x00,
                'p', 'w', 'm', 'd',
                0xFF, 0xFE, 0x0C, 0x1A
            };
                

            /*Zap the header block prefix*/
            blockPrefixHeader[27] = (char) (19 & 0x00FF);
            blockPrefixHeader[28] = (char) (19 >> 8);

            /*Write the header block prefix*/
            for (char oneByte : blockPrefixHeader) {
                bos.write(oneByte);
            }

            /*Construct the header block full data, including id byte and checksum*/
            int[] headerFullData = new int[19];
            
            headerFullData[0] = 0x00;
            System.arraycopy(oneProxy.getHeaderData(),0,headerFullData,1,oneProxy.getHeaderData().length);
            
            int chsum = headerFullData[0];
            for (int i = 0; i < 17; i++) {
                chsum = chsum ^ headerFullData[1 + i];
            }
            headerFullData[12 + 6] = chsum;

            /*Write the header block data*/
            for (int oneByte : headerFullData) {
                bos.write(oneByte);
            }

            /*Termination pulse*/
            final char[] pwmlBlob = {
                'p', 'w', 'm', 'l',
                0x04, 0x00, 0x00, 0x00, 0x06, 0x00, 0x06, 0x00};
            for (int oneByte : pwmlBlob) {
                bos.write(oneByte);
            }

            /*Prepare block prefix - header*/
            char[] blockPrefixData = {
                'p', 'w', 'm', 'c',
                0x03, 0x00, 0x00, 0x00, 0x20, 0x00, 0x0C,
                'p', 'w', 'm', 'l',
                0x04, 0x00, 0x00, 0x00, 0x05, 0x00, 0x05, 0x00,
                'p', 'w', 'm', 'd',
                0xFF, 0xFE, 0x0C, 0x1A
            };
                

            /*Zap the data block prefix*/
            blockPrefixData[27] = (char) ((oneProxy.getLength() + 2) & 0x00FF);
            blockPrefixData[28] = (char) ((oneProxy.getLength() + 2) >> 8);

            /*Write the data block prefix*/
            for (char oneByte : blockPrefixData) {
                bos.write(oneByte);
            }

            /*Write the file data*/
            chsum = 0xFF;
            bos.write(0xFF);
            for (int oneByte : oneProxy.getFileData()) {
                chsum = chsum ^ oneByte;
                bos.write(oneByte);
            }
            bos.write(chsum);

            /*Termination pulse*/
            for (int oneByte : pwmlBlob) {
                bos.write(oneByte);
            }
        }

    }
    
    public static DiskInfo getDiskInfo(List<FileProxy> proxies) {
        
        DiskInfo di = new DiskInfo();
        
        final int SAMPLES_ONE = 26;
        final int SAMPLES_ZERO = 13;
        final int SAMPLES_HEAD_PILOT = 200497;
        final int SAMPLES_DATA_PILOT = 100247;
        final int SAMPLES_GAP_SHORT = 9667;
        final int SAMPLES_GAP_MEDIUM = 45787;
        final int SAMPLES_GAP_LONG = 133012;
        
        long numZeros=0;
        long numOnes=0;
        
        for(FileProxy oneProxy:proxies) {
            
            int[] fileData = oneProxy.getFileData();
            int[] fullData = new int[fileData.length+2];
            
            System.arraycopy(fileData,0,fullData,1,fileData.length);
            fullData[0]=0xFF;
            
            int chsum=fullData[0];
            for(int i=0;i<fileData.length;i++) {
                chsum = chsum^fileData[i];
            }
            fullData[fullData.length-1]=chsum;
            
            int[] headerData = oneProxy.getHeaderData();
            int[] fullHeaderData = new int[headerData.length+2];
            System.arraycopy(headerData,0,fullHeaderData,1,headerData.length);
            fullHeaderData[0]=0x00;
            
            chsum=fullHeaderData[0];
            for(int i=0;i<headerData.length;i++) {
                chsum = chsum^headerData[i];
            }
            fullHeaderData[fullHeaderData.length-1]=chsum;
            
            long headerOnes = getOneBitCount(fullHeaderData);
            long dataOnes = getOneBitCount(fullData);
            
            long headerZeros = fullHeaderData.length*8-headerOnes;
            long dataZeros = fullData.length*8-dataOnes;
            
            numZeros+=(headerZeros+dataZeros);
            numOnes+=(headerOnes+dataOnes);
        }
        
        
        
        long numBasicSamples = numZeros*SAMPLES_ZERO;
        numBasicSamples+=numOnes*SAMPLES_ONE;
        numBasicSamples+=proxies.size()*SAMPLES_HEAD_PILOT;
        numBasicSamples+=proxies.size()*SAMPLES_DATA_PILOT;
        
        int numGaps = proxies.size()-1;
        if (numGaps<0) numGaps=0;
        
        di.numSamplesShort=numBasicSamples+numGaps*SAMPLES_GAP_SHORT;
        di.numSamplesMedium=numBasicSamples+numGaps*SAMPLES_GAP_MEDIUM;        
        di.numSamplesLong=numBasicSamples+numGaps*SAMPLES_GAP_LONG;        
        di.numBytes=(numZeros+numOnes)/8;
        
        return di;
    }
    
    private static long getOneBitCount(int[] oktets) {
        
        long counter=0;
        for (int oneOktet: oktets) {
            
            int mask=128;
            for(int i=0;i<8;i++) {
                if ((oneOktet & mask) == mask) counter++;
                mask=mask>>1;
            }
        }
        return counter;
    
    }
    
     public static String getTimeStringForSamples(long numSamples,int sampleRate) {
        
        long totalSeconds = numSamples/sampleRate;
        
        long hours = totalSeconds/3600;
        long minutes = (totalSeconds-hours*3600)/60;
        long seconds = (totalSeconds-hours*3600-minutes*60);
        
        if (hours>0) {
            return String.format("%02d:%02d:%02d",hours,minutes,seconds);
        }
        else {
            return String.format("%02d:%02d",minutes,seconds);
        }
        
    }
    
    
    public static class DiskInfo {
        long numBytes;
        long numSamplesShort;
        long numSamplesMedium;
        long numSamplesLong;
        
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
