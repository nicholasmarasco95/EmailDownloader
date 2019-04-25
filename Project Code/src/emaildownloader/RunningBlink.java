/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package emaildownloader;

import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;

/**
 *
 * @author nicho
 */
public class RunningBlink implements Runnable{
    
    private JLabel labelRunning;
    private boolean running;
    private Settings settings;
    
    public RunningBlink(JLabel labelRunning){
        this.labelRunning= labelRunning;
        this.running= true;
        this.settings= new Settings();
    }

    @Override
    public void run() {
        this.labelRunning.setText("RUNNING");
        while(running){
            try {
                this.labelRunning.setForeground(Color.red);
                Thread.sleep(1000);
                this.labelRunning.setForeground(Color.black);
                Thread.sleep(1000);
                if(settings.getBoolValue("emailStopped")){
                    this.labelRunning.setText("STANDBY");
                    running= false;
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(RunningBlink.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
}
