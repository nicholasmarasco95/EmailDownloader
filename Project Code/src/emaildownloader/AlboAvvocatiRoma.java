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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.Element;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 *
 * @author nicho
 */
public class AlboAvvocatiRoma implements Runnable{

    private String htmlSearchpageStr;
    private String htmlUserPageStr;
    private LinkedList<String> listUsersId;
    private String searchPageUrl;
    private String searchPageOriginalUrl;
    private static WebDriver driver;
    private String[] arrayLinks;
    private LinkedList<String> listUser;
    private int startPageNumber;
    private boolean run;
    private JTextArea txtAreaEmail;
    private JLabel labelEmailsNumber;
    private Settings settings;
    private int emailNumber;
    private WebElement we;
    private String userOriginalURL;
    private ArrayList<String> tabs;
    private int pageNumber;
    private LinkedList<String> sessionEmailList;
    private String fileCheckName;
    private String fileWriteName;
    
    public AlboAvvocatiRoma(JTextArea txtAreaEmail, JLabel labelEmailsNumber){
        this.settings= new Settings();
        this.txtAreaEmail= txtAreaEmail;
        this.labelEmailsNumber= labelEmailsNumber;
        this.searchPageOriginalUrl= "https://sfera.sferabit.com/servizi/alboonline/index.php?id=1118";
        System.setProperty("webdriver.chrome.driver", settings.getStringValue("driverPath"));
        this.listUsersId= new LinkedList<String>();
        this.listUser= new LinkedList();
        this.run= true;
        this.emailNumber= 0;
        this.userOriginalURL= "https://sfera.sferabit.com/servizi/alboonline/stampaPersona.php?id=1118&idPratica=";
        this.pageNumber= 1;
        this.sessionEmailList= new LinkedList<String>();
        this.fileCheckName= "checkFileOrdineAvvocatiRoma.csv";
        this.fileWriteName= "ordineAvvocatiRoma.csv";
    }
    
    
    public void run() {
        start();
    }
    
    private void start(){
        if(!settings.getBoolValue("autostartEmail")){
            String startPageStr= JOptionPane.showInputDialog("Start Page");
            try{
                Integer.valueOf(startPageStr);
                startPageNumber= Integer.valueOf(startPageStr);
                if(startPageNumber==0){
                    startPageNumber=1;
                }
            }catch(NumberFormatException e){
                JOptionPane.showMessageDialog(null, "Invalid input, start from page 0");
                this.startPageNumber= 1;
            }
        }else{
            readCheckFile();
        }
        this.searchPageUrl= this.searchPageOriginalUrl;
        
        if(settings.getBoolValue("headlessChrome")){
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            this.driver= new ChromeDriver(options);
        }
        else this.driver= new ChromeDriver();
        driver.get(searchPageUrl);
        
        we= driver.findElement(By.id("filtroCitta"));
        we.sendKeys("Roma");
        driver.findElement(By.id("cerca")).click();
        
        if(this.startPageNumber>1){
            for(int i=1; i<this.startPageNumber; i++){
                boolean click= clickAvanti(true);
                while(!click){
                    click= clickAvanti(true);
                }
            }
        }
        
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
    
    private void checkSearchPage(){
        System.out.println("Call CheckSearch Page");
        String strToFind= "apriModaleAlboOnline('";
        we= driver.findElement(By.id("risultatoRicerca"));
        this.htmlSearchpageStr= we.getAttribute("innerHTML");
        if(!this.htmlSearchpageStr.contains(strToFind)){
            we= driver.findElement(By.id("filtroCitta"));
            we.clear();
            we.sendKeys("Roma");
            driver.findElement(By.id("cerca")).click();
            for(int i=0; i<this.pageNumber; i++){
                boolean click= clickAvanti(false);
                while(!click){
                    click= clickAvanti(false);
                }
            }
        }
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
            this.startPageNumber= Integer.parseInt((toParseStr.substring(toParseStr.indexOf(":")+1, toParseStr.length())).trim());
        }
    }
    
