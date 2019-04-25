/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package emaildownloader;

import java.io.BufferedWriter;
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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

/**
 *
 * @author nicho
 */
public class ConsiglioNazionaleNotai implements Runnable{
    
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
    private String urlToAdd;
    
    
    public ConsiglioNazionaleNotai(JTextArea txtAreaEmail, JLabel labelEmailsNumber){
        this.settings= new Settings();
        this.txtAreaEmail= txtAreaEmail;
        this.labelEmailsNumber= labelEmailsNumber;
        this.searchPageOriginalUrl= "https://www.notariato.it/it/trova-notaio?search=1&nome=&cognome=&cap=&regione_provincia=PROV_67";
        //URL Notary in Rome, this method will works with other cities too!
        System.setProperty("webdriver.chrome.driver", settings.getStringValue("driverPath"));
        this.listUsersLink= new LinkedList<String>();
        this.listUser= new LinkedList();
        this.run= true;
        this.emailNumber= 0;
        this.urlToAdd="https://www.notariato.it";
        
    }
    
    public void run(){
        start();
    }
    
    private void start(){
        String startPageStr= JOptionPane.showInputDialog("Start Page");
        try{
            Integer.valueOf(startPageStr);
            currentPageNumber= Integer.valueOf(startPageStr);
            if(currentPageNumber==0){
                currentPageNumber=1;
            }
        }catch(NumberFormatException e){
            JOptionPane.showMessageDialog(null, "Invalid input, start from page 0");
            this.currentPageNumber= 20;
        }
        this.searchPageUrl= this.searchPageOriginalUrl+ "&showpage=" + currentPageNumber;
        this.driver= new ChromeDriver();
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
        if(!this.htmlSearchpageStr.contains("class=\"btn-ajax\">")){
            this.run=false;
            return;
        }
        this.listUsersLink= new LinkedList<String>();
        String strToFind= "class=\"btn-ajax\">";
        int indexStartName= htmlSearchpageStr.indexOf(strToFind, 0);
        int indexEndName= htmlSearchpageStr.indexOf("<", indexStartName);
        int indexStartLink;
        String nameCheck;
        String tmpLink;
        while(indexStartName>0 && indexEndName>0){
            nameCheck= this.htmlSearchpageStr.substring(indexStartName+strToFind.length(), indexEndName);
            if(!nameCheck.contains("Vedi scheda")){
                indexStartLink= htmlSearchpageStr.lastIndexOf("href=\"", indexStartName);
                tmpLink= htmlSearchpageStr.substring(indexStartLink, indexStartName);
                tmpLink= tmpLink.replaceAll("href=\"", "");
                tmpLink= tmpLink.replaceAll("\"", "");
                tmpLink= this.urlToAdd+tmpLink;
                listUsersLink.add(tmpLink);
            }
            indexStartName= htmlSearchpageStr.indexOf(strToFind, indexEndName);
            indexEndName= htmlSearchpageStr.indexOf("<", indexStartName);
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
    
    private void backToSearchPage(){
        driver.get(searchPageUrl);
        String handle= driver.getWindowHandle();
        driver.switchTo().window(handle);
    }
    
    private void nextSearchPage(){
        this.currentPageNumber+=1;
        this.searchPageUrl= this.searchPageOriginalUrl+ "&showpage=" + currentPageNumber;
        driver.get(searchPageUrl);
        String handle= driver.getWindowHandle();
        driver.switchTo().window(handle);
    }
    
    private void writeFile(){
        String destPath= settings.getStringValue("destDir");
        String fileName= "consNazNotai.csv";
        destPath= destPath+"\\"+fileName;
        try {
            FileWriter fw= new FileWriter(destPath, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);
            Iterator<String> it= this.listUser.iterator();
            while(it.hasNext()){
                out.println(it.next());
            }
            this.listUser= new LinkedList();
            out.close();
        }catch (IOException ex) {
            Logger.getLogger(OrdineCommercialistiRoma.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private boolean checkStop(){
        if(settings.getBoolValue("emailStopped")){
            JOptionPane.showMessageDialog(null, "Stopped!");
            return true;
        }
        return false;
    }
    
    private void parseUsersPages(){
        String strToFindName= "Nome</span>";
        String strToFindLastName= "Cognome</span>";
        String strToFindEmail= "Email</span>";
        String strToFindAddress= "Indirizzo</span>";
        String strToFindPhone= "Telefono</span>";
        String name= ""; 
        String lastName= "";
        String email= "";
        String address= "";
        String phone= "";
        String strUser= "";
        int indexStart= 0;
        int indexEnd= 0;
        for(int i=0; i<this.arrayLinks.length; i++){
            nextUserPage(this.arrayLinks[i]);
            indexStart= this.htmlUserPageStr.indexOf(strToFindName);
            if(indexStart>0){
                indexStart= this.htmlUserPageStr.indexOf(">",indexStart+strToFindName.length());
                indexEnd= this.htmlUserPageStr.indexOf("<", indexStart);
                name= this.htmlUserPageStr.substring(indexStart, indexEnd);
                name= name.replaceAll(">", "");
                name= name.trim();
            }else{
                name="";
            }
            indexStart= this.htmlUserPageStr.indexOf(strToFindLastName);
            if(indexStart>0){
                indexStart= this.htmlUserPageStr.indexOf(">",indexStart+strToFindLastName.length());
                indexEnd= this.htmlUserPageStr.indexOf("<", indexStart);
                lastName= this.htmlUserPageStr.substring(indexStart, indexEnd);
                lastName= lastName.replaceAll(">", "");
                lastName= lastName.trim();
            }else{
                lastName="";
            }
            indexStart= this.htmlUserPageStr.indexOf(strToFindEmail);
            if(indexStart>0){
                indexStart= this.htmlUserPageStr.indexOf(">",indexStart+strToFindEmail.length());
                indexEnd= this.htmlUserPageStr.indexOf("<", indexStart);
                email= this.htmlUserPageStr.substring(indexStart, indexEnd);
                email= email.replaceAll(">", "");
                email= email.trim();
            }else{
                email="";
            }
            indexStart= this.htmlUserPageStr.indexOf(strToFindAddress);
            if(indexStart>0){
                indexStart= this.htmlUserPageStr.indexOf(">",indexStart+strToFindAddress.length());
                indexEnd= this.htmlUserPageStr.indexOf("<", indexStart);
                address= this.htmlUserPageStr.substring(indexStart, indexEnd);
                address= address.replaceAll(">", "");
                address= address.trim();
            }else{
                address="";
            }
            indexStart= this.htmlUserPageStr.indexOf(strToFindPhone);
            if(indexStart>0){
                indexStart= this.htmlUserPageStr.indexOf(">",indexStart+strToFindPhone.length());
                indexEnd= this.htmlUserPageStr.indexOf("<", indexStart);
                phone= this.htmlUserPageStr.substring(indexStart, indexEnd);
                phone= phone.replaceAll(">", "");
                phone= phone.trim();
            }else{
                phone="";
            }
            if(name.length()!=0 && lastName.length()!=0 && email.length()!=0){
                strUser= "," + lastName + "," + name + ",Notary," + email + ",Roma," + address + "," + phone + ",";
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
