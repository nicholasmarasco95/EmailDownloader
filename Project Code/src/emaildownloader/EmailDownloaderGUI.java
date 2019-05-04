/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package emaildownloader;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Container;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import javafx.application.Application;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.jimmc.jshortcut.JShellLink;

/**
 *
 * @author nicho
 */
public class EmailDownloaderGUI extends javax.swing.JFrame {

    /**
     * Creates new form EmailDownloaderGUI
     */
    private Settings settings;
    private static boolean visible;
    private boolean isWindows;
    
    public EmailDownloaderGUI() {
        this.setTitle("Email Downloader");
        this.settings= new Settings();
        if(settings.getBoolValue("startMin")) visible=false;
        else visible= true;
        initComponents();
        this.settings.SaveSetting("boolean", "emailStopped", "false");
        this.btnStop.setEnabled(false);
        this.isWindows= checkWindows();
        SystemTrayMethod();
        refresh();
        if(settings.getBoolValue("autostartEmail")) autoStart();
        this.labelRunningStatus.setText("STANDBY");
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }
    
    private void refresh(){
        if(settings.getStringValue("destDir").length()==0){
            labelDest.setText("ERROR");
            labelDest.setForeground(Color.red);
        }else{
            labelDest.setText("OK");
            labelDest.setForeground(Color.blue);
        }
        if(settings.getStringValue("driverPath").length()==0){
            labelDriver.setText("ERROR");
            labelDriver.setForeground(Color.red);
        }else{
            labelDriver.setText("OK");
            labelDriver.setForeground(Color.blue);
        }
        if(settings.getBoolValue("headlessChrome")) checkBoxHeadless.setSelected(true);
        else checkBoxHeadless.setSelected(false);
        if(!this.isWindows){
            checkAutoStart.setEnabled(false);
            settings.SaveSetting("boolean", "autostartEmail", "false");
            checkAutoStart.setSelected(false);
        }else{
            checkAutoStart.setEnabled(true);
            if(settings.getBoolValue("autostartEmail")){
            if(!settings.getStringValue("srvLastChoice").equals("aar") && !settings.getStringValue("srvLastChoice").equals("pg")){
                JOptionPane.showMessageDialog(this, "Autostart not available for this service\nLast choice was: " + settings.getStringValue("srvLastChoice"), "Error", JOptionPane.ERROR_MESSAGE);
                settings.SaveSetting("boolean", "autostartEmail", "false");
                refresh();
            }else{
                checkAutoStart.setSelected(true);
                shortcutAutostartAdd();
            }
            }
            else{
                checkAutoStart.setSelected(false);
                shortcutAutostartRemove();
            }
        }
        if(settings.getBoolValue("startMin")) checkStrMin.setSelected(true);
        else checkStrMin.setSelected(false);
    }
    
    private void autoStart(){
        boolean checkStart= false;
        String choice= settings.getStringValue("srvLastChoice");
        if(choice==null || choice.length()<=1){
            System.err.println("No srvLastChoice \nsrvLastChoice set to aar \nexit 0");
            settings.SaveSetting("string", "srvLastChoice", "aar");
            System.exit(0);
        }
        if(choice.equals("ocr")){
            OrdineCommercialistiRoma ocr= new OrdineCommercialistiRoma(txtAreaEmail, labelEmailsNumber);
            Thread thrOcr= new Thread(ocr);
            thrOcr.start();
            checkStart= true;
        }
        else if(choice.equals("cnn")){
            ConsiglioNazionaleNotai cnn= new ConsiglioNazionaleNotai(txtAreaEmail, labelEmailsNumber);
            Thread cnnOcr= new Thread(cnn);
            cnnOcr.start();
            checkStart= true;
        }
        else if(choice.equals("pg")){ 
            PagineGialle pg= new PagineGialle(txtAreaEmail, labelEmailsNumber);
            Thread pgOcr= new Thread(pg);
            pgOcr.start();
            checkStart= true;
        }
        else if(choice.equals("aar")){
            AlboAvvocatiRoma aar= new AlboAvvocatiRoma(txtAreaEmail, labelEmailsNumber);
            Thread aarThr= new Thread(aar);
            aarThr.start();
            checkStart= true;
        }
        if(checkStart){
            this.labelEmailsNumber.setForeground(Color.blue);
            RunningBlink rb= new RunningBlink(this.labelRunningStatus);
            Thread thrRb= new Thread(rb);
            thrRb.start();
            this.btnStop.setEnabled(true);
            this.btnStart.setEnabled(false);
        }
    }
    
    private boolean checkWindows(){
        return (System.getProperty("os.name").toLowerCase().contains("windows"));
    }
    
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
    
