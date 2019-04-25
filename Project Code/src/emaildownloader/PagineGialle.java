/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package emaildownloader;

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
import javax.swing.JTextArea;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 *
 * @author nicho
 */
public class PagineGialle implements Runnable{
    
    private String htmlSearchpageStr;
    private String htmlUserPageStr;
    private LinkedList<String> listUsersLink;
    private String searchPageUrl;
    private String searchPageOriginalUrl;
    private static WebDriver driver;
    private String[] arrayLinks;
    private LinkedList<String> listUser;
    private int currentPageNumber;
    private boolean run;
    private JTextArea txtAreaEmail;
    private JLabel labelEmailsNumber;
    private Settings settings;
    private int emailNumber;
    private String keywordSearch;
    private String city;
    private String fileCheckName;
    private String fileWriteName;
    
    public PagineGialle(JTextArea txtAreaEmail, JLabel labelEmailsNumber){
        this.settings= new Settings();
        this.txtAreaEmail= txtAreaEmail;
        this.labelEmailsNumber= labelEmailsNumber;
        this.searchPageOriginalUrl= "https://www.paginegialle.it/ricerca/";
        System.setProperty("webdriver.chrome.driver", settings.getStringValue("driverPath"));
        this.listUsersLink= new LinkedList<String>();
        this.listUser= new LinkedList();
        this.run= true;
        this.emailNumber= 0;
        this.fileCheckName= "";
        this.fileWriteName= "";
    }
    
    public void run() {
        start();
    }
    
    private void start(){
        if(settings.getBoolValue("autostartEmail") && settings.getIntValue("lastPageGialle")>=0){
            this.keywordSearch= settings.getStringValue("lastKetwordSrc");
            this.city= settings.getStringValue("lastCity");
            this.currentPageNumber= settings.getIntValue("lastPageGialle");
            this.fileWriteName= "pagineGialle_" + keywordSearch + ".csv";
            this.fileCheckName= "checkFilePg_" + keywordSearch + ".csv";
            readCheckFile();
        }else{
            keywordSearch= JOptionPane.showInputDialog("Enter keyword to search");
            while(keywordSearch.length()<=1){
                JOptionPane.showMessageDialog(null, "Input must be >=2");
                keywordSearch= JOptionPane.showInputDialog("Enter keyword to search");
            }
            keywordSearch= keywordSearch.toLowerCase();
            settings.SaveSetting("string", "lastKetwordSrc", keywordSearch);
            city= JOptionPane.showInputDialog("Enter City");
            if(city.length()<2){
                JOptionPane.showMessageDialog(null, "Invalid input, Italy set");
                city= "";
            }
            if(city.toLowerCase().equals("italia") || city.toLowerCase().equals("ita") || city.toLowerCase().equals("italy")){
                city= "";
            }
            settings.SaveSetting("string", "lastCity", city);
            
            String startPageStr= JOptionPane.showInputDialog("Start Page");
            try{
                Integer.valueOf(startPageStr);
                currentPageNumber= Integer.valueOf(startPageStr);
                if(currentPageNumber==0){
                    currentPageNumber=1;
                }
            }catch(NumberFormatException e){
                JOptionPane.showMessageDialog(null, "Invalid input, start from page 0");
                this.currentPageNumber= 1;
            }
            settings.SaveSetting("int", "lastPageGialle", Integer.toString(this.currentPageNumber));
            this.fileWriteName= "pagineGialle_" + keywordSearch + ".csv";
            this.fileCheckName= "checkFilePg_" + keywordSearch + ".csv";
        }
        if(city.length()>2){
            this.searchPageUrl= this.searchPageOriginalUrl+ keywordSearch + "/" + city + "/p-" + currentPageNumber;
        }
        else{
            this.searchPageUrl= this.searchPageOriginalUrl+ keywordSearch + "/p-" + currentPageNumber;
        }
        
        if(settings.getBoolValue("headlessChrome")){
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            this.driver= new ChromeDriver(options);
        }
        else this.driver= new ChromeDriver();
        driver.get(searchPageUrl);
        
        while(this.run){
            parseAlboNames();   
            if(checkStop()) return;
            parseUsersPages();
            if(checkStop()) return;
            writeFile();
            if(checkStop()) return;
            nextSearchPage();
            if(checkStop()) return;
        }
    }
    
