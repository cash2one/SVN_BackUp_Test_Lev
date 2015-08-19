package event;

import java.io.InputStream;

import net.sourceforge.htmlunit.flash.Flash;

import org.junit.Test;

public class Example1 {

    @Test
    public void test() throws Exception {
        //See
        //MovieClip(this.root).gotoAndStop(5);
        //http://throk.net/blog/223/how-access-main-timeline-flash-actionscript-3

        final InputStream in = getClass().getResourceAsStream("as3keyboard.swf");
        new Flash(in);
    }

}
