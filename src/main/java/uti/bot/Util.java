package uti.bot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Util {
    /**saves a JSONArray to a file
     * @param arr the json array to save
     * @param file the name of the file to save to
     */
    public static void saveJSONArray(JSONArray arr, String file){
        //save the file
        try {
            OutputStream out;
            //automatically apply gzip compression if specified at the end of the file path
            if(file.endsWith(".gz")){
                out = new GZIPOutputStream(new FileOutputStream(file));
            }else{
                out = new FileOutputStream(file);
            }
            saveJSONArray(arr,out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    /**saves a JSONArray to a file
     * @param arr the json array to save
     * @param out the output stream to write the file to. NOTE: the stream will NOT be closed
     */
    public static void saveJSONArray(JSONArray arr, OutputStream out){
        //save the file
        try {
            Writer wr = new PrintWriter(out);
            arr.write(wr,2,1);
            wr.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    /**since there is not buit in method to load a json array from a file and the process requires some effort, this method creates a json array from  json file
     * @param filePath the path to the json file
     * @return json array created from the file
     */
    public static JSONArray loadJSONArray(String filePath) throws IOException{
        InputStream in;
        //if the json file is compressed as gzip then automatically decompress it
        if(filePath.endsWith(".gz")){
            in = new GZIPInputStream(new FileInputStream(filePath));
        }else{
            in = new FileInputStream(filePath);
        }

        return loadJSONArray(in);

    }

    public static JSONArray loadJSONArray(InputStream in) throws IOException{
        StringBuilder rawContent = new StringBuilder();
        int bytesRead;
        byte[] buffer = new byte[1024];
        while((bytesRead = in.read(buffer)) != -1) {
            rawContent.append(new String(buffer, 0, bytesRead));
        }

        return new JSONArray(rawContent.toString());

    }

    /**saves a JSONObject to a file
     * @param obj the json array to save
     * @param file the name of the file to save to
     */
    public static void saveJSONObject(JSONObject obj, String file){
        //save the file
        try {
            OutputStream out;
            //automatically apply gzip compression if specified at the end of the file path
            if(file.endsWith(".gz")){
                out = new GZIPOutputStream(new FileOutputStream(file));
            }else{
                out = new FileOutputStream(file);
            }
            saveJSONObject(obj,out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    /**saves a JSONObject to a file
     * @param obj the json array to save
     * @param out the output stream to write the file to. NOTE: the stream will NOT be closed
     */
    public static void saveJSONObject(JSONObject obj, OutputStream out){
        //save the file
        try {
            Writer wr = new PrintWriter(out);
            obj.write(wr,2,1);
            wr.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    /**since there is not buit in method to load a json array from a file and the process requires some effort, this method creates a json array from  json file
     * @param filePath the path to the json file
     * @return json array created from the file
     */
    public static JSONObject loadJSONObject(String filePath) throws IOException{
        InputStream in;
        //if the json file is compressed as gzip then automatically decompress it
        if(filePath.endsWith(".gz")){
            in = new GZIPInputStream(new FileInputStream(filePath));
        }else{
            in = new FileInputStream(filePath);
        }

        return loadJSONObject(in);

    }

    public static JSONObject loadJSONObject(InputStream in) throws IOException{
        StringBuilder rawContent = new StringBuilder();
        int bytesRead;
        byte[] buffer = new byte[1024];
        while((bytesRead = in.read(buffer)) != -1) {
            rawContent.append(new String(buffer, 0, bytesRead));
        }

        return new JSONObject(rawContent.toString());

    }

    public static boolean isValidName(String name){
        String allowed = "^([0-9a-zA-Z ']+)$";
        if(name.matches(allowed)){
            //PRN, AUX, NUL, COM1-9, LPT1-9
            return !name.equalsIgnoreCase("CON") && !name.equalsIgnoreCase("PRN") && !name.equalsIgnoreCase("AUX") &&
                    !name.equalsIgnoreCase("NUL") && !name.equalsIgnoreCase("NULL") &&
                    !name.equalsIgnoreCase("COM1") && !name.equalsIgnoreCase("COM2") && !name.equalsIgnoreCase("COM3") && !name.equalsIgnoreCase("COM4") && !name.equalsIgnoreCase("COM5") && !name.equalsIgnoreCase("COM6") && !name.equalsIgnoreCase("COM7") && !name.equalsIgnoreCase("COM8") && !name.equalsIgnoreCase("COM9") &&
                    !name.equalsIgnoreCase("LPT1") && !name.equalsIgnoreCase("LPT2") && !name.equalsIgnoreCase("LPT3") && !name.equalsIgnoreCase("LPT4") && !name.equalsIgnoreCase("LPT5") && !name.equalsIgnoreCase("LPT6") && !name.equalsIgnoreCase("LPT7") && !name.equalsIgnoreCase("LPT8") && !name.equalsIgnoreCase("LPT9");

        }
        return false;
    }
}
