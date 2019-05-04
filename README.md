# Email Downloader
Email Downloader is software to get emails from web. It works with Chromium Driver. 
Some features were created to manage Mail Chimp service.

## FYI

This project was created for educational purpose only. Please don't use this software to commit any illegal actions.


# User Guide

This paragrapth explains how user can use Email Downloader by GUI.
![GitHub Logo](pictures/home.png)

**Close App**

To close application user must click on Exit button. X button is used to reduce app in System Tray.

**Dest Folder**

Here User can choose Destination Folder where a CSV file will be created. In that file there will be emails downloaded.

**Driver Folder**

User must specify where Chromium exe file is stored.

**Headless Chrome**

This feature allows Chromium to start in background, this means that no Chrome window will be show.

**Auto Start**

If enabled, Email Downloader will copy a shortcut to startup folder. This allows the software to automatically startup after Windows Login. This feature works in Windows environment only. The software checks the environment every time it starts, if it detects another OS, it will deactivate this function.

**Start minimized**

If enabled the software will start in System Tray.


### Services

User can choose between 4 services:
* Ordine Commercialisti di Roma
* Consiglio Nazionale Notai
* Pagine Gialle
* Albo Avvocati di Roma


## Merge Files

This feature can add or remove email from a file.

![GitHub Logo](pictures/merge_files.png)

**Complete List**

This is the file that will be affected by changes (add/remove)

**To Merge File**

File that will be used to adding or remove emails from Complete List.

**Files Type**

![GitHub Logo](pictures/file_type.png)

User must select type to distinguish a file that was exported by Mail Chimp Services and a file that was generated by Email Downloader.

**Remove/Add**

This allows user to decide to Add or Remove emails. In both cases, the software will read "To Merge List" file and will add/remove these emails in "Complete List" file.

**Clean Duplicate**

This feature will check and delete duplicates from Complete List file.


## Email Extractor

This feature was created to extract emails to send to manage Mail Chimp free subscription.

![GitHub Logo](pictures/email_extractor.png)

"Unsubscribed" and "Sent" are not required.
User can filter email by "City", "Group".
**Email Number** is required.


# Code Guide
In this guide will be explained some method used.

## Start

When start button is pressed, selected service is checked. A new thread will be created to run service routine. **RunningBlink** thread will make a label blink in Home folder.

**Start Service Routine Example**

```java
if(service.equals("Ordine Commercialisti Roma")){
            settings.SaveSetting("string", "srvLastChoice", "ocr");
            OrdineCommercialistiRoma ocr= new OrdineCommercialistiRoma(txtAreaEmail, labelEmailsNumber);
            Thread thrOcr= new Thread(ocr);
            thrOcr.start();
            this.labelEmailsNumber.setForeground(Color.blue);
            RunningBlink rb= new RunningBlink(this.labelRunningStatus);
            Thread thrRb= new Thread(rb);
            thrRb.start();
            this.btnStop.setEnabled(true);
            this.btnStart.setEnabled(false);
        }
```

## Autostart

To make Email Downloader start automatically after Windows Login, is necessary to create a link and copy it into startup folder. To do this JShellLink project was used.
**System.getProperty("user.name")** get Windows user name, this will be used to build startup folder path.

```java
private void shortcutAutostartAdd(){
        JShellLink link= new JShellLink();
        String jarName= "EmailDownloader.jar";
        String filePath= System.getProperty("user.dir")+"\\" + jarName;
        String windowsUser= System.getProperty("user.name");
        String startupFolder= "C:\\Users\\" + windowsUser + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";
        link.setFolder(startupFolder);
        link.setName(jarName);
        link.setPath(filePath);
        link.save();
    }
```

**Disable Autostart**

When Autostart is disabled, Email Downloader shortcut must be removed from startup folder.

```java
private void shortcutAutostartRemove(){
        String jarName= "EmailDownloader.jar.lnk";
        String windowsUser= System.getProperty("user.name");
        String startupFolder= "C:\\Users\\" + windowsUser + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup" + "\\" + jarName;
        File toDelFile= new File(startupFolder);
        if(toDelFile.exists()){
            toDelFile.delete();
        }
    }
```

## System tray

This metod hide Email Downloader in System Tray.

```java
private void SystemTrayMethod(){
        if(!SystemTray.isSupported()){
            System.out.println("System tray is not supported !!! ");
            return ;
        }
        SystemTray systemTray = SystemTray.getSystemTray();
        Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/hacker-icon-126x126.png"));
        PopupMenu trayPopupMenu = new PopupMenu();
        MenuItem action = new MenuItem("Open");
        action.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(true);
            }
        });     
        trayPopupMenu.add(action);
        TrayIcon trayIcon = new TrayIcon(image, "EM", trayPopupMenu);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                setVisible(true);
            }
        });
        try{
            systemTray.add(trayIcon);
        }catch(AWTException awtException){
            awtException.printStackTrace();
        }
    }
```

## Settings

With Settings class, Email Downloader can save information in Windows System Variables. This allows software to get info back when is restarted.

```java
public void SaveSetting(String type, String key, String value){
        if(type.toLowerCase().equals("string")){
            setting.put(key, value);
        }
        if(type.toLowerCase().equals("bool") || type.toLowerCase().equals("boolean")){
            boolean bool = Boolean.parseBoolean(value.toLowerCase());
            setting.putBoolean(key, bool);
        }
        if(type.toLowerCase().equals("int") || type.toLowerCase().equals("integer")){
            if(value!=null){
                Integer intValue= Integer.parseInt(value);
                setting.putInt(key, intValue);
            }
        }
        if(type.toLowerCase().equals("long")){
            if(value!=null){
                Long longValue= Long.parseLong(value);
                setting.putLong(key, longValue);
            }
        }
    }
```

In this method there are **get** methods to read saved info.


## Service Routine Example
This will illustrate an example of how a Service Routine works.
### Pagine Gialle

When Pagine Gialle Thread is created, the first method to be executed is **start**. It will check if the Thread was created automatically by Autostart or by Start click. If was generated by Autostart, Thread will retrieve information by System Variables and will Read Last Page in CheckFile. If Start button was clicked, a series of JOptionPane will ask to user info. Once information were retrieved, Headless feature is checked. Finally, routine is started with a "while" cycle. After every method, **checkStop()** will check if Stop Button was pressed.

```java
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
```
### Parse Albo Names
This method will parse results link that will populate an array. This links will be used to browser results pages.

```java
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
```

### Parse Users Pages
Once current search page's links were retrieved, this method will iterate the array to visit results pages. If page contains an email address, the software will parse other information (e.g. name, address, city, phone), then a csv string will be created, it will be added to a list ("listUser"), and the home Text Area will be updated with new results. Finally, the method will navigate back to the search page.

```java
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
```

### Write File
After parsing users pages, the software will write on csv file the results written on "listUser", then it will overwrite the list to clear it.

```java
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
```

### Write Check File
Once the result's file was updated, checkfile will be updated. It will update the number of email retrieved in this session and the number of current page. This will be read in future sessions to continue retrieving new emails without restarting from beginning.

```java
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
```

### Next Search Page
This method will navigate to the next search page. Then a new cycle will be started.

```java
private void nextSearchPage(){
        this.currentPageNumber+=1;
        this.searchPageUrl= this.searchPageOriginalUrl+ keywordSearch + "/Roma" + "/p-" + currentPageNumber;
        driver.get(searchPageUrl);
        String handle= driver.getWindowHandle();
        driver.switchTo().window(handle);
    }
```