    private void writeCheckFile(){
        String destPath= settings.getStringValue("destDir");
        destPath= destPath+"\\"+this.fileCheckName;
        FileWriter fw;
        try {
            fw = new FileWriter(destPath, false);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);
            out.println("Ordine Avvocati di Roma");
            out.println("Email Number:\t" + this.emailNumber);
            out.println("Page Number:\t" + this.pageNumber);
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(AlboAvvocatiRoma.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void writeFile(){
        String destPath= settings.getStringValue("destDir");
        destPath= destPath+"\\"+fileWriteName;
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
    
    private void parseAlboNames(){
        int wait=500;
        int count=0;
        String strToFind= "apriModaleAlboOnline('";
        we= driver.findElement(By.id("risultatoRicerca"));
        this.htmlSearchpageStr= we.getAttribute("innerHTML");
        while(!this.htmlSearchpageStr.contains(strToFind)){
            we= driver.findElement(By.id("risultatoRicerca"));
            this.htmlSearchpageStr= we.getAttribute("innerHTML");
            try {
                Thread.sleep(wait);
            } catch (InterruptedException ex) {
                Logger.getLogger(AlboAvvocatiRoma.class.getName()).log(Level.SEVERE, null, ex);
            }
            wait+=500;
            count++;
            if(count>30){
                this.run=false;
                JOptionPane.showMessageDialog(null, "Timeout");
                System.exit(0);
            }
        }
        this.listUsersId= new LinkedList<String>();
        int indexStartName= htmlSearchpageStr.indexOf(strToFind, 0);
        int indexEndName= htmlSearchpageStr.indexOf("'", indexStartName+strToFind.length());
        String tmpLink;
        while(indexStartName>0 && indexEndName>0){
            tmpLink= htmlSearchpageStr.substring(indexStartName+(strToFind.length()), indexEndName);
            listUsersId.add(tmpLink);
            indexStartName= htmlSearchpageStr.indexOf(strToFind, indexEndName);
            indexEndName= htmlSearchpageStr.indexOf("'", indexStartName+strToFind.length());
        }
        arrayLinks= new String[listUsersId.size()];
        int i=0;
        Iterator<String> it= listUsersId.iterator();
        while(it.hasNext()){
            arrayLinks[i]= it.next();
            i++;
        }
    }
    
    private boolean checkStop(){
        if(settings.getBoolValue("emailStopped")){
            JOptionPane.showMessageDialog(null, "Stopped!");
            return true;
        }
        return false;
    }
    
    private boolean clickAvanti(boolean newPage){
        try{
            Thread.sleep(1000);
            driver.findElement(By.xpath("//*[text() = 'Avanti >']")).click();
        }catch(org.openqa.selenium.NoSuchElementException e){
            return false;
        } catch (InterruptedException ex) {
            Logger.getLogger(AlboAvvocatiRoma.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(newPage) this.pageNumber++;
        return true;
    }
    
    private void nextSearchPage(){
        checkSearchPage();
        
        boolean click= clickAvanti(true);
        while(!click){
            click= clickAvanti(true);
        }
        
        String handle= driver.getWindowHandle();
        driver.switchTo().window(handle);
    }
    
    private boolean loadNextUserPage(String url){
        ((JavascriptExecutor)driver).executeScript("window.open();");
        tabs = new ArrayList<String> (driver.getWindowHandles());
        driver.switchTo().window(tabs.get(1));
        
        driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
        try{
            driver.get(url);
        }catch (TimeoutException e) {
            driver.close();
            driver.switchTo().window(tabs.get(0));
            return false;
        }
        return true;
    }
    
    private void nextUserPage(String userId){
        boolean checkLoad= loadNextUserPage(this.userOriginalURL + userId);
        while(!checkLoad){
            checkLoad= loadNextUserPage(this.userOriginalURL + userId);
        }
        this.htmlUserPageStr= driver.getPageSource();
    }
    
    private void backToSearchPage(){
        driver.close();
        driver.switchTo().window(tabs.get(0));
        driver.get(searchPageUrl);
    }
    
    private void parseUsersPages(){
        String strToFindFirmName= "src=\"https://sfera.sferabit.com/img/stampa.gif\" /></a>";
        String strToFindEmail= "mailto:";
        String strToFindAddress= "Indirizzo";
        String strToFindPhone= "Cellulare:";
        String name= ""; 
        String last= ""; 
        String email= "";
        String address= "";
        String phone= "";
        String strUser= "";
        String tmpNameLast= "";
        int indexStart= 0;
        int indexEnd= 0;
        int indexSpace=0;
        for(int i=0; i<this.arrayLinks.length; i++){
            nextUserPage(this.arrayLinks[i]);
            indexStart= this.htmlUserPageStr.indexOf(strToFindFirmName);
            if(indexStart>0){
                indexStart= this.htmlUserPageStr.indexOf("<b>",indexStart+strToFindFirmName.length());
                indexStart= this.htmlUserPageStr.indexOf(" ",indexStart);
                indexEnd= this.htmlUserPageStr.indexOf("<",indexStart);
                if(indexEnd>0 && indexEnd>indexStart){
                    tmpNameLast= this.htmlUserPageStr.substring(indexStart, indexEnd);
                    tmpNameLast= tmpNameLast.trim();
                    indexSpace= tmpNameLast.indexOf(" ");
                    if(tmpNameLast.indexOf(" ", indexSpace+1)>0){
                        indexSpace= tmpNameLast.indexOf(" ", indexSpace+1);
                    }
                    if(indexSpace>0){
                        last= tmpNameLast.substring(0, indexSpace);
                        name= tmpNameLast.substring(indexSpace, tmpNameLast.length());
                        name= name.trim();
                        last= last.trim();
                    }else {name=""; last="";}
                }else {name=""; last="";}
            }else{
                name="";
                last="";
            }
            indexStart= this.htmlUserPageStr.indexOf(strToFindEmail);
            if(indexStart>0){
                indexEnd= this.htmlUserPageStr.indexOf("\"",indexStart+strToFindEmail.length());
                if(indexEnd>0 && indexEnd>indexStart){
                    email= this.htmlUserPageStr.substring(indexStart+strToFindEmail.length(), indexEnd);
                    email= email.trim();
                } else email= "";                
            }else{
                email="";
            }
            indexStart= this.htmlUserPageStr.indexOf(strToFindAddress);
            if(indexStart>0){
                indexStart= this.htmlUserPageStr.indexOf("<br />",indexStart+strToFindAddress.length());
                indexEnd= this.htmlUserPageStr.indexOf("<br />",indexStart+1);
                address= this.htmlUserPageStr.substring(indexStart, indexEnd);
                indexEnd= address.lastIndexOf("-");
                if(indexEnd>0 && indexEnd>indexStart){
                    address= address.substring(0, indexEnd);
                    address= address.replaceAll("<br />", "");
                    address= address.trim();
                    while(address.indexOf(" ")==1){
                        address= address.substring(2, address.length());
                    }
                    address= address.replaceAll(",", "");
                }else address="";
            }else{
                address="";
            }
            indexStart= this.htmlUserPageStr.indexOf(strToFindPhone);
            if(indexStart>0){
                indexStart+= strToFindPhone.length();
                indexEnd= this.htmlUserPageStr.indexOf("<", indexStart);
                if(indexEnd>0 && indexEnd>indexStart){
                    phone= this.htmlUserPageStr.substring(indexStart, indexEnd);
                    phone= phone.trim();
                }else phone="";
            }else{
                strToFindPhone= "Tel.:";
                indexStart= this.htmlUserPageStr.indexOf(strToFindPhone);
                if(indexStart>0){
                    indexStart+= strToFindPhone.length();
                    indexEnd= this.htmlUserPageStr.indexOf("<", indexStart);
                    if(indexEnd>0 && indexEnd>indexStart){
                        phone= this.htmlUserPageStr.substring(indexStart, indexEnd);
                        phone= phone.trim();
                    }else phone= "";
                }else{
                    phone="";
                }
            }
            if(name.length()!=0 && last.length()!=0 && email.length()!=0){
                if(!this.sessionEmailList.contains(email)){
                    strUser= "," + last + "," + name + "," + "lawyer," + email + "," + "roma," + address + "," + phone + ",";
                    strUser= strUser.toLowerCase();
                    listUser.add(strUser);
                    this.sessionEmailList.add(email);
                    this.emailNumber++;
                    this.labelEmailsNumber.setText(Integer.toString(this.emailNumber));
                    this.txtAreaEmail.append(strUser+"\n");
                }
                System.out.println(strUser);
            }
            
            backToSearchPage();
        }
        
        //this.pageNumber++;
    }
    
}
