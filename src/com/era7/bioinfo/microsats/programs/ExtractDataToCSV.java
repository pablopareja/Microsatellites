/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.microsats.programs;

import com.era7.bioinfo.microsats.MicrosatellitesManager;
import com.era7.bioinfo.microsats.NodeRetriever;
import com.era7.bioinfo.microsats.nodes.RepetitionLengthNode;
import com.era7.bioinfo.microsats.nodes.RepetitionNode;
import com.era7.bioinfo.microsats.nodes.SequenceNode;
import com.era7.bioinfo.microsats.relationships.MicrosatelliteFoundRel;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class ExtractDataToCSV {

    public static void main(String[] args) {

        if (args.length != 4) {
            System.out.println("This program expects 4 parameters:\n"
                    + "1. Neo4j database folder" + "\n"
                    + "2. Output filename" + "\n"
                    + "3. Tuple length" + "\n"
                    + "4. Minimum number of tuple copies threshold" + "\n");
        } else {

            MicrosatellitesManager manager = null;
            int minimumNumberOfCopies = Integer.parseInt(args[3]);

            try {

                BufferedWriter outBuff = new BufferedWriter(new FileWriter(new File(args[1])));
                //writing header
                outBuff.write("MICROSATELLITE_SEQUENCE\tTUPLE\tTUPLE_LENGTH\tREPETITIONS\tSEQUENCE_ID\tSTART_POSITION\n");


                manager = new MicrosatellitesManager(args[0]);
                NodeRetriever nodeRetriever = new NodeRetriever(manager);

                MicrosatelliteFoundRel microsatelliteFoundRel = new MicrosatelliteFoundRel(null);

                //---looping trough all repetition length values
                //for (int i = 2; i <= threshold; i++) {

                int i = Integer.parseInt(args[2]); //tuple length

                System.out.println("Looking for repetitions of length " + i);

                RepetitionLengthNode repetitionLengthNode = nodeRetriever.getRepetitionLengthByValue(i);
                if (repetitionLengthNode != null) {

                    List<RepetitionNode> repetitions = repetitionLengthNode.getRepetitions();

                    if (repetitions.size() > 0) {
                        System.out.println("Some repetitions found!");
                    } else {
                        System.out.println("There are none...");
                    }

                    for (RepetitionNode repetitionNode : repetitions) {

                        Iterator<Relationship> relsIt = repetitionNode.getNode().getRelationships(microsatelliteFoundRel, Direction.INCOMING).iterator();

                        while (relsIt.hasNext()) {

                            MicrosatelliteFoundRel microSatRel = new MicrosatelliteFoundRel(relsIt.next());
                            SequenceNode sequenceNode = new SequenceNode(microSatRel.getStartNode());

                            if (microSatRel.getNumberOfRepetitions() >= minimumNumberOfCopies) {
                                
                                String microsatString = "";
                                String tuple = repetitionNode.getString();
                                for (int j = 0; j < microSatRel.getNumberOfRepetitions(); j++) {
                                    microsatString += tuple;
                                }
                                outBuff.write(microsatString + "\t"
                                        + tuple + "\t"
                                        + i + "\t"
                                        + microSatRel.getNumberOfRepetitions() + "\t"
                                        + sequenceNode.getId() + "\t"
                                        + microSatRel.getStartPosition() + "\n");
                            }
                        }
                    }

                }

                //}



                outBuff.close();

            } catch (Exception ex) {

                Logger.getLogger(CreateMicrosatellitesDB.class.getName()).log(Level.SEVERE, null, ex);

            } finally {

                manager.shutDown();
            }

            System.out.println("Done!!");

        }
    }
}
