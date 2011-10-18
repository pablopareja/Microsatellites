/*
 * Copyright (C) 2011  "Oh no sequences!"
 *
 *
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.era7.bioinfo.microsats.programs;

import com.era7.bioinfo.microsats.MicrosatellitesManager;
import com.era7.bioinfo.microsats.NodeIndexer;
import com.era7.bioinfo.microsats.NodeRetriever;
import com.era7.bioinfo.microsats.nodes.ProjectNode;
import com.era7.bioinfo.microsats.nodes.RepetitionLengthNode;
import com.era7.bioinfo.microsats.nodes.RepetitionNode;
import com.era7.bioinfo.microsats.nodes.SequenceNode;
import com.era7.bioinfo.microsats.relationships.MicrosatelliteFoundRel;
import com.era7.bioinfo.microsats.relationships.ProjectSequenceRel;
import com.era7.bioinfo.microsats.relationships.RepetitionLengthRel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Transaction;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class CreateMicrosatellitesDB {

    public static void main(String[] args) {

        if (args.length != 3) {
            System.out.println("This program expects 3 parameters:\n"
                    + "1. Multifasta input file" + "\n"
                    + "2. Repetition threshold (integer number)" + "\n"
                    + "3. Neo4j database folder");
        } else {

            MicrosatellitesManager manager = null;
            Transaction txn = null;

            ProjectSequenceRel projectSequenceRel = new ProjectSequenceRel(null);
            RepetitionLengthRel repetitionLengthRel = new RepetitionLengthRel(null);

            try {

                int threshold = Integer.parseInt(args[1]);

                manager = new MicrosatellitesManager(args[2]);
                txn = manager.beginTransaction();
                NodeRetriever nodeRetriever = new NodeRetriever(manager);
                NodeIndexer nodeIndexer = new NodeIndexer(manager);

                //----creating repetition length nodes-------
                for (int i = 2; i <= threshold; i++) {
                    RepetitionLengthNode repetitionLengthNode = new RepetitionLengthNode(manager.createNode());
                    repetitionLengthNode.setValue(i);
                    nodeIndexer.indexRepetitionLengthByValue(repetitionLengthNode, txn, false);
                }

                String line = null;
                boolean first = true;
                BufferedReader reader = new BufferedReader(new FileReader(new File(args[0])));
                StringBuilder seqStBuilder = null;

                int seqCounter = 0;

                String projectName = "",seqID ="" ,geneSt = "";
                int seqLength = -1;

                while ((line = reader.readLine()) != null) {

                    if (line.startsWith(">")) {                        

                        if (!first) {

                            //creating project node if necessary
                            ProjectNode projectNode = nodeRetriever.getProjectByName(projectName);
                            if (projectNode == null) {
                                projectNode = new ProjectNode(manager.createNode());
                                projectNode.setName(projectName);
                                nodeIndexer.indexProjectByName(projectNode, txn, true);
                                txn = manager.beginTransaction();
                            }
                            //creating sequence node
                            SequenceNode sequenceNode = new SequenceNode(manager.createNode());
                            sequenceNode.setSequence(seqStBuilder.toString());
                            sequenceNode.setLength(seqLength);
                            sequenceNode.setGene(geneSt);
                            sequenceNode.setId(seqID);

                            nodeIndexer.indexSequenceById(sequenceNode, txn, true);
                            txn = manager.beginTransaction();

                            projectNode.getNode().createRelationshipTo(sequenceNode.getNode(), projectSequenceRel);


                            //---search for microsatellites----
                            lookForMicrosatellites(sequenceNode, nodeIndexer, nodeRetriever, manager, threshold, txn, repetitionLengthRel);

                            txn.success();
                            txn.finish();
                            txn = manager.beginTransaction();

                        }
                        
                        String[] columns = line.substring(1).split("\\|");
                        projectName = columns[0];
                        seqID = columns[1];
                        geneSt = columns[2].split("gene=")[1];
                        seqLength = Integer.parseInt(columns[3].split("length=")[1]);

                        first = false;
                        seqStBuilder = new StringBuilder();

                        seqCounter++;
//                        if (seqCounter >= 2) {
//                            break;
//                        }
                        if(seqCounter % 100 == 0){
                            System.out.println("Current seq = " + seqID);
                            System.out.println("seqCounter = " + seqCounter);
                        }

                    } else {
                        seqStBuilder.append(line);
                    }
                }

                reader.close();

                txn.success();

            } catch (Exception ex) {

                Logger.getLogger(CreateMicrosatellitesDB.class.getName()).log(Level.SEVERE, null, ex);
                txn.failure();

            } finally {

                txn.finish();
                manager.shutDown();

            }

            System.out.println("Done!!");

        }
    }

    private static void lookForMicrosatellites(SequenceNode seqNode,
            NodeIndexer nodeIndexer,
            NodeRetriever nodeRetriever,
            MicrosatellitesManager manager,
            int threshold,
            Transaction txn,
            RepetitionLengthRel repetitionLengthRel) {



        String sequence = seqNode.getSequence().toLowerCase();

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

                        if (!(currentPos + (j * 2) <= sequence.length())) {
                            break;
                        }
                    }

                    if (microSatFound) {

                        //String microsatString = sequence.substring(i, i + (microsatRepetitions * j));
                        String tuple = sequence.substring(i, i + j);

                        RepetitionNode repetitionNode = nodeRetriever.getRepetitionByString(tuple);
                        //----------creating repetition node if necessary--------
                        if (repetitionNode == null) {
                            repetitionNode = new RepetitionNode(manager.createNode());
                            repetitionNode.setString(tuple);
                            repetitionNode.getNode().createRelationshipTo(nodeRetriever.getRepetitionLengthByValue(j).getNode(), repetitionLengthRel);
                            nodeIndexer.indexRepetitionByString(repetitionNode, txn, true);
                            txn = manager.beginTransaction();
                        }

                        MicrosatelliteFoundRel microsatelliteFoundRel = new MicrosatelliteFoundRel(null);
                        microsatelliteFoundRel = new MicrosatelliteFoundRel(seqNode.getNode().createRelationshipTo(repetitionNode.getNode(), microsatelliteFoundRel));
                        microsatelliteFoundRel.setNumberOfRepetitions(microsatRepetitions);
                        //---we add 1 to i for human-like coordinates---
                        microsatelliteFoundRel.setStartPosition(i + 1);

                    }

                }
            }
        }

    }
}
