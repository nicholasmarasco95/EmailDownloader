/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package emaildownloader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
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
public class OrdineCommercialistiRoma implements Runnable{
    
    private String htmlSearchpageStr;
    private String htmlUserPageStr;
    private LinkedList<String> listSubArgument;
    private String searchPageUrl;
    private String searchPageOriginalUrl;
    private static WebDriver driver;
    private String[] arrayParams;
    private LinkedList<String> listUser;
    private int currentPageNumber;
    private boolean run;
    private Settings settings;
    private JTextArea txtAreaEmail;
    private JLabel labelEmailsNumber;
    private int emailNumber;
    
    
    public OrdineCommercialistiRoma(JTextArea txtAreaEmail, JLabel labelEmailsNumber){
        this.settings= new Settings();
        this.txtAreaEmail= txtAreaEmail;
        this.labelEmailsNumber= labelEmailsNumber;
        this.searchPageOriginalUrl= "https://www.odcec.roma.it/index.php?option=com_wbmalbo&elenco=completo&chiavi[qualealbo]=-1&Itemid=64";
        //System.setProperty("webdriver.chrome.driver", "C:\\Users\\nicho\\Desktop\\chromedriver_win32\\chromedriver.exe");
        System.setProperty("webdriver.chrome.driver", settings.getStringValue("driverPath"));
        this.listSubArgument= new LinkedList<String>();
        this.listUser= new LinkedList();
        this.run= true;
        this.emailNumber= 0;
    }
    
    public void run() {
        start();
    }
    
    private void start(){
        String startPageStr= JOptionPane.showInputDialog("Start Page");
        try{
            Integer.valueOf(startPageStr);
            currentPageNumber= Integer.valueOf(startPageStr);
            if(currentPageNumber!=0){
                currentPageNumber*=20;
                currentPageNumber-=20;
            }
            System.out.println(currentPageNumber);
        }catch(NumberFormatException e){
            JOptionPane.showMessageDialog(null, "Invalid input, start from page 0");
            this.currentPageNumber= 20;
        }
        this.searchPageUrl= this.searchPageOriginalUrl+ "&limitstart=" + currentPageNumber;
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
        if(this.htmlSearchpageStr.contains("Nessun nominativo soddisfa")){
            this.run=false;
            return;
        }
        this.listSubArgument= new LinkedList<String>();
        String strToFind= "submitform(";
        int indexFun= htmlSearchpageStr.indexOf(strToFind, 0);
        int indexBracket= htmlSearchpageStr.indexOf(")", indexFun);
        String subArgumentStr;
        int subArgumentInt;
        do{
            subArgumentStr= this.htmlSearchpageStr.substring(indexFun+strToFind.length(), indexBracket);
            try{
                subArgumentInt= Integer.valueOf(subArgumentStr);
            }catch(NumberFormatException e){
                subArgumentInt= -1;
            }
            if(subArgumentInt>0){
                listSubArgument.add(subArgumentStr);
            }
            indexFun= htmlSearchpageStr.indexOf(strToFind, indexBracket);
            indexBracket= htmlSearchpageStr.indexOf(")", indexFun);
        }while(indexFun>0 && indexBracket>0);
        
        arrayParams= new String[listSubArgument.size()];
        int i=0;
        Iterator<String> it= listSubArgument.iterator();
        while(it.hasNext()){
            arrayParams[i]= it.next();
            i++;
        }
    }
    
    private void nextUserPage(String arg){
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("submitform(arguments[0])", arg);
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
        this.currentPageNumber+=20;
        this.searchPageUrl= this.searchPageOriginalUrl+ "&limitstart=" + currentPageNumber;
        driver.get(searchPageUrl);
        String handle= driver.getWindowHandle();
        driver.switchTo().window(handle);
    }
    
    private void writeFile(){
        String destPath= settings.getStringValue("destDir");
        String fileName= "commRomaEmails.csv";
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
        String strToFindName= "titolonome\">";
        String strToFindEmail= "Pec*</th><td>";
        String strToFindAddress= "Studio</th><td>";
        String strToFindPhone= "Telefono fisso</th><td>+39.";
        String name= ""; 
        String lastname= "";
        String email= "";
        String address= "";
        String phone= "";
        String strUser= "";
        int indexStart= 0;
        int indexEnd= 0;
        for(int i=0; i<this.arrayParams.length; i++){
            nextUserPage(this.arrayParams[i]);
            indexStart= this.htmlUserPageStr.indexOf(strToFindName);
            if(indexStart>0){
                indexEnd= this.htmlUserPageStr.indexOf("<", indexStart+strToFindName.length());
                name= this.htmlUserPageStr.substring(indexStart+strToFindName.length(), indexEnd);
                name= name.trim();
                lastname= name.substring(name.indexOf(" ")+1, name.length());
                name= name.replaceAll(" " + lastname, "");
            }else{
                name="";
                lastname="";
            }
            indexStart= this.htmlUserPageStr.indexOf(strToFindEmail);
            if(indexStart>0){
                indexEnd= this.htmlUserPageStr.indexOf("<", indexStart+strToFindEmail.length());
                email= this.htmlUserPageStr.substring(indexStart+strToFindEmail.length(), indexEnd);
            }else{
                email="";
            }
            indexStart= this.htmlUserPageStr.indexOf(strToFindAddress);
            if(indexStart>0){
                indexEnd= this.htmlUserPageStr.indexOf("-", indexStart+strToFindAddress.length());
                address= this.htmlUserPageStr.substring(indexStart+strToFindAddress.length(), indexEnd);
                address= address.replaceAll(",", "");
                address= address.trim();
            }else{
                address="";
            }
            indexStart= this.htmlUserPageStr.indexOf(strToFindPhone);
            if(indexStart>0){
                indexEnd= this.htmlUserPageStr.indexOf("<", indexStart+strToFindPhone.length());
                phone= this.htmlUserPageStr.substring(indexStart+strToFindPhone.length(), indexEnd);
            }else{
                phone="";
            }
            
            if(name.length()!=0 && lastname.length()!=0 && email.length()!=0){
                strUser= "," + name + "," + lastname + ",Bus. consultant," + email + ",Roma," + address + "," + phone + ","; 
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
