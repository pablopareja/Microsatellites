/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.microsats.programs;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class Test {

    public static void main(String[] args) {

        int threshold = 10;

        String sequence = "CccTTCACCTACTAAAGGCACACCTTCTTCCGAAGTTACGGTGTCAATTTGCCGAGTTCCTTCTCCTGAGTTCTCTCAAGCGCCTTAGAATACTCATCTCGCGCACCAGTGTCGGTTTGCGGTACGGTCGTGTGTAGCTGAAGCTTAGTGGCTTTTCCTGGAAGCAGGGTATCACTCACTTCGGCTGCAAGCAGCCTCGTTATCACCCCTCATCTAAGCCCGGCGGATTTACCTACCAGGCACGACTACAGGCTTGAACCAACATATCCAACAGTTGGCTGAGCTAACCTTCtCCGtCCCCACATCGCACTACACATCGGTACAGGAATATTGACCTGTTTCCCATCAACTACGCATCTCTGCCTCGCCTTAGGGGCCGACTCACTCTACGCCGATGAACGTTGCGTAGAAAACCTTGCGCTTACGGCGAGGGGGCTTTTCACCCCCTTTAACGCTACTCATGTCAGCATTCGCACTTCTGATACCTCCAGCACGCTTTACAACG".toLowerCase();

        for (int i = 0; i < sequence.length() - 3; i++) {

            //loop for the different lengths of patterns repeated in the microsats
            for (int j = 2; j <= threshold; j++) {

                int currentPos = i;
                int microsatRepetitions = 1;
                boolean microSatFound = false;

                if (currentPos + (j * 2) <= sequence.length()) {

                    while (sequence.substring(currentPos, currentPos + j).equals(sequence.substring(currentPos + j, currentPos + (2 * j)))) {

                        microsatRepetitions++;
                        microSatFound = true;

                        currentPos += j;
                        
                        if(! (currentPos + (j * 2) <= sequence.length())){
                            break;
                        }
                    }

                    if (microSatFound) {
                        System.out.println("start microsat = " + i);
                        System.out.println("microsatRepetitions = " + microsatRepetitions);
                        System.out.println("Microsatfound, begin: " + i + " microsatRepetitions: " + microsatRepetitions + " seq: " + sequence.substring(i, i + (microsatRepetitions * j)));
                    }

                }


            }

        }
    }
}
