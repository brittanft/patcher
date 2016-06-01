package nl.xupwup.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *
 * @author Rick Hendricksen
 */
public class ShaderProgram {
    public int program = 0;
    public int vertShader, fragShader;
    
    /**
     * Load a shader. If you only need a vertex shader, leave the argument for 'frag' empty. (empty string) The same holds the other way around.
     * @param vert  The vertex shader
     * @param frag  The fragment shader
     */
    public ShaderProgram(String vert, String frag){
    }
    
    
    public int getUniformLocation(String name){
        return 0;
    }
    
    public static int genShader(String code, boolean type){
        return 0;
    }
    
    public void enable(){
    }
    public void disable(){
    }
    
    public static ShaderProgram getFromStream(InputStream fragStream, InputStream vertStream) throws IOException{
        StringBuilder frag = new StringBuilder();
        try(BufferedReader r = new BufferedReader(
                new InputStreamReader(fragStream))){
            
            String s;
            while((s = r.readLine()) != null){
                frag.append(s).append("\n");
            }
        }
        StringBuilder vert = new StringBuilder();
        try(BufferedReader r = new BufferedReader(
                new InputStreamReader(vertStream))){
            
            String s;
            while((s = r.readLine()) != null){
                vert.append(s).append("\n");
            }
        }
        return new ShaderProgram(vert.toString(), frag.toString());
    }
}