    private void parseAlboNames(){
        this.htmlSearchpageStr= driver.getPageSource();
        if(!this.htmlSearchpageStr.contains("<a title=\"https://")){
            this.run=false;
            return;
        }
        this.listUsersLink= new LinkedList<String>();
        String strToFind= "<a title=\"https://";
        int indexStartName= htmlSearchpageStr.indexOf(strToFind, 0);
        int indexEndName= htmlSearchpageStr.indexOf("\"", indexStartName+strToFind.length());
        String tmpLink;
        while(indexStartName>0 && indexEndName>0){
            tmpLink= htmlSearchpageStr.substring(indexStartName, indexEndName);
            tmpLink= tmpLink.replaceAll("<a title=\"", "");
            listUsersLink.add(tmpLink);
            indexStartName= htmlSearchpageStr.indexOf(strToFind, indexEndName);
            indexEndName= htmlSearchpageStr.indexOf("\"", indexStartName+strToFind.length());
        }
        arrayLinks= new String[listUsersLink.size()];
        int i=0;
        Iterator<String> it= listUsersLink.iterator();
        while(it.hasNext()){
            arrayLinks[i]= it.next();
            i++;
        }
    }
    
    private void nextUserPage(String link){
        driver.get(link);
        String handle= driver.getWindowHandle();
        driver.switchTo().window(handle);
        this.htmlUserPageStr= driver.getPageSource();
    }
    
    private boolean checkStop(){
        if(settings.getBoolValue("emailStopped")){
            JOptionPane.showMessageDialog(null, "Stopped!");
            return true;
        }
        return false;
    }
    
