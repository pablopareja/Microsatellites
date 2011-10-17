/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.microsats.programs;

import com.era7.bioinfo.bioinfoneo4j.Neo4jManager;
import com.era7.bioinfo.microsats.MicrosatellitesManager;
import com.era7.bioinfo.microsats.NodeIndexer;
import com.era7.bioinfo.microsats.NodeRetriever;
import com.era7.bioinfo.microsats.nodes.ProjectNode;
import com.era7.bioinfo.microsats.nodes.SequenceNode;
import com.era7.bioinfo.microsats.relationships.MicrosatelliteFoundRel;
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

            try {

                int threshold = Integer.parseInt(args[1]);

                manager = new MicrosatellitesManager(args[2]);
                txn = manager.beginTransaction();
                NodeRetriever nodeRetriever = new NodeRetriever(manager);
                NodeIndexer nodeIndexer = new NodeIndexer(manager);

                String line = null;
                boolean first = true;
                BufferedReader reader = new BufferedReader(new FileReader(new File(args[0])));
                StringBuilder seqStBuilder = null;
                
                while ((line = reader.readLine()) != null) {
                    
                    if (line.startsWith(">")) {
                        
                        String[] columns = line.substring(1).split("\\|");
                        String projectName = columns[0];
                        String seqID = columns[1];
                        String geneSt = columns[2].split("gene=")[1];
                        int seqLength = Integer.parseInt(columns[3].split("length=")[1]);
                                                
                        
                        if (!first) {        
                            
                            //creating project node if necessary
                            ProjectNode projectNode = nodeRetriever.getProjectByName(projectName);
                            if(projectNode == null){
                                projectNode = new ProjectNode(manager.createNode());
                                projectNode.setName(projectName);
                                nodeIndexer.indexProjectByName(projectNode,txn,true);
                                txn = manager.beginTransaction();
                            }
                            //creating sequence node
                            SequenceNode sequenceNode = new SequenceNode(manager.createNode());
                            sequenceNode.setSequence(seqStBuilder.toString());
                            sequenceNode.setLength(seqLength);
                            sequenceNode.setGene(geneSt);
                            sequenceNode.setId(seqID);
                            
                            
                        }
                        
                        first = false;
                        seqStBuilder = new StringBuilder();
                        
                    } else {
                        seqStBuilder.append(line);
                    }
                }

                reader.close();


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
    
}
