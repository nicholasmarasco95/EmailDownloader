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
public class EmailExtractor implements Runnable{
    
    private LinkedList<String> listCompleteList;
    private LinkedList<String> listUnsubList;
    private LinkedList<String> listSentList;
    private LinkedList<String> listDestList;
    private String destPath;
    private String completeListPath;
    private String unsubListPath;
    private String sentListPath;
    private boolean unsub;
    private boolean sent;
    private FileReader frComList;
    private BufferedReader brComList;
    private BufferedReader brUnsubList;
    private BufferedReader brSentList;
    private FileWriter fwDestList;
    private BufferedWriter bwDestList;
    private PrintWriter pwDestList;
    private int emailsNumber;
    private String fileName;
    private JLabel counterLabel;
    private int emailCounter;
    private String city;
    private String group;
    private boolean cityCheck;
    private boolean groupCheck;
    private boolean sentMyCSV;
    private boolean unsubMyCSV;
    
    public EmailExtractor(String completeListPath, String unsubListPath, String sentListPath, String destPath, int emailsNumber, JLabel counterLabel, String city, String group){
        this.listCompleteList= new LinkedList<String>();
        this.listUnsubList= new LinkedList<String>();
        this.listSentList= new LinkedList<String>();
        this.listDestList= new LinkedList<String>();
        this.destPath= destPath;
        this.completeListPath= completeListPath;
        this.unsubListPath= unsubListPath;
        this.sentListPath= sentListPath;
        this.emailsNumber= emailsNumber;
        this.fileName= "emailExtract.csv";
        this.counterLabel= counterLabel;
        this.city= city;
        this.group= group;
        this.sentMyCSV= false;
        this.unsubMyCSV= false;
    }

    @Override
    public void run() {
        start();
    }
    
    private void start(){
        if(this.unsubListPath.length()<2) this.unsub= false;
        else this.unsub= true;
        if(this.sentListPath.length()<2) this.sent= false;
        else this.sent= true;
        this.counterLabel.setForeground(Color.blue);
        this.counterLabel.setText("000");
        if(this.city.length()<2) this.cityCheck= false;
        else this.cityCheck= true;
        if(this.group.length()<2) this.groupCheck= false;
        else this.groupCheck= true;
        
        openFiles();
        readLists();
        selectEmailsOnly();
        extract();
        writeFile();
        closeFiles();
        
        JOptionPane.showMessageDialog(null, "Extraction Completed");
    }
    
    private void extract(){
        String tmpLine;
        String tmpEmail;
        String tmpCity;
        String tmpGroup;
        boolean exists;
        boolean cityTmpCheck= true;
        boolean groupTmpCheck= true;
        this.emailCounter= 0;
        Iterator<String> compIt= this.listCompleteList.iterator();
        for(int i=0; i<emailsNumber; i++){
            exists= false;
            if(compIt.hasNext()){
                tmpLine= compIt.next();
                String[] arrayLine = tmpLine.split(",", -1);
                //System.out.println("emailArray: " + arrayLine[4]);
                tmpEmail= arrayLine[4];
                if(unsub && this.listUnsubList.contains(tmpEmail)) exists= true;
                if(!exists && sent && this.listSentList.contains(tmpEmail)) exists= true;
                if(!exists && this.cityCheck && arrayLine.length>5){
                    cityTmpCheck= false;
                    System.out.println("array Length: " + arrayLine.length);
                    tmpCity= arrayLine[5];
                    if(tmpCity.toLowerCase().trim().equals(this.city)) cityTmpCheck= true;
                }
                if(!exists && this.groupCheck && arrayLine.length>3){
                    groupTmpCheck= false;
                    tmpGroup= arrayLine[3];
                    if(tmpGroup.toLowerCase().equals(this.group)) groupTmpCheck= true;
                }
                if(!exists && cityTmpCheck && groupTmpCheck){
                    this.listDestList.add(tmpLine);
                    this.emailCounter++;
                    this.counterLabel.setText(Integer.toString(this.emailCounter));
                }
                else i--;
            }
            else break;
        }
    }
    
    private void writeFile(){
        String firstLine= "Firm Name,First Name,Last Name,Group,Email,City,Address,Phone,Website";
        pwDestList.println(firstLine.toLowerCase());
        Iterator<String> listDestIt= this.listDestList.iterator();
        while(listDestIt.hasNext()){
            pwDestList.println(listDestIt.next().toLowerCase());
        }
    }
    