    private void shortcutAutostartRemove(){
        String jarName= "EmailDownloader.jar.lnk";
        String windowsUser= System.getProperty("user.name");
        String startupFolder= "C:\\Users\\" + windowsUser + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup" + "\\" + jarName;
        File toDelFile= new File(startupFolder);
        if(toDelFile.exists()){
            toDelFile.delete();
        }
    }
    
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

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton2 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        comboBox = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtAreaEmail = new javax.swing.JTextArea();
        labelRunningStatus = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        labelEmailsNumber = new javax.swing.JLabel();
        btnStart = new javax.swing.JButton();
        btnStop = new javax.swing.JButton();
        btnDestFolder = new javax.swing.JButton();
        btnDriver = new javax.swing.JButton();
        labelDest = new javax.swing.JLabel();
        labelDriver = new javax.swing.JLabel();
        btnMerge = new javax.swing.JButton();
        btnExtractor = new javax.swing.JButton();
        checkBoxHeadless = new javax.swing.JCheckBox();
        checkAutoStart = new javax.swing.JCheckBox();
        checkStrMin = new javax.swing.JCheckBox();
        btnExit = new javax.swing.JButton();

        jButton2.setFont(new java.awt.Font("Microsoft New Tai Lue", 1, 18)); // NOI18N
        jButton2.setForeground(new java.awt.Color(0, 204, 0));
        jButton2.setText("START");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        jLabel1.setFont(new java.awt.Font("Microsoft Tai Le", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 0, 255));
        jLabel1.setText("Email Downloader");

        jLabel2.setFont(new java.awt.Font("Microsoft Tai Le", 1, 18)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(51, 51, 51));
        jLabel2.setText("Select Service");

        comboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { " ", "Ordine Commercialisti Roma", "Consiglio Nazionale Notai", "Pagine Gialle", "Albo Avvocati Roma" }));
        comboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboBoxActionPerformed(evt);
            }
        });

        txtAreaEmail.setColumns(20);
        txtAreaEmail.setRows(5);
        jScrollPane1.setViewportView(txtAreaEmail);

        labelRunningStatus.setFont(new java.awt.Font("Microsoft Tai Le", 1, 18)); // NOI18N
        labelRunningStatus.setText("RUNNING");

        jLabel4.setFont(new java.awt.Font("Microsoft Tai Le", 1, 18)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(51, 51, 51));
        jLabel4.setText("Emails");

        labelEmailsNumber.setFont(new java.awt.Font("Microsoft Tai Le", 1, 18)); // NOI18N
        labelEmailsNumber.setForeground(new java.awt.Color(51, 51, 51));
        labelEmailsNumber.setText("000");

        btnStart.setFont(new java.awt.Font("Microsoft New Tai Lue", 1, 18)); // NOI18N
        btnStart.setForeground(new java.awt.Color(0, 204, 0));
        btnStart.setText("START");
        btnStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartActionPerformed(evt);
            }
        });

        btnStop.setFont(new java.awt.Font("Microsoft New Tai Lue", 1, 18)); // NOI18N
        btnStop.setForeground(new java.awt.Color(255, 0, 0));
        btnStop.setText("STOP");
        btnStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopActionPerformed(evt);
            }
        });

        btnDestFolder.setFont(new java.awt.Font("Microsoft New Tai Lue", 1, 14)); // NOI18N
        btnDestFolder.setText("Dest Folder");
        btnDestFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDestFolderActionPerformed(evt);
            }
        });

        btnDriver.setFont(new java.awt.Font("Microsoft Tai Le", 1, 14)); // NOI18N
        btnDriver.setText("Driver Folder");
        btnDriver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDriverActionPerformed(evt);
            }
        });

        labelDest.setFont(new java.awt.Font("Microsoft Tai Le", 1, 18)); // NOI18N
        labelDest.setText("OK");

        labelDriver.setFont(new java.awt.Font("Microsoft Tai Le", 1, 18)); // NOI18N
        labelDriver.setText("OK");

        btnMerge.setFont(new java.awt.Font("Microsoft New Tai Lue", 1, 18)); // NOI18N
        btnMerge.setForeground(new java.awt.Color(0, 0, 204));
        btnMerge.setText("MERGE FILES");
        btnMerge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMergeActionPerformed(evt);
            }
        });

        btnExtractor.setFont(new java.awt.Font("Microsoft New Tai Lue", 1, 18)); // NOI18N
        btnExtractor.setForeground(new java.awt.Color(0, 0, 204));
        btnExtractor.setText("Email Extractor");
        btnExtractor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExtractorActionPerformed(evt);
            }
        });

        checkBoxHeadless.setFont(new java.awt.Font("Microsoft Tai Le", 1, 18)); // NOI18N
        checkBoxHeadless.setText("Headless Chrome");
        checkBoxHeadless.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkBoxHeadlessActionPerformed(evt);
            }
        });

        checkAutoStart.setFont(new java.awt.Font("Microsoft Tai Le", 1, 18)); // NOI18N
        checkAutoStart.setText("Auto Start");
        checkAutoStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkAutoStartActionPerformed(evt);
            }
        });

        checkStrMin.setFont(new java.awt.Font("Microsoft Tai Le", 1, 18)); // NOI18N
        checkStrMin.setText("Start minimized");
        checkStrMin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkStrMinActionPerformed(evt);
            }
        });

        btnExit.setFont(new java.awt.Font("Microsoft Tai Le", 1, 14)); // NOI18N
        btnExit.setText("Exit");
        btnExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExitActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(btnMerge)
                                .addGap(18, 18, 18)
                                .addComponent(btnExtractor)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnStop)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnStart))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 539, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addGap(18, 18, 18)
                                .addComponent(comboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(labelRunningStatus)
                                .addGap(35, 35, 35)
                                .addComponent(jLabel4)
                                .addGap(18, 18, 18)
                                .addComponent(labelEmailsNumber))
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(btnDestFolder)
                                        .addGap(29, 29, 29)
                                        .addComponent(labelDest))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(btnDriver)
                                        .addGap(18, 18, 18)
                                        .addComponent(labelDriver)))
                                .addGap(41, 41, 41))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(btnExit)
                                .addContainerGap())))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(checkBoxHeadless)
                        .addGap(32, 32, 32)
                        .addComponent(checkAutoStart)
                        .addGap(18, 18, 18)
                        .addComponent(checkStrMin)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel1)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(comboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(labelRunningStatus)
                            .addComponent(jLabel4)
                            .addComponent(labelEmailsNumber)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnExit)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnDestFolder)
                            .addComponent(labelDest))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnDriver)
                            .addComponent(labelDriver))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkBoxHeadless)
                    .addComponent(checkAutoStart)
                    .addComponent(checkStrMin))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnStart)
                    .addComponent(btnStop)
                    .addComponent(btnMerge)
                    .addComponent(btnExtractor))
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartActionPerformed
        String service= (String) comboBox.getSelectedItem();
        if(service.length()<=2){
            JOptionPane.showMessageDialog(null, "Please select a service");
            return;
        }
        if(settings.getStringValue("destDir").length()==0){
            JOptionPane.showMessageDialog(null, "Please select dest folder");
            return;
        }
        if(settings.getStringValue("driverPath").length()==0){
            JOptionPane.showMessageDialog(null, "Please select driver path");
            return;
        }
        this.settings.SaveSetting("boolean", "emailStopped", "false");
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
        if(service.equals("Consiglio Nazionale Notai")){
            settings.SaveSetting("string", "srvLastChoice", "cnn");
            ConsiglioNazionaleNotai cnn= new ConsiglioNazionaleNotai(txtAreaEmail, labelEmailsNumber);
            Thread cnnOcr= new Thread(cnn);
            cnnOcr.start();
            this.labelEmailsNumber.setForeground(Color.blue);
            RunningBlink rb= new RunningBlink(this.labelRunningStatus);
            Thread thrRb= new Thread(rb);
            thrRb.start();
            this.btnStop.setEnabled(true);
            this.btnStart.setEnabled(false);
        }
        if(service.equals("Pagine Gialle")){
            settings.SaveSetting("string", "srvLastChoice", "pg");
            PagineGialle pg= new PagineGialle(txtAreaEmail, labelEmailsNumber);
            Thread pgOcr= new Thread(pg);
            pgOcr.start();
            this.labelEmailsNumber.setForeground(Color.blue);
            RunningBlink rb= new RunningBlink(this.labelRunningStatus);
            Thread thrRb= new Thread(rb);
            thrRb.start();
            this.btnStop.setEnabled(true);
            this.btnStart.setEnabled(false);
        }
        if(service.equals("Albo Avvocati Roma")){
            settings.SaveSetting("string", "srvLastChoice", "aar");
            AlboAvvocatiRoma aar= new AlboAvvocatiRoma(txtAreaEmail, labelEmailsNumber);
            Thread aarThr= new Thread(aar);
            aarThr.start();
            this.labelEmailsNumber.setForeground(Color.blue);
            RunningBlink rb= new RunningBlink(this.labelRunningStatus);
            Thread thrRb= new Thread(rb);
            thrRb.start();
            this.btnStop.setEnabled(true);
            this.btnStart.setEnabled(false);
        }
    }//GEN-LAST:event_btnStartActionPerformed

    private void btnDestFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDestFolderActionPerformed
        FolderSelect fs= new FolderSelect(this, true, "dest");
        fs.setVisible(true);
        refresh();
    }//GEN-LAST:event_btnDestFolderActionPerformed

    private void btnDriverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDriverActionPerformed
        FolderSelect fs= new FolderSelect(this, true, "driver");
        fs.setVisible(true);
        refresh();
    }//GEN-LAST:event_btnDriverActionPerformed

    private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
        JOptionPane.showMessageDialog(null, "Wait for the confirm");
        this.settings.SaveSetting("boolean", "emailStopped", "true");
        this.btnStop.setEnabled(false);
        this.btnStart.setEnabled(true);
    }//GEN-LAST:event_btnStopActionPerformed

    private void btnMergeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMergeActionPerformed
        MergeFilesGUI mfg= new MergeFilesGUI(this, false);
        mfg.setVisible(true);
    }//GEN-LAST:event_btnMergeActionPerformed

    private void btnExtractorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExtractorActionPerformed
        EmailExtractorGUI eeg= new EmailExtractorGUI(this, false);
        eeg.setVisible(true);
    }//GEN-LAST:event_btnExtractorActionPerformed

    private void checkBoxHeadlessActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkBoxHeadlessActionPerformed
        //feature implemented for some site only
        if(settings.getBoolValue("headlessChrome")){
            settings.SaveSetting("boolean", "headlessChrome", "false");
        }
        else{
            settings.SaveSetting("boolean", "headlessChrome", "true");
        }
        refresh();
    }//GEN-LAST:event_checkBoxHeadlessActionPerformed

    private void checkAutoStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkAutoStartActionPerformed
        if(settings.getBoolValue("autostartEmail"))settings.SaveSetting("boolean", "autostartEmail", "false");
        else{
            if(!settings.getStringValue("srvLastChoice").equals("aar") && !settings.getStringValue("srvLastChoice").equals("pg")){
                JOptionPane.showMessageDialog(this, "Autostart not available for this service\nLast choice was: " + settings.getStringValue("srvLastChoice"), "Error", JOptionPane.ERROR_MESSAGE);
                settings.SaveSetting("boolean", "autostartEmail", "false");
            }else{
                JOptionPane.showMessageDialog(this, "Last choice was " + settings.getStringValue("srvLastChoice"), "Info", JOptionPane.INFORMATION_MESSAGE);
                settings.SaveSetting("boolean", "autostartEmail", "true");
                checkAutoStart.setSelected(true);
            }
        }
        refresh();
    }//GEN-LAST:event_checkAutoStartActionPerformed

    private void checkStrMinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkStrMinActionPerformed
        if(settings.getBoolValue("startMin")) settings.SaveSetting("boolean", "startMin", "false");
        else settings.SaveSetting("boolean", "startMin", "true");
        refresh();
    }//GEN-LAST:event_checkStrMinActionPerformed

    private void comboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboBoxActionPerformed
        String service= (String) comboBox.getSelectedItem();
        if(service!=null && service.length()>2){
            if(service.equals("Ordine Commercialisti Roma")) settings.SaveSetting("string", "srvLastChoice", "ocr");
            if(service.equals("Consiglio Nazionale Notai")) settings.SaveSetting("string", "srvLastChoice", "cnn");
            if(service.equals("Pagine Gialle")) settings.SaveSetting("string", "srvLastChoice", "pg");
            if(service.equals("Albo Avvocati Roma")) settings.SaveSetting("string", "srvLastChoice", "aar");
        }else settings.SaveSetting("string", "srvLastChoice", "");
        refresh();
    }//GEN-LAST:event_comboBoxActionPerformed

    private void btnExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExitActionPerformed
        System.exit(0);
    }//GEN-LAST:event_btnExitActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(EmailDownloaderGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(EmailDownloaderGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(EmailDownloaderGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(EmailDownloaderGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new EmailDownloaderGUI().setVisible(visible);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnDestFolder;
    private javax.swing.JButton btnDriver;
    private javax.swing.JButton btnExit;
    private javax.swing.JButton btnExtractor;
    private javax.swing.JButton btnMerge;
    private javax.swing.JButton btnStart;
    private javax.swing.JButton btnStop;
    private javax.swing.JCheckBox checkAutoStart;
    private javax.swing.JCheckBox checkBoxHeadless;
    private javax.swing.JCheckBox checkStrMin;
    private javax.swing.JComboBox<String> comboBox;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel labelDest;
    private javax.swing.JLabel labelDriver;
    private javax.swing.JLabel labelEmailsNumber;
    private javax.swing.JLabel labelRunningStatus;
    private javax.swing.JTextArea txtAreaEmail;
    // End of variables declaration//GEN-END:variables
}
