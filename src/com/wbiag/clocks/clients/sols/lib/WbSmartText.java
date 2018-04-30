package com.wbiag.clocks.clients.sols.lib;

import java.util.Hashtable;
import org.apache.log4j.Logger;

/**
 * Helper class used by the virtual clock.
 * This class provides support for text localization, colors and sounds.
 * @author Rafal Bujko
 * @author Octavian Tarcea
 */ 
public class WbSmartText {
    private static Logger logger = Logger.getLogger(WbSmartText.class);
    private Hashtable text = new Hashtable();
    private static String defaultLanguage = "en";
    private static String currentLanguage = defaultLanguage;
    private String color = null;
    private int numberOfBeeps = 0;

    /**
     * Constructs a WbSmartText object and sets up its default text. 
     * @param text The default text.
     */
    public WbSmartText(String text) {
        setText(text);
    }

    /**
     * Constructs a WbSmartText object and sets up its default text. 
     */
    public WbSmartText() {
    }

    /**
     * Returns the text in the current language.
     * @return The text in the current language.
     * @see #setText(String, String)
     * @see #setText(String)
     * @see #getText(String)
     */
    public String getText() {
        return getText(currentLanguage);
    }

    /**
     * Gets the text for a particular language.
     * @param language The language for which the text will be retrieved.
     * @return The appropriate text based on the language.
     * @see #setText(String, String)
     * @see #getText()
     * @see #setText(String)
     */
    public String getText(String language) {
        if(text == null){
            return "";
        }else{
            String s = (language != null)?(String)text.get(language):null;
            if(s == null){
                s = (String)text.get(defaultLanguage);
                if(s == null){
                    return "";
                }else{
                    return s;
                }
            }else{
                return s;
            }
        }
    }

    /** 
     * @return the real content for current language
     */
    public Object getContent(){
        if(text == null){
            return null;
        }else{
            return text.get(currentLanguage);
        }        
    }

    /**
     * Sets the text in the current language.
     * @see #setText(String, String)
     * @see #getText()
     * @see #getText(String)
     * @param text The text in the current language.
     */
    public void setText(String text) {
        setText(currentLanguage, text);
    }

    /**
     * Sets the text for the given language.
     * @param language The language for which the text will be set.
     * @param text The text in the given language.
     * @see #setText(String)
     * @see #getText()
     * @see #getText(String)
     */
    public void setText(String language, String text) {
        if(text == null || language == null){
            return;
        }
        if(this.text == null){
            this.text = new Hashtable();
        }
        this.text.put(language, text);
    }

    /**
     * Sets the current languange.
     * @param aLanguage The current language. 
     * @see #getCurrentLanguage()
     */
    public static void setCurrentLanguage(String aLanguage) {
        currentLanguage = aLanguage;
    }

    /**
     * Returns the default language.
     * @return The default language .
     */
    public static String getDefaultLanguage() {
        return defaultLanguage;
    }

    /**
     * Returns the current language.
     * @return The current language.
     * @see #setCurrentLanguage(String)
     */
    public static String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * Returns the color associated with this text.
     * @return The current color associated with this text.
     * @see #setColor(String)
     */
    public String getColor() {
        if(color == null){
            return "black";
        }else{
            return color;
        }
    }

    /**
     * This method exists only for backward compatibility only. The getColor() method
     * used to return a java.awt.Color and it made sense to use have this method.
     * Now we have modified the getColor() method to return the string but we want to
     * preserve this method just in case some client might be using it.
     * Returns the color associated with this text.
     * @return The current color associated with this text.
     * @see #setColor(String)
     */
    public String getColorString() {
        return getColor();
    }

    
    /**
     * Converts the string into a java Color object and it sets it for this text.
     * @param color The new color of this object as a string.
     * @see #getColor()
     */
    public void setColor(String color) {
        this.color = color;
    }

    /**
     * Returns the number of beeps associated with this text.
     * @return The number of beeps associated with the text.
     * @see #setNumberOfBeeps(int)
     * @see #setNumberOfBeeps(String)
     */
    public int getNumberOfBeeps() {
        return numberOfBeeps;
    }

    /**
     * Sets the number of beeps associated with this text.
     * @param i The number of beeps.
     * @see #getNumberOfBeeps()
     * @see #setNumberOfBeeps(String)
     */
    public void setNumberOfBeeps(int i) {
        numberOfBeeps = i;
    }
    
    /**
     * Sets the number of beeps associated with this text.
     * @param i The number of beeps as a string.
     * @see #setNumberOfBeeps(int)
     * @see #getNumberOfBeeps()
     */
    public void setNumberOfBeeps(String i) {
        try{
            numberOfBeeps = Integer.parseInt(i);
        }catch(Exception e){
            logger.warn("Could not parse string: " + i + " defaulting to 0 beeps");
            numberOfBeeps = 0;
        }
    }

    /**
     * Same as getText(). Fail safe for all those cases that use toString() instead of getText()
     * @see #setText(String, String)
     * @see #getText()
     * @see #getText(String)
     * @return The text for the current language.
      */
    public String toString(){
        return getText();
    }
}