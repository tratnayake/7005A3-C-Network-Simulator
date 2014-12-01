/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Elton
 */
import java.io.*;
import java.util.*;

public class Config {
    
    private Properties prop;
    
    public Config() throws Exception
    {
    prop = new Properties();
    InputStream input = null;
 
            input = new FileInputStream("config.properties");
            prop.load(input);

            if (input != null) {
                    input.close(); 
            }
    }

    /**
     * @return the prop
     */
    public Properties getProp() {
        return prop;
    }

    /**
     * @param prop the prop to set
     */
    public void setProp(Properties prop) {
        this.prop = prop;
    }
}