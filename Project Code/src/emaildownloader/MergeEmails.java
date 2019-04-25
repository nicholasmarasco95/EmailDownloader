/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package emaildownloader;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 *
 * @author nicho
 */
public class MergeEmails implements Runnable{

    private String completeListPath;
    private String toMergeListPath;
    private BufferedReader brComList;
    private BufferedReader brToMerList;
    private LinkedList<String> emailsToMergeList;
    private PrintWriter pwComList;
    private boolean remove;
    private FileReader frComList;
    private LinkedList<String> listCompleteFile;
    private FileWriter fwComList;
    private BufferedWriter bwComList;
    private boolean toMergeMailChimp;
    private JLabel labelCountCom;
    private JLabel labelCountMer;
    private JLabel labelCounter;
    private int compCount;
    private int toMerCount;
    private int counter;
    private boolean cleanOnly;
    private boolean compListMailChimp;
    private String firstLineMailChimp;
    
    
    public MergeEmails(String completeListPath, String toMergeListPath, boolean remove, boolean toMergeMailChimp, boolean compListMailChimp,  JLabel labelCountCom, JLabel labelCountMer, JLabel labelCounter, boolean cleanOnly){
        this.completeListPath= completeListPath;
        this.toMergeListPath= toMergeListPath;
        this.remove= remove; //true if unsubscribed emails
        this.toMergeMailChimp= toMergeMailChimp;
        this.labelCountCom= labelCountCom;
        this.labelCountMer= labelCountMer;
        this.labelCounter= labelCounter;
        this.compCount= 0;
        this.toMerCount= 0;
        this.counter= 0;
        this.labelCountCom.setForeground(Color.blue);
        this.labelCountMer.setForeground(Color.blue);
        this.labelCounter.setForeground(Color.blue);
        this.cleanOnly= cleanOnly;
        this.compListMailChimp= compListMailChimp;
    }
    
    public void run() {
        if(!cleanOnly){
            openFiles();
            if(!this.compListMailChimp) checkMergeCompMyCSV();
            else checkMergeCompMailChimp();
            mergeCompMyCSV();
            closeFiles();            
        }
        else{
            openFilesCleanOnly();
            cleanFileComp();
            writeComplList();
            closeFilesOpenOnly();
        }
        
    }
    
