package lolpatcher;


import java.io.IOException;
import nl.xupwup.Util.FrameBuffer;
import nl.xupwup.Util.ShaderProgram;
import nl.xupwup.Util.Texture;

/**
 *
 * @author Rick
 */
public class Flow {
    
    private FrameBuffer framebuffer1;
    private FrameBuffer framebuffer2;
    private boolean front = true;
    private final ShaderProgram sp, sp2, logodump;
    private final FrameBuffer screenBuffer;
    private Texture title;
    private boolean firstFrame = true;
    
    int width = 600, height = 378;
    
    public Flow(FrameBuffer screenBuffer) throws IOException{
        this.screenBuffer = screenBuffer;
        /*framebuffer1 = new FrameBuffer(width, height, GL_TEXTURE_RECTANGLE, GL30.GL_RG16F, GL_RG);
        framebuffer2 = new FrameBuffer(width, height, GL_TEXTURE_RECTANGLE, GL30.GL_RG16F, GL_RG);
        GL11.glBindTexture(GL_TEXTURE_RECTANGLE, framebuffer1.framebuffertex);
        glTexParameteri(GL_TEXTURE_RECTANGLE, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_RECTANGLE, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);
        // border color defaults to 0,0,0,0, which is fine
        GL11.glBindTexture(GL_TEXTURE_RECTANGLE, framebuffer2.framebuffertex);
        glTexParameteri(GL_TEXTURE_RECTANGLE, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_RECTANGLE, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);
        // border color defaults to 0,0,0,0, which is fine
        GL11.glBindTexture(GL_TEXTURE_RECTANGLE, 0);*/
        sp = ShaderProgram.getFromStream(this.getClass().getResourceAsStream("resources/flow.frag"),
                this.getClass().getResourceAsStream("resources/flow.vert"));
        sp2 = ShaderProgram.getFromStream(this.getClass().getResourceAsStream("resources/texture.frag"),
                this.getClass().getResourceAsStream("resources/texture.vert"));
        logodump = ShaderProgram.getFromStream(this.getClass().getResourceAsStream("resources/logodump.frag"),
                this.getClass().getResourceAsStream("resources/logodump.vert"));
        title = Texture.fromStream(this.getClass().getResourceAsStream("resources/title.png"));
    }
    
    public int draw(int x, int y){
        if(framebuffer1 == null){
            return 378;
        }
        front = !front;
        
        FrameBuffer frontBuffer = front ? framebuffer1 : framebuffer2;
        FrameBuffer backBuffer = front ? framebuffer2 : framebuffer1;
        frontBuffer.bind();
  
        
        sp.enable();
        /*GL11.glDisable(GL_BLEND);
        GL20.glUniform2f(sp.getUniformLocation("cursor"), x, height - y);
        GL11.glBindTexture(GL_TEXTURE_2D, 0);
        GL11.glBindTexture(GL_TEXTURE_RECTANGLE, backBuffer.framebuffertex);

        glBegin(GL_TRIANGLES);
            glVertex2f(-1, -1);
            glVertex2f(-1, 1);
            glVertex2f(1, 1);
            
            glVertex2f(-1, -1);
            glVertex2f(1, -1);
            glVertex2f(1, 1);
        GL11.glEnd();
        
        sp.disable();
        if(firstFrame){
            firstFrame = false;
            logodump.enable();
            GL11.glBindTexture(GL_TEXTURE_RECTANGLE, 0);
            title.bind();
            glBegin(GL_TRIANGLES);
                glVertex2f(-1, -1);
                glVertex2f(-1, 1);
                glVertex2f(1, 1);

                glVertex2f(-1, -1);
                glVertex2f(1, -1);
                glVertex2f(1, 1);
            GL11.glEnd();
            title.unbind();
            logodump.disable();
            title.destroy();
            title = null;
        }
        
        screenBuffer.bind();
        
        sp2.enable();
        GL11.glBindTexture(GL_TEXTURE_RECTANGLE, backBuffer.framebuffertex);
        glBegin(GL_QUADS);
            glTexCoord2f(0, height);
            glVertex2f(0, 0);
            glTexCoord2f(0, 0);
            glVertex2f(0, 378);
            glTexCoord2f(width, 0);
            glVertex2f(600, 378);
            glTexCoord2f(width, height);
            glVertex2f(600, 0);
        glEnd();
        sp2.disable();
        
        GL11.glBindTexture(GL_TEXTURE_RECTANGLE, 0);
        
        GL11.glEnable(GL_BLEND);*/
        
        return 378;
    }
    
}