    private void selectEmailsOnly(){
        if(unsub){
            LinkedList<String> tmpUnsub= (LinkedList<String>) this.listUnsubList.clone();
            Iterator<String> unsubIt= tmpUnsub.iterator();
            this.listUnsubList= new LinkedList<String>();
            String tmpStr;
            if(!this.unsubMyCSV){
                while(unsubIt.hasNext()){
                    tmpStr= unsubIt.next();
                    this.listUnsubList.add(tmpStr.substring(0, tmpStr.indexOf(",")));
                }
            }
            else{
                while(unsubIt.hasNext()){
                    tmpStr= unsubIt.next();
                    String[] arrayLine = tmpStr.split(",", -1);
                    this.listUnsubList.add(arrayLine[4]);
                }
            }
        }
        if(sent){
            LinkedList<String> tmpSent= (LinkedList<String>) this.listSentList.clone();
            Iterator<String> sentIt= tmpSent.iterator();
            this.listSentList= new LinkedList<String>();
            String tmpStr;
            if(!this.sentMyCSV){
                while(sentIt.hasNext()){
                    tmpStr= sentIt.next();
                    this.listSentList.add(tmpStr.substring(0, tmpStr.indexOf(",")));
                }
            }
            else{
                while(sentIt.hasNext()){
                    tmpStr= sentIt.next();
                    String[] arrayLine = tmpStr.split(",", -1);
                    this.listSentList.add(arrayLine[4]);
                }
            }
        }
    }
    
    private void closeFiles(){
        try {
            this.brComList.close();
            //this.bwDestList.close();
            this.pwDestList.close();
            if(unsub) this.brUnsubList.close();
            if(sent) this.brSentList.close();
        } catch (IOException ex) {
            Logger.getLogger(EmailExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void openFiles(){
        File fileCheck= new File(completeListPath);
        if(!fileCheck.exists()){
            JOptionPane.showMessageDialog(null, "Complete List doesn't exists");
            System.exit(0);
        }
        fileCheck= new File(unsubListPath);
        if(unsub && !fileCheck.exists()){
            JOptionPane.showMessageDialog(null, "Unsub File doesn't exists");
            System.exit(0);
        }
        fileCheck= new File(sentListPath);
        if(sent && !fileCheck.exists()){
            JOptionPane.showMessageDialog(null, "Sent File doesn't exists");
            System.exit(0);
        }
        try{
            frComList= new FileReader(completeListPath);
            this.brComList= new BufferedReader(frComList);
            if(unsub){
                FileReader frUnsubList= new FileReader(unsubListPath);
                this.brUnsubList= new BufferedReader(frUnsubList);
            }
            if(sent){
                FileReader frSentList= new FileReader(sentListPath);
                this.brSentList= new BufferedReader(frSentList);                
            }
            fwDestList= new FileWriter(destPath + "\\" + fileName , false);
            bwDestList = new BufferedWriter(fwDestList);
            this.pwDestList = new PrintWriter(bwDestList);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MergeEmails.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MergeEmails.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void readLists(){
        try{
            String tmpStr;
            while((tmpStr=this.brComList.readLine())!=null && tmpStr.length()>1){
                if(!tmpStr.toLowerCase().contains("firm name")){
                    this.listCompleteList.add(tmpStr);
                }
            }
            tmpStr= "";
            if(this.unsub){
                tmpStr=this.brUnsubList.readLine().toLowerCase();
                if(tmpStr.contains("firm name")) this.unsubMyCSV= true;
                else if(tmpStr.contains("email address")) this.unsubMyCSV= false;
                else{
                    JOptionPane.showMessageDialog(null, "Unsub File not recognized", "Fatal Error", JOptionPane.ERROR_MESSAGE);
                    JOptionPane.showMessageDialog(null, "Closing App", "Info", JOptionPane.INFORMATION_MESSAGE);
                    System.exit(0);
                }
                while((tmpStr=this.brUnsubList.readLine())!=null && tmpStr.length()>1){
                if(!tmpStr.toLowerCase().contains("firm name") && !tmpStr.toLowerCase().contains("email address")){
                    this.listUnsubList.add(tmpStr);
                    }
                }
            }
            if(this.sent){
                tmpStr=this.brSentList.readLine().toLowerCase();
                if(tmpStr.contains("firm name")) this.sentMyCSV= true;
                else if(tmpStr.contains("email address")) this.sentMyCSV= false;
                else{
                    JOptionPane.showMessageDialog(null, "Sent File not recognized", "Fatal Error", JOptionPane.ERROR_MESSAGE);
                    JOptionPane.showMessageDialog(null, "Closing App", "Info", JOptionPane.INFORMATION_MESSAGE);
                    System.exit(0);
                }
                while((tmpStr=this.brSentList.readLine())!=null && tmpStr.length()>1){
                if(!tmpStr.toLowerCase().contains("firm name") && !tmpStr.toLowerCase().contains("email address")){
                    this.listSentList.add(tmpStr);
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MergeEmails.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