    private void writeCheckFile(){
        String destPath= settings.getStringValue("destDir");
        destPath= destPath+"\\"+this.fileCheckName;
        FileWriter fw;
        try {
            fw = new FileWriter(destPath, false);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);
            out.println("Pagine Gialle - " + this.keywordSearch + " - " + this.city);
            out.println("Email Number:\t" + this.emailNumber);
            out.println("Page Number:\t" + this.currentPageNumber);
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(AlboAvvocatiRoma.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void writeFile(){
        String destPath= settings.getStringValue("destDir");
        destPath= destPath+"\\"+this.fileWriteName;
        File fileCheck= new File(destPath);
        boolean newFile= false;
        try {
            if(!fileCheck.exists()){
                newFile= true;
            }
            FileWriter fw= new FileWriter(destPath, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);
            if(newFile){
                out.println("Firm Name,Last Name,First Name,Group,Email,City,Address,Phone,Website");
            }
            Iterator<String> it= this.listUser.iterator();
            while(it.hasNext()){
                out.println(it.next());
            }
            this.listUser= new LinkedList();
            out.close();
        }catch (IOException ex) {
            Logger.getLogger(OrdineCommercialistiRoma.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        writeCheckFile();
    }
    
    private void readCheckFile(){
        String destPath= settings.getStringValue("destDir");
        destPath= destPath+"\\"+this.fileCheckName;
        File fileCheck= new File(destPath);
        String toParseStr="";
        if(fileCheck.exists()){
            try{
                FileReader fr= new FileReader(destPath);
                BufferedReader br= new BufferedReader(fr);
                br.readLine();  //Service Name
                br.readLine();  //Email Number
                toParseStr= br.readLine();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(AlboAvvocatiRoma.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(AlboAvvocatiRoma.class.getName()).log(Level.SEVERE, null, ex);
            }
            this.currentPageNumber= Integer.parseInt((toParseStr.substring(toParseStr.indexOf(":")+1, toParseStr.length())).trim());
        }
    }
    
    private void backToSearchPage(){
        driver.get(searchPageUrl);
        String handle= driver.getWindowHandle();
        driver.switchTo().window(handle);
    }
    
    private void nextSearchPage(){
        this.currentPageNumber+=1;
        this.searchPageUrl= this.searchPageOriginalUrl+ keywordSearch + "/Roma" + "/p-" + currentPageNumber;
        driver.get(searchPageUrl);
        String handle= driver.getWindowHandle();
        driver.switchTo().window(handle);
    }
    
    private void parseUsersPages(){
        String strToFindFirmName= "\"name\" :";
        String strToFindEmail= "\"email\" :";
        String strToFindAddress= "\"streetAddress\" :";
        String strToFindPhone= "\"telephone\" :";
        String strToFindCity= "addressLocality\" : ";
        String strToFindRegion= "addressRegion\" : ";
        String firmName= ""; 
        String email= "";
        String address= "";
        String phone= "";
        String strUser= "";
        String city="";
        String tmpRegion= "";
        int indexStart= 0;
        int indexEnd= 0;
        for(int i=0; i<this.arrayLinks.length; i++){
            nextUserPage(this.arrayLinks[i]);
            indexStart= this.htmlUserPageStr.indexOf(strToFindFirmName);
            if(indexStart>0){
                indexStart= this.htmlUserPageStr.indexOf("\"",indexStart+strToFindFirmName.length());
                indexEnd= this.htmlUserPageStr.indexOf(",", indexStart);
                firmName= this.htmlUserPageStr.substring(indexStart, indexEnd);
                firmName= firmName.replaceAll("\"", "");
                firmName= firmName.trim();
            }else{
                firmName="";
            }
            indexStart= this.htmlUserPageStr.indexOf(strToFindEmail);
            if(indexStart>0){
                indexStart= this.htmlUserPageStr.indexOf("\"",indexStart+strToFindEmail.length());
                indexEnd= this.htmlUserPageStr.indexOf(",", indexStart);
                email= this.htmlUserPageStr.substring(indexStart, indexEnd);
                email= email.replaceAll("\"", "");
                email= email.trim();
            }else{
                email="";
            }
            indexStart= this.htmlUserPageStr.indexOf(strToFindAddress);
            if(indexStart>0){
                indexStart= this.htmlUserPageStr.indexOf("\"",indexStart+strToFindAddress.length());
                indexEnd= this.htmlUserPageStr.indexOf(",", indexStart);
                address= this.htmlUserPageStr.substring(indexStart, indexEnd);
                address= address.replaceAll("\"", "");
                address= address.trim();
            }else{
                address="";
            }
            indexStart= this.htmlUserPageStr.indexOf(strToFindPhone);
            if(indexStart>0){
                indexStart= this.htmlUserPageStr.indexOf("\"",indexStart+strToFindPhone.length());
                indexEnd= this.htmlUserPageStr.indexOf("}", indexStart);
                phone= this.htmlUserPageStr.substring(indexStart, indexEnd);
                phone= phone.replaceAll("\"", "");
                phone= phone.replaceAll("\\+39", "");
                phone= phone.replaceAll(" ", "");
                phone= phone.trim();
            }else{
                phone="";
            }
            indexStart= this.htmlUserPageStr.indexOf(strToFindCity);
            if(indexStart>0){
                indexStart+=strToFindCity.length();
                indexEnd= this.htmlUserPageStr.indexOf(",", indexStart);
                city= this.htmlUserPageStr.substring(indexStart, indexEnd);
                city= city.replaceAll("\"", "");
                city= city.trim();
                indexStart= this.htmlUserPageStr.indexOf(strToFindRegion);
                if(indexStart>0){
                    indexEnd= this.htmlUserPageStr.indexOf(",", indexStart);
                    indexStart= indexStart+strToFindRegion.length();
                    tmpRegion= this.htmlUserPageStr.substring(indexStart, indexEnd);
                    tmpRegion= tmpRegion.replaceAll("\"", "");
                    tmpRegion= tmpRegion.trim();
                    city= city + " " + tmpRegion;
                }
            }else{
                city="";
            }
            if(firmName.length()!=0 && email.length()!=0){
                strUser= firmName + "," + "," + "," + keywordSearch + "," + email + "," + city + "," + address + "," + phone + ",";
                strUser= strUser.toLowerCase();
                listUser.add(strUser);
                this.emailNumber++;
                this.labelEmailsNumber.setText(Integer.toString(this.emailNumber));
                this.txtAreaEmail.append(strUser+"\n");
                //System.out.println(strUser);
            }

            backToSearchPage();
        }
    }
    
}