    private void openFilesCleanOnly(){
        File fileCheck= new File(completeListPath);
        if(!fileCheck.exists()){
            JOptionPane.showMessageDialog(null, "Complete List doesn't exists");
            System.exit(0);
        }
        try{
            frComList= new FileReader(completeListPath);
            fwComList= new FileWriter(completeListPath, true);
            this.brComList= new BufferedReader(frComList);
            bwComList = new BufferedWriter(fwComList);
            this.pwComList = new PrintWriter(bwComList);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MergeEmails.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MergeEmails.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void closeFilesOpenOnly(){
        try {
            this.brComList.close();
            this.pwComList.close();
        } catch (IOException ex) {
            Logger.getLogger(MergeEmails.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void openFiles(){
        File fileCheck= new File(completeListPath);
        if(!fileCheck.exists()){
            JOptionPane.showMessageDialog(null, "Complete List File doesn't exists");
            System.exit(0);
        }
        fileCheck= new File(toMergeListPath);
        if(!fileCheck.exists()){
            JOptionPane.showMessageDialog(null, "To Merge List File doesn't exists");
            System.exit(0);
        }
        try{
            frComList= new FileReader(completeListPath);
            fwComList= new FileWriter(completeListPath, true);
            this.brComList= new BufferedReader(frComList);
            bwComList = new BufferedWriter(fwComList);
            this.pwComList = new PrintWriter(bwComList);
            FileReader frToMerList= new FileReader(toMergeListPath);
            this.brToMerList= new BufferedReader(frToMerList);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MergeEmails.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MergeEmails.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void checkMergeCompMailChimp(){
        String completeListStr= "";
        String tmpStr;
        String tmpEmail;
        emailsToMergeList= new LinkedList();
        try {
            while((tmpStr= this.brComList.readLine())!=null && tmpStr.length()>1){
                if(!tmpStr.toLowerCase().contains("email address")){
                    completeListStr+= " " + tmpStr;                    
                }
            }
            while((tmpStr=this.brToMerList.readLine())!=null && tmpStr.length()>1){
                    if(!tmpStr.toLowerCase().contains("email address")){
                        tmpEmail= tmpStr.substring(0, tmpStr.indexOf(",", 1)).toLowerCase();
                        if(this.remove && completeListStr.contains(tmpEmail)){
                            emailsToMergeList.add(tmpEmail);
                            toMerCount+=1;
                            this.labelCountMer.setText(Integer.toString(toMerCount));
                        }
                        if(!this.remove && !completeListStr.contains(tmpEmail)){
                            emailsToMergeList.add(tmpStr.toLowerCase());
                            toMerCount+=1;
                            this.labelCountMer.setText(Integer.toString(toMerCount));
                        }
                    }
            }
        } catch (IOException ex) {
            Logger.getLogger(MergeEmails.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void checkMergeCompMyCSV(){
        String completeListStr= "";
        String tmpStr;
        String tmpEmail;
        emailsToMergeList= new LinkedList();
        try {
            while((tmpStr= this.brComList.readLine())!=null){
                if(!tmpStr.toLowerCase().contains("firm name")){
                    completeListStr+= tmpStr;                    
                }
            }
            if(!this.toMergeMailChimp){
                tmpStr="";
                while((tmpStr=this.brToMerList.readLine())!=null && tmpStr.length()>1){
                    if(!tmpStr.toLowerCase().contains("firm name") && tmpStr.contains("@")){
                        String[] arrayLine = tmpStr.split(",", -1);
                        tmpEmail= arrayLine[4];
                        if(!this.remove && !completeListStr.contains(tmpEmail)){
                            emailsToMergeList.add(tmpStr.toLowerCase());
                            toMerCount+=1;
                        }
                        if(this.remove && completeListStr.contains(tmpEmail)){
                            emailsToMergeList.add(tmpEmail);
                            toMerCount+=1;
                        }
                        this.labelCountMer.setText(Integer.toString(toMerCount));
                    }
                }
            }
            else{
                while((tmpStr=this.brToMerList.readLine())!=null && tmpStr.length()>1){
                    if(!tmpStr.toLowerCase().contains("email address")){
                        tmpEmail= tmpStr.substring(0, tmpStr.indexOf(",", 1));
                        if(this.remove && completeListStr.contains(tmpEmail)){
                            emailsToMergeList.add(tmpEmail);
                            toMerCount+=1;
                            this.labelCountMer.setText(Integer.toString(toMerCount));
                        }
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MergeEmails.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void closeFiles(){
        try {
            //this.brComList.close();
            this.pwComList.close();
            this.brToMerList.close();
        } catch (IOException ex) {
            Logger.getLogger(MergeEmails.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void readComplList(){
        try{
            this.frComList= new FileReader(completeListPath);
            this.brComList= new BufferedReader(frComList);
            this.listCompleteFile= new LinkedList();
            String tmpStr;
            while((tmpStr=this.brComList.readLine())!=null && tmpStr.length()>1){
                if(!this.compListMailChimp && !tmpStr.toLowerCase().contains("firm name")){
                    this.listCompleteFile.add(tmpStr);
                    this.compCount+=1;
                    this.labelCountCom.setText(Integer.toString(compCount));
                }
                if(this.compListMailChimp && !tmpStr.toLowerCase().contains("email address")){
                    this.listCompleteFile.add(tmpStr);
                    this.compCount+=1;
                    this.labelCountCom.setText(Integer.toString(compCount));
                }
                if(this.compListMailChimp && tmpStr.toLowerCase().contains("email address")){
                    this.firstLineMailChimp= tmpStr.toLowerCase();
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MergeEmails.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void writeComplList(){
        try {
            fwComList= new FileWriter(completeListPath, false);
            bwComList = new BufferedWriter(fwComList);
            this.pwComList = new PrintWriter(bwComList);
            String firstLine;
            if(!this.compListMailChimp) firstLine= "Firm Name,First Name,Last Name,Group,Email,City,Address,Phone,Website";
            else firstLine= this.firstLineMailChimp;
            pwComList.println(firstLine.toLowerCase());
            //System.out.println("CompleteList Dims WRITE: " + this.completeListPath.length());
            Iterator<String> listCompleteIt= this.listCompleteFile.iterator();
            while(listCompleteIt.hasNext()){
                pwComList.println(listCompleteIt.next().toLowerCase());
            }
        } catch (IOException ex) {
            Logger.getLogger(MergeEmails.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void cleanFileComp(){
        if(!this.cleanOnly) closeFiles();
        readComplList();
        LinkedList<String> listCompleteToClean= (LinkedList<String>) this.listCompleteFile.clone();
        this.listCompleteFile= new LinkedList<String>();
        Iterator<String> listToCleanIt= listCompleteToClean.iterator();
        String tmpEmailStr;
        String tmpEmailEx;
        String tmpNewEmails;
        boolean exists;
        while(listToCleanIt.hasNext()){
            exists= false;
            tmpEmailStr= listToCleanIt.next();
            if(!this.compListMailChimp){
                String[] arrayLine = tmpEmailStr.split(",", -1);
                tmpEmailEx= arrayLine[4];
                tmpEmailEx= tmpEmailEx.trim();
            }
            else{
                tmpEmailEx= tmpEmailStr.substring(0, tmpEmailStr.indexOf(",", 1)).toLowerCase();
            }
            if(tmpEmailEx.contains("@")){
                Iterator<String> lctcIt= this.listCompleteFile.iterator();
                while(lctcIt.hasNext()){
                   tmpNewEmails= lctcIt.next();
                   if(tmpNewEmails.contains(tmpEmailEx)){
                       exists= true;
                       break;
                   }
                }
                if(!exists){
                    this.listCompleteFile.add(tmpEmailStr);
                }
            }
        }
    }
        
    private void mergeCompMyCSV(){
        if(this.emailsToMergeList.size()>0){
            Iterator<String> emailIt= this.emailsToMergeList.iterator();
            String tmpStrMerge;
            String tmpStrComplete;
            if(this.remove){
                readComplList();
                while(emailIt.hasNext()){
                    tmpStrMerge= emailIt.next();
                    Iterator<String> listCompleteIt= this.listCompleteFile.iterator();
                    while(listCompleteIt.hasNext()){
                        tmpStrComplete= listCompleteIt.next();
                        if(tmpStrComplete.contains(tmpStrMerge)){
                            this.listCompleteFile.remove(tmpStrComplete);
                            this.counter+=1;
                            this.labelCounter.setText(Integer.toString(counter));
                            break;
                        }
                    }
                }
                writeComplList();
            }
            else{
                this.labelCountCom.setText("---");
                while(emailIt.hasNext()){
                    this.pwComList.println(emailIt.next());
                    this.counter+=1;
                    this.labelCounter.setText(Integer.toString(counter));
                }
            }
        }
        JOptionPane.showMessageDialog(null, "Merge Completed!");
    }
    
    
    
}
